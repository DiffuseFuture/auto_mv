import {ElMessage} from 'element-plus';
import {sendChatMessageSSE, resumeChatSSE, clarifyChatSSE} from '@/api/creation';
import {normalizeCreationSseEvent} from '@/views/creation/utils/creationSseEventApplier';
import {isNotEmpty} from '@/utils/index.js';
import {useI18nText} from '@/i18n';
import {useModelStore} from '@/store/model';

/**
 * Creation SSE 聊天编排（发送/续连）。
 *
 * 该 composable 通过注入依赖实现“逻辑搬运”，尽量保持 index.vue 原行为不变。
 */
export function useCreationChatSse({
  userStore,
  router,
  creationBus,
  sessionId,
  chatTitle,
  messages,
  currentAIMessageId,
  loading,
  applySSEEvent,
  handleChatFinished,
  registerTaskController,
  taskControllers,
  scrollToBottom,
  resetTypewriter, // 可选：仅由调用方在更高层调用
  createAiMessage,
  appendBlock,
}) {
  const {t} = useI18nText();
  const modelStore = useModelStore();
  /**
   * 发送用户消息并开启 SSE 流接收 AI 回复。
   * @param {{ payload?: any, options?: any }} event
   * @returns {Promise<void>}
   */
  const handleSend = async (event = {}) => {
    const payload = event.payload || {};
    const options = event.options || {};

    if (!isNotEmpty(payload.userMessage)) return;

    if (!userStore.isLoggedIn) {
      ElMessage.warning(t('creation.chatPanel.sendNeedLogin'));
      return;
    }

    const userMessage = payload.userMessage;
    const currentSessionId = sessionId.value;
    // 新会话首条消息发送即锁定本次会话分辨率：之后所有模型选择框（含分镜编辑弹窗）都按此分辨率
    // 展示可选模型，不再受全局 selectedResolution 后续变化影响。已有会话沿用其锁定值（加载历史时已设置）。
    if (!isNotEmpty(currentSessionId)) {
      modelStore.setConversationResolution(modelStore.selectedResolution);
    }
    const audioFileId = payload.audioFileId || '';
    const editContext = payload.editContext || '';
    const onceAudioLyrics = isNotEmpty(options.audioLyrics) ? options.audioLyrics : '';
    const currentAttachments = payload.currentAttachments || {audio: null, images: []};
    const imageFiles = payload.imageFiles || [];
    // 仅 SCENE「完成制作」路径会带 finalMakeExtras（subtitle / lipSync 两个开关）
    const finalMakeExtras = options.finalMakeExtras || null;

    // 立即显示用户消息（messageId 后端契约为 string，本地占位也用 string 保持单一类型）
    messages.value.push({
      messageId: String(Date.now()),
      senderType: 'USER',
      content: userMessage,
      attachments: currentAttachments,
    });
    scrollToBottom(true);

    // 插入 AI 回复占位，blocks 初始含一个空 TEXT 块（用于显示打字动画）
    const tempMessageId = String(Date.now() + 1);
    messages.value.push(createAiMessage(tempMessageId, {sessionId: currentSessionId || ''}));

    // 取 Vue 代理后的对象引用，确保后续修改能触发响应式更新
    const aiMsg = messages.value[messages.value.length - 1];
    currentAIMessageId.value = tempMessageId;
    loading.value = true;
    scrollToBottom(true);

    // 同类任务启动前先中止旧任务，避免重复连接
    const controller = registerTaskController('send');

    try {
      let didAcceptRequest = false;
      await sendChatMessageSSE(
          {
            sessionId: currentSessionId || '',
            prompt: userMessage,
            audioFileId: audioFileId || '',
            model: modelStore.selectedModel,
            // 分辨率仅在会话首条消息生效、之后锁定：新会话取用户所选、已有会话取该会话锁定值（effectiveResolution 已统一）
            resolution: modelStore.effectiveResolution,
            ...(isNotEmpty(onceAudioLyrics) ? {audioLyrics: onceAudioLyrics} : {}),
            ...(imageFiles.length > 0 ? {subjectImgs: imageFiles} : {}),
            ...(isNotEmpty(editContext) ? {editContext} : {}),
            // SCENE「完成制作」两个开关：未勾选传 false（后端可接受 null/false 等效）
            ...(finalMakeExtras ? {subtitle: finalMakeExtras.subtitle, lipSync: finalMakeExtras.lipSync} : {}),
          },
          {
            onOpen: () => {
              // SSE 连接建立即视为服务端已接收本次发送，立即清空 edit-context 基准
              if (didAcceptRequest) return;
              didAcceptRequest = true;
              creationBus.emit('chat:submit:success', {
                originSessionId: currentSessionId ?? '',
                sessionId: sessionId.value ?? '',
              });
            },
            onMessage: (event) => {
              // 会话已切换，丢弃旧会话的消息
              if (currentSessionId && sessionId.value !== currentSessionId) return;

              const {type, payload} = normalizeCreationSseEvent(event); // 归一化事件结构，兼容新旧返回格式

              // INIT 事件：服务端返回新建的会话 ID 和标题
              // INIT data 后端契约：{ id:string, name:string }
              if (type === 'INIT') {
                if (payload?.id) {
                  sessionId.value = payload.id;
                  aiMsg.sessionId = payload.id;
                  const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
                  const sessionName = (payload.name || '').trim();
                  router.replace({
                    name: 'creation',
                    params: {lang},
                    query: {sessionId: payload.id, sessionName},
                  });

                  // 空白对话首次发送后，立即刷新左侧历史项目列表
                  if (!currentSessionId) {
                    creationBus.emit('history:refresh');
                  }
                }
                if (payload?.name) chatTitle.value = payload.name;
                return;
              }

              // 首次收到后端 messageId 时，替换本地占位 ID
              if (event.messageId != null && aiMsg.messageId !== event.messageId) {
                aiMsg.messageId = event.messageId;
                currentAIMessageId.value = event.messageId;
              }

              applySSEEvent(aiMsg, event);
            },
            onError: () => {
              // SSE 连接出错：仅在当前仍是同一会话时处理。
              if (currentSessionId && sessionId.value !== currentSessionId) return;
              if (!aiMsg.content) {
                appendBlock(aiMsg, 'TEXT', t('creation.requestFailRetry'));
              }
              ElMessage.error(t('creation.chatPanel.connectionFailed'));
              loading.value = false;
            },
            onClose: () => {
              // 同 onError：仅在“有旧会话快照且已切会话”时忽略
              if (currentSessionId && sessionId.value !== currentSessionId) return;
              handleChatFinished();
            },
          },
          controller.signal,
      );

    } catch (e) {
      if (e?.name !== 'AbortError') {
        loading.value = false;
      }
    } finally {
      if (taskControllers.send === controller) taskControllers.send = null;
    }
  };

  /**
   * 恢复未完成的 SSE 对话（切换到历史会话时，若服务端标记未完成则调用）。
   * @param {string} sid 后端 sessionId（string）
   * @param {string} mid 后端 messageId（string）
   * @returns {Promise<void>}
   */
  const handleResumeChat = async (sid, mid) => {
    // 同类任务启动前先中止旧任务，避免续连重复
    const controller = registerTaskController('resume');

    loading.value = true;
    currentAIMessageId.value = mid;

    // 找到对应的 AI 消息，确保有初始 TEXT 块以显示打字动画
    const targetMsg = messages.value.find(m => m.messageId === mid);
    if (!targetMsg) {
      // 历史消息中不存在该 messageId，创建新的占位消息以承接流式内容
      messages.value.push(createAiMessage(mid, {sessionId: sid}));
      scrollToBottom(true);
    } else if (!targetMsg.blocks.length) {
      targetMsg.blocks.push({type: 'TEXT', text: ''});
      if (!targetMsg.sessionId) targetMsg.sessionId = sid;
    } else if (!targetMsg.sessionId) {
      targetMsg.sessionId = sid;
    }

    // 重新查找，确保拿到的是 Vue 响应式代理对象
    const reactiveMsg = messages.value.find(m => m.messageId === mid);

    try {
      await resumeChatSSE(
          {sessionId: sid, historyMessageId: mid},
          {
            onMessage: (event) => {
              if (sessionId.value !== sid) return;
              if (reactiveMsg) applySSEEvent(reactiveMsg, event);
            },
            onError: () => {
              loading.value = false;
              currentAIMessageId.value = null;
            },
            onClose: () => {
              handleChatFinished();
            },
          },
          controller.signal,
      );
    } catch (e) {
      // AbortError 是主动中止，不视为错误
      if (e?.name !== 'AbortError') {
        loading.value = false;
      }
    } finally {
      if (taskControllers.resume === controller) taskControllers.resume = null;
    }
  };

  /**
   * ACTION_REQUIRED 语义澄清提交：
   * - 输入框提交时先插入 USER 气泡（userMessage）
   * - 单独起一次 `/sse/vio/chat/clarify` SSE
   * - 使用现有 applySSEEvent 接续渲染新的 AI 回复
   *
   * @param {{actionId:string|number, optionId:string, query?:string, userMessage?:string}} payload
   * @returns {Promise<void>}
   */
  const handleClarifyActionRequired = async (payload = {}) => {
    if (!userStore.isLoggedIn) {
      ElMessage.warning(t('creation.chatPanel.sendNeedLogin'));
      return;
    }

    const currentSessionId = sessionId.value;
    // ACTION_REQUIRED data：actionId / optionId / kind / query 后端皆 string
    const clarificationId = payload.actionId || '';
    const rawOptionId = payload.optionId || '';
    const isCustom = rawOptionId === '__ACTION_REQUIRED_CUSTOM_OPTION__';
    const optionId = isCustom ? 'custom' : rawOptionId;
    const customText = isCustom ? (payload.query || '') : undefined;
    const userMessage = (payload.userMessage || '').trim();
    if (!clarificationId || !optionId) return;

    if (userMessage) {
      // messageId 后端契约为 string，本地占位也用 string 保持单一类型
      messages.value.push({
        messageId: String(Date.now()),
        senderType: 'USER',
        content: userMessage,
      });
      scrollToBottom(true);
    }

    const tempMessageId = String(Date.now() + 2);
    messages.value.push(createAiMessage(tempMessageId, {sessionId: currentSessionId || ''}));
    const aiMsg = messages.value[messages.value.length - 1];

    scrollToBottom(true);
    currentAIMessageId.value = tempMessageId;
    loading.value = true;

    const controller = registerTaskController('clarify');

    try {
      await clarifyChatSSE(
          {
            sessionId: currentSessionId || '',
            clarificationId,
            optionId,
            ...(customText ? {customText} : {}),
          },
          {
            onOpen: () => {},
            onMessage: (event) => {
              if (currentSessionId && sessionId.value !== currentSessionId) return;

              const {type, payload: normalizedPayload} = normalizeCreationSseEvent(event);
              // INIT data 后端契约：{ id:string, name:string }
              if (type === 'INIT') {
                if (normalizedPayload?.id) {
                  sessionId.value = normalizedPayload.id;
                  aiMsg.sessionId = normalizedPayload.id;
                  const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
                  const sessionName = (normalizedPayload.name || '').trim();
                  router.replace({
                    name: 'creation',
                    params: {lang},
                    query: {sessionId: normalizedPayload.id, sessionName},
                  });
                  if (!currentSessionId) {
                    creationBus.emit('history:refresh');
                  }
                }
                if (normalizedPayload?.name) chatTitle.value = normalizedPayload.name;
                return;
              }

              if (event.messageId != null && aiMsg.messageId !== event.messageId) {
                aiMsg.messageId = event.messageId;
                currentAIMessageId.value = event.messageId;
              }

              applySSEEvent(aiMsg, event);
            },
            onError: () => {
              if (currentSessionId && sessionId.value !== currentSessionId) return;
              if (!aiMsg.content) {
                appendBlock(aiMsg, 'TEXT', t('creation.requestFailRetry'));
              }
              ElMessage.error(t('creation.chatPanel.connectionFailed'));
              loading.value = false;
            },
            onClose: () => {
              if (currentSessionId && sessionId.value !== currentSessionId) return;
              handleChatFinished();
            },
          },
          controller.signal,
      );
    } catch (e) {
      if (e?.name !== 'AbortError') {
        loading.value = false;
      }
    } finally {
      if (taskControllers.clarify === controller) taskControllers.clarify = null;
    }
  };

  return {
    handleSend,
    handleResumeChat,
    handleClarifyActionRequired,
  };
}
