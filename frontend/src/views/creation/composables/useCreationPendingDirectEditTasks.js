import {isNotEmpty} from '@/utils/index.js';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {upsertSubjectItem, upsertSceneItem} from '@/views/creation/utils/creationMessageChunks';

/**
 * 进入创作页 / 切换会话时，扫描已加载消息中的 SUBJECT / SCENE，命中 store 里
 * 进行中（含 localStorage 续上的）任务则后台续上轮询并把结果回写到消息。
 *
 * 版本变更记录已由 store 在轮询成功分支统一处理，本 composable 只管把最终
 * data upsert 到 msg 上。
 */
export const useCreationPendingDirectEditTasks = () => {
  const store = useCreationSubjectEditStore();

  /**
   * 字段契约：
   * - sessionId / chunkId(=messageChunkId) / taskType / result.type：string
   * - taskType 由调用方以常量传入（'SUBJECT'/'SCENE'），与 result.type 大小写一致；不再 toUpperCase。
   */
  const waitAndApply = async ({taskType, sessionId, chunkId, msg}) => {
    const finalTask = await store.waitForTask(sessionId, chunkId, {}, taskType);
    if (!finalTask) return;
    if (finalTask.status === 'error') {
      store.clearTask(sessionId, chunkId, taskType);
      return;
    }
    if (finalTask.status !== 'success') return;

    const result = finalTask.result;
    if (result?.type !== taskType) {
      store.clearTask(sessionId, chunkId, taskType);
      return;
    }
    const upsert = taskType === 'SUBJECT' ? upsertSubjectItem : upsertSceneItem;
    upsert(msg, result?.data || {});
    store.clearTask(sessionId, chunkId, taskType);
  };

  const resumeAllPendingDirectEditTasks = (sessionId, messages = []) => {
    if (!isNotEmpty(sessionId) || messages.length === 0) return;

    const visit = (taskType, list, msg) => {
      // messageChunkId 后端契约 string，绝不私自 Number()
      (list || []).forEach((item) => {
        const chunkId = item?.messageChunkId;
        if (!isNotEmpty(chunkId)) return;
        const task = store.getTask(sessionId, chunkId, taskType);
        if (!task || (task.status !== 'running' && task.status !== 'success')) return;
        // 后台异步处理，避免阻塞页面初始化
        waitAndApply({taskType, sessionId, chunkId, msg}).catch(() => {});
      });
    };

    messages.forEach((msg) => {
      visit('SUBJECT', msg?.subjectList, msg);
      visit('SCENE', msg?.sceneList, msg);
    });
  };

  return {resumeAllPendingDirectEditTasks};
};
