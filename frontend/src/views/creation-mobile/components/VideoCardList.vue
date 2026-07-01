<template>
  <div v-if="currentVideo" class="mt-3 mb-4">
    <div class="relative overflow-hidden rounded-[5px] bg-black">
      <!-- 视频本体：16:9 容器，使用 object-cover 避免黑边 -->
      <video
        v-if="currentVideo.videoUrl"
        :ref="(el) => setVideoRef(currentVideo, el)"
        :src="currentVideo.videoUrl"
        :poster="currentVideo.coverUrl || undefined"
        preload="metadata"
        playsinline
        webkit-playsinline
        x5-playsinline="true"
        x5-video-player-type="h5-page"
        x5-video-player-fullscreen="false"
        x5-video-orientation="portraint"
        t7-video-player-type="inline"
        disablepictureinpicture
        disableremoteplayback
        controlslist="nodownload nofullscreen noremoteplayback"
        class="aspect-video w-full"
        @loadedmetadata="cacheDuration(currentVideo, $event)"
        @timeupdate="syncProgress(currentVideo, $event)"
        @play="handleVideoPlay(currentVideo)"
        @pause="handleVideoPause(currentVideo)"
        @ended="handleVideoEnded(currentVideo)"
        @click.stop="handleVideoSurfaceTap(currentVideo)"
      ></video>
      <div v-else class="aspect-video w-full bg-black/40"></div>

      <!-- 覆盖层：顶部下载按钮、中间播放按钮、底部时间/进度/全屏控件 -->
      <div
        class="absolute inset-0 z-[1] flex flex-col justify-between p-[12px]"
        :class="shouldShowControls(currentVideo) ? 'bg-[linear-gradient(180deg,rgba(0,0,0,0)_0%,rgba(0,0,0,0.08)_52%,rgba(0,0,0,0.42)_100%)]' : ''"
        @click="handleVideoSurfaceTap(currentVideo)"
      >
        <div v-if="shouldShowControls(currentVideo)" class="flex justify-end items-center gap-[14px]">
          <button
            v-if="currentVideo.projectId"
            type="button"
            :title="t('common.share')"
            @click.stop="handleShare(currentVideo)"
          >
            <svg-icon name="gy-share" size="16" color="#C2FF00"></svg-icon>
          </button>
          <button
            type="button"
            :title="t('creation.messageArea.downloadVideoTitle')"
            @click.stop="handleDownload(currentVideo)"
          >
            <svg-icon name="gy-download" size="16" color="#C2FF00"></svg-icon>
          </button>
        </div>

        <div v-if="shouldShowControls(currentVideo)" class="flex flex-1 items-center justify-center">
          <button type="button" class="flex h-[54px] w-[54px] items-center justify-center rounded-full" @click.stop="togglePlay(currentVideo)">
            <svg-icon :name="isVideoPlaying(currentVideo) ? 'gy-pause' : 'gy-play2'" size="32" color="#C2FF00"></svg-icon>
          </button>
        </div>
        <div v-else class="flex-1"></div>

        <div v-if="shouldShowControls(currentVideo)" class="flex items-center gap-[8px] text-[10px] leading-[14px] text-white">
          <span class="shrink-0">{{ formatDuration(getCurrentTime(currentVideo)) }}</span>
          <button class="min-w-0 flex-1" type="button" @click.stop="seekVideo(currentVideo, $event)">
            <div class="h-[4px] rounded-full bg-[rgba(255,255,255,0.28)]">
              <div class="h-full rounded-full bg-[#C2FF00]" :style="{width: `${getProgressPercent(currentVideo)}%`}"></div>
            </div>
          </button>
          <span class="shrink-0">{{ getVideoDuration(currentVideo) }}</span>
          <button
            type="button"
            class="flex h-[18px] w-[18px] shrink-0 items-center justify-center text-white/92"
            @click.stop="handleFullscreen(currentVideo)"
          >
            <svg-icon name="gy-enlarge" size="13"></svg-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, reactive} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import {formatDuration} from '@/utils/index.js';
import {shareByProjectId} from '@/utils/share.js';
import {saveUserTracking} from '@/api/tracking.js';
import {useI18nText} from '@/i18n';

/**
 * 移动端视频卡片：
 * - 默认只渲染当前消息下的第一条视频
 * - 支持卡片内直接播放/暂停
 * - 支持真实进度展示、点击进度条跳转、下载和全屏
 */
