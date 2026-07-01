<template>
  <el-dialog
      v-model="visible"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      append-to-body
      align-center
      class="mobile-music-trimmer-dialog"
      @close="handleCancel"
      width="335px"
  >
    <div class="bg-[#1A1A1A] rounded-[12px] p-3 border border-white/10 shadow-2xl overflow-visible">
      <!-- 波形轨道 -->
      <div
          ref="waveContainerRef"
          class="music-trimmer-waveform flex items-center h-[72px] bg-[#2B2B2B] rounded-[10px] overflow-visible mb-2 select-none"
      >
        <!-- 播放按钮 -->
        <button
            class="w-8 h-8 flex-center text-white hover:text-[#C2FF00] transition-colors cursor-pointer outline-none focus:outline-none focus-visible:outline-none focus-visible:ring-0"
            @click="togglePreview"
            :disabled="!previewUrl"
            :class="!previewUrl ? 'opacity-40 cursor-not-allowed' : ''"
        >
          <svg-icon :name="isPlaying ? 'gy-pause' : 'gy-play2'" size="20" color="currentColor"/>
        </button>

        <div
            ref="trackRef"
            class="relative h-full flex-1 mx-2"
        >
          <!-- 真实波形 -->
          <div class="absolute inset-0 flex items-center justify-between pointer-events-none gap-[1px]">
            <div
                v-for="(h, i) in waveHeights"
                :key="i"
                class="flex-1 rounded-full min-w-[1px] transition-colors duration-75"
                :style="{
                  height: Math.max(4, h) + '%',
                  background: getWaveBarColor(i),
                  opacity: getWaveBarOpacity(i),
                }"
            ></div>
          </div>

          <audio
              ref="previewAudioRef"
              :src="previewUrl"
              preload="metadata"
              @loadedmetadata="onAudioLoadedMetadata"
              @timeupdate="onTimeUpdate"
              @ended="onAudioEnded"
          ></audio>

          <!-- 选区/手柄轨道 -->
          <div class="absolute inset-0 z-10">
            <!-- 选区整体拖拽层 -->
            <div
                class="absolute top-0 bottom-0 z-[12] bg-[#C2FF00]/10 border border-[#C2FF00]"
                :class="(isRangeDragging || isSeekingPlayhead) ? 'shadow-[0_0_0_1px_rgba(194,255,0,0.6),0_0_10px_rgba(194,255,0,0.25)]' : ''"
                :style="trimRangeStyle"
                @touchstart.stop="startDragRange"
            ></div>

            <!-- 播放指针 -->
            <div
                v-if="showPlayhead"
                class="absolute top-0 bottom-0 z-20 w-[2px] bg-[#C2FF00] shadow-[0_0_8px_rgba(194,255,0,0.8)]"
                :style="{ left: `calc(${playheadPercent}% - 1px)` }"
                @touchstart.stop="startSeekPlayhead"
            >
              <div
                  class="absolute -top-[6px] left-1/2 -translate-x-1/2 w-0 h-0 border-l-[5px] border-r-[5px] border-t-[6px] border-l-transparent border-r-transparent border-t-[#C2FF00]"
              ></div>
              <div
                  class="absolute -top-[8px] left-[6px] z-30 px-[5px] py-0 rounded-full bg-[#5A5A5A]/80 border border-white/20 text-white text-[10px] leading-[14px] font-mono whitespace-nowrap"
              >
                {{ formatDuration(clampedPreviewTime) }}
              </div>
            </div>

            <div
                class="absolute top-0 bottom-0 w-[12px] bg-[#C2FF00] rounded-l-[10px] flex-center"
                :style="{ left: `calc(${startPercent}% - 12px)` }"
                @touchstart.stop="startDrag('start')"
            >
              <div class="w-[1.5px] h-[28px] bg-black/60 rounded-full"></div>
            </div>

            <div
                class="absolute top-0 bottom-0 w-[12px] bg-[#C2FF00] rounded-r-[10px] flex-center"
                :style="{ left: `${endPercent}%` }"
                @touchstart.stop="startDrag('end')"
            >
              <div class="w-[1.5px] h-[28px] bg-black/60 rounded-full"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 时长信息 -->
      <div class="flex items-center justify-between">
        <div class="text-[#C2FF00] text-[13px] font-medium">
          {{ t('creation.musicTrimmer.selectedDuration') }}{{ formatDuration(displaySelectedDuration) }}
        </div>
        <div class="bg-[#2B2B2B] rounded-[8px] px-2 py-[2px] border border-white/10">
            <span class="text-[#C2FF00] text-[13px] font-mono">
              {{ formatDuration(displayRange.start) }}-{{ formatDuration(displayRange.end) }}
            </span>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="flex items-center shrink-0">
        <el-button size="small" @click="handleCancel">{{ t('common.cancel') }}</el-button>
        <el-button
            size="small"
            type="primary"
            :loading="isProcessing"
            :disabled="props.preparing"
            @click="handleConfirm"
            class="!bg-[#C2FF00] !text-black !border-none"
        >
          <span v-if="isProcessing">{{ t('creation.musicTrimmer.processing') }}</span>
          <span v-else>{{ t('creation.musicTrimmer.confirmGenerate') }}</span>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {ref, computed, onBeforeUnmount, watch, nextTick} from 'vue';
