import {ref, computed, onMounted, onBeforeUnmount, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import {shareData} from '@/api/share';
import {saveUserTracking} from '@/api/tracking.js';
import {formatDuration} from '@/utils/index.js';
import {shareUrl} from '@/utils/share.js';
import {useShareSeo, removeJsonLd} from '@/composables/useSeo.js';
import {useI18nText} from '@/i18n';

export function useSharePage() {
  const route = useRoute();
  const router = useRouter();
  const {t} = useI18nText();

  const shareId = route.query.shareId;

  const loading = ref(false);
  const shareInfo = ref(null);
  const videoRef = ref(null);

  // ── 音频播放状态（音乐分享）──
  const audioRef = ref(null);
  const isPlaying = ref(false);
  const currentTime = ref(0);
  const duration = ref(0);

  const progressPercent = computed(() => {
    if (!duration.value) return 0;
    return (currentTime.value / duration.value) * 100;
  });

  /** 统一歌词文本：将字面量换行转为真实换行 */
  const normalizedLyricText = computed(() => {
    let result = String(shareInfo.value?.lyrics || shareInfo.value?.prompt || '').replace(/\r\n/g, '\n');
    for (let i = 0; i < 8; i++) {
      const prev = result;
      result = result.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n');
      if (result === prev) break;
    }
    return result;
  });

  /** 移动端歌词原始行（按换行拆分） */
  const mobileLyricTexts = computed(() => {
    return normalizedLyricText.value
      .split(/\r?\n/)
      .map(line => line.trim())
      .filter(Boolean);
  });

  /** PC 端歌词容器引用 */
  const pcLyricListRef = ref(null);
  /** PC 端歌词列表 translateY（负数代表上移），CSS transition 自动平滑过渡 */
  const pcLyricTranslateY = ref(0);
  /** PC 端歌词拖拽中：拖拽时关掉 transition 跟手 */
  const isPcLyricDragging = ref(false);
  /** PC 端用户最近主动滚动过：true 时暂停自动跟随，空闲 3s 恢复 */
  const isPcLyricUserActive = ref(false);
  let pcLyricUserActiveTimer = null;
  let pcLyricDragStartY = 0;
  let pcLyricDragStartTranslate = 0;
  const PC_LYRIC_LINE_HEIGHT = 30;

  const getPcLyricMaxScroll = () => {
    const c = pcLyricListRef.value;
    if (!c) return 0;
    return Math.max(0, pcLyricLines.value.length * PC_LYRIC_LINE_HEIGHT - c.clientHeight);
  };

  const clampPcLyricTranslate = (v) => Math.max(-getPcLyricMaxScroll(), Math.min(0, v));

  const markPcLyricUserActive = () => {
    isPcLyricUserActive.value = true;
    if (pcLyricUserActiveTimer) clearTimeout(pcLyricUserActiveTimer);
    pcLyricUserActiveTimer = setTimeout(() => {
      isPcLyricUserActive.value = false;
      syncLyricTranslateByIndex();
    }, 3000);
  };

  const handlePcLyricWheel = (event) => {
    event.preventDefault();
    markPcLyricUserActive();
    pcLyricTranslateY.value = clampPcLyricTranslate(pcLyricTranslateY.value - event.deltaY);
  };

  const handlePcLyricMouseMove = (event) => {
    pcLyricTranslateY.value = clampPcLyricTranslate(pcLyricDragStartTranslate + event.clientY - pcLyricDragStartY);
  };

  const handlePcLyricMouseUp = () => {
    isPcLyricDragging.value = false;
    // 还原全局光标
    document.body.style.cursor = '';
    markPcLyricUserActive();
    document.removeEventListener('mousemove', handlePcLyricMouseMove);
    document.removeEventListener('mouseup', handlePcLyricMouseUp);
  };

  const handlePcLyricMouseDown = (event) => {
    event.preventDefault();
    isPcLyricDragging.value = true;
    pcLyricDragStartY = event.clientY;
    pcLyricDragStartTranslate = pcLyricTranslateY.value;
    // 拖拽期间鼠标可能移出容器边界，cursor 只设在元素上会丢——挂到 body 全局生效
    document.body.style.cursor = 'grabbing';
    document.addEventListener('mousemove', handlePcLyricMouseMove);
    document.addEventListener('mouseup', handlePcLyricMouseUp);
  };

  /** 去除行内时间戳 */
  const stripLyricTimestamp = (text) => String(text || '').replace(/\[(\d{1,2}:\d{1,2}(?:\.\d{1,3})?)\]/g, '').trim();

  /** 移动端歌词时间轴 */
  const mobileLyricTimeline = ref([]);
  /** 移动端当前高亮歌词索引 */
  const currentMobileLyricIndex = ref(0);
  /** 歌词高亮提前量：按时间戳对齐声音，不做提前 */
  const lyricLeadSeconds = ref(0);
  /** 歌词列表偏移量 */
  const mobileLyricTranslateY = ref(0);
  /** 歌词拖动中状态 */
  const isLyricDragging = ref(false);
  /** 歌词触摸起始位置 */
  const lyricDragStartY = ref(0);
  /** 歌词触摸起始偏移 */
  const lyricDragStartTranslateY = ref(0);
  /** 拖动结束后恢复自动跟随定时器 */
  let lyricDragResumeTimer = null;

  const MOBILE_LYRIC_LINE_HEIGHT = 36;
  const MOBILE_LYRIC_VISIBLE_LINES = 2;
  let lyricSyncRafId = 0;

  /** PC 端歌词文本（不显示时间戳） */
  const pcLyricLines = computed(() => mobileLyricTimeline.value.map(item => stripLyricTimestamp(item.text)).filter(Boolean));

  /**
   * 解析 LRC 时间字符串为秒
   * @param {string} timeStr - 例如 01:23.45
   * @returns {number|null}
   */
  const parseLyricTimeToSeconds = (timeStr) => {
    const [minPart, secPart] = String(timeStr).split(':');
    const minutes = Number(minPart);
    const seconds = Number(secPart);
    if (Number.isNaN(minutes) || Number.isNaN(seconds)) return null;
    return minutes * 60 + seconds;
  };

  /**
   * 从原始歌词中解析 LRC 时间轴
   * @param {string} rawLyrics - 原始歌词文本
   * @returns {Array<{time:number,text:string}>}
   */
  const parseLrcTimeline = (rawLyrics) => {
    if (!rawLyrics) return [];
    const result = [];
    const rows = rawLyrics.split(/\r?\n/);

    rows.forEach((row) => {
      const timeMatches = [...row.matchAll(/\[(\d{1,2}:\d{1,2}(?:\.\d{1,3})?)\]/g)];
      if (!timeMatches.length) return;

      const text = row.replace(/\[(\d{1,2}:\d{1,2}(?:\.\d{1,3})?)\]/g, '').trim();
      timeMatches.forEach((match) => {
        const parsedTime = parseLyricTimeToSeconds(match[1]);
        if (parsedTime !== null) {
          result.push({time: parsedTime, text});
        }
      });
    });

    return result.sort((a, b) => a.time - b.time);
  };

  /**
   * 在无 LRC 时间戳时，按总时长平均分配每句歌词时间
   * @param {string[]} texts - 歌词文本行
   * @param {number} totalDuration - 音频总时长（秒）
   * @returns {Array<{time:number,text:string}>}
   */
  const buildEstimatedTimeline = (texts, totalDuration) => {
    if (!texts.length) return [];
    const safeDuration = totalDuration > 0 ? totalDuration : texts.length * 3;
    const step = safeDuration / texts.length;
    return texts.map((text, index) => ({
      time: index * step,
      text,
    }));
  };

  /**
   * 生成移动端歌词时间轴，优先使用 LRC 时间戳
   * @returns {Array<{time:number,text:string}>}
   */
  const buildMobileLyricTimeline = () => {
    const lrcTimeline = parseLrcTimeline(normalizedLyricText.value);
    if (lrcTimeline.length) return lrcTimeline;
    return buildEstimatedTimeline(mobileLyricTexts.value, duration.value);
  };

  /**
   * 查找当前时间对应的歌词索引（二分查找）
   * @param {number} time 当前播放时间
   * @returns {number}
   */
  const findLyricIndexByTime = (time) => {
    const timeline = mobileLyricTimeline.value;
    if (!timeline.length) return 0;

    let left = 0;
    let right = timeline.length - 1;
    let answer = 0;

    while (left <= right) {
      const mid = Math.floor((left + right) / 2);
      if (timeline[mid].time <= time) {
        answer = mid;
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }

    return answer;
  };

  /**
   * 根据播放时间更新当前歌词索引
   * @param {number} time - 当前播放秒数
   */
  const updateCurrentLyricIndex = (time) => {
    const timeline = mobileLyricTimeline.value;
    if (!timeline.length) {
      currentMobileLyricIndex.value = 0;
      return;
    }

    const syncTime = Math.max(0, time + lyricLeadSeconds.value);
    currentMobileLyricIndex.value = findLyricIndexByTime(syncTime);
  };

  /** 启动歌词高频同步循环（提高对齐精度） */
  const startLyricSyncLoop = () => {
    if (lyricSyncRafId) return;

    const tick = () => {
      if (!audioRef.value || audioRef.value.paused || audioRef.value.ended) {
        lyricSyncRafId = 0;
        return;
      }
      currentTime.value = audioRef.value.currentTime;
      updateCurrentLyricIndex(currentTime.value);
      syncLyricTranslateByIndex();
      lyricSyncRafId = requestAnimationFrame(tick);
    };

    lyricSyncRafId = requestAnimationFrame(tick);
  };

  /** 停止歌词高频同步循环 */
  const stopLyricSyncLoop = () => {
    if (!lyricSyncRafId) return;
    cancelAnimationFrame(lyricSyncRafId);
    lyricSyncRafId = 0;
  };

  /**
   * 计算歌词滚动边界
   * @returns {{minY:number,maxY:number}}
   */
  const getLyricTranslateBoundary = () => {
    const maxY = 0;
    const minY = Math.min(
      0,
      -(mobileLyricTimeline.value.length * MOBILE_LYRIC_LINE_HEIGHT - MOBILE_LYRIC_VISIBLE_LINES * MOBILE_LYRIC_LINE_HEIGHT),
    );
    return {minY, maxY};
  };

  /**
   * 根据索引计算歌词容器偏移，使当前行尽量贴合第一行
   * @param {number} index - 歌词索引
   * @returns {number}
   */
  const getTranslateYByIndex = (index) => {
    const {minY, maxY} = getLyricTranslateBoundary();
    const targetY = -(index * MOBILE_LYRIC_LINE_HEIGHT);
    return Math.max(minY, Math.min(maxY, targetY));
  };

  /**
   * 根据当前索引同步歌词滚动位置
   * 业界最佳实践（QQ 音乐 / 网易云 / Apple Music web）：容器 overflow:hidden，内层用 transform
   * translateY 上移，CSS transition 自动做平滑过渡，不依赖浏览器 scrollTo smooth 黑盒。
   */
  const syncLyricTranslateByIndex = () => {
    if (isLyricDragging.value) return;
    mobileLyricTranslateY.value = getTranslateYByIndex(currentMobileLyricIndex.value);

    // 用户正在用滚轮 / 拖拽浏览歌词时不抢视图
    if (isPcLyricUserActive.value || isPcLyricDragging.value) return;

    const c = pcLyricListRef.value;
    if (!c) return;
    // 让当前行的中线靠近容器中线；首尾 clamp 到 [-(maxScroll), 0]，保证开头不出现上方空挡
    const center = currentMobileLyricIndex.value * PC_LYRIC_LINE_HEIGHT + PC_LYRIC_LINE_HEIGHT / 2;
    pcLyricTranslateY.value = clampPcLyricTranslate(c.clientHeight / 2 - center);
  };

  /**
   * 根据偏移量反推当前歌词索引
   * @param {number} translateY - 当前偏移量
   * @returns {number}
   */
  const getLyricIndexByTranslateY = (translateY) => {
    if (!mobileLyricTimeline.value.length) return 0;
    const rawIndex = Math.round(-translateY / MOBILE_LYRIC_LINE_HEIGHT);
    return Math.max(0, Math.min(mobileLyricTimeline.value.length - 1, rawIndex));
  };

  /**
   * 将歌词索引同步到音频时间
   * @param {number} index - 歌词索引
   */
  const syncAudioTimeByLyricIndex = (index) => {
    const line = mobileLyricTimeline.value[index];
    if (!line || !audioRef.value) return;
    audioRef.value.currentTime = Math.max(0, line.time);
    currentTime.value = audioRef.value.currentTime;
    currentMobileLyricIndex.value = index;
    syncLyricTranslateByIndex();
  };

  /**
   * 点击歌词行，强绑定跳转音频进度
   * @param {number} index - 点击的歌词索引
   */
  const handleLyricLineClick = (index) => {
    syncAudioTimeByLyricIndex(index);
  };

  /**
   * 歌词拖动开始
   * @param {TouchEvent} event - 触摸事件
   */
  const handleLyricTouchStart = (event) => {
    if (!mobileLyricTimeline.value.length) return;
    isLyricDragging.value = true;
    lyricDragStartY.value = event.touches[0].clientY;
    lyricDragStartTranslateY.value = mobileLyricTranslateY.value;
    if (lyricDragResumeTimer) {
      clearTimeout(lyricDragResumeTimer);
      lyricDragResumeTimer = null;
    }
  };

  /**
   * 歌词拖动中
   * @param {TouchEvent} event - 触摸事件
   */
  const handleLyricTouchMove = (event) => {
    if (!isLyricDragging.value) return;
    const deltaY = event.touches[0].clientY - lyricDragStartY.value;
    const {minY, maxY} = getLyricTranslateBoundary();
    const targetY = lyricDragStartTranslateY.value + deltaY;
    mobileLyricTranslateY.value = Math.max(minY, Math.min(maxY, targetY));
  };

  /**
   * 歌词拖动结束：歌词与音频进度强绑定
   */
  const handleLyricTouchEnd = () => {
    if (!isLyricDragging.value) return;
    isLyricDragging.value = false;

    const targetIndex = getLyricIndexByTranslateY(mobileLyricTranslateY.value);
    syncAudioTimeByLyricIndex(targetIndex);

    lyricDragResumeTimer = setTimeout(() => {
      syncLyricTranslateByIndex();
    }, 120);
  };

  /** 播放/暂停切换 */
  const toggleAudio = () => {
    if (!audioRef.value) return;
    if (isPlaying.value) {
      audioRef.value.pause();
    } else {
      audioRef.value.play();
    }
    isPlaying.value = !isPlaying.value;
  };

  /** 音频时间更新时同步进度和歌词 */
  const handleTimeUpdate = () => {
    if (!audioRef.value) return;
    currentTime.value = audioRef.value.currentTime;
    updateCurrentLyricIndex(currentTime.value);
  };

  /** 音频开始播放 */
  const handleAudioPlay = () => {
    isPlaying.value = true;
    startLyricSyncLoop();
  };

  /** 音频暂停 */
  const handleAudioPause = () => {
    isPlaying.value = false;
    stopLyricSyncLoop();
  };

  /** 音频结束 */
  const handleAudioEnded = () => {
    isPlaying.value = false;
    stopLyricSyncLoop();
  };

  /** 音频元数据加载后更新总时长并重建歌词时间轴 */
  const handleLoadedMetadata = () => {
    if (!audioRef.value) return;
    duration.value = audioRef.value.duration;
    mobileLyricTimeline.value = buildMobileLyricTimeline();
    updateCurrentLyricIndex(currentTime.value);
    syncLyricTranslateByIndex();
  };

  /** 点击进度条跳转 */
  const handleSeek = (e) => {
    if (!audioRef.value || !duration.value) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const ratio = (e.clientX - rect.left) / rect.width;
    audioRef.value.currentTime = ratio * duration.value;
    currentTime.value = audioRef.value.currentTime;
    updateCurrentLyricIndex(currentTime.value);
    syncLyricTranslateByIndex();
  };

  /** 根据 shareId 获取分享数据 */
  const loadShareData = async () => {
    if (!shareId) return;
    loading.value = true;
    try {
      shareInfo.value = await shareData({shareId});
      if (shareInfo.value) {
        useShareSeo({...shareInfo.value, shareId});
      }
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || t('share.loadFail'));
    } finally {
      loading.value = false;
    }
  };

  /** 复制提示词 */
  const handleCopyPrompt = async () => {
    if (!shareInfo.value?.prompt) return;
    try {
      await navigator.clipboard.writeText(shareInfo.value.prompt);
      ElMessage.success(t('share.copyPromptSuccess'));
    } catch {
      ElMessage.error(t('share.copyFailed'));
    }
  };

  /** 跳转创作页：先打点（查看创作过程） */
  const handleGoCreate = () => {
    if (!shareInfo.value?.sessionId) return;
    saveUserTracking({target: 'HOME_SHARE_VIEW_PROCESS'}).catch((error) => {
      console.error('分享页查看创作过程埋点上报失败:', error);
    });
    const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
    router.push({
      name: 'creation',
      params: {lang},
      query: {
        sessionId: shareInfo.value.sessionId,
        creatorUserId: shareInfo.value.userId,
      },
    });
  };

  /** 关闭分享页：直接跳首页（按当前语言前缀） */
  const handleClose = () => {
    const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
    router.push({name: 'home', params: {lang}});
  };

  /** 复制当前分享链接（分享页"分享"按钮）：当前页 URL 已含 shareId，无需再调 shareLink 接口。 */
  const handleShareLink = async () => {
    saveUserTracking({target: 'HOME_SHARE_SHARE'}).catch((error) => {
      console.error('分享页分享按钮埋点上报失败:', error);
    });
    const mode = await shareUrl(location.href);
    if (mode === 'native' || mode === 'cancelled') return;
    if (mode === 'clipboard') ElMessage.success(t('share.shareLinkCopied'));
    else ElMessage.error(t('share.copyFailed'));
  };

  watch(
    () => shareInfo.value,
    () => {
      mobileLyricTimeline.value = buildMobileLyricTimeline();
      updateCurrentLyricIndex(currentTime.value);
      syncLyricTranslateByIndex();
    },
    {immediate: true},
  );

  watch(normalizedLyricText, () => {
    mobileLyricTimeline.value = buildMobileLyricTimeline();
    updateCurrentLyricIndex(currentTime.value);
    syncLyricTranslateByIndex();
  });

  watch(currentMobileLyricIndex, () => {
    syncLyricTranslateByIndex();
  });

  onMounted(() => loadShareData());

  onBeforeUnmount(() => {
    if (lyricDragResumeTimer) {
      clearTimeout(lyricDragResumeTimer);
      lyricDragResumeTimer = null;
    }
    if (pcLyricUserActiveTimer) {
      clearTimeout(pcLyricUserActiveTimer);
      pcLyricUserActiveTimer = null;
    }
    document.removeEventListener('mousemove', handlePcLyricMouseMove);
    document.removeEventListener('mouseup', handlePcLyricMouseUp);
    // 若拖拽过程中页面被卸载，确保 body cursor 还原
    document.body.style.cursor = '';
    stopLyricSyncLoop();
    if (audioRef.value) audioRef.value.pause();
    if (videoRef.value) videoRef.value.pause();
    removeJsonLd('share-content');
  });

  return {
    t,
    route,
    router,
    shareId,
    loading,
    shareInfo,
    videoRef,
    audioRef,
    isPlaying,
    currentTime,
    duration,
    progressPercent,
    pcLyricListRef,
    pcLyricLines,
    pcLyricTranslateY,
    isPcLyricDragging,
    handlePcLyricWheel,
    handlePcLyricMouseDown,
    mobileLyricTimeline,
    currentMobileLyricIndex,
    mobileLyricTranslateY,
    isLyricDragging,
    formatDuration,
    loadShareData,
    handleCopyPrompt,
    handleGoCreate,
    handleShareLink,
    handleClose,
    toggleAudio,
    handleTimeUpdate,
    handleLoadedMetadata,
    handleAudioPlay,
    handleAudioPause,
    handleAudioEnded,
    handleSeek,
    handleLyricTouchStart,
    handleLyricTouchMove,
    handleLyricTouchEnd,
    handleLyricLineClick,
  };
}

