import {upsertSubjectItem, upsertSceneItem} from '@/views/creation/utils/creationMessageChunks';

/**
 * SSE event → {type, payload}。messageChunkId 在事件顶层，由消费方按需取。
 */
export const normalizeCreationSseEvent = (event) => ({
  type: event.content?.type || '',
  payload: event.content?.data,
});

// 不同资产类型在消息上的定位规则：判断一条历史消息是否承载着该 messageChunkId 的资产
const ASSET_MATCH = {
  SUBJECT: (msg, messageChunkId) => msg.subjectList.some((s) => s.messageChunkId === messageChunkId),
  SCENE: (msg, messageChunkId) => msg.sceneList.some((s) => s.messageChunkId === messageChunkId),
  SCENE_SCRIPT: (msg, messageChunkId) => msg.blocks.some((b) => b.type === 'SCENE_SCRIPT' && b.messageChunkId === messageChunkId),
  ACTION_REQUIRED: (msg, messageChunkId) => msg.blocks.some((b) => b.type === 'ACTION_REQUIRED' && b.messageChunkId === messageChunkId),
};

export const createCreationSseEventApplier = ({
  appendBlock,
  flushTypewriter,
  enqueueTypewriterText,
  setTypewriterBlock,
  creationBus,
  scrollToBottom,
  handleChatFinished,
  handleSSEErrorEvent,
  messages,
  // AUDIO / SUBJECT / SCENE 生成完毕时回调，用来及时刷新顶部积分
  // （不能只等 COMPLETE：一次对话可能生成多个资产，每个都扣积分，等到结束才刷新中间过程显示陈旧）
  onAssetGenerated,
}) => {
  // 倒序找最近一条承载同 messageChunkId 资产的 AI 消息（用于 chat 重生成新版本时定位原条目）
  const findMessageByChunkId = (messageChunkId, assetType) => {
    if (!messageChunkId) return null;
    const matcher = ASSET_MATCH[assetType];
    for (let i = messages.value.length - 1; i >= 0; i -= 1) {
      const item = messages.value[i];
      if (item.senderType === 'USER') continue;
      if (matcher(item, messageChunkId)) return item;
    }
    return null;
  };

  // SUBJECT / SCENE 共用：找原 msg → upsert → 命中旧条目时广播 version-reset → 广播 received
  const applyAssetUpsert = ({assetType, listKey, receivedEvent, msg, messageChunkId, payload}) => {
    const targetMsg = findMessageByChunkId(messageChunkId, assetType) || msg;
    const upsert = assetType === 'SUBJECT' ? upsertSubjectItem : upsertSceneItem;
    const hasReplacedItem = !!messageChunkId && targetMsg[listKey].some((it) => it.messageChunkId === messageChunkId);
    const idx = upsert(targetMsg, {...payload, messageChunkId});
    if (!targetMsg.sessionId && msg.sessionId) targetMsg.sessionId = msg.sessionId;
    if (hasReplacedItem) {
      const nextItem = targetMsg[listKey][idx];
      creationBus.emit('edit-context:version-reset', {
        type: assetType,
        messageId: targetMsg.messageId,
        itemKey: nextItem.messageChunkId,
        activeVersion: nextItem.activeVersion,
      });
    }
    creationBus.emit(receivedEvent, {msgId: targetMsg.messageId});
  };

  // SCENE_SCRIPT / ACTION_REQUIRED 共用：找原 msg → 整块按 messageChunkId 幂等替换 / 追加
  const upsertNamedBlock = ({blockType, msg, messageChunkId, block}) => {
    const targetMsg = findMessageByChunkId(messageChunkId, blockType) || msg;
    const idx = targetMsg.blocks.findIndex((b) => b.type === blockType && b.messageChunkId === messageChunkId);
    if (idx >= 0) targetMsg.blocks.splice(idx, 1, block);
    else targetMsg.blocks.push(block);
    return targetMsg;
  };

  return (msg, event) => {
    const {type, payload} = normalizeCreationSseEvent(event);
    // 后端两个 endpoint 对同一字段类型不一致：history 接口返回 "string"，SSE 序列化时不带引号变 number。
    // JSON 类型差异在网络面板肉眼看不出（同一个数字 469846 / "469846"），但 JS === 不命中导致
    // chat 重新生成版本时找不到原条目、错误追加为新卡片。在 SSE 入口归一为 string，跟历史侧对齐。
    const messageChunkId = event.messageChunkId == null ? '' : String(event.messageChunkId);

    if (type === 'TEXT') {
      const text = payload?.text || '';
      msg.content += text;
      const lastBlock = msg.blocks[msg.blocks.length - 1];
      if (lastBlock?.type === 'TEXT') {
        setTypewriterBlock(lastBlock);
      } else {
        const block = {type: 'TEXT', text: ''};
        msg.blocks.push(block);
        setTypewriterBlock(block);
      }
      enqueueTypewriterText(text);
      return;
    }

    if (type === 'AUDIO') {
      flushTypewriter();
      msg.audioList.push(...payload);
      scrollToBottom();
      onAssetGenerated?.('AUDIO');
      return;
    }

    if (type === 'VIDEO') {
      flushTypewriter();
      msg.videoList.push(...payload);
      scrollToBottom();
      return;
    }

    if (type === 'SUBJECT') {
      flushTypewriter();
      applyAssetUpsert({assetType: 'SUBJECT', listKey: 'subjectList', receivedEvent: 'subject:received', msg, messageChunkId, payload});
      scrollToBottom();
      onAssetGenerated?.('SUBJECT');
      return;
    }

    if (type === 'SCENE') {
      flushTypewriter();
      applyAssetUpsert({assetType: 'SCENE', listKey: 'sceneList', receivedEvent: 'scene:received', msg, messageChunkId, payload});
      scrollToBottom();
      onAssetGenerated?.('SCENE');
      return;
    }

    if (type === 'SCENE_SCRIPT') {
      flushTypewriter();
      const targetMsg = upsertNamedBlock({
        blockType: 'SCENE_SCRIPT', msg, messageChunkId,
        block: {type: 'SCENE_SCRIPT', messageChunkId, scenes: payload},
      });
      creationBus.emit('scene-script:received', {msgId: targetMsg.messageId});
      scrollToBottom();
      return;
    }

    if (type === 'ACTION_REQUIRED') {
      flushTypewriter();
      upsertNamedBlock({
        blockType: 'ACTION_REQUIRED', msg, messageChunkId,
        block: {type: 'ACTION_REQUIRED', messageChunkId, data: payload},
      });
      scrollToBottom();
      return;
    }

    if (type === 'LYRICS') {
      flushTypewriter();
      appendBlock(msg, 'LYRICS', payload?.lyrics || '');
      scrollToBottom();
      return;
    }

    if (type === 'ERROR') {
      handleSSEErrorEvent(msg, payload);
      return;
    }

    if (type === 'COMPLETE') {
      flushTypewriter();
      handleChatFinished();
    }
  };
};
