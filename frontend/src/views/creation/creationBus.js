/** @typedef {(payload?: any) => void} CreationBusHandler */

const listeners = new Map();

/**
 * 订阅 creation 模块内跨组件事件（轻量 pub/sub）。
 * @param {string} event
 * @param {CreationBusHandler} handler
 * @returns {void}
 */
const on = (event, handler) => {
  if (!listeners.has(event)) listeners.set(event, new Set());
  listeners.get(event).add(handler);
};

/**
 * 取消订阅。
 * @param {string} event
 * @param {CreationBusHandler} handler
 * @returns {void}
 */
const off = (event, handler) => {
  const set = listeners.get(event);
  if (!set) return;
  set.delete(handler);
  if (!set.size) listeners.delete(event);
};

/**
 * 同步广播事件；单个 handler 抛错会被 catch 并打日志，不影响其它监听者。
 * @param {string} event
 * @param {any} [payload]
 * @returns {void}
 */
const emit = (event, payload) => {
  const set = listeners.get(event);
  if (!set) return;
  set.forEach((handler) => {
    try {
      handler(payload);
    } catch (error) {
      console.error(error);
    }
  });
};

export const creationBus = {on, off, emit};
