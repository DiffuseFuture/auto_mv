<template>
  <div class="w-[360px] shrink-0 border-l border-white/10 bg-[#0A0A0A] flex flex-col">
    <template v-if="storyboardMode">
      <div class="text-white text-[24px] px-5 mb-[18px] flex-between">
        <span>分镜编辑</span>
        <svg-icon name="gy-closure" size="24" color="#C2FF00" class="cursor-pointer" @click="emit('close')"></svg-icon>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto custom-scrollbar px-4">
        <div v-if="storyboardLoading" class="flex-col-center py-10 text-gray-500">
          <el-icon :size="32" class="animate-spin mb-2">
            <Loading/>
          </el-icon>
          <span class="text-sm">加载分镜中...</span>
        </div>
        <div
            v-else
            v-for="(scene, idx) in storyboardScenes"
            :key="scene.sceneId"
            class="h-[142px] mb-4 flex gap-3 bg-[rgba(93,99,74,0.5)] rounded-[10px] overflow-hidden cursor-pointer py-4 px-[10px] hover:bg-[rgba(102,109,82,0.6)] transition-colors"
            @click="emit('open-scene-edit', scene, idx)"
        >
          <div class="w-[120px] h-[110px] rounded-[10px] shrink-0 overflow-hidden">
            <img v-if="scene.coverFile?.fileUrl" :src="scene.coverFile.fileUrl" class="w-full h-full object-cover"/>
            <div v-else class="w-full h-full bg-white/5 flex-center min-h-[80px]">
              <svg-icon name="gy-play2" size="20" color="#C2FF00"></svg-icon>
            </div>
          </div>
          <div class="flex-1 min-w-0 overflow-hidden">
            <div class="text-[#C2FF00] text-[20px] leading-[28px]">分镜{{ idx + 1 }}</div>
            <div class="text-white/70 text-[14px] leading-[22px] mt-1 line-clamp-3" :title="formatScenePrompt(scene)">
              {{ formatScenePrompt(scene) }}
            </div>
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-3 p-4 pt-5">
        <button
            class="w-full h-[44px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] hover:bg-[#242424] rounded-[10px] flex-center gap-2 text-black text-[16px] transition-colors cursor-pointer"
            :class="{ 'opacity-50 pointer-events-none': storyboardRemaking }"
            @click="emit('remake-video')"
        >
          <el-icon :size="18" :class="{ 'animate-spin': storyboardRemaking }">
            <Refresh/>
          </el-icon>
          <span>{{ storyboardRemaking ? '重新生成中...' : '重新生成' }}</span>
        </button>
      </div>
    </template>

    <template v-else>
      <div class="flex-1 min-h-0 flex flex-col">
        <div class="text-white text-[24px] px-5 mb-3 flex-between">
          <span>音乐播放</span>
          <svg-icon name="gy-closure" size="24" color="#C2FF00" class="cursor-pointer" @click="emit('close')"></svg-icon>
        </div>

        <div class="flex-1 min-h-0 overflow-y-auto custom-scrollbar pb-4">
          <div class="px-[30px] mb-5">
            <div class="rounded-[18px] overflow-hidden bg-[#2b2b2b] shadow-[0px_8px_24px_rgba(0,0,0,0.6)]">
              <div class="relative aspect-[4/3] bg-black">
                <img v-if="playerState.currentAudio?.coverUrl" :src="playerState.currentAudio.coverUrl" class="absolute inset-0 w-full h-full object-cover" alt="cover"/>
                <div v-else class="absolute inset-0 w-full h-full bg-white/5 flex-center">
                  <svg-icon name="gy-play2" size="44" color="#C2FF00"></svg-icon>
                </div>
                <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent"></div>

                <div class="absolute left-4 right-4 bottom-3">
                  <div class="text-white text-[18px] font-semibold leading-tight line-clamp-2">
                    {{ playerState.currentAudio?.title || '未命名' }}
                  </div>
                  <div class="mt-1 text-[#C2FF00] text-[14px] font-medium truncate">
                    {{ userStore.userName || '创作者' }}
                  </div>
                </div>
              </div>

              <div class="px-4 pt-3">
                <div class="h-[3px] bg-white/20 rounded-full cursor-pointer" @click="handleProgressClick">
                  <div class="h-full bg-[#C2FF00] rounded-full transition-[width] duration-200" :style="{ width: progressPercent + '%' }"></div>
                </div>
                <div class="mt-2 flex items-center justify-between text-white/70 text-[12px]">
                  <span>{{ formatDuration(playerState.currentTime) }}</span>
                  <span>{{ formatDuration(playerState.duration) }}</span>
                </div>
              </div>

              <div class="flex items-center justify-center gap-10 pb-2.5">
                <button class="text-white/80 hover:text-white transition-colors cursor-pointer" aria-label="prev" @click="handlePrev">
                  <el-icon :size="22">
                    <CaretLeft/>
                  </el-icon>
                </button>

                <button class="w-12 h-12 rounded-full bg-white/90 hover:bg-white flex-center text-black transition-colors cursor-pointer" aria-label="toggle" @click="handlePanelTogglePlay">
                  <svg-icon v-if="playerState.isPlaying" name="gy-pause" size="24" color="black"></svg-icon>
                  <svg-icon v-else name="gy-play2" size="24" color="black"></svg-icon>
                </button>

                <button class="text-white/80 hover:text-white transition-colors cursor-pointer" aria-label="next" @click="handleNext">
                  <el-icon :size="22">
                    <CaretRight/>
                  </el-icon>
                </button>
              </div>
            </div>
          </div>

          <div class="px-5 mb-5">
            <div class="flex items-center justify-between mb-2">
              <span class="text-white/90 text-[16px] font-semibold">{{ t('creation.messageArea.audioStyleTitle') }}</span>
              <button class="text-white/60 hover:text-white transition-colors cursor-pointer" aria-label="copy-style" @click="handleCopyStylePrompt">
                <svg-icon name="gy-copy" size="16" color="currentColor"></svg-icon>
              </button>
            </div>

            <div class="rounded-[14px] bg-[rgba(90,96,74,0.5)] text-[#EDEFE6] overflow-hidden">
              <div class="p-3 text-[14px] leading-relaxed">
                <div v-if="!stylePromptText" class="text-white/70">{{ t('creation.messageArea.noAudioStyle') }}</div>
                <div v-else>
                  <div :class="stylePromptExpanded ? '' : 'line-clamp-1'">
                    {{ stylePromptText }}
                  </div>
                </div>
              </div>
              <button
                  v-if="stylePromptText"
                  class="w-full h-11 border-t border-white/20 flex-center gap-2 text-[#EDEFE6] hover:bg-white/10 transition-colors cursor-pointer"
                  @click="stylePromptExpanded = !stylePromptExpanded"
              >
                <span class="text-[14px]">{{ stylePromptExpanded ? '收起' : '展开' }}</span>
                <span class="text-[#C2FF00] text-[18px] leading-[28px]" :class="stylePromptExpanded ? '' : 'rotate-180'">^</span>
              </button>
            </div>
          </div>

          <div class="px-5 mb-5">
            <div class="flex items-center justify-between mb-2">
              <span class="text-white text-[22px] leading-[32px]">{{ t('creation.messageArea.lyricsTitle') }}</span>
              <button class="text-white/60 hover:text-white transition-colors cursor-pointer" aria-label="copy-lyrics" @click="handleCopyLyrics">
                <svg-icon name="gy-copy" size="16" color="currentColor"></svg-icon>
              </button>
            </div>
            <div class="pl-[10px] text-white text-[16px] leading-[28px] whitespace-pre-line">
              {{ lyricsText || t('creation.messageArea.noLyrics') }}
            </div>
          </div>
        </div>

        <div v-if="canEditCreation" class="p-4">
          <button class="w-full h-[44px] cursor-pointer bg-white hover:bg-gray-100 rounded-[12px] flex items-center justify-center gap-2 text-black font-medium transition-colors" @click="emit('prepare-mv')">
            <svg-icon name="gy-MV" size="20"></svg-icon>
            <span>制作MV</span>
          </button>
        </div>

        <div class="p-4 pt-0">
          <a
              :href="playerState.currentAudio?.audioUrl || '#'"
              :download="chatTitle || '下载'"
              @click="handleDownloadAudioTrack"
              class="w-full h-[44px] bg-[linear-gradient(299deg,_#BEFA00_0%,_#82FF79_100%)] hover:opacity-90 rounded-[10px] flex items-center justify-center gap-[14px] text-black text-[16px] transition-opacity"
              :class="{ 'pointer-events-none opacity-50': !playerState.currentAudio?.audioUrl }"
          >
            <svg-icon name="gy-download" size="20"></svg-icon>
            <span>下载</span>
          </a>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {CaretLeft, CaretRight, Loading, Refresh} from '@element-plus/icons-vue';
