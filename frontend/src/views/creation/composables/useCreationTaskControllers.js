/**
 * Creation 任务控制器（中止/注册/轮询）抽离。
 *
 * 目标：把 `index.vue` 中与任务控制相关的逻辑从视图编排中解耦，
 * 保持行为完全一致。
 */
import {remakeVideoQuery} from '@/api/creation';

export function useCreationTaskControllers({
  creationBus,
  loading,
  loadingMessages,
  currentAIMessageId,
  storyboardRemaking,
}) {
  // 页面内可中断任务控制器
  const taskControllers = {
    send: null,
    resume: null,
    clarify: null,
    remake: null,
  };

  // 分镜重生成轮询：仅保留一个 timer，防止泄漏
  const REMAKE_POLL_INTERVAL = 3000;
  let remakePollTimer = null;

  /**
   * 清除分镜重生成轮询定时器，防止泄漏。
   * @returns {void}
   */
  const clearRemakePollTimer = () => {
    if (remakePollTimer) {
      clearTimeout(remakePollTimer);
      remakePollTimer = null;
    }
  };

  /**
   * 中止指定任务
   * @param {'send'|'resume'|'clarify'|'remake'} task
   * @returns {void}
   */
  const abortTask = (task) => {
    const controller = taskControllers[task];
    if (controller) {
      controller.abort();
      taskControllers[task] = null;
    }

    if (task === 'remake') {
      clearRemakePollTimer();
    }
  };

  /**
   * 注册任务控制器（同类任务会先中止旧任务）
   * @param {'send'|'resume'|'clarify'|'remake'} task
   * @returns {AbortController}
   */
  const registerTaskController = (task) => {
    abortTask(task);
    const controller = new AbortController();
    taskControllers[task] = controller;
    return controller;
  };

  /**
   * cleanup：轮询类任务完成后清理 timer + 置空 controller。
   * @param {'send'|'resume'|'remake'} task
   * @param {AbortController} controller
   * @returns {void}
   */
  const cleanupTaskControllerIfSame = (task, controller) => {
    if (task === 'remake') clearRemakePollTimer();
    if (taskControllers[task] === controller) taskControllers[task] = null;
  };

  /**
   * 中止所有流式连接，并重置发送态。
   * 用于新建会话、切换会话等需要“立即可发送”的场景。
   * @returns {void}
   */
  const resetStreamingState = () => {
    abortTask('send');
    abortTask('resume');
    abortTask('clarify');
    abortTask('remake');
    loading.value = false;
    loadingMessages.value = false;
    currentAIMessageId.value = null;
    storyboardRemaking.value = false;

    // 切换会话/中断任务时，清空 SUBJECT 等待确认状态
    creationBus.emit('subject:reset');
  };

  /**
   * 分镜重生成轮询：按固定间隔查询直到成功/失败/取消
   * @param {string|number} taskId
   * @param {AbortController} controller
   * @returns {Promise<{videoFileId:string, videoUrl:string, coverFileId?:string, coverUrl?:string}>}
   */
  const pollRemakeTask = (taskId, controller) => new Promise((resolve, reject) => {
    let finished = false;

    const finish = (handler, payload) => {
      if (finished) return;
      finished = true;
      clearRemakePollTimer();
      controller.signal.removeEventListener('abort', onAbort);
      handler(payload);
    };

    const onAbort = () => {
      finish(reject, new DOMException('Aborted', 'AbortError'));
    };

    const poll = async () => {
      if (controller.signal.aborted) {
        onAbort();
        return;
      }

      try {
        const queryRes = await remakeVideoQuery({taskId});
        const state = queryRes?.state;

        if (state === 2) {
          finish(reject, new Error('分镜重生成失败'));
          return;
        }

        if (state === 0) {
          const videos = Array.isArray(queryRes?.data?.data) ? queryRes.data.data : [];
          const finalVideo = videos[0];

          if (!finalVideo?.videoFileId || !finalVideo?.videoUrl) {
            finish(reject, new Error('未获取到重生成视频'));
            return;
          }

          finish(resolve, {
            videoFileId: finalVideo.videoFileId,
            videoUrl: finalVideo.videoUrl,
            coverFileId: finalVideo.coverFileId,
            coverUrl: finalVideo.coverUrl,
          });
          return;
        }

        remakePollTimer = setTimeout(() => {
          void poll();
        }, REMAKE_POLL_INTERVAL);
      } catch (error) {
        finish(reject, error);
      }
    };

    controller.signal.addEventListener('abort', onAbort, {once: true});
    void poll();
  });

  return {
    taskControllers,
    registerTaskController,
    abortTask,
    resetStreamingState,
    pollRemakeTask,
    cleanupTaskControllerIfSame,
  };
}