const props = defineProps({
  /** 当前消息下的视频列表，通常只取第一条展示。 */
  videoList: {type: Array, default: () => []},
});

const {t} = useI18nText();
const router = useRouter();

/** 当前实际用于渲染的视频对象。 */
const currentVideo = computed(() => props.videoList?.[0] || null);

/** 缓存视频 DOM，便于后续播放控制与全屏调用。 */
const videoElMap = new Map();
/** 记录视频总时长。 */
const durationMap = reactive({});
/** 记录视频当前播放时间。 */
const currentTimeMap = reactive({});
/** 记录视频当前是否处于播放态。 */
const playingMap = reactive({});
/** 记录视频控件是否可见（播放中会自动隐藏）。 */
const controlsVisibleMap = reactive({});
const controlsTimerMap = new Map();
const AUTO_HIDE_CONTROLS_MS = 2000;

/**
 * 为每个视频生成稳定 key。VIDEO 字段 videoFileId / videoUrl 后端皆 string。
 * @param {any} video
 * @returns {string}
 */
const getVideoKey = (video) => video?.videoFileId || video?.videoUrl || '';

/**
 * 注册/清理视频元素引用。
 * @param {any} video
 * @param {HTMLVideoElement|null} el
 * @returns {void}
 */
const setVideoRef = (video, el) => {
  const key = getVideoKey(video);
  if (!key) return;
  if (!el) {
    videoElMap.delete(key);
    clearAutoHideControls(video);
    return;
  }
  videoElMap.set(key, el);
};

const setControlsVisible = (video, visible) => {
  const key = getVideoKey(video);
  if (!key) return;
  controlsVisibleMap[key] = visible;
};

const clearAutoHideControls = (video) => {
  const key = getVideoKey(video);
  if (!key) return;
  const timer = controlsTimerMap.get(key);
  if (timer) {
    clearTimeout(timer);
    controlsTimerMap.delete(key);
  }
};

const scheduleAutoHideControls = (video) => {
  const key = getVideoKey(video);
  if (!key) return;
  clearAutoHideControls(video);
  const timer = setTimeout(() => {
    controlsVisibleMap[key] = false;
    controlsTimerMap.delete(key);
  }, AUTO_HIDE_CONTROLS_MS);
  controlsTimerMap.set(key, timer);
};

const shouldShowControls = (video) => {
  const key = getVideoKey(video);
  return controlsVisibleMap[key] !== false;
};

/**
 * 缓存视频总时长，供底部时间与进度条使用。
 * @param {any} video
 * @param {Event} event
 * @returns {void}
 */
const cacheDuration = (video, event) => {
  const key = getVideoKey(video);
  if (!key) return;
  // HTMLMediaElement.duration 元数据未就绪时可能 NaN，|| 0 兜底
  const duration = event?.target?.duration || 0;
  if (duration > 0) {
    durationMap[key] = duration;
    // 初始暂停态也遵循“2 秒显示后自动隐藏控件”的规则
    setControlsVisible(video, true);
    scheduleAutoHideControls(video);
  }
};

/** @returns {string} 当前视频总时长的格式化文本。 */
const getVideoDuration = (video) => {
  const key = getVideoKey(video);
  return formatDuration(durationMap[key] || 0);
};

/** @returns {number} 当前视频已播放时间（秒）。 */
const getCurrentTime = (video) => {
  const key = getVideoKey(video);
  return currentTimeMap[key] || 0;
};

/** @returns {number} 当前视频播放进度百分比（0~100）。 */
const getProgressPercent = (video) => {
  const key = getVideoKey(video);
  const duration = durationMap[key] || 0;
  if (duration <= 0) return 0;
  return Math.max(0, Math.min(100, ((currentTimeMap[key] || 0) / duration) * 100));
};

/** @returns {boolean} 当前视频是否处于播放中。 */
const isVideoPlaying = (video) => {
  const key = getVideoKey(video);
  return Boolean(playingMap[key]);
};

/**
 * 暂停除当前视频之外的其它视频，避免多个视频同时播放。
 * @param {string} currentKey
 * @returns {void}
 */
const pauseOtherVideos = (currentKey) => {
  videoElMap.forEach((el, key) => {
    if (key !== currentKey && el && !el.paused) el.pause();
  });
};