import {useUserStore} from '@/store/user';
import {formatDuration} from '@/utils/index.js';
import {saveUserTracking} from '@/api/tracking';
import {useCreationAudioController} from '@/views/creation/composables/useCreationAudioController';
import {replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();

const props = defineProps({
  storyboardMode: {type: Boolean, default: false},
  storyboardLoading: {type: Boolean, default: false},
  storyboardScenes: {type: Array, default: () => []},
  storyboardRemaking: {type: Boolean, default: false},
  canEditCreation: {type: Boolean, default: false},
  playerState: {type: Object, required: true},
  audioPlayerRef: {type: Object, default: null},
  pauseAllVideos: {type: Function, required: true},
  chatTitle: {type: String, default: ''},
});

const userStore = useUserStore();

const emit = defineEmits([
  'close',
  'open-scene-edit',
  'remake-video',
  'prepare-mv',
]);

const audioController = useCreationAudioController({
  audioPlayerRef: props.audioPlayerRef,
  // 避免 setup 时 props.audioPlayerRef 还没就绪导致捕获 null：
  // 每次操作时动态读取最新的 audio DOM。
  getAudioEl: () => props.audioPlayerRef?.value ?? props.audioPlayerRef,
  playerState: props.playerState,
  pauseAllVideos: props.pauseAllVideos,
});

const stylePromptExpanded = ref(false);
const progressPercent = computed(() =>
    props.playerState.duration ? (props.playerState.currentTime / props.playerState.duration) * 100 : 0,
);
const stylePromptText = computed(() => props.playerState.currentAudio?.style || '');
const lyricsText = computed(() => props.playerState.currentAudio?.lyrics || '');

/**
 * 分镜条文案（侧栏 storyboard 列表）；@ 提及替换为展示名。
 * @param {any} scene
 * @returns {string}
 */
const formatScenePrompt = (scene) => {
  if (!scene?.visualPrompt) return '暂无提示词';
  return replaceMentionValuesToLabels(scene.visualPrompt, scene.subject || [], {
    idFields: ['subjectId'],
  });
};

/**
 * @param {string} text
 * @param {string} label
 * @returns {void}
 */
const handleCopyText = (text, label) => {
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`${label}已复制`);
  });
};

