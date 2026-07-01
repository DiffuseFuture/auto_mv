import {defineStore} from 'pinia';
import {reactive} from 'vue';
import {directEditQuery} from '@/api/creation';
import {translate as t} from '@/i18n';
import {useCreationEditContextStore} from '@/store/creationEditContext';

const STORAGE_KEY = 'creation_subject_edit_tasks_v1';
const MAX_TASK_AGE_MS = 6 * 60 * 60 * 1000;
const DEFAULT_TASK_TYPE = 'SUBJECT';
const POLL_INTERVAL_MS = 3000;

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
// taskType 由调用方以常量字符串传入（'SUBJECT'/'SCENE'），toUpperCase 仅做大小写收敛
const normalizeTaskType = (taskType) => (taskType || DEFAULT_TASK_TYPE).toUpperCase();

/**
 * SUBJECT / SCENE 直连编辑任务的全局轮询管理。
 * - 任务键：`${type}::${sessionId}::${chunkId}`
 * - 用 setTimeout 递归调度，stopPolling 用 clearTimeout 干净中止
 * - 轮询拿到 success 时直接 recordVersionChange，确保后台完成的任务也能记录版本变更
 *
 * 字段契约（与 useCreationDirectEditTask 对齐）：
 * - sessionId / chunkId / taskId / messageId：string
 * - activeVersion：number
 */