/**
 * 切换当前视频播放/暂停状态。
 * @param {any} video
 * @returns {Promise<void>}
 */
const togglePlay = async (video) => {
  const key = getVideoKey(video);
  const el = videoElMap.get(key);
  if (!el) return;
  if (el.paused) {
    pauseOtherVideos(key);
    try {
      await el.play();
    } catch {
      ElMessage.warning(t('creation.messageArea.noVideo'));
    }
    return;
  }
  el.pause();
};

const handleVideoSurfaceTap = (video) => {
  setControlsVisible(video, true);
  scheduleAutoHideControls(video);
};

/**
 * 同步视频当前播放时间。
 * @param {any} video
 * @param {Event} event
 * @returns {void}
 */
const syncProgress = (video, event) => {
  const key = getVideoKey(video);
  if (!key) return;
  // HTMLMediaElement.currentTime 是 number；|| 0 兜底极端 NaN
  currentTimeMap[key] = event?.target?.currentTime || 0;
};

/**
 * 点击进度条后按比例跳转当前视频。
 * @param {any} video
 * @param {MouseEvent} event
 * @returns {void}
 */
const seekVideo = (video, event) => {
  const key = getVideoKey(video);
  const el = videoElMap.get(key);
  const duration = durationMap[key] || 0;
  if (!el || duration <= 0) return;
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = (event.clientX - rect.left) / rect.width;
  el.currentTime = Math.max(0, Math.min(duration, duration * Math.max(0, Math.min(1, ratio))));
};

/**
 * 尝试让当前视频进入全屏。
 * 优先使用标准全屏 API，其次兼容 iOS 的 `webkitEnterFullscreen`。
 * @param {any} video
 * @returns {Promise<void>}
 */
const handleFullscreen = async (video) => {
  const key = getVideoKey(video);
  const el = videoElMap.get(key);
  if (!el) return;
  if (typeof el.requestFullscreen === 'function') {
    await el.requestFullscreen();
    return;
  }
  if (typeof el.webkitEnterFullscreen === 'function') {
    el.webkitEnterFullscreen();
  }
};

/** 视频开始播放时同步播放状态，并暂停其它视频。 */
const handleVideoPlay = (video) => {
  const key = getVideoKey(video);
  if (!key) return;
  pauseOtherVideos(key);
  playingMap[key] = true;
  setControlsVisible(video, true);
  scheduleAutoHideControls(video);
};

/** 视频暂停时同步播放状态。 */
const handleVideoPause = (video) => {
  const key = getVideoKey(video);
  if (!key) return;
  playingMap[key] = false;
  setControlsVisible(video, true);
  scheduleAutoHideControls(video);
};

/** 视频播放结束后重置播放态与播放时间。 */
const handleVideoEnded = (video) => {
  const key = getVideoKey(video);
  if (!key) return;
  playingMap[key] = false;
  currentTimeMap[key] = 0;
  setControlsVisible(video, true);
  scheduleAutoHideControls(video);
};

/** 分享当前视频：projectId 由后端在 VIDEO chunk 数据中提供。 */
const handleShare = async (video) => {
  const {mode, error} = await shareByProjectId({
    projectId: video?.projectId,
    langPrefix: router.currentRoute.value?.params?.lang || 'zh',
    trackingTarget: 'CREATE_MV_SHARE',
  });
  if (mode === 'native' || mode === 'cancelled') return;
  if (mode === 'clipboard') ElMessage.success(t('resource.shareSuccess'));
  else ElMessage.error(error?.message || t('resource.shareFailed'));
};

/**
 * 下载当前视频，并复用现有下载埋点。
 * @param {any} video
 * @returns {void}
 */
const handleDownload = (video) => {
  // VIDEO 字段：videoUrl / videoFileId 后端皆 string
  const url = video?.videoUrl;
  if (!url) {
    ElMessage.warning(t('creation.messageArea.noVideo'));
    return;
  }

  saveUserTracking({
    target: 'CREATE_MV_DOWNLOAD_VIDEO',
  }).catch((error) => {
    console.error('创作页移动端视频下载埋点上报失败:', error);
  });

  const link = document.createElement('a');
  link.href = url;
  link.download = video?.videoFileId || 'mv_video';
  link.target = '_blank';
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

onBeforeUnmount(() => {
  controlsTimerMap.forEach((timer) => clearTimeout(timer));
  controlsTimerMap.clear();
});
</script>