import {trimAudio} from '@/utils/audio';
import {ElMessage, ElLoading} from 'element-plus';
import svgIcon from '@/components/SvgIcon.vue';
import {useI18nText} from '@/i18n';
import {isEmpty} from '@/utils';

const props = defineProps({
  modelValue: Boolean,
  rawFile: {type: [File, Blob, null], default: null},
  duration: {type: Number, default: 0},
  audioUrl: {type: String, default: ''},
  preparing: {type: Boolean, default: false},
});

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel', 'play-start']);
const {t} = useI18nText();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

// 预览音频地址：
// - 远程音频直接用 props.audioUrl
// - 本地文件使用 objectURL，并在生命周期内手动释放，避免内存泄漏
const previewUrl = ref('');
const objectPreviewUrl = ref('');

// 运行时音频时长（来自 loadedmetadata），用于兜底 props.duration 不可用的场景
const runtimeDuration = ref(0);

const isProcessing = ref(false);
// 标记：处理中（FFmpeg 裁剪期间）用户点了取消，需要丢弃本次裁剪结果，避免误上传
const cancelledDuringProcessing = ref(false);
const waveContainerRef = ref(null);
const trackRef = ref(null);
const previewAudioRef = ref(null);

const MIN_TRIM_SECONDS = 10;
const MAX_TRIM_SECONDS = 300;
// 播放指针统一按 1 秒精度吸附
const PLAYHEAD_SNAP_SECONDS = 0.5;

// 选区百分比（相对于整条波形）
const startPercent = ref(0);
const endPercent = ref(30);

const isDragging = ref(null);
const isSeekingPlayhead = ref(false);
const isRangeDragging = ref(false);
const shouldResumeAfterSeek = ref(false);
const rangeDragStartX = ref(0);
const rangeDragInitialStart = ref(0);
const rangeDragInitialEnd = ref(0);
const isPlaying = ref(false);

// 当前试听进度（秒）。暂停时保留该值，以便继续播放
const currentPreviewTime = ref(0);

const waveHeights = ref([]);

// 本地文件播放（WebAudio）相关状态：规避 <audio> 在部分浏览器下 objectURL 播放不稳定
// decodedAudioBuffer: generateWaveform 时解码得到的 PCM 缓冲，供试听复用
// playbackCtxRef: 当前试听使用的 AudioContext（每次播放创建，停止时关闭）
// playbackSourceRef: 单次播放的 BufferSource（一次性节点，stop 后不可复用）
// playbackRafRef: 用于驱动播放指针动画的 requestAnimationFrame 句柄
// playbackStartedAt/playbackOffset: 记录“context 起播时刻 + 音频偏移”，用于计算当前播放进度
const decodedAudioBuffer = ref(null);
const playbackCtxRef = ref(null);
const playbackSourceRef = ref(null);
const playbackRafRef = ref(null);
const playbackStartedAt = ref(0);
const playbackOffset = ref(0);
const waveContainerLoading = ref(null);


/**
 * 从 URL 下载音频为 ArrayBuffer（优先携带凭证，兼容依赖 Cookie 的资源服务）。
 * @param {string} url
 * @returns {Promise<ArrayBuffer>}
 */
const fetchAudioArrayBufferFromUrl = async (url) => {
  const requestUrl = String(url || '').trim();
  if (!requestUrl) throw new Error(t('creation.musicTrimmer.emptyAudioUrl'));

  let response = await fetch(requestUrl);
  if (!response.ok) {
    throw new Error(`音频下载失败: HTTP ${response.status}`);
  }
  return await response.arrayBuffer();
};

/**
 * 将远程音频 URL 下载为 File，供裁剪逻辑复用。
 * @param {string} url
 * @returns {Promise<File>}
 */