export const useCreationSubjectEditStore = defineStore('creationSubjectEdit', () => {
  const tasks = reactive({});
  /** key → 当前 setTimeout 的 timerId（值为 0 表示正在 tick 中、暂无 pending timer）。entry 存在即视为该任务在轮询。 */
  const pollings = new Map();

  const buildKey = (taskType, sessionId, chunkId) => `${normalizeTaskType(taskType)}::${sessionId || ''}::${chunkId || ''}`;

  const persistRunningTasks = () => {
    try {
      const snapshot = {};
      Object.keys(tasks).forEach((key) => {
        const item = tasks[key];
        if (item?.status !== 'running') return;
        snapshot[key] = {
          taskType: item.taskType, sessionId: item.sessionId, chunkId: item.chunkId,
          taskId: item.taskId, status: 'running', startedAt: item.startedAt,
          previousVersion: item.previousVersion ?? null, messageId: item.messageId || '',
        };
      });
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
    } catch {/* ignore */}
  };

  // 启动时恢复 running 任务
  if (typeof window !== 'undefined') {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      const nowTs = Date.now();
      Object.keys(parsed || {}).forEach((key) => {
        const item = parsed[key];
        if (!item || item.status !== 'running' || !item.sessionId || !item.chunkId || !item.taskId) return;
        if (item.startedAt && nowTs - item.startedAt > MAX_TASK_AGE_MS) return;
        tasks[key] = {
          ...item,
          taskType: normalizeTaskType(item.taskType),
          startedAt: item.startedAt || nowTs,
          updatedAt: nowTs,
          previousVersion: item.previousVersion ?? null,
          messageId: item.messageId || '',
          result: null,
          error: null,
        };
      });
    } catch {/* ignore */}
  }

  const getTask = (sessionId, chunkId, taskType = DEFAULT_TASK_TYPE) => tasks[buildKey(taskType, sessionId, chunkId)] || null;

  /** 浅合并写入并持久化。defaults 在前，prev 居中保留历史字段，patch 在后覆盖。 */
  const setTask = (sessionId, chunkId, patch, taskType = DEFAULT_TASK_TYPE) => {
    const key = buildKey(taskType, sessionId, chunkId);
    const ts = Date.now();
    tasks[key] = {
      taskType: normalizeTaskType(taskType),
      sessionId: sessionId || '', chunkId: chunkId || '',
      status: 'idle', taskId: '', result: null, error: null,
      previousVersion: null, messageId: '',
      startedAt: ts,
      ...tasks[key],
      updatedAt: ts,
      ...patch,
    };
    persistRunningTasks();
    return tasks[key];
  };

  const clearTask = (sessionId, chunkId, taskType = DEFAULT_TASK_TYPE) => {
    const key = buildKey(taskType, sessionId, chunkId);
    delete tasks[key];
    stopPollingByKey(key);
    persistRunningTasks();
  };

  const startTask = ({sessionId, chunkId, taskId, taskType = DEFAULT_TASK_TYPE, previousVersion = null, messageId = ''}) => {
    setTask(sessionId, chunkId, {
      taskId: taskId || '', status: 'running', result: null, error: null,
      previousVersion, messageId: messageId || '',
    }, taskType);
    ensurePolling(sessionId, chunkId, taskType);
    return buildKey(taskType, sessionId, chunkId);
  };

  /**
   * 用 setTimeout 调度后台轮询。pollings 中存在 key 即视为活跃。
   * 一次成功 / 失败响应即终止；stopPolling 用 clearTimeout + delete 干净中止。
   */
  const ensurePolling = (sessionId, chunkId, taskType = DEFAULT_TASK_TYPE) => {
    const key = buildKey(taskType, sessionId, chunkId);
    if (pollings.has(key)) return;
    const task = getTask(sessionId, chunkId, taskType);
    if (!task || task.status !== 'running' || !task.taskId) return;

    pollings.set(key, 0);

    const tick = async () => {
      if (!pollings.has(key)) return;
      const latest = getTask(sessionId, chunkId, taskType);
      if (!latest || latest.status !== 'running' || !latest.taskId) {
        pollings.delete(key);
        return;
      }

      try {
        const res = await directEditQuery({taskId: latest.taskId});
        if (res?.state === 0) {
          const {previousVersion, messageId} = latest;
          const result = res.data || null;
          setTask(sessionId, chunkId, {status: 'success', result, error: null}, taskType);
          // 后台 success 也要记录版本变更，否则用户离开页面期间完成的任务，回来后下一步聊天接口会丢「使用新版本」操作。
          // 注意 result 形如 {type, data: {activeVersion, versions, ...}}，activeVersion 在嵌套的 data 里。
          const nextVersion = result?.data?.activeVersion;
          if (nextVersion != null && messageId) {
            useCreationEditContextStore().recordVersionChange(sessionId, normalizeTaskType(taskType), {
              messageId, itemKey: chunkId, previousVersion, nextVersion,
            });
          }
          pollings.delete(key);
          return;
        }
        if (res?.state === 2) {
          const prefix = normalizeTaskType(taskType) === 'SCENE' ? 'creation.sceneEditDialog' : 'creation.subjectEditDialog';
          setTask(sessionId, chunkId, {status: 'error', error: {message: res.message || t(`${prefix}.generateFailed`)}, result: null}, taskType);
          pollings.delete(key);
          return;
        }
        // 仍 pending：下一轮调度（中途被 stopPolling 则不再调度）
        if (!pollings.has(key)) return;
        pollings.set(key, setTimeout(tick, POLL_INTERVAL_MS));
      } catch (error) {
        setTask(sessionId, chunkId, {status: 'error', error, result: null}, taskType);
        pollings.delete(key);
      }
    };

    tick();
  };

  /**
   * 等待任务终态（用于弹窗 reopen 续上状态、页面级 resume）。
   * 不设超时：SUBJECT/SCENE 后端任务时长不可预测，超时只会让后台跑完时无人 upsert + toast。
   * 任务被 clearTask 清掉时返回 null；调用方走 fire-and-forget。
   */
  const waitForTask = async (sessionId, chunkId, options = {}, taskType = DEFAULT_TASK_TYPE) => {
    ensurePolling(sessionId, chunkId, taskType);
    while (true) {
      const task = getTask(sessionId, chunkId, taskType);
      if (!task) return null;
      if (task.status === 'success' || task.status === 'error') return task;
      await sleep(300);
    }
  };

  const stopPollingByKey = (key) => {
    const timerId = pollings.get(key);
    if (timerId) clearTimeout(timerId);
    pollings.delete(key);
  };

  /** 中止指定 / 全部正在跑的轮询（不改任务状态，下次 ensurePolling 可重启）。 */
  const stopPolling = (sessionId, chunkId, taskType = DEFAULT_TASK_TYPE) => stopPollingByKey(buildKey(taskType, sessionId, chunkId));
  const stopAllPolling = () => {
    pollings.forEach((timerId) => { if (timerId) clearTimeout(timerId); });
    pollings.clear();
  };

  return {tasks, getTask, startTask, ensurePolling, waitForTask, clearTask, stopPolling, stopAllPolling};
});
