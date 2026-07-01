import {appendBlock} from '@/views/creation/utils/creationMessageBlocks';

/**
 * 将新 subject upsert 回 msg.subjectList。身份判定只看 messageChunkId。
 * - 命中：splice 整体替换（SSE 推送的是完整 versions 数组，整条覆盖即可）
 * - 未命中：push 追加为全新 subject
 * assetKey 跨消息会复用、跨对话轮也会从 subject_1 重启，**不能**作为识别依据。
 */
export const upsertSubjectItem = (msg, payload) => {
  const idx = msg.subjectList.findIndex((item) => item.messageChunkId === payload.messageChunkId);
  if (idx < 0) {
    msg.subjectList.push(payload);
    return msg.subjectList.length - 1;
  }
  msg.subjectList.splice(idx, 1, payload);
  return idx;
};

/**
 * 将新 scene upsert 回 msg.sceneList。规则同 upsertSubjectItem。
 */
export const upsertSceneItem = (msg, payload) => {
  const idx = msg.sceneList.findIndex((item) => item.messageChunkId === payload.messageChunkId);
  if (idx < 0) {
    msg.sceneList.push(payload);
    return msg.sceneList.length - 1;
  }
  msg.sceneList.splice(idx, 1, payload);
  return idx;
};

/**
 * 后端历史消息 messageChunks → 页面可渲染消息结构。
 *
 * 三类结构化资产（与 SSE 流式事件一一对应）：
 * - SUBJECT：msg.subjectList[]，多版本 versions[].prompt/imgUrl/...，以 activeVersion 命中的版本为准
 * - SCENE：msg.sceneList[]，多版本 versions[].visualPrompt/videoUrl/subjects；duration/startTime/endTime 仅在顶层
 * - SCENE_SCRIPT：msg.blocks 中 type==='SCENE_SCRIPT' 的块，scenes[] 每行一条脚本，无 versions 模型
 */
export const convertMessageChunks = (chunks = []) => {
  const msg = {content: '', audioList: [], videoList: [], subjectList: [], sceneList: [], blocks: []};

  for (const chunk of chunks) {
    const {type, data} = chunk.content;
    const messageChunkId = chunk.messageChunkId;

    if (type === 'TEXT') {
      msg.content += data.text;
      appendBlock(msg, 'TEXT', data.text);
    } else if (type === 'AUDIO') {
      msg.audioList.push(...data);
    } else if (type === 'VIDEO') {
      msg.videoList.push(...data);
    } else if (type === 'SUBJECT') {
      upsertSubjectItem(msg, {...data, messageChunkId});
    } else if (type === 'SCENE') {
      upsertSceneItem(msg, {...data, messageChunkId});
    } else if (type === 'SCENE_SCRIPT') {
      msg.blocks.push({type: 'SCENE_SCRIPT', messageChunkId, scenes: data});
    } else if (type === 'ACTION_REQUIRED') {
      msg.blocks.push({type: 'ACTION_REQUIRED', messageChunkId, data});
    } else if (type === 'LYRICS') {
      appendBlock(msg, 'LYRICS', data.lyrics);
    } else if (type === 'ERROR') {
      // 历史回显错误：直接 push 一个 TEXT 块（不走 appendBlock 的合并逻辑，避免
      // 跟前面那条正常 AI 文本气泡粘连成一个气泡，用户分不清哪段是错误）。
      // 流式聊天里这个错误另外还会弹 toast，但回放历史时没有 toast 触发的机会，
      // 必须无条件渲染到 blocks 里才看得见。
      const errorText = data?.text || '请求失败，请稍后重试';
      msg.blocks.push({type: 'TEXT', text: errorText});
      if (!msg.content) msg.content = errorText;
    }
  }

  return msg;
};
