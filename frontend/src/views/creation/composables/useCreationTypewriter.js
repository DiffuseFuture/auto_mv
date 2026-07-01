/**
 * Creation 打字机效果（SSE TEXT 逐字渲染）。
 *
 * 该 composable 只负责：
 * - 管理缓冲队列与当前 block 引用
 * - requestAnimationFrame 消费缓冲队列并逐字更新 block.text
 * - 提供 flush/reset 能力用于“COMPLETE/切换会话/出错”等场景
 *
 * 与业务编排解耦：由调用方传入 `scrollToBottom()` 回调。
 */
export function useCreationTypewriter({scrollToBottom}) {
  // 待渲染的字符队列
  let typewriterBuffer = '';
  // 当前正在打字的 TEXT block 引用
  let typewriterBlock = null;
  // requestAnimationFrame ID
  let typewriterRafId = null;
  // 上次渲染时间戳
  let typewriterLastTime = 0;

  // 每次 tick 消费的字符数（20ms 一个字符 → 约 50 字/秒）
  const TYPEWRITER_INTERVAL = 20;
  // 每次 tick 最多消费的字符数，防止积压时一次性吐出太多
  const TYPEWRITER_MAX_CHARS_PER_TICK = 3;

  /**
   * 设置当前打字的 TEXT block。
   * @param {any|null} block
   * @returns {void}
   */
  const setTypewriterBlock = (block) => {
    typewriterBlock = block;
  };

  /**
   * 将文本入缓冲队列并启动逐字渲染。
   * @param {string} text
   * @returns {void}
   */
  const enqueueTypewriterText = (text) => {
    if (!text) return;
    typewriterBuffer += text;
    startTypewriter();
  };

  /**
   * 启动 rAF 循环消费缓冲区；已在运行时不重复注册。
   * @returns {void}
   */
  const startTypewriter = () => {
    if (typewriterRafId) return; // 已在运行
    typewriterLastTime = performance.now();

    const tick = (now) => {
      if (!typewriterBuffer || !typewriterBlock) {
        typewriterRafId = null;
        return;
      }

      const elapsed = now - typewriterLastTime;

      const charsToConsume = Math.min(
          Math.floor(elapsed / TYPEWRITER_INTERVAL),
          TYPEWRITER_MAX_CHARS_PER_TICK,
          typewriterBuffer.length,
      );

      if (charsToConsume > 0) {
        typewriterBlock.text += typewriterBuffer.slice(0, charsToConsume);
        typewriterBuffer = typewriterBuffer.slice(charsToConsume);
        typewriterLastTime = now;
        scrollToBottom();
      }

      if (typewriterBuffer) {
        typewriterRafId = requestAnimationFrame(tick);
      } else {
        typewriterRafId = null;
      }
    };

    typewriterRafId = requestAnimationFrame(tick);
  };

  /**
   * 立即刷完缓冲区中的所有文本（用于 COMPLETE、切换会话等场景）。
   * @returns {void}
   */
  const flushTypewriter = () => {
    if (typewriterRafId) {
      cancelAnimationFrame(typewriterRafId);
      typewriterRafId = null;
    }

    if (typewriterBuffer && typewriterBlock) {
      typewriterBlock.text += typewriterBuffer;
      typewriterBuffer = '';
      scrollToBottom();
    }

    typewriterBlock = null;
  };

  /**
   * 重置打字机状态（切换会话、离开页面时调用）。
   * @returns {void}
   */
  const resetTypewriter = () => {
    if (typewriterRafId) {
      cancelAnimationFrame(typewriterRafId);
      typewriterRafId = null;
    }

    typewriterBuffer = '';
    typewriterBlock = null;
  };

  return {
    setTypewriterBlock,
    enqueueTypewriterText,
    startTypewriter,
    flushTypewriter,
    resetTypewriter,
  };
}

