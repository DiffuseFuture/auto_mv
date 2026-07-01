<template>
  <!-- 加载中 -->
  <div v-if="loading" class="bg-black w-full h-full flex-col-center text-white/30">
    <el-icon :size="32" class="animate-spin mb-4">
      <Loading/>
    </el-icon>
    <span class="text-[16px]">{{ t('share.loading') }}</span>
  </div>

  <!-- 加载失败 -->
  <div v-else-if="!shareInfo" class="bg-black w-full h-full flex-center text-white/30 text-[16px]">
    {{ t('share.invalid') }}
  </div>

  <!-- MV 分享：全屏黑色蒙层 + 1200px 居中内容 -->
  <div v-else-if="shareInfo.type === 'VIDEO'" class="hidden md:flex flex-center w-full h-full bg-black/90 backdrop-blur-sm relative">
    <svg-icon
        name="gy-closure"
        :size="28"
        color="#FFFFFF"
        class="absolute top-10 right-10 cursor-pointer z-10"
        @click="handleClose"
    ></svg-icon>
    <div class="w-[1200px]">
      <!-- 顶部：返回按钮 + 项目名 -->
      <div class="mb-[18px] flex items-center gap-3">
        <button
            class="h-8 w-8 shrink-0 cursor-pointer rounded-full flex-center text-white/90 hover:text-white hover:bg-white/10 transition-colors"
            @click="handleClose"
        >
          <svg-icon name="gy-enter" size="20" class="rotate-180"></svg-icon>
        </button>
        <div class="min-w-0 text-[36px] leading-[44px] font-semibold text-white truncate">
          {{ shareInfo.projectName || t('share.unnamed') }}
        </div>
      </div>

      <!-- 视频 + 提示词两列 -->
      <div class="flex gap-[18px] h-[486px]">
        <div class="flex-1 min-w-0 bg-[#121212] rounded-[20px] overflow-hidden">
          <video
              ref="videoRef"
              :src="shareInfo.fileUrl"
              :poster="shareInfo.coverUrl"
              class="w-full h-full object-contain"
              controls
          ></video>
        </div>

        <!-- 右列：统一用 gap-3 收敛 mb-3 / mt-[14px] / space-y-3 三套间距 -->
        <div class="w-[320px] shrink-0 flex flex-col gap-3 min-h-0">
          <div class="flex-between shrink-0">
            <span class="text-[20px] leading-[28px] font-medium text-[#C2FF00]">{{ t('share.prompt') }}</span>
            <svg-icon name="gy-copy" size="20" class="cursor-pointer text-[#C2FF00]" @click="handleCopyPrompt"></svg-icon>
          </div>
          <div class="flex-1 min-h-0 overflow-y-auto no-scrollbar rounded-[16px] px-[18px] py-[18px] bg-[#F0F6DD] text-black text-[15px] leading-[24px] whitespace-pre-wrap break-words">
            {{ shareInfo.prompt || t('share.noPrompt') }}
          </div>
          <button
              class="h-[44px] w-full shrink-0 cursor-pointer rounded-[16px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] flex-center gap-[10px] text-[18px] text-black hover:brightness-105 transition"
              @click="handleGoCreate"
          >
            <svg-icon name="gy-process" size="20" color="#000000"></svg-icon>
            <span>{{ t('share.createProcess') }}</span>
          </button>
          <button
              class="h-[44px] w-full shrink-0 cursor-pointer rounded-[16px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] flex-center gap-[10px] text-[18px] text-black hover:brightness-105 transition"
              @click="handleShareLink"
          >
            <svg-icon name="gy-share" size="20" color="#000000"></svg-icon>
            <span>{{ t('common.share') }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 音乐分享 -->
  <div v-else class="w-full h-full relative bg-white">
    <!-- 光晕 -->
    <div class="absolute top-[18px] right-12 w-[480px] aspect-square bg-[#95FD88] blur-[300px]"></div>
    <div class="absolute top-[144px] left-0 w-[660px] aspect-square bg-[#BEFA00] blur-[300px]"></div>
    <div class="w-full h-full flex flex-col overflow-hidden bg-black/50 relative">
      <svg-icon
          name="gy-closure"
          :size="28"
          color="#FFFFFF"
          class="absolute top-10 right-10 cursor-pointer z-10"
          @click="handleClose"
      ></svg-icon>
      <!-- logo -->
      <div class="shrink-0 px-10 pt-[74px] flex items-center gap-4">
        <img :src="logoImg" alt="logo" class="w-9 h-9"/>
        <span class="text-white text-[36px] font-bold">ohyesai.com</span>
      </div>

      <!-- 卡片：自身 mx-auto + my-10 + flex-1 直接居中并占满中间区域，省去居中 wrapper -->
      <div class="flex-1 min-h-0 w-[800px] mx-auto my-10 p-5 rounded-[20px] bg-[#F0F6DD]/30 relative flex flex-col">
        <!-- 右上角操作按钮 -->
        <div class="absolute top-6 right-6 flex gap-3">
          <el-tooltip :content="t('share.createProcess')" placement="top">
            <button
                type="button"
                class="w-11 h-11 rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] flex-center cursor-pointer hover:opacity-90 transition-opacity"
                @click="handleGoCreate"
            >
              <svg-icon name="gy-process" size="20" color="black"></svg-icon>
            </button>
          </el-tooltip>
          <el-tooltip :content="t('common.share')" placement="top">
            <button
                type="button"
                class="w-11 h-11 rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] flex-center cursor-pointer hover:opacity-90 transition-opacity"
                @click="handleShareLink"
            >
              <svg-icon name="gy-share" size="20" color="black"></svg-icon>
            </button>
          </el-tooltip>
        </div>

        <!-- 封面 + 标题/进度条 -->
        <div class="flex gap-5 mb-6 shrink-0">
          <!-- 封面：播放图标用 absolute + translate -1/2 直接居中，省覆盖层 wrapper -->
          <div
              class="w-[200px] h-[200px] rounded-[20px] overflow-hidden shrink-0 relative cursor-pointer select-none"
              @click="toggleAudio"
          >
            <img v-if="shareInfo.coverUrl" :src="shareInfo.coverUrl" class="w-full h-full object-cover"/>
            <div v-else class="w-full h-full flex-center bg-black/30">
              <svg-icon name="gy-audio" size="48" color="rgba(255,255,255,0.2)"></svg-icon>
            </div>
            <svg-icon
                :name="isPlaying ? 'gy-pause' : 'gy-play'"
                size="46"
                color="#C2FF00"
                class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
            ></svg-icon>
          </div>

          <div class="flex-1 flex flex-col justify-between min-w-0">
            <div class="text-white text-[36px] leading-[44px] font-bold truncate">
              {{ shareInfo.projectName || t('share.unnamed') }}
            </div>
            <div class="flex flex-col gap-2">
              <div
                  class="w-full h-1 rounded-full bg-white/20 cursor-pointer relative hover:h-1.5"
                  @click="handleSeek"
              >
                <div
                    class="absolute left-0 top-0 h-full rounded-full bg-[#C2FF00]"
                    :style="{ width: progressPercent + '%' }"
                ></div>
              </div>
              <div class="flex justify-between text-[12px] text-white/50">
                <span>{{ formatDuration(currentTime) }}</span>
                <span>{{ formatDuration(duration) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 歌词：overflow-hidden + 内层 transform；wheel/mousedown 浏览；拖拽时关 transition 跟手 -->
        <div
            ref="pcLyricListRef"
            class="flex-1 min-h-0 overflow-hidden select-none"
            :class="isPcLyricDragging ? 'cursor-grabbing' : 'cursor-grab'"
            @wheel.prevent="handlePcLyricWheel"
            @mousedown="handlePcLyricMouseDown"
        >
          <div
              class="will-change-transform"
              :class="isPcLyricDragging ? '' : 'transition-transform duration-500 ease-out'"
              :style="{ transform: `translateY(${pcLyricTranslateY}px)` }"
          >
            <div
                v-for="(line, index) in pcLyricLines"
                :key="`pc-lyric-${index}`"
                class="h-[30px] leading-[30px] text-[16px] transition-colors"
                :class="index === currentMobileLyricIndex ? 'text-white font-semibold' : 'text-white/60'"
            >
              {{ line || '...' }}
            </div>
          </div>
        </div>
      </div>

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
  pcLyricListRef,
  pcLyricLines,
  pcLyricTranslateY,
  isPcLyricDragging,
  handlePcLyricWheel,
  handlePcLyricMouseDown,
  currentMobileLyricIndex,
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
} = useSharePage();
</script>

<style lang="scss" scoped>
.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}
</style>
