/**
 * Creation 消息 blocks 与附件的基础工具。
 */

/**
 * 向 AI 消息的 `blocks` 数组中追加内容块。
 * - TEXT 类型会合并到最后一个 TEXT 块中（流式渲染）。
 * - 其它类型直接追加新块。
 *
 * @param {any} msg - AI 消息对象（要求含 `blocks` 数组）
 * @param {'TEXT'|'LYRICS'} type - 块类型
 * @param {string} text - 要追加/合并的文本
 * @returns {void}
 */
export const appendBlock = (msg, type, text) => {
  if (!text) return;
  const lastBlock = msg.blocks[msg.blocks.length - 1];

  if (type === 'TEXT' && lastBlock?.type === 'TEXT') {
    // TEXT 块流式追加，避免产生多个相邻 TEXT 气泡
    lastBlock.text += text;
    return;
  }

  msg.blocks.push({type, text});
};