const fetchAudioFileFromUrl = async (url) => {
  const arrayBuffer = await fetchAudioArrayBufferFromUrl(url);
  const requestUrl = String(url || '').trim();
  const extMatch = requestUrl.match(/\.([a-zA-Z0-9]+)(?:\?|#|$)/);
  const ext = extMatch?.[1] ? String(extMatch[1]).toLowerCase() : 'mp3';
  const fileName = `mv_source_${Date.now()}.${ext}`;
  const mimeType = ext === 'wav' ? 'audio/wav' : ext === 'm4a' ? 'audio/mp4' : 'audio/mpeg';
  return new File([arrayBuffer], fileName, {type: mimeType});
};


/**
 * 仅在波形容器上展示 loading，避免全屏遮罩影响页面其它区域。
 * @param {string} text
 */
const openWaveContainerLoading = (text) => {
  if (!waveContainerRef.value) return;
  if (waveContainerLoading.value) {
    waveContainerLoading.value.close();
    waveContainerLoading.value = null;
  }
  waveContainerLoading.value = ElLoading.service({
    target: waveContainerRef.value,
    body: false,
    fullscreen: false,
    lock: true,
    text,
    background: 'rgba(0, 0, 0, 0.2)',
    customClass: 'music-trimmer-waveform-loading',
  });
};

/** 关闭波形容器 loading。 */
const closeWaveContainerLoading = () => {
  if (!waveContainerLoading.value) return;
  waveContainerLoading.value.close();
  waveContainerLoading.value = null;
};

/**
 * 将 File/Blob 转为 ArrayBuffer（用于解码波形）。
 * @param {File|Blob|null|undefined} fileOrBlob
 * @returns {Promise<ArrayBuffer|null>}
 */
const toArrayBuffer = async (fileOrBlob) => {
  if (!fileOrBlob) return null;
  if (fileOrBlob.arrayBuffer) return await fileOrBlob.arrayBuffer();
  return null;
};

/**
 * 由单声道 PCM 峰值归一化生成柱状高度（0–100）。
 * @param {Float32Array} channelData
 * @param {number} barsCount
 * @returns {number[]}
 */
const buildWaveformHeights = (channelData, barsCount) => {
  const len = channelData.length;
  if (!len || barsCount <= 0) return [];

  const blockSize = Math.floor(len / barsCount) || 1;
  const peaks = new Float32Array(barsCount);

  for (let i = 0; i < barsCount; i++) {
    const start = i * blockSize;
    const end = Math.min(start + blockSize, len);
    let peak = 0;
    for (let j = start; j < end; j++) {
      const v = Math.abs(channelData[j]);
      if (v > peak) peak = v;
    }
    peaks[i] = peak;
  }

  let max = 0;
  for (let i = 0; i < peaks.length; i++) max = Math.max(max, peaks[i]);
  if (max <= 0) return Array.from({length: barsCount}, () => 10);

  return Array.from(peaks, (p) => (p / max) * 100);
};

/**
 * 生成波形柱高：
 * - 仅对本地 rawFile 生成（远程 url 在本组件里没有原始 PCM，不做解码）
 * - 失败时回退到固定高度，保证 UI 可操作
 */
const generateWaveform = async () => {
  if (!visible.value) return;
  if (props.preparing) return;

  const barsCount = 90;
  openWaveContainerLoading(t('creation.musicTrimmer.generatingWaveform'));

  let audioCtx = null;
  try {
    let arrayBuffer = null;
    if (props.rawFile) {
      arrayBuffer = await toArrayBuffer(props.rawFile);
    } else if (previewUrl.value) {
      arrayBuffer = await fetchAudioArrayBufferFromUrl(previewUrl.value);
    }
    if (!arrayBuffer) throw new Error(t('creation.musicTrimmer.audioReadFailed'));

    const AudioCtx = window.AudioContext || window['webkitAudioContext'];
    if (!AudioCtx) throw new Error(t('creation.musicTrimmer.audioContextUnsupported'));

    audioCtx = new AudioCtx();
    const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer.slice(0));
    decodedAudioBuffer.value = audioBuffer;

    // 对部分 VBR MP3，<audio>.duration 可能返回 Infinity，优先使用解码后的真实时长兜底
    const decodedDuration = audioBuffer.duration;
    if (decodedDuration > 0) runtimeDuration.value = decodedDuration;

    const channelData = audioBuffer.getChannelData(0);
    waveHeights.value = buildWaveformHeights(channelData, barsCount);
  } catch (e) {
    console.error('生成波形失败:', e);
    decodedAudioBuffer.value = null;
    waveHeights.value = [];
  } finally {
    if (audioCtx) {
      try {
        await audioCtx.close();
      } catch (_) {
      }
    }
    closeWaveContainerLoading();
  }
};

const effectiveDuration = computed(() => {
  if (props.duration > 0) return props.duration;
  if (runtimeDuration.value > 0) return runtimeDuration.value;
  const ad = previewAudioRef.value?.duration || 0;
  return ad > 0 ? ad : 0;
});

/** 秒级吸附：统一拖拽精度到 1s，避免 59s/60s 边界抖动。 */
const snapToSecond = (seconds) => {
  if (isEmpty(seconds)) return 0;
  return Math.round(seconds || 0);
};

/** 将时间映射为百分比（含边界保护）。 */
const timeToPercent = (time, duration) => {
  if (duration <= 0) return 0;
  const t = Math.max(0, Math.min(duration, time));
  return (t / duration) * 100;
};

/** 以秒为单位设置选区，再同步到百分比。 */
const setRangeByTime = (nextStartSec, nextEndSec) => {
  const duration = effectiveDuration.value;
  if (duration <= 0) return;

  const durationSec = snapToSecond(duration);
  const safeStart = Math.max(0, Math.min(durationSec, snapToSecond(nextStartSec)));
  const safeEnd = Math.max(safeStart, Math.min(durationSec, snapToSecond(nextEndSec)));

  startPercent.value = timeToPercent(safeStart, duration);
  endPercent.value = timeToPercent(safeEnd, duration);
};

// 裁剪区间按“手柄内侧边缘”定义：
// - 左手柄内侧在右边，对应 startPercent
// - 右手柄内侧在左边，对应 endPercent
const startTime = computed(() => (startPercent.value / 100) * effectiveDuration.value);
const endTime = computed(() => (endPercent.value / 100) * effectiveDuration.value);

const startTimeSeconds = computed(() => {
  const t = startTime.value;
  if (!t || t < 0) return 0;
  return Math.max(0, snapToSecond(t));
});

const endTimeSeconds = computed(() => {
  const t = endTime.value;
  if (!t || t < 0) return 0;
  return Math.max(0, snapToSecond(t));
});

const selectedDurationSeconds = computed(() => {
  const d = endTimeSeconds.value - startTimeSeconds.value;
  return Math.max(0, d);
});

const displayRange = computed(() => ({
  start: startTimeSeconds.value,
  end: endTimeSeconds.value,
}));

// 展示统一按秒级，避免最大时长边界（59.999...）闪烁
const displaySelectedDuration = computed(() => selectedDurationSeconds.value);

const clampedPreviewTime = computed(() => {
  const duration = effectiveDuration.value;
  const base = currentPreviewTime.value || 0;
  if (duration <= 0) return Math.max(0, base);
  return Math.max(0, Math.min(duration, base));
});

const playheadPercent = computed(() => {
  if (effectiveDuration.value <= 0) return startPercent.value;
  return (clampedPreviewTime.value / effectiveDuration.value) * 100;
});

const wavePlayedIndex = computed(() => {
  const total = waveHeights.value.length || 0;
  if (!total) return -1;
  const ratio = Math.max(0, Math.min(1, playheadPercent.value / 100));
  return Math.floor(ratio * (total - 1));
});

/** 当前裁剪区间对应的波形柱索引范围。 */
const trimRangeWaveIndex = computed(() => {
  const total = waveHeights.value.length || 0;
  if (!total) return {start: 0, end: -1};
  const maxIndex = total - 1;
  const start = Math.max(0, Math.min(maxIndex, Math.floor((startPercent.value / 100) * maxIndex)));
  const end = Math.max(0, Math.min(maxIndex, Math.ceil((endPercent.value / 100) * maxIndex)));
  return {start, end};
});

/**
 * 波形柱颜色：仅由播放进度决定，始终跟随播放头。
 * @param {number} index
 * @returns {string}
 */
const getWaveBarColor = (index) => {
  const inPlayedRange = index <= wavePlayedIndex.value;
  if (!inPlayedRange) return 'rgba(194,255,0,0.35)';

  const {start, end} = trimRangeWaveIndex.value;
  const inTrimRange = index >= start && index <= end;
  if (inTrimRange) return '#C2FF00';
  return 'rgba(194,255,0,0.35)';
};

/**
 * @param {number} index
 * @returns {number}
 */
const getWaveBarOpacity = (index) => {
  const inPlayedRange = index <= wavePlayedIndex.value;
  if (!inPlayedRange) return 0.7;

  const {start, end} = trimRangeWaveIndex.value;
  const inTrimRange = index >= start && index <= end;
  if (inTrimRange) return 1;
  return 0.7;
};

const showPlayhead = computed(() => {
  const duration = effectiveDuration.value;
  return duration > 0;
});

const trimRangeStyle = computed(() => {
  const width = Math.max(0, endPercent.value - startPercent.value);
  return {
    left: `${startPercent.value}%`,
    width: `${width}%`,
  };
});

/** 是否展示“确认生成 + 分割线 + 积分”结构。 */

/** 释放本地 objectURL，避免内存泄漏和旧地址残留 */
const revokeObjectPreviewUrl = () => {
  if (!objectPreviewUrl.value) return;
  URL.revokeObjectURL(objectPreviewUrl.value);
  objectPreviewUrl.value = '';
};

/** 根据 props 统一初始化预览地址（远程 url / 本地 blob） */
const initPreviewUrl = () => {
  revokeObjectPreviewUrl();
  const remoteUrl = String(props.audioUrl || '');
  if (remoteUrl) {
    // Reason: 历史会话里可能残留已失效 blob URL（页面刷新后无效），继续挂到 <audio> 会触发无限 ERR_FILE_NOT_FOUND。
    if (remoteUrl.startsWith('blob:') && !props.rawFile) {
      previewUrl.value = '';
      return;
    }
    previewUrl.value = remoteUrl;
    return;
  }
  if (props.rawFile) {
    objectPreviewUrl.value = URL.createObjectURL(props.rawFile);
    previewUrl.value = objectPreviewUrl.value;
    return;
  }
  previewUrl.value = '';
};

/** 计算默认选区（30秒或全部） */
const calculateDefaultEndPercent = (duration) => {
  if (duration <= 0) return 100;
  if (duration >= 30) {
    return (30 / duration) * 100;
  }
  return 100;
};

/** 打开弹窗时的统一初始化，确保每次进入都是可播放、可裁剪状态 */
const initTrimmerState = () => {
  startPercent.value = 0;
  runtimeDuration.value = props.duration > 0 ? props.duration : 0;
  const duration = runtimeDuration.value || props.duration || 0;

  // 默认选取30秒，不足30秒则选中整个音频
  endPercent.value = calculateDefaultEndPercent(duration);
  currentPreviewTime.value = 0;
  initPreviewUrl();
};

watch(() => visible.value, async (newVal) => {
  if (newVal) {
    initTrimmerState();

    // 等待弹窗内容真正渲染出来，确保 <audio> ref 已可用
    await nextTick();

    if (props.preparing) {
      openWaveContainerLoading(t('creation.musicTrimmer.processingAudioFile'));
    } else {
      await generateWaveform();
    }

    // 本地文件场景兜底：某些浏览器下 metadata 事件不稳定，主动探测时长
    if (effectiveDuration.value <= 0 && props.rawFile && previewAudioRef.value) {
      const audio = previewAudioRef.value;
      try {
        audio.load();
        await new Promise((resolve) => {
          const done = () => {
            audio.removeEventListener('loadedmetadata', done);
            resolve();
          };
          audio.addEventListener('loadedmetadata', done, {once: true});
          setTimeout(resolve, 1200);
        });
      } catch (error) {
        console.error('音频 metadata 兜底加载失败:', error);
      }
    }
  } else {
    stopPreview();
    closeWaveContainerLoading();
    runtimeDuration.value = 0;
    decodedAudioBuffer.value = null;
    previewUrl.value = '';
    revokeObjectPreviewUrl();
  }
});

// 监听有效时长变化，重新计算默认选区（确保30秒或全部选中）
watch(() => runtimeDuration.value, (newDuration) => {
  if (visible.value && newDuration > 0) {
    endPercent.value = calculateDefaultEndPercent(newDuration);
  }
});

watch(() => props.preparing, async (isPreparing) => {
  if (!visible.value) return;
  if (isPreparing) {
    openWaveContainerLoading(t('creation.musicTrimmer.processingAudioFile'));
    waveHeights.value = [];
    return;
  }

  closeWaveContainerLoading();
  await nextTick();
  await generateWaveform();
});

// 本地上传源变化时：重建 objectURL、重置播放进度并重新解码波形
watch(() => props.rawFile, () => {
  if (!visible.value) return;
  if (props.audioUrl) return;
  if (props.preparing) return;

  initPreviewUrl();
  currentPreviewTime.value = 0;
  runtimeDuration.value = props.duration > 0 ? props.duration : runtimeDuration.value;
  generateWaveform();
});

/**
 * @param {number} seconds
 * @returns {string} `mm:ss`
 */
const formatDuration = (seconds) => {
  if (isEmpty(seconds) || seconds < 0) return '00:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
};

/**
 * 计算恢复播放时间：
 * - 有有效时长时：强制限制在当前裁剪区间 [startTime, endTime]
 * - 无有效时长时：允许从 >=0 的已记录时间恢复
 */
const getResumeTime = () => {
  const duration = effectiveDuration.value;
  const base = currentPreviewTime.value || 0;
  if (duration <= 0) {
    return Math.max(0, base);
  }
  return Math.max(0, Math.min(duration, base));
};

/** 取消播放指针的 RAF 循环，避免重复调度与内存泄漏 */
const cancelWebAudioTick = () => {
  if (!playbackRafRef.value) return;
  cancelAnimationFrame(playbackRafRef.value);
  playbackRafRef.value = null;
};

/** 停止并断开当前 BufferSource（source 为一次性节点，必须重新创建） */
const stopWebAudioSource = () => {
  const source = playbackSourceRef.value;
  if (!source) return;
  // 主动 stop 时先移除 onended，避免误触发“跳到末尾”的收尾逻辑
  source.onended = null;
  try {
    source.stop(0);
  } catch (_) {
  }
  try {
    source.disconnect();
  } catch (_) {
  }
  playbackSourceRef.value = null;
};

/** 关闭试听 AudioContext（释放底层音频设备与资源） */
const closePlaybackCtx = async () => {
  const ctx = playbackCtxRef.value;
  playbackCtxRef.value = null;
  if (!ctx) return;
  try {
    if (ctx.state !== 'closed') await ctx.close();
  } catch (_) {
  }
};

/**
 * 通过 AudioContext 时钟驱动播放指针：
 * - 不依赖 <audio> 的 timeupdate，保证本地 WebAudio 试听时指针稳定刷新
 * - 到达选区末尾后自动停止
 */
const tickWebAudioPlayhead = () => {
  const ctx = playbackCtxRef.value;
  if (!ctx || !isPlaying.value) return;

  const elapsed = Math.max(0, ctx.currentTime - playbackStartedAt.value);
  const t = playbackOffset.value + elapsed;
  currentPreviewTime.value = t;

  if (effectiveDuration.value > 0 && t >= effectiveDuration.value) {
    stopPreview();
    currentPreviewTime.value = effectiveDuration.value;
    return;
  }

  playbackRafRef.value = requestAnimationFrame(tickWebAudioPlayhead);
};

/** 暂停试听并保留当前进度（再次播放从当前位置继续） */
const stopPreview = () => {
  const audio = previewAudioRef.value;

  // WebAudio 路径：先根据上下文时间刷新当前进度
  const ctx = playbackCtxRef.value;
  if (ctx && isPlaying.value) {
    const elapsed = Math.max(0, ctx.currentTime - playbackStartedAt.value);
    currentPreviewTime.value = playbackOffset.value + elapsed;
  } else if (audio) {
    currentPreviewTime.value = audio.currentTime;
  }

  cancelWebAudioTick();
  stopWebAudioSource();
  closePlaybackCtx();

  if (audio) audio.pause();
  isPlaying.value = false;
};

/**
 * 启动 WebAudio 试听。
 * @param {number} startAt 从音频第几秒开始播放
 * @param {number} endAt 在音频第几秒停止（用于 onended 后的指针归位）
 * @returns {Promise<void>}
 */
const startWebAudioPreview = async (startAt, endAt) => {
  const AudioCtx = window.AudioContext || window['webkitAudioContext'];
  if (!AudioCtx) throw new Error(t('creation.musicTrimmer.audioContextUnsupported'));

  await closePlaybackCtx();
  const ctx = new AudioCtx();
  playbackCtxRef.value = ctx;
  if (ctx.state === 'suspended') await ctx.resume();

  const source = ctx.createBufferSource();
  source.buffer = decodedAudioBuffer.value;
  source.connect(ctx.destination);
  playbackSourceRef.value = source;

  const remain = Math.max(0, endAt - startAt);
  const safeDuration = remain > 0 ? remain : undefined;
  source.onended = () => {
    if (!isPlaying.value) return;
    stopPreview();
    currentPreviewTime.value = endAt;
  };

  playbackOffset.value = startAt;
  playbackStartedAt.value = ctx.currentTime;
  source.start(0, startAt, safeDuration);

  isPlaying.value = true;
  cancelWebAudioTick();
  playbackRafRef.value = requestAnimationFrame(tickWebAudioPlayhead);
};

/** 当前是否走 WebAudio 试听路径（本地文件且已有解码缓冲）。 */
const canUseWebAudioPreview = () => {
  return !!props.rawFile && !!decodedAudioBuffer.value;
};

/**
 * 使用 `<audio>` 从指定时间开始播放。
 * @param {HTMLAudioElement} audio
 * @param {number} startAt
 * @param {{ensureLoaded?: boolean}} [options]
 * @returns {Promise<void>}
 */
const playHtmlAudioFromTime = async (audio, startAt, options = {}) => {
  const {ensureLoaded = false} = options;
  if (ensureLoaded && audio.networkState === 0) audio.load();
  try {
    audio.currentTime = startAt;
  } catch (_) {
  }
  await audio.play();
  isPlaying.value = true;
};

/** 播放/暂停切换：
 * - 本地文件优先走 WebAudio（更稳定）
 * - 远程音频走 <audio>
 */
const togglePreview = async () => {
  const audio = previewAudioRef.value;
  if (!audio || !previewUrl.value) return;
  if (props.preparing) return;

  if (isPlaying.value) {
    stopPreview();
    return;
  }

  // 即将开始裁剪器试听，通知父组件暂停外部播放器
  emit('play-start');

  try {
    const resumeTime = getResumeTime();
    currentPreviewTime.value = resumeTime;

    if (canUseWebAudioPreview()) {
      await startWebAudioPreview(resumeTime, effectiveDuration.value);
      return;
    }

    // 远程音频回退到 <audio>
    await playHtmlAudioFromTime(audio, resumeTime, {ensureLoaded: true});
  } catch (error) {
    console.error('试听播放失败:', error);
    isPlaying.value = false;

    const errName = error?.name || '';
    if (errName === 'NotAllowedError') {
      ElMessage.warning(t('creation.musicTrimmer.autoplayBlocked'));
    } else if (errName === 'NotSupportedError') {
      ElMessage.error(t('creation.musicTrimmer.audioCodecUnsupported'));
    } else {
      ElMessage.warning(t('creation.musicTrimmer.playFailed'));
    }
  }
};

/** 远程音频元数据就绪：更新时长并限制最大选区 */
const onAudioLoadedMetadata = () => {
  const audio = previewAudioRef.value;
  const d = audio?.duration || 0;
  if (d > 0) {
    runtimeDuration.value = d;
    // 限制最大选区不超过300秒
    const maxGapPercent = (MAX_TRIM_SECONDS / d) * 100;
    if (endPercent.value - startPercent.value > maxGapPercent) {
      endPercent.value = Math.min(100, startPercent.value + maxGapPercent);
    }
    // 不足30秒的音频默认选中整个
    if (d < 30 && endPercent.value < 100) {
      endPercent.value = 100;
    }
  }
};

/** `<audio>` 试听进度（非 WebAudio 路径）。 */
const onTimeUpdate = () => {
  const audio = previewAudioRef.value;
  if (!audio || !isPlaying.value) return;
  currentPreviewTime.value = audio.currentTime;

  // 仅在有效时长可用且选区有效时，才执行到达终点自动暂停
  if (effectiveDuration.value > 0 && audio.currentTime >= effectiveDuration.value) {
    stopPreview();
    currentPreviewTime.value = effectiveDuration.value;
  }
};

/** 远程 `<audio>` 自然播放结束。 */
const onAudioEnded = () => {
  isPlaying.value = false;
  currentPreviewTime.value = endTime.value;
};

/**
 * @param {'start'|'end'} type
 * @returns {void}
 */
const startDrag = (type) => {
  if (isRangeDragging.value) return;
  isDragging.value = type;
  stopPreview();
  document.addEventListener('touchmove', handleTouchMove, {passive: false});
  document.addEventListener('touchend', stopDrag);
};

/** 开始拖动播放指针（拖动中仅更新 UI，松手后再真正 seek）。 */
const startSeekPlayhead = () => {
  if (isDragging.value || isRangeDragging.value) return;
  shouldResumeAfterSeek.value = isPlaying.value;
  isSeekingPlayhead.value = true;
  if (isPlaying.value) stopPreview();
  document.addEventListener('touchmove', handleTouchMove, {passive: false});
  document.addEventListener('touchend', stopSeekPlayhead);
};

/** 开始拖动整个选区（保持选区长度不变，仅左右平移）。 */
const startDragRange = (e) => {
  if (isDragging.value || isSeekingPlayhead.value || !trackRef.value) return;

  if (isPlaying.value) stopPreview();

  isRangeDragging.value = true;
  rangeDragStartX.value = getClientX(e);

  const duration = effectiveDuration.value;
  if (duration > 0) {
    const startSec = (startPercent.value / 100) * duration;
    const endSec = (endPercent.value / 100) * duration;
    rangeDragInitialStart.value = snapToSecond(startSec);
    rangeDragInitialEnd.value = snapToSecond(endSec);
  } else {
    rangeDragInitialStart.value = startPercent.value;
    rangeDragInitialEnd.value = endPercent.value;
  }

  document.addEventListener('touchmove', handleTouchMove, {passive: false});
  document.addEventListener('touchend', stopRangeDrag);
};

/**
 * 拖拽裁剪手柄/选区时更新位置，并约束在合法范围。
 * @param {TouchEvent} e
 * @returns {void}
 */
const handleTouchMove = (e) => {
  if (!trackRef.value) return;

  if (e.cancelable) e.preventDefault();

  if (isRangeDragging.value) {
    const rect = trackRef.value.getBoundingClientRect();
    const usableWidth = rect.width;
    if (usableWidth <= 0) return;

    const duration = effectiveDuration.value;
    if (duration <= 0) return;

    const durationSec = snapToSecond(duration);
    const deltaSec = snapToSecond(((getClientX(e) - rangeDragStartX.value) / usableWidth) * duration);
    const widthSec = Math.max(0, rangeDragInitialEnd.value - rangeDragInitialStart.value);

    let nextStartSec = rangeDragInitialStart.value + deltaSec;
    nextStartSec = Math.max(0, Math.min(durationSec - widthSec, nextStartSec));
    const nextEndSec = Math.min(durationSec, nextStartSec + widthSec);

    setRangeByTime(nextStartSec, nextEndSec);
    currentPreviewTime.value = nextStartSec;
    return;
  }

  if (isSeekingPlayhead.value) {
    const rect = trackRef.value.getBoundingClientRect();
    const usableWidth = rect.width;
    if (usableWidth <= 0) return;

    const ratio = (getClientX(e) - rect.left) / usableWidth;
    const percent = Math.max(0, Math.min(100, ratio * 100));
    const duration = effectiveDuration.value;
    if (duration <= 0) return;

    const seekTime = (percent / 100) * duration;
    let nextTime = snapToSecond(Math.max(0, Math.min(duration, seekTime)));
    if (Math.abs(nextTime - 0) <= PLAYHEAD_SNAP_SECONDS) nextTime = 0;
    if (Math.abs(nextTime - duration) <= PLAYHEAD_SNAP_SECONDS) nextTime = duration;
    currentPreviewTime.value = nextTime;
    return;
  }

  if (!isDragging.value) return;
  const rect = trackRef.value.getBoundingClientRect();
  const usableWidth = rect.width;
  if (usableWidth <= 0) return;

  const duration = effectiveDuration.value;
  if (duration <= 0) return;

  let percent = ((getClientX(e) - rect.left) / usableWidth) * 100;
  percent = Math.max(0, Math.min(100, percent));
  const draggedSec = snapToSecond((percent / 100) * duration);

  const currentStartSec = snapToSecond((startPercent.value / 100) * duration);
  const currentEndSec = snapToSecond((endPercent.value / 100) * duration);

  if (isDragging.value === 'start') {
    const minStartSec = Math.max(0, currentEndSec - MAX_TRIM_SECONDS);
    const maxStartSec = Math.max(0, currentEndSec - MIN_TRIM_SECONDS);
    const nextStartSec = Math.max(minStartSec, Math.min(maxStartSec, draggedSec));
    setRangeByTime(nextStartSec, currentEndSec);
  } else {
    const minEndSec = Math.min(duration, currentStartSec + MIN_TRIM_SECONDS);
    const maxEndSec = Math.min(duration, currentStartSec + MAX_TRIM_SECONDS);
    const nextEndSec = Math.max(minEndSec, Math.min(maxEndSec, draggedSec));
    setRangeByTime(currentStartSec, nextEndSec);
  }
};

/** 获取当前触点的 clientX。 */
const getClientX = (event) => {
  if ('touches' in event && event.touches?.length) return event.touches[0].clientX;
  if ('changedTouches' in event && event.changedTouches?.length) return event.changedTouches[0].clientX;
  return 0;
};

/**
 * 结束手柄拖拽；手柄落下后从裁剪起点自动试听。
 * @param {boolean} [shouldAutoPlay=true]
 * @returns {Promise<void>}
 */
const stopDrag = async (shouldAutoPlay = true) => {
  isDragging.value = null;
  document.removeEventListener('touchmove', handleTouchMove);
  document.removeEventListener('touchend', stopDrag);
  currentPreviewTime.value = startTime.value;

  if (shouldAutoPlay && !isPlaying.value) {
    await togglePreview();
  }
};

/** 结束选区整体拖拽：更新积分并立即开始播放。 */
const stopRangeDrag = async () => {
  if (!isRangeDragging.value) return;

  isRangeDragging.value = false;
  document.removeEventListener('touchmove', handleTouchMove);
  document.removeEventListener('touchend', stopRangeDrag);

  currentPreviewTime.value = startTime.value;

  if (!isPlaying.value) {
    await togglePreview();
  }
};

/** 结束拖动播放指针：执行 seek；若拖动前在播放则继续播放。 */
const stopSeekPlayhead = async () => {
  if (!isSeekingPlayhead.value) return;

  isSeekingPlayhead.value = false;
  document.removeEventListener('touchmove', handleTouchMove);
  document.removeEventListener('touchend', stopSeekPlayhead);

  const duration = effectiveDuration.value;
  let seekTime = snapToSecond(Math.max(0, Math.min(duration, currentPreviewTime.value)));
  if (Math.abs(seekTime - 0) <= PLAYHEAD_SNAP_SECONDS) seekTime = 0;
  if (Math.abs(seekTime - duration) <= PLAYHEAD_SNAP_SECONDS) seekTime = duration;
  currentPreviewTime.value = seekTime;

  const audio = previewAudioRef.value;
  if (audio && !props.rawFile) {
    try {
      audio.currentTime = seekTime;
    } catch (_) {
    }
  }

  if (!shouldResumeAfterSeek.value) return;

  try {
    if (canUseWebAudioPreview()) {
      await startWebAudioPreview(seekTime, endTime.value);
      return;
    }

    if (audio) {
      await playHtmlAudioFromTime(audio, seekTime);
    }
  } catch (error) {
    console.error('拖动后恢复播放失败:', error);
    isPlaying.value = false;
  } finally {
    shouldResumeAfterSeek.value = false;
  }
};


/**
 * 校验选区时长后裁剪并 `emit('confirm', { blob, timeRange })`。
 * @returns {Promise<void>}
 */
const handleConfirm = async () => {
  if (props.preparing) return;
  const safeDuration = selectedDurationSeconds.value;

  if (safeDuration < MIN_TRIM_SECONDS) {
    ElMessage.warning(t('creation.musicTrimmer.minTrimDuration', {seconds: MIN_TRIM_SECONDS}));
    return;
  }

  if (safeDuration > MAX_TRIM_SECONDS) {
    ElMessage.warning(t('creation.musicTrimmer.maxTrimDuration', {seconds: MAX_TRIM_SECONDS}));
    return;
  }


  isProcessing.value = true;
  cancelledDuringProcessing.value = false;
  try {
    const sourceFile = props.rawFile || (props.audioUrl ? await fetchAudioFileFromUrl(props.audioUrl) : null);
    if (!sourceFile) {
      ElMessage.warning(t('creation.musicTrimmer.missingAudioSource'));
      return;
    }

    const trimStart = startTimeSeconds.value;
    const trimmedBlob = await trimAudio(sourceFile, trimStart, safeDuration);

    // 处理中被取消：丢弃裁剪结果，不再 emit confirm，避免后续上传
    if (cancelledDuringProcessing.value) return;

    const safeEndTime = trimStart + safeDuration;
    const timeRange = `${formatDuration(trimStart)}-${formatDuration(safeEndTime)}`;
    emit('confirm', {blob: trimmedBlob, timeRange, durationSeconds: safeDuration});
  } catch (error) {
    console.error('裁剪失败:', error);
    ElMessage.error(t('creation.musicTrimmer.musicProcessFailed'));
  } finally {
    isProcessing.value = false;
  }
};

/** 停止试听并 `emit('cancel')`。 */
const handleCancel = () => {
  // 若处于"处理中"，让 handleConfirm 在 trimAudio 收尾后丢弃结果
  if (isProcessing.value) {
    cancelledDuringProcessing.value = true;
  }
  stopPreview();
  emit('cancel');
};

/**
 * 暴露给父组件，用于跨媒体互斥：当页面其它地方有音频/视频开始播放时，
 * 父级可以调用 `stopPreview()` 强制停止 WebAudio / `<audio>` 试听。
 * WebAudio 路径不发原生 play/pause 事件，文档级监听器抓不到，必须显式停。
 */
defineExpose({stopPreview});

onBeforeUnmount(() => {
  stopPreview();
  closeWaveContainerLoading();
  stopDrag(false);
  isRangeDragging.value = false;
  isSeekingPlayhead.value = false;
  shouldResumeAfterSeek.value = false;
  document.removeEventListener('touchend', stopRangeDrag);
  document.removeEventListener('touchend', stopSeekPlayhead);
  revokeObjectPreviewUrl();
});
</script>

<style lang="scss">
.mobile-music-trimmer-dialog {
  padding: 0;

  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    padding: 6px 8px 0;
  }

  .el-dialog__footer {
    padding: 6px 8px 8px;
  }

  .music-trimmer-waveform {
    touch-action: none;
    user-select: none;
  }

  // 波形生成 loading：保持默认样式，仅同步主题色
  .music-trimmer-waveform-loading {
    z-index: 9999 !important;
    --el-color-primary: #C2FF00 !important;
    --el-loading-spinner-text-color: #C2FF00 !important;
  }

  .music-trimmer-waveform-loading .el-loading-spinner .path {
    stroke: #C2FF00 !important;
  }

  .music-trimmer-waveform-loading .el-loading-text,
  .music-trimmer-waveform-loading .el-loading-spinner .el-loading-text {
    color: #C2FF00 !important;
  }
}
</style>
