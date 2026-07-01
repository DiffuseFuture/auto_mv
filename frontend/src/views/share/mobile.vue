<template>
  <div class="relative h-full overflow-hidden bg-black/50">
    <!-- 光晕：设计稿 750 → 屏宽 375，尺寸 / 位置 / blur 全部 ÷2 -->
    <div class="w-[240px] aspect-square bg-[#95FD88] blur-[150px] absolute top-[9px] -right-[111px]"></div>
    <div class="w-[379px] aspect-square bg-[#C2FF00] blur-[150px] absolute bottom-[21px] -left-[133px]"></div>

    <!-- 右上角关闭按钮：固定视口，跨所有分支可见；样式与登录弹窗一致——纯图标无背景 -->
    <svg-icon
        name="gy-closure"
        :size="22"
        color="#FFFFFF"
        class="fixed top-4 right-4 cursor-pointer z-30"
        @click="handleClose"
    ></svg-icon>

    <!-- 加载中 -->
    <div v-if="loading" class="flex-1 flex-center">
      <div class="flex-col-center text-white/30">
        <el-icon :size="32" class="animate-spin mb-4">
          <Loading/>
        </el-icon>
        <span class="text-[16px]">{{ t('share.loading') }}</span>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="!shareInfo" class="flex-1 flex-center">
      <div class="flex-col-center text-white/30">
        <span class="text-[16px]">{{ t('share.invalid') }}</span>
      </div>
    </div>

    <!-- MV 分享（移动端） -->
    <div v-else-if="shareInfo.type === 'VIDEO'" class="w-full h-full relative px-5 pt-9">
      <div class="flex items-center gap-[5px] mb-6">
        <img :src="logoImg" alt="logo" class="w-[18px] h-[18px]"/>
        <span class="text-white text-[15px] font-bold">ohyesai.com</span>
      </div>

      <div class="flex items-end gap-3 mb-4">
        <div class="text-white text-[18px] leading-[26px] font-bold truncate max-w-[65%]">
          {{ shareInfo.projectName || t('share.unnamed') }}
        </div>
      </div>

      <div class="rounded-[5px] overflow-hidden bg-black/60 mb-4">
        <video
            ref="videoRef"
            :src="shareInfo.fileUrl"
            :poster="shareInfo.coverUrl"
            class="w-full aspect-video object-contain"
            playsinline
            webkit-playsinline
            preload="metadata"
            controls
        ></video>
      </div>

      <div class="flex-between mb-[6px]">
        <span class="text-[#C2FF00] text-[12px] leading-[16px] font-medium">{{ t('share.prompt') }}</span>
        <div class="cursor-pointer" @click="handleCopyPrompt">
          <svg-icon name="gy-copy" size="12" color="#C2FF00"></svg-icon>
        </div>
      </div>

      <div class="bg-[#F0F6DD]/30 h-[200px] rounded-[10px] p-[10px] overflow-auto text-white text-[10px] leading-[16px]">
        {{ shareInfo.prompt || t('share.noPrompt') }}
      </div>

      <div class="flex gap-4 z-20 absolute bottom-7 w-full px-5 left-0">
        <button
            class="flex-1 h-[30px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[12px] font-bold flex-center gap-2"
            @click="handleGoCreate"
        >
          <svg-icon name="gy-process" size="12" color="black"></svg-icon>
          <span>{{ t('share.createProcess') }}</span>
        </button>
        <button
            class="flex-1 h-[30px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[12px] font-bold flex-center gap-2"
            @click="handleShareLink"
        >
          <svg-icon name="gy-share" size="12" color="black"></svg-icon>
          <span>{{ t('common.share') }}</span>
        </button>
      </div>
    </div>

    <!-- 音乐分享（移动端） -->
    <div v-else class="relative px-[30px] pt-[20px] pb-[48px]">
      <div class="flex items-center gap-2 mb-[35px]">
        <img :src="logoImg" alt="logo" class="w-[18px] h-[18px]"/>
        <span class="text-white text-[15px] font-semibold">ohyesai.com</span>
      </div>

      <div class="text-center mb-[26px]">
        <div class="text-white text-[18px] leading-[25px] font-bold mb-[10px]">
          {{ shareInfo.projectName || t('share.unnamed') }}
        </div>
      </div>

      <div class="relative w-[208px] h-[208px] mx-auto rounded-[24px] overflow-hidden mb-[34px] bg-black/30 shadow-[0px_0px_34px_20px_rgba(255,255,255,0.3)]">
        <img v-if="shareInfo.coverUrl" :src="shareInfo.coverUrl" class="w-full h-full object-cover"/>
        <div v-else class="w-full h-full flex-center">
          <svg-icon name="gy-audio" size="36" color="rgba(255,255,255,0.2)"></svg-icon>
        </div>
        <div class="absolute inset-0 flex-center cursor-pointer" @click="toggleAudio">
          <svg-icon :name="isPlaying ? 'gy-pause' : 'gy-play2'" size="56" color="#C2FF00"></svg-icon>
        </div>
      </div>

      <div
          class="relative h-[72px] overflow-hidden touch-pan-y mb-[52px]"
          @touchstart="handleLyricTouchStart"
          @touchmove.prevent="handleLyricTouchMove"
          @touchend="handleLyricTouchEnd"
      >
        <div
            class="will-change-transform"
            :class="isLyricDragging ? '' : 'transition-transform duration-[260ms] ease-[cubic-bezier(0.22,1,0.36,1)]'"
            :style="{ transform: `translateY(${mobileLyricTranslateY}px)` }"
        >
          <div
              v-for="(line, index) in mobileLyricTimeline"
              :key="`${line.time}-${index}`"
              class="h-9 leading-9 text-[18px] font-medium text-center truncate transition-colors duration-200"
              :class="index === currentMobileLyricIndex ? 'text-white' : 'text-white/45'"
              @click="handleLyricLineClick(index)"
          >
            {{ line.text || '...' }}
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-2 mb-[24px]">
        <div class="progress-bar w-full h-[4px] rounded-full bg-white/25 cursor-pointer relative" @click="handleSeek">
          <div class="absolute left-0 top-0 h-full rounded-full bg-[#C2FF00]" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <div class="flex justify-between text-white/90 text-[14px] leading-[20px]">
          <span>{{ formatDuration(currentTime) }}</span>
          <span>{{ formatDuration(duration) }}</span>
        </div>
      </div>

      <div class="fixed w-full py-4 px-5 bottom-0 left-0 flex-center gap-9 z-20">
        <button
            class="flex-1 h-[30px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[10px] font-bold flex-center gap-2"
            @click="handleGoCreate"
        >
          <svg-icon name="gy-process" size="12" color="black"></svg-icon>
          <span>{{ t('share.createProcess') }}</span>
        </button>
        <button
            class="flex-1 h-[30px] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[10px] font-bold flex-center gap-2"
            @click="handleShareLink"
        >
          <svg-icon name="gy-share" size="12" color="black"></svg-icon>
          <span>{{ t('common.share') }}</span>
        </button>
      </div>

      <!-- 隐藏的 audio 元素 -->
      <audio
          ref="audioRef"
          :src="shareInfo.fileUrl"
          @timeupdate="handleTimeUpdate"
          @loadedmetadata="handleLoadedMetadata"
          @play="handleAudioPlay"
          @pause="handleAudioPause"
          @ended="handleAudioEnded"
      ></audio>
    </div>
  </div>
</template>

<script setup>
import {Loading} from '@element-plus/icons-vue';
import logoImg from '@/assets/common/logo2.png';
import {useSharePage} from './useSharePage';

const {
  t,
  loading,
  shareInfo,
  videoRef,
  audioRef,
  isPlaying,
  currentTime,
  duration,
  progressPercent,
  mobileLyricTimeline,
  currentMobileLyricIndex,
  mobileLyricTranslateY,
  isLyricDragging,
  formatDuration,
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
} = useSharePage();
</script>