const handleCopyStylePrompt = () => handleCopyText(stylePromptText.value, t('creation.messageArea.audioStyleTitle'));
const handleCopyLyrics = () => handleCopyText(lyricsText.value, t('creation.messageArea.lyricsTitle'));

/**
 * 侧栏大播放键：对当前曲目 toggle，不重复打开面板。
 * @returns {void}
 */
const handlePanelTogglePlay = () => {
  if (!props.playerState.currentAudio) return;
  void audioController.togglePlay({
    item: props.playerState.currentAudio,
    index: props.playerState.currentIndex,
    audioList: props.playerState.audioList,
    openPanel: false,
  });
};

/** @returns {void} */
const handlePrev = () => void audioController.playPrev();

/** @returns {void} */
const handleNext = () => void audioController.playNext();

/**
 * 点击进度条按位置 seek。
 * @param {MouseEvent} event
 * @returns {void}
 */
const handleProgressClick = (event) => {
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = (event.clientX - rect.left) / rect.width;
  audioController.seekByRatio(ratio);
};

const handleDownloadAudioTrack = () => {
  saveUserTracking({
    target: 'CREATE_MUSIC_DOWNLOAD_AUDIO',
  }).catch((error) => {
    console.error('创作页音乐下载埋点上报失败:', error);
  });
};
</script>

<style scoped lang="scss">
.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background-color: rgba(255, 255, 255, 0.05);
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 3px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.3);
    }
  }
}
</style>
