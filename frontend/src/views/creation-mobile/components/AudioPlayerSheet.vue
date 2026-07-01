<template>
  <div v-if="visible" class="mobile-audio-player fixed inset-0 z-[60] flex flex-col text-white">
    <div class="mobile-audio-player__bg"></div>

    <!-- 顶部栏：返回当前创作页、显示会话标题、右侧分享 -->
    <div class="relative z-[1] flex items-center gap-[10px] px-[14px] pt-[14px]">
      <button type="button" @click="emit('close')">
        <svg-icon class="rotate-180" name="gy-enter" size="16" color="#FFFFFF"></svg-icon>
      </button>
      <div class="flex-1 truncate text-[16px] text-white">{{t('creation.musicPlay')}}</div>
      <button
          v-if="playerState?.currentAudio?.projectId"
          type="button"
          :title="t('common.share')"
          @click="handleShare"
      >
        <svg-icon name="gy-share" size="18" color="#FFFFFF"></svg-icon>
      </button>
    </div>

    <!-- 主体内容：封面、歌曲信息、进度条、歌词 -->
    <div class="relative z-[1] flex-1 overflow-y-auto px-[14px] pb-[14px] pt-[14px]">
      <div class="flex items-start gap-[14px]">
        <!-- 封面区：点击封面可切换播放/暂停 -->
        <button
            type="button"
            class="relative h-[100px] w-[100px] shrink-0 overflow-hidden rounded-[10px] bg-white/8"
            @click="emit('toggle-play')"
        >
          <img v-if="playerState.currentAudio?.coverUrl" :src="playerState.currentAudio.coverUrl" class="h-full w-full object-cover" alt="cover"/>
          <div v-else class="flex h-full w-full items-center justify-center">
            <svg-icon name="gy-audio" size="24" color="#C2FF00"></svg-icon>
          </div>
          <div class="absolute inset-0 flex items-center justify-center">
            <svg-icon :name="playerState.isPlaying ? 'gy-pause' : 'gy-play'" size="23" color="#C2FF00"></svg-icon>
          </div>
        </button>

        <!-- 右侧信息区：标题、创作者、进度条与时间 -->
        <div class="flex h-[100px] min-w-0 flex-1 flex-col justify-between pt-[2px]">
          <div>
            <div class="line-clamp-2 text-[17px] font-semibold leading-[24px]">{{ currentTitle }}</div>
          </div>

          <div class="rounded-none px-0 py-0 select-none">
            <!--
              用 touch + mouse 双轨事件处理点击和拖拽，避免：
              - opacity:0 的 <input type="range"> 在 OPPO / UC / 夸克等浏览器渲染不一致或不派发触摸事件
              - Pointer Events 在 OPPO 老 WebView 上派发不稳定
              Touch / Mouse 事件从 iOS3 / Android4 时代就稳定支持，是兼容性最高的方案。
            -->
            <div
              ref="trackRef"
              class="relative h-7 flex items-center cursor-pointer touch-none"
              @touchstart.prevent="handleProgressTouchStart"
              @mousedown="handleProgressMouseDown"
            >
              <div class="relative w-full h-1 rounded-full bg-[#5D634A] overflow-hidden pointer-events-none">
                <div
                  class="absolute left-0 top-0 h-full rounded-full bg-[#C2FF00] pointer-events-none"
                  :style="{width: `${progressPercent}%`}"
                ></div>
              </div>
              <div
                class="absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-white shadow-[0_0_4px_rgba(0,0,0,0.4)] pointer-events-none"
                :style="{left: `calc(${progressPercent}% - 6px)`}"
              ></div>
            </div>
            <div class="mt-1 flex items-center justify-between text-[11px] leading-4 text-white/88">
              <span>{{ formatDuration(playerState.currentTime) }}</span>
              <span>{{ formatDuration(playerState.duration) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 曲风提示词（与 PC 对齐：展示+复制） -->
      <div class="mt-[16px]">
        <div class="flex items-center justify-between mb-[8px]">
          <div class="text-[12px] font-semibold leading-[17px] text-white/85">{{ t('creation.messageArea.audioStyleTitle') }}</div>
          <button
            v-if="stylePromptText"
            type="button"
            class="w-[28px] h-[28px] flex-center text-white/70 hover:text-white transition-colors"
            aria-label="copy-style"
            @click="handleCopyStylePrompt"
          >
            <svg-icon name="gy-copy" size="14" color="currentColor"></svg-icon>
          </button>
        </div>

        <div class="rounded-[10px] bg-[rgba(90,96,74,0.5)] text-[#EDEFE6] overflow-hidden px-[12px] py-[10px]">
          <div v-if="!stylePromptText" class="text-white/70 text-[12px] leading-[17px]">{{ t('creation.messageArea.noAudioStyle') }}</div>
          <div v-else>
            <div class="text-[12px] leading-[17px] whitespace-pre-wrap">
              {{ stylePromptText }}
            </div>
          </div>
        </div>
      </div>

      <div class="mt-[18px]">
        <div class="flex items-center justify-between mb-[8px]">
          <div class="text-[12px] font-semibold leading-[17px] text-white/85">{{ t('creation.messageArea.lyricsTitle') }}</div>
          <button
            v-if="lyricsText && lyricsText !== t('creation.messageArea.noLyrics')"
            type="button"
            class="w-[28px] h-[28px] flex-center text-white/70 hover:text-white transition-colors"
            aria-label="copy-lyrics"
            @click="handleCopyLyrics"
          >
            <svg-icon name="gy-copy" size="14" color="currentColor"></svg-icon>
          </button>
        </div>
        <div class="text-[12px] leading-[17px] text-white whitespace-pre-line">
          {{ lyricsText }}
        </div>
      </div>
    </div>

    <!-- 底部操作区：制作 MV 与下载 -->
    <div class="relative z-[1] shrink-0 px-[20px] pb-[18px] pt-[10px]">
      <div class="flex-between gap-[15px]">
        <button
            type="button"
            class="h-[40px] w-[160px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-[15px] text-[#192100]"
            :disabled="!canEditCreation"
            :class="{'opacity-55': !canEditCreation}"
            @click="emit('prepare-mv')"
        >
          <svg-icon name="gy-MV" size="15" color="#231815"></svg-icon>
          <span class="ml-[6px]">制作MV</span>
        </button>
        <button
            type="button"
            class="h-[40px] w-[160px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-[15px] text-[#192100]"
            @click="emit('download')"
        >
          <svg-icon name="gy-download" size="15" color="#231815"></svg-icon>
          <span class="ml-[6px]">{{ t('resource.actions.download') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import {useI18nText} from '@/i18n';
import {formatDuration} from '@/utils/index.js';
import {shareByProjectId} from '@/utils/share.js';

/**
 * 移动端音频播放器弹层：
 * - 展示当前选中音频的封面、标题、歌词和播放进度
 * - 提供播放/暂停、进度跳转、制作 MV、分享等交互入口
 * - 具体播放控制逻辑由父组件统一管理
 */
const props = defineProps({
  /** 是否显示播放器面板。 */
  visible: {type: Boolean, default: false},
  /** 当前是否允许继续编辑/制作 MV。 */
  canEditCreation: {type: Boolean, default: false},
  /** 共享音频播放器状态。 */
  playerState: {type: Object, required: true},
});

/**
 * 对外事件：
 * - `close` 关闭弹层
 * - `toggle-play` 切换播放状态
 * - `prepare-mv` 使用当前音频制作 MV
 * - `download` 下载当前音频
 * - `seek` 根据进度条位置跳转
 */
const emit = defineEmits(['close', 'toggle-play', 'prepare-mv', 'download', 'seek']);

const {t} = useI18nText();
const router = useRouter();

/** 分享当前音频：projectId 由后端在 AUDIO chunk 数据中提供。 */
const handleShare = async () => {
  const {mode, error} = await shareByProjectId({
    projectId: props.playerState?.currentAudio?.projectId,
    langPrefix: router.currentRoute.value?.params?.lang || 'zh',
    trackingTarget: 'CREATE_MUSIC_SHARE',
  });
  if (mode === 'native' || mode === 'cancelled') return;
  if (mode === 'clipboard') ElMessage.success(t('resource.shareSuccess'));
  else ElMessage.error(error?.message || t('resource.shareFailed'));
};

/** 当前音频标题，缺省时回退到占位文案。 */
const currentTitle = computed(() => props.playerState?.currentAudio?.title || '歌曲名称');
/** 当前歌词内容，缺省时展示占位文案。 */
const lyricsText = computed(() => props.playerState?.currentAudio?.lyrics || '暂无歌词');
/** 当前曲风提示词，缺省时展示空字符串 */
const stylePromptText = computed(() => props.playerState?.currentAudio?.style || '');

/**
 * 复制文本到剪贴板，并做轻提示。
 * @param {string} text
 * @param {string} label
 */
const handleCopyText = async (text, label) => {
  // text 来自 lyricsText / stylePromptText computed，已确保 string
  const content = (text || '').trim();
  if (!content) return;
  try {
    await navigator.clipboard.writeText(content);
    ElMessage.success(t('creation.messageArea.copiedWithLabel', {label}));
  } catch {
    ElMessage.error(t('share.copyFailed'));
  }
};

const handleCopyStylePrompt = () => handleCopyText(stylePromptText.value, t('creation.messageArea.audioStyleTitle'));
const handleCopyLyrics = () => handleCopyText(lyricsText.value, t('creation.messageArea.lyricsTitle'));

/** 播放进度（0~100），用于进度条显示。 */
const progressPercent = computed(() => {
  const { duration, currentTime } = props.playerState || {};
  if (!duration || duration <= 0) return 0;
  return Math.min(100, Math.max(0, (currentTime / duration) * 100));
});

const trackRef = ref(null);

/** 把 pointer 的 clientX 映射成 [0, 1] 的进度比例。 */
const computeProgressRatio = (clientX) => {
  const el = trackRef.value;
  if (!el) return 0;
  const rect = el.getBoundingClientRect();
  if (rect.width <= 0) return 0;
  return Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
};

/** 触摸：起按即跳转，按住继续监听 touchmove 实现拖拽。 */
const handleProgressTouchStart = (event) => {
  const touch = event.touches[0];
  if (!touch) return;
  emit('seek', computeProgressRatio(touch.clientX));
  const onMove = (e) => {
    const t = e.touches[0];
    if (t) emit('seek', computeProgressRatio(t.clientX));
  };
  const onEnd = () => {
    window.removeEventListener('touchmove', onMove);
    window.removeEventListener('touchend', onEnd);
    window.removeEventListener('touchcancel', onEnd);
  };
  window.addEventListener('touchmove', onMove, {passive: false});
  window.addEventListener('touchend', onEnd);
  window.addEventListener('touchcancel', onEnd);
};

/** 鼠标：桌面端 / 部分浏览器在 mouse 通道上派发。 */
const handleProgressMouseDown = (event) => {
  emit('seek', computeProgressRatio(event.clientX));
  const onMove = (e) => emit('seek', computeProgressRatio(e.clientX));
  const onUp = () => {
    window.removeEventListener('mousemove', onMove);
    window.removeEventListener('mouseup', onUp);
  };
  window.addEventListener('mousemove', onMove);
  window.addEventListener('mouseup', onUp);
};
</script>

<style scoped lang="scss">
.mobile-audio-player {
  background: radial-gradient(120% 72% at 24% 70%, rgba(184, 255, 0, 0.3) 0%, rgba(184, 255, 0, 0.08) 28%, rgba(0, 0, 0, 0) 56%),
  linear-gradient(90deg, #0b1206 0%, #0b1206 45%, #1d2b12 100%);

  &__bg {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(6, 10, 4, 0.82) 0%, rgba(9, 14, 6, 0.3) 38%, rgba(6, 10, 4, 0.92) 100%);
    pointer-events: none;
  }
}
</style>
