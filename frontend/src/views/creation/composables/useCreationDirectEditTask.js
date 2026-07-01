import {isNotEmpty} from '@/utils/index.js';

/**
 * 通用 direct-edit 任务恢复与结果应用逻辑。
 * 用于 SUBJECT / SCENE 等同构任务，减少弹窗组件重复代码。
 *
 * 当传入 `upsertItem` + `listKey` 时，composable 会在 success 分支里
 * 自行调用 upsert，并对比 upsert 前后的 `activeVersion`，差异时通过
 * `emitVersionChange` 通知调用方记录版本变化（避免每个组件重复实现）。
 * 此时 `onSuccess` 的第三个参数 `context` 会带上 `updatedItem`，组件无需再自己 upsert。
 *
 * 后端字段契约：
 * - sessionId / chunkId(=messageChunkId) / messageId / taskId / task.status：string
 * - result.type：string（与 expectedType 大小写一致）
 * - activeVersion / version：number
 *
 * @param {{
 *   store: any,
 *   getSessionId: () => string,
 *   getChunkId: () => string,
 *   getSourceMsg: () => any,
 *   setEditing: (editing: boolean) => void,
 *   taskType: string,
 *   expectedType: string,
 *   successMessage: string,
 *   onSuccess: (resultData: any, sourceMsg: any, context?: {sessionId:string, messageChunkId:string, updatedItem?: any}) => Promise<void> | void,
 *   onError: (message: string) => void,
 *   onNotifySuccess?: (message: string) => void,
 *   upsertItem?: (msg: any, payload: any) => number,
 *   listKey?: 'subjectList'|'sceneList',
 *   emitVersionChange?: (payload: {type: string, messageId: string, itemKey: string, previousVersion: number, nextVersion: number}) => void,
 * }} options
 */
export const useCreationDirectEditTask = (options) => {
  const {
    store,
    getSessionId,
    getChunkId,
    getSourceMsg,
    setEditing,
    taskType,
    expectedType,
    successMessage,
    onSuccess,
    onError,
    onNotifySuccess,
    upsertItem,
    listKey,
    emitVersionChange,
  } = options;

  /**
   * 是否具备有效的 sessionId + chunkId（直连编辑任务键）。
   * 二者均为 string，按非空字符串校验。
   * @returns {boolean}
   */
  const isValidTaskKey = () => isNotEmpty(getSessionId()) && isNotEmpty(getChunkId());

  /**
   * 判断当前 apply 是否仍“拥有”该任务的处理权。
   * 由于一个任务可能被多个页面/弹层同时等待，任务完成时会触发多次 apply；
   * 这里通过“先校验 store 中仍是同一个任务，再清理任务”的方式让 apply 幂等。
   * @param {string} sessionId
   * @param {string} chunkId
   * @param {any} task
   * @returns {boolean}
   */
  const canConsumeTask = (sessionId, chunkId, task) => {
    if (!task) return false;
    const latest = store.getTask(sessionId, chunkId, taskType);
    if (!latest) return false;
    if (latest.taskId !== task.taskId) return false;
    if (latest.status !== task.status) return false;
    return true;
  };

  /**
   * 根据 store 中的任务终态应用结果：失败提示并清理；成功则调用 onSuccess 并可选成功提示。
   * @param {any} task
   * @param {{sessionId?: string, messageChunkId?: string, sourceMsg?: any}} [runtimeContext]
   * @returns {Promise<void>}
   */
  const applyTaskResult = async (task, runtimeContext = {}) => {
    const sessionId = runtimeContext.sessionId ?? getSessionId();
    const chunkId = runtimeContext.messageChunkId ?? getChunkId();
    const sourceMsg = runtimeContext.sourceMsg ?? getSourceMsg();
    if (!task || !isNotEmpty(sessionId) || !isNotEmpty(chunkId)) return;

    try {
      if (task.status === 'error') {
        // 幂等：只有“首个拿到处理权”的调用才提示并清理
        if (!canConsumeTask(sessionId, chunkId, task)) return;
        store.clearTask(sessionId, chunkId, taskType);
        const message = task?.error?.message || '生成失败，请重试';
        onError(message);
        return;
      }

      if (task.status !== 'success') return;
      // 幂等：避免多个等待者在任务完成时重复 apply & 重复 toast
      if (!canConsumeTask(sessionId, chunkId, task)) return;
      // 先清理任务，确保并发等待者后续无法重复消费
      store.clearTask(sessionId, chunkId, taskType);
      const result = task.result;
      if (result?.type !== expectedType) {
        onError('返回类型异常');
        return;
      }

      if (!sourceMsg) {
        return;
      }

      // 若调用方提供了 upsertItem + listKey，则由 composable 统一负责：
      // 1. upsert 前抓 previousVersion；2. upsert；3. 对比 activeVersion 并触发 emitVersionChange。
      // 这样多个编辑组件的 onSuccess 不必各自重写一遍。
      let updatedItem = null;
      if (upsertItem && listKey) {
        const beforeList = sourceMsg?.[listKey] || [];
        const beforeItem = beforeList.find((item) => item?.messageChunkId === chunkId);
        const previousVersion = beforeItem?.activeVersion;

        // 必须把 messageChunkId 拼进 payload：upsertItem 按 messageChunkId 命中原条目做版本合入，
        // result.data 是后端返回的 SUBJECT/SCENE 内层 data（不含 messageChunkId），不拼会找不到原条目变成 push 新卡片。
        const idx = upsertItem(sourceMsg, {...(result?.data || {}), messageChunkId: chunkId});
        updatedItem = sourceMsg?.[listKey]?.[idx] ?? null;
        const nextVersion = updatedItem?.activeVersion;

        if (
          emitVersionChange
          && previousVersion !== undefined
          && nextVersion !== undefined
          && previousVersion !== nextVersion
        ) {
          emitVersionChange({
            type: taskType,
            messageId: sourceMsg?.messageId,
            itemKey: chunkId,
            previousVersion,
            nextVersion,
          });
        }
      }

      await onSuccess(result?.data || {}, sourceMsg, {sessionId, messageChunkId: chunkId, updatedItem});
      if (successMessage) {
        (onNotifySuccess || onError)(successMessage);
      }
    } finally {
      setEditing(false);
    }
  };

  /**
   * 弹窗打开时：若 store 中已有进行中的任务则等待结束并应用；若已成功则直接应用。
   * @returns {Promise<void>}
   */
  const resumeTaskIfNeeded = async () => {
    if (!isValidTaskKey()) return;
    const sessionId = getSessionId();
    const chunkId = getChunkId();
    const sourceMsg = getSourceMsg();
    const runtimeContext = {sessionId, messageChunkId: chunkId, sourceMsg};
    const task = store.getTask(sessionId, chunkId, taskType);
    if (!task) return;

    if (task.status === 'success') {
      await applyTaskResult(task, runtimeContext);
      return;
    }

    if (task.status === 'running') {
      setEditing(true);
      const finalTask = await store.waitForTask(sessionId, chunkId, {}, taskType);
      await applyTaskResult(finalTask, runtimeContext);
    }
  };

  return {
    applyTaskResult,
    resumeTaskIfNeeded,
  };
};
