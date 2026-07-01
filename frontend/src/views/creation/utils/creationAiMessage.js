/**
 * Creation AI 消息结构工具。
 *
 * 后端契约（见 数据结构.txt）：
 * - 顶层消息字段：`messageId`(string)、`seqNo`(number)、`senderType`(string "USER"|"MODEL")、`messageChunks`(array)
 */

/**
 * 判断消息是否为 AI 类型。
 * @param {any} msg
 * @returns {boolean}
 */
export const isAiMessage = (msg) => msg?.senderType === 'MODEL';

/**
 * 获取最后一个 AI 消息。
 * @param {any[]} messages
 * @returns {any|undefined}
 */
export const getLastAiMessage = (messages = []) => {
  if (!messages.length) return undefined;
  return [...messages].reverse().find(isAiMessage);
};

/**
 * 创建空的 AI 消息占位结构，用于：
 * - 发送后立即插入 UI 占位（逐字渲染承接到 blocks）
 * - 续连时补齐历史中缺失的部分消息结构
 *
 * @param {string} messageId 后端 messageId 为 string；本地占位也用 string 保持单一类型
 * @param {Object} overrides
 * @returns {any}
 */
export const createAiMessage = (messageId, overrides = {}) => ({
  messageId,
  senderType: 'MODEL',
  content: '',
  audioList: [],
  videoList: [],
  subjectList: [],
  sceneList: [],
  blocks: [{type: 'TEXT', text: ''}],
  ...overrides,
});
