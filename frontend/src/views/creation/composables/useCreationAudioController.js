/**
 * Creation 音频播放控制器（复用逻辑）。
 *
 * 该 composable 只负责：
 * - 维护 `playerState`（currentAudio/currentIndex/audioList/isPlaying/currentTime/duration）
 * - 控制页面唯一 `<audio>` 元素（src/load/play/pause）
 * - 处理“同一音频重复点击时的暂停/继续”
 *
 * 不包含任何 creationBus 业务副作用（例如 open/close 面板），由调用方通过 `onOpenPanel` 注入。
 */
export function useCreationAudioController({
  audioPlayerRef, // Ref<HTMLAudioElement> 或 HTMLAudioElement（可能在 setup 时为 null）
  getAudioEl, // 可选：动态获取最新 audio DOM（推荐，用于避免 setup 捕获 null）
  playerState, // reactive 对象（父组件传入）
  pauseAllVideos, // () => void
  onOpenPanel, // () => void（可选）
}) {
  /**
   * 获取当前 audio DOM 引用。
   * @returns {HTMLAudioElement|null}
   */
  const resolveAudioEl = () => {
    if (typeof getAudioEl === 'function') return getAudioEl();

    const target = audioPlayerRef; // 可能为 ref 包装或直接 DOM
    if (!target) return null; // 空引用
    return target?.value ?? target; // 兼容 ref 与直接 DOM
  };

  /**
   * 暂停当前 `<audio>` 并同步状态。
   * @returns {void}
   */
  const pauseAudio = () => {
    const audio = resolveAudioEl(); // 当前 audio 元素
    if (audio) audio.pause(); // 暂停播放器
    if (playerState) playerState.isPlaying = false; // 更新 UI 状态
  };

  /**
   * 在播放前统一暂停页面内视频，避免多媒体冲突。
   * @returns {void}
   */
  const safePauseAllVideos = () => {
    if (typeof pauseAllVideos === 'function') pauseAllVideos(); // 外部注入的策略
  };

  /**
   * 设置当前音频并完成 `<audio>` 的 src/load。
   * @param {any} item 音频条目（需包含 audioUrl/audioFileId）
   * @param {number} index 当前索引
   * @param {any[]} audioList 音频列表
   * @param {{openPanel?: boolean}} options
   * @returns {void}
   */
  const setCurrentAudio = (item, index, audioList, options = {}) => {
    const {openPanel = true} = options; // 是否在切换/开始播放时打开详情面板

    if (!playerState) return; // 容错：缺少状态对象

    playerState.currentAudio = item; // 当前音频对象
    playerState.currentIndex = index; // 当前索引
    playerState.audioList = audioList; // 当前列表
    playerState.isPlaying = false; // 切换后先标记为未播放
    playerState.currentTime = 0; // 重置播放位置
    playerState.duration = 0; // 重置时长（由 loadedmetadata 更新）

    if (openPanel && typeof onOpenPanel === 'function') onOpenPanel(); // 注入业务副作用

    const audio = resolveAudioEl(); // 当前 audio 元素
    if (!audio) return; // 无 DOM 不处理

    audio.pause(); // 切换音频前暂停
    audio.src = item?.audioUrl || ''; // 设置音频源
    audio.load(); // 触发加载
  };

  /**
   * 真正执行 play，并同步状态。
   * @param {HTMLAudioElement} audio
   * @returns {Promise<void>}
   */
  const playAudio = async (audio) => {
    safePauseAllVideos(); // 开始播放前暂停视频

    try {
      await audio.play(); // 触发播放
      playerState.isPlaying = true; // 同步 UI 状态
    } catch (e) {
      playerState.isPlaying = false; // 播放失败，保持非播放状态
    }
  };

  /**
   * readyState 达标时立即播放；否则等待 canplay 后播放。
   * @param {HTMLAudioElement} audio
   * @returns {Promise<void>}
   */
  const playAudioWhenReady = (audio) => {
    if (audio.readyState >= 2) return playAudio(audio); // HAVE_CURRENT_DATA

    return new Promise((resolve) => {
      const handleReady = () => {
        audio.removeEventListener('canplay', handleReady); // 防止重复触发
        void playAudio(audio).finally(resolve); // 兜底：最终都 resolve
      };

      audio.addEventListener('canplay', handleReady, {once: true}); // one-shot 监听
    });
  };

  /**
   * 切换播放/暂停（支持同一音频重复点击）。
   * @param {{item:any, index:number, audioList:any[], openPanel?: boolean}} param0
   * @returns {Promise<void>}
   */
  const togglePlay = async ({item, index, audioList, openPanel = true} = {}) => {
    if (!playerState) return; // 容错
    if (!item) return; // 无条目不处理

    const audio = resolveAudioEl(); // 当前音频 DOM
    if (!audio || !item.audioUrl) return; // 缺少 DOM 或音频源

    const isSame = playerState.currentAudio?.audioFileId === item.audioFileId; // 是否同一音频

    if (isSame) {
      if (playerState.isPlaying) {
        audio.pause(); // 同一音频正在播放：暂停
        playerState.isPlaying = false; // 同步 UI 状态
        return;
      }

      // 同一音频已暂停：继续播放
      // 为了保持与原实现一致：直接触发播放（不等待 canplay），避免 canplay 事件错过造成“无反应”。
      void playAudio(audio); // 恢复播放
      if (openPanel && typeof onOpenPanel === 'function') onOpenPanel(); // 可选：恢复时打开面板
      return;
    }

    // 不同音频：切换并开始播放
    setCurrentAudio(item, index, audioList, {openPanel}); // 更新状态 + 加载音频
    // 与原实现保持一致：不等待播放完成，避免点击链路被阻塞。
    void playAudioWhenReady(audio); // 播放新音频
  };

  /**
   * 播放上一首。
   * @returns {Promise<void>}
   */
  const playPrev = async () => {
    if (!playerState) return;
    // playerState 是 reactive 内部状态：audioList 是 array、currentIndex 是 number
    const list = playerState.audioList || [];
    if (!list.length) return;

    const idx = (playerState.currentIndex - 1 + list.length) % list.length;
    await togglePlay({item: list[idx], index: idx, audioList: list, openPanel: false});
  };

  /**
   * 播放下一首。
   * @returns {Promise<void>}
   */
  const playNext = async () => {
    if (!playerState) return;
    const list = playerState.audioList || [];
    if (!list.length) return;

    const idx = (playerState.currentIndex + 1) % list.length;
    await togglePlay({item: list[idx], index: idx, audioList: list, openPanel: false});
  };

  /**
   * 按比例跳转播放进度。
   * 直接从 audio DOM 读 duration——OPPO 等浏览器 loadedmetadata 不一定派发，
   * playerState.duration 可能还是 0；但只要音频已 readyState>=1，audio.duration 就是可用的。
   * @param {number} ratio 0..1
   * @returns {void}
   */
  const seekByRatio = (ratio) => {
    const audio = resolveAudioEl();
    if (!audio) return;

    const duration = audio.duration;
    if (!Number.isFinite(duration) || duration <= 0) return;

    const safeRatio = Math.max(0, Math.min(1, ratio));
    audio.currentTime = safeRatio * duration;
  };

  return {
    setCurrentAudio, // 切换当前音频（不一定自动播放）
    pauseAudio, // 暂停并更新状态
    togglePlay, // 同一/不同音频切换与播放
    playPrev, // 上一首
    playNext, // 下一首
    seekByRatio, // 进度跳转
  };
}

