<template>
  <!-- pl-[76px]：内容整体右移让出悬浮菜单栏宽度，恢复原布局；背景视频与右上角账户区为 absolute，不受此内边距影响，仍横向铺满整页（含菜单栏区域） -->
  <div ref="homeScrollEl" class="relative h-full bg-black overflow-x-hidden overflow-y-auto home-scroll-container pl-[76px]" @scroll="handleWorksScroll">
    <!-- 顶部背景视频：五个视频顺序轮播，容器按 16:9 固定比例占位；两个常驻 <video> 一个在播、一个预载下一段，靠 opacity 交叉淡化切换，彻底消除换源时的黑闪 -->
    <div class="bg-video-stage absolute left-0 top-0 z-0 w-full pointer-events-none select-none">
      <video ref="bgVideoA" class="bg-video" muted playsinline preload="auto" @ended="handleBgVideoEnded"></video>
      <video ref="bgVideoB" class="bg-video" :class="{ 'is-hidden': bgActiveIsA }" muted playsinline preload="auto" @ended="handleBgVideoEnded"></video>
      <!-- 底部过渡矩形：上透明下纯黑的渐变块，下沿比视频下边界再低 10px，把视频与背景之间的硬分界压在近黑处过渡掉 -->
      <div class="absolute inset-x-0 -bottom-[10px] h-48 bg-gradient-to-b from-transparent to-black"></div>
    </div>

    <!--
      右上角账户区：原本在左侧菜单栏底部，现挪到首页顶部。
      未登录态显示「升级 / 立即登录」，登录态显示「邀请 / 升级(+积分hover) / 头像(+用户信息hover)」。
      用 absolute（相对外层 .relative 滚动容器）：随页面一起滚走，避免下滑到 MV 预览时盖在视频上；
      z-[2] 高于背景光晕和 hero，但远低于 Element Plus 弹窗（默认 z-[2000+]）。
    -->
    <div class="absolute top-5 right-6 z-[2]">
      <TopBar mode="menu" upgrade-tracking="HOME_CLICK_UPGRADE" />
    </div>

    <!-- 站点标题：与菜单栏 logo 右侧对齐（left/top 复刻 logo 几何），随滚动容器一起向上滚走，不再常驻 -->
    <span class="absolute left-[66px] top-[48px] -translate-y-1/2 z-[2] text-[32px] font-extrabold leading-none tracking-tight whitespace-nowrap select-none pointer-events-none">
      <span class="text-white">Oh</span><span class="text-[#C2FF00]">Yes</span><span class="text-white">AI</span>
    </span>

    <!-- 第一屏：标题 + 创建区，最小占满一屏，确保瀑布流不出现 -->
    <div class="home-hero-screen relative z-[1] flex-center">
    <!-- 头部标题区域 -->
    <header class="absolute left-0 right-0 top-0 flex-col-center pt-[140px]">
      <h1 class="text-[64px] leading-[72px] font-bold text-white mb-6 select-none" style="font-family: Alimama ShuHeiTi, Alimama ShuHeiTi;">
        Visualize your sound
      </h1>
      <p class="text-[36px] leading-[44px] font-normal text-white select-none" style="font-family: Source Han Sans, Source Han Sans;">
        {{ t('home.heroSubtitle') }}
      </p>
    </header>

    <!-- 搜索/创建区域 -->
    <div>
      <div class="relative w-[1046px]">
        <!-- 输入框 -->
        <div
            class="home-search-inner relative mb-[6px] rounded-[20px] px-[14px] py-[10px] shadow-[0px_0px_40px_10px_rgba(194,255,0,0.3)] min-h-[190px] flex flex-col"
            :class="[{ 'is-drag-over': dragEnterCount > 0 }]"
            @dragenter.prevent="userStore.isLoggedIn && dragEnterCount++"
            @dragleave="dragEnterCount = Math.max(0, dragEnterCount - 1)"
            @dragover.prevent
            @drop.prevent="handleDrop"
        >
          <input ref="fileInputRef" type="file" :accept="CREATION_UPLOAD_ACCEPT" multiple class="hidden" @change="handleFileChange"/>
          <MusicTrimmer
              v-model="showTrimmer"
              :raw-file="rawFile"
              :duration="audioDuration"
              :preparing="isPreparingAudio"
              @confirm="handleTrimmerConfirm"
              @cancel="showTrimmer = false"
          />

          <div class="flex-1 min-h-0 flex flex-col pb-[52px]">
            <!-- 已上传附件展示区域 -->
            <div v-if="hasAttachments" class="mb-2">
              <!-- 音频标签 -->
              <div v-if="uploadedFile.fileId" class="relative h-[36px] max-w-[176px] inline-flex items-center px-2 rounded-[10px] border border-[#A9D936] bg-[rgba(216,216,216,0.3)] mb-2">
                <svg-icon class="shrink-0" name="gy-audiofiles" size="20" color="#C2FF00"></svg-icon>
                <span class="ml-2 text-white text-[14px] leading-[20px] truncate pr-2">{{
                    uploadedFile.fileName
                  }}</span>
                <span v-if="isAudioUploading" class="audio-upload-loading shrink-0 ml-1">
                  <i class="audio-upload-loading__spinner"></i>
                  {{ t('common.loading') }}
                </span>
                <svg-icon
                    name="gy-closure"
                    size="14"
                    color="white"
                    class="cursor-pointer shrink-0 absolute -top-[5px] -right-[5px]"
                    @click="clearUploadedAudio"
                ></svg-icon>
              </div>
              <!-- 图片缩略图 -->
              <div v-if="uploadedImages.length" class="flex flex-wrap items-start gap-2">
                <div v-for="(img, idx) in uploadedImages" :key="img.id" class="intention-select-host relative group w-[88px] h-[88px] shrink-0">
                  <el-image
                      :src="img.previewUrl"
                      :preview-src-list="uploadedImages.map(i => i.previewUrl)"
                      :hide-on-click-modal="true"
                      :initial-index="idx"
                      preview-teleported
                      fit="cover"
                      class="w-[88px] h-[88px] rounded-[10px] cursor-pointer"
                  />
                  <svg-icon
                      name="gy-closure"
                      size="14"
                      color="white"
                      class="cursor-pointer shrink-0 absolute -top-[5px] -right-[5px] opacity-0 group-hover:opacity-100 transition-opacity z-[2]"
                      @click="handleRemoveImage(idx)"
                  ></svg-icon>
                  <el-select
                      v-model="img.intention"
                      size="small"
                      class="intention-select"
                      :class="{ 'is-expanded': img.uiExpanded }"
                      popper-class="intention-dropdown"
                      :teleported="true"
                      @visible-change="(visible) => handleImageIntentionVisibleChange(img, visible)"
                  >
                    <template #prefix>
                      <svg-icon :name="intentionOptions.find(opt => opt.value === img.intention)?.icon" size="14" color="white"></svg-icon>
                    </template>
                    <el-option v-for="opt in intentionOptions" :key="opt.value" :label="opt.label" :value="opt.value">
                      <div class="intention-option">
                        <svg-icon :name="opt.icon" size="12" color="currentColor"></svg-icon>
                        <span>{{ opt.label }}</span>
                      </div>
                    </el-option>
                  </el-select>
                </div>
              </div>
            </div>

            <div class="relative flex-1 min-h-[56px] overflow-hidden flex">
              <textarea
                  v-model="searchText"
                  class="home-textarea flex-1 block w-full bg-transparent text-white text-[18px] outline-none resize-none"
                  :class="hasAttachments ? 'pt-1' : ''"
                  @keydown.enter="handleTextareaEnter"
                  @paste="handlePasteUpload"
                  @compositionstart="handleTextareaCompositionStart"
                  @compositionend="handleTextareaCompositionEnd"
              ></textarea>
              <Transition name="placeholder-slide">
                <span
                    v-if="!searchText && !isComposing"
                    :key="placeholderIndex"
                    class="absolute inset-0 text-white/60 pointer-events-none whitespace-nowrap overflow-hidden text-ellipsis leading-7 text-[18px]"
                    :class="hasAttachments ? 'pt-1' : ''"
                >{{ rotatingPlaceholder }}</span>
              </Transition>
            </div>
          </div>

          <div class="absolute left-[14px] right-[14px] bottom-[10px] flex items-end justify-between">
            <div class="flex items-center gap-2">
              <!-- 加号：悬浮提示 + 触发文件选择 -->
              <el-popover placement="bottom-end" trigger="hover" popper-class="upload-tip-popover" :show-arrow="false" :offset="10">
                <template #reference>
                  <span class="cursor-pointer flex-center" @click="ensureLoggedInForUpload() && fileInputRef.click()">
                    <svg-icon name="gy-upload" size="24" color="#ADE300"></svg-icon>
                  </span>
                </template>
                <div class="upload-tip-content">
                  <div class="upload-tip-title">{{ t('home.upload.title') }}</div>
                  <div class="upload-tip-desc">{{ t('home.upload.imageLimit') }}</div>
                  <div class="upload-tip-desc">{{ t('home.upload.audioTypes') }}</div>
                </div>
              </el-popover>

              <el-popover
                  v-model:visible="modelPopoverVisible"
                  placement="bottom-start"
                  trigger="click"
                  popper-class="scene-model-select-dropdown"
                  :show-arrow="false"
                  :offset="10"
              >
                <template #reference>
                  <div class="inline-flex h-6 items-center gap-1.5 rounded-[6px] bg-[#D8D8D8]/10 px-[10px] cursor-pointer text-[13px] text-white shrink-0">
                    <svg-icon name="gy-model" size="18" color="#BEFA00"></svg-icon>
                    <span>{{ selectedModelLabel }}</span>
                  </div>
                </template>
                <div class="el-select-dropdown__header">{{ t('home.model') }}</div>
                <div
                    v-for="item in modelStore.modelOptions"
                    :key="item.value"
                    class="el-select-dropdown__item"
                    :class="{ 'is-selected': item.value === modelStore.selectedModel }"
                    @click="handleSelectModel(item.value)"
                >
                  <span class="scene-model-item-main">{{ item.label }}</span>
                  <span class="scene-model-item-meta">{{ item.costText }}</span>
                </div>
              </el-popover>

              <el-popover
                  v-model:visible="resolutionPopoverVisible"
                  placement="bottom-start"
                  trigger="click"
                  popper-class="resolution-select-dropdown"
                  :show-arrow="false"
                  :offset="10"
              >
                <template #reference>
                  <div class="inline-flex h-6 items-center gap-1 rounded-[6px] bg-[#D8D8D8]/10 px-[10px] cursor-pointer text-[13px] shrink-0">
                    <span class="text-[#C2FF00]">{{ selectedResolutionOption?.tag }}</span>
                    <span class="text-white">{{ selectedResolutionOption?.label }}</span>
                  </div>
                </template>
                <div class="resolution-select-body">
                  <div
                      v-for="item in RESOLUTION_OPTIONS"
                      :key="item.value"
                      class="el-select-dropdown__item"
                      :class="{ 'is-selected': item.value === modelStore.selectedResolution }"
                      @click="handleSelectResolution(item.value)"
                  >
                    <span class="scene-model-item-main">{{ item.label }}</span>
                  </div>
                </div>
              </el-popover>
            </div>
            <button class="inline-flex h-[42px] items-center rounded-[10px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] px-[30px] text-[16px] text-[#000000] font-normal cursor-pointer hover:opacity-90 transition-opacity" @click="handleStartCreation">
              <span>{{ t('home.createNow') }}</span>
              <span v-if="isNotEmpty(uploadedAudioEstimatedPoints)" class="mx-2 h-4 w-px bg-black/30"></span>
              <span v-if="isNotEmpty(uploadedAudioEstimatedPoints)" class="text-[16px]">
                {{ uploadedAudioEstimatedPoints }}{{ t('common.points') }}
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>

    </div>

    <!-- 作品展示区域：瀑布流组件 -->
    <section ref="worksSectionEl" class="px-8 pb-12" :aria-label="t('home.worksAriaLabel')">
      <!-- 加载中（首次） -->
      <div v-if="worksLoading && worksList.length === 0" class="flex-center py-20 text-white/30">
        <span class="text-[16px]">{{ t('common.loading') }}</span>
      </div>
      <!-- 空状态 -->
      <div v-else-if="!worksLoading && worksList.length === 0" class="flex-center py-20 text-white/30">
        <span class="text-[16px]">{{ t('home.emptyWorks') }}</span>
      </div>
      <div
          v-else
          :class="{ 'max-h-[1400px] overflow-hidden': !worksExpanded }"
          class="relative"
      >
        <MasonryWall
          :items="worksList"
          :column-width="280"
          :max-columns="5"
          :gap="24"
          :scroll-container="worksMasonryScrollTarget"
          @redraw="onWorksMasonryRedraw"
        >
          <template #default="{ item }">
          <div
              class="group cursor-pointer transition-transform duration-300 hover:scale-[1.03]"
              @click="handleOpenShare(item)"
              @mouseenter="handleCardEnter"
              @mouseleave="handleCardLeave"
          >
            <div class="relative rounded-2xl overflow-hidden shadow-lg bg-white/5" :style="{ aspectRatio: item._coverRatio || 16/9 }">
              <img v-if="item.fileCoverUrl" :src="item.fileCoverUrl" :alt="item.projectName ? t('home.projectCoverAlt', {name: item.projectName}) : t('home.workCoverAlt')" class="w-full h-full object-cover block" loading="lazy"/>
              <div v-else class="w-full h-full bg-white/5 flex-center">
                <svg-icon name="gy-MV" size="40" color="rgba(255,255,255,0.2)"></svg-icon>
              </div>
              <!--
                悬浮预览视频：mouseenter 时 play()，mouseleave 时 pause() 并归零。
                muted/playsinline 满足浏览器自动播放策略；preload="none" + poster 保证未触发时不占带宽且首帧无白屏。
              -->
              <video
                  v-if="item.fileUrl"
                  class="mv-preview-video absolute inset-0 w-full h-full object-cover opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none"
                  :src="item.fileUrl"
                  :poster="item.fileCoverUrl"
                  :muted="isPreviewMuted"
                  loop
                  playsinline
                  preload="none"
              ></video>
              <!-- 底部信息条：头像+昵称 / 静音切换 + 时长。hover 时淡入，与播放视频共存（视频在下，信息条在上） -->
              <div class="absolute bottom-0 left-0 right-0 px-3 pb-2.5 pt-8 bg-gradient-to-t from-black/70 to-transparent flex justify-between items-end opacity-0 group-hover:opacity-100 transition-opacity z-10">
                <div class="flex items-center gap-1.5 min-w-0">
                  <img
                      :src="item.avatar || defaultAvatar"
                      :alt="item.nickName ? t('home.avatarAltByName', {name: item.nickName}) : t('home.avatarAltFallback')"
                      class="w-5 h-5 rounded-full shrink-0 object-cover"
                  />
                  <span class="text-white text-[12px] truncate">{{ item.nickName || t('common.user') }}</span>
                </div>
                <div class="flex items-center gap-2 shrink-0 ml-2">
                  <!-- 静音切换：@click.stop 阻止冒泡到卡片跳转分享页；状态走全局 isPreviewMuted，所有卡片共享 -->
                  <button
                      type="button"
                      class="bg-transparent border-0 p-0 cursor-pointer flex-center hover:opacity-80 transition-opacity"
                      @click.stop="handleTogglePreviewMute"
                  >
                    <svg-icon :name="isPreviewMuted ? 'gy-muted' : 'gy-unmute'" size="16" color="rgba(255,255,255,0.85)"></svg-icon>
                  </button>
                  <span v-if="item.duration" class="text-white/80 text-[12px]">{{ formatDuration(item.duration) }}</span>
                </div>
              </div>
            </div>
            <div class="mt-3 text-white/90 text-[14px] truncate px-1">《{{ item.projectName || t('common.untitled') }}》</div>
          </div>
        </template>
      </MasonryWall>
        <!-- 展开按钮：渐变遮罩 + 点击展开全部 -->
        <div
            v-if="!worksExpanded && worksList.length > 0"
            class="absolute bottom-0 left-0 right-0 h-[200px] bg-gradient-to-t from-black via-black/60 to-transparent flex items-end justify-center pb-6 cursor-pointer"
            @click="worksExpanded = true"
        >
          <span class="text-white/80 text-[14px] hover:text-white transition-colors flex items-center gap-1">{{ t('home.expandWorks') }} <svg-icon name="gy-expand-1" size="14" color="rgba(255,255,255,0.8)"></svg-icon></span>
        </div>
      </div>
      <!-- 底部加载更多 -->
      <div v-if="worksLoading && worksList.length > 0" class="flex-center py-6 text-white/30">
        <span class="text-[14px]">{{ t('home.loadMore') }}</span>
      </div>
    </section>

    <!-- 数据亮点条（黑色 billboard，底部分隔线，与下方制作流程留出间距） -->
    <section class="bg-black px-8 py-[72px] border-b border-white/[0.06]">
      <div class="max-w-[1100px] mx-auto flex justify-center">
        <div
            v-for="(s, i) in homeStats"
            :key="i"
            class="flex-1 flex flex-col items-center text-center px-8"
            :class="i > 0 ? 'border-l border-white/10' : ''"
        >
          <div class="flex items-baseline justify-center leading-none">
            <span class="text-[64px] font-extrabold text-[#C2FF00]">{{ s.value }}</span>
            <span v-if="s.unit" class="ml-1 text-[26px] font-bold text-[#6b7a3a]">{{ s.unit }}</span>
          </div>
          <div class="mt-4 text-[14px] text-white/45">{{ s.label }}</div>
        </div>
      </div>
    </section>

    <!-- 制作流程板块 -->
    <HomeWorkflow />

    <!-- 六大核心优势板块 -->
    <HomeFeatures />

    <!-- 用户反馈板块（暂时隐藏） -->
    <HomeTestimonials v-if="false" />

    <!-- 常见问题板块 -->
    <HomeFaq />

    <!-- 定价板块 -->
    <HomePricing />

    <!-- 最终 CTA -->
    <HomeCta />

    <!-- 页脚 -->
    <HomeFooter />

    <!-- 回到顶部：滚动超过一屏后淡入，fixed 固定右下角不随内容滚走，点击平滑滚回顶部 -->
    <button
        class="back-to-top-btn fixed bottom-8 right-8 z-20 cursor-pointer"
        :class="{ 'is-visible': showBackToTop }"
        :aria-label="t('home.backToTop')"
        :title="t('home.backToTop')"
        @click="scrollToTop"
    >
      <svg-icon class="opacity-50" name="gy-go-top" size="40" color="#C2FF00"></svg-icon>
    </button>

  </div>
</template>

<script setup>
import {ref, computed, onMounted, nextTick, onBeforeUnmount, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {ElMessage} from 'element-plus';
import MasonryWall from '@yeger/vue-masonry-wall';
import HomeWorkflow from './components/HomeWorkflow.vue';
import HomeFeatures from './components/HomeFeatures.vue';
import HomeTestimonials from './components/HomeTestimonials.vue';
import HomeFaq from './components/HomeFaq.vue';
import HomePricing from './components/HomePricing.vue';
import HomeCta from './components/HomeCta.vue';
import HomeFooter from './components/HomeFooter.vue';
import {getPointsPrice, uploadFile} from '@/api/creation';
import {shareLink} from '@/api/share';
import {saveUserTracking} from '@/api/tracking';
import {
  isNotEmpty,
  CREATION_UPLOAD_ACCEPT,
  compressImageBeforeUpload,
  formatDuration,
  handlePasteUploadFiles,
  handleDropUploadFiles,
  processUploadFiles,
  isSupportedAudioUpload,
  isSupportedImageUpload,
} from '@/utils/index.js';
import {useUserStore} from '@/store/user';
import {useModelStore, RESOLUTION_OPTIONS} from '@/store/model';
import MusicTrimmer from '@/views/creation/components/MusicTrimmer.vue';
import {useAudioTrimUpload} from '@/composables/useAudioTrimUpload';
import defaultAvatar from '@/assets/common/avatar.jpg';
import bgVideo1 from '@/assets/home/1.mp4';
import bgVideo2 from '@/assets/home/2.mp4';
import bgVideo3 from '@/assets/home/3.mp4';
import bgVideo4 from '@/assets/home/4.mp4';
import bgVideo5 from '@/assets/home/5.mp4';
import TopBar from '@/components/topbar/TopBar.vue';
import {useTopBarAccount} from '@/composables/useTopBarAccount.js';
import {useI18nText} from '@/i18n';
import {useRotatingPlaceholder} from './composables/useRotatingPlaceholder.js';
import {useHomeWorksWall} from './composables/useHomeWorksWall.js';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const {t} = useI18nText();

// ==== 顶部背景视频：十个视频交叉淡化轮播 ====
/** 十个背景视频，按下标顺序循环播放。 */
const bgVideos = [bgVideo1, bgVideo2, bgVideo3, bgVideo4, bgVideo5];
/** 两个常驻 <video>：一个在播（可见），另一个预载下一段（隐藏）；交叉淡化切换，避免换源黑闪。 */
const bgVideoA = ref(null);
const bgVideoB = ref(null);
/** 当前可见的是不是 A（仅 B 的 opacity 参与过渡，A 始终在底、不透明垫着，避免交叉淡化中途变暗）。 */
const bgActiveIsA = ref(true);
/** 当前可见视频对应的下标。 */
let bgIndex = 0;
/** 淡出/遮盖完成后才给旧视频换源预载的延时句柄。 */
let bgSwapTimer = null;

/** 当前活动 / 备用（预载中）视频元素。 */
const bgActiveEl = () => (bgActiveIsA.value ? bgVideoA.value : bgVideoB.value);
const bgStandbyEl = () => (bgActiveIsA.value ? bgVideoB.value : bgVideoA.value);

/** 一段播完：备用视频（已预载下一段）淡入、当前视频淡出，交叉淡化无黑闪；淡出/遮盖完成后再给旧视频换源预载再下一段。 */
const handleBgVideoEnded = () => {
  const incoming = bgStandbyEl();
  const outgoing = bgActiveEl();
  if (!incoming || !outgoing) return;
  incoming.currentTime = 0;
  incoming.play().catch((error) => console.error('背景视频续播失败:', error));
  bgActiveIsA.value = !bgActiveIsA.value; // 触发 CSS opacity 交叉淡化（仅 B 过渡）
  bgIndex = (bgIndex + 1) % bgVideos.length;
  // 此时旧视频已被遮盖/淡出到不可见，再给它换源预载下一段，换源的黑帧看不到
  if (bgSwapTimer) clearTimeout(bgSwapTimer);
  bgSwapTimer = window.setTimeout(() => {
    outgoing.src = bgVideos[(bgIndex + 1) % bgVideos.length];
    outgoing.load();
  }, 850);
};

onMounted(() => {
  // A 播第一段、B 预载第二段
  if (!bgVideoA.value || !bgVideoB.value) return;
  bgVideoA.value.src = bgVideos[0];
  bgVideoB.value.src = bgVideos[1 % bgVideos.length];
  bgVideoA.value.play().catch((error) => console.error('背景视频播放失败:', error));
});

onBeforeUnmount(() => {
  if (bgSwapTimer) clearTimeout(bgSwapTimer);
  // 释放视频解码内存
  [bgVideoA.value, bgVideoB.value].forEach((v) => {
    if (!v) return;
    v.pause();
    v.removeAttribute('src');
    v.load();
  });
});

/** 首页底部数据亮点条：数值/单位为通用符号直接写死，仅描述文案走 i18n。 */
const homeStats = computed(() => [
  {value: '2,700', label: t('home.stats.signupPoints')},
  {value: '5', unit: 'min', label: t('home.stats.fullTrack')},
  {value: '3,000+', label: t('home.stats.creators')},
  {value: '5', unit: t('home.stats.stepUnit'), label: t('home.stats.steps')},
]);

// ==== 右上角账户区：dialog 状态与跳转方法已封进 TopBar
// 此处只取 composable 暴露的 ref 用于 onMounted 时回填邀请码并打开登录弹窗 ====
const {loginVisible, referralCode} = useTopBarAccount();

/**
 * 若 URL 带 `?login=1`，未登录用户自动打开登录弹窗，然后从地址栏剥掉这个参数，
 * 避免刷新后再次弹出。该参数用于"未登录从订阅页跳回首页"的统一登录入口。
 */
const consumeLoginQuery = () => {
  if (route.query.login !== '1') return;
  if (!userStore.isLoggedIn) loginVisible.value = true;
  const {login: _login, ...rest} = route.query;
  router.replace({query: rest});
};
/** 首页主滚动根节点（瀑布流与加载更多均依赖此层 overflow） */
const homeScrollEl = ref(null);
/** 作品区容器：用于观察瀑布流重排/图片解码导致的高度变化 */
const worksSectionEl = ref(null);
/** 作品区是否展开全部 */
const worksExpanded = ref(false);

/**
 * 从节点向上查找实际发生纵向滚动的元素；未找到时退回 el 自身。
 * flex 子项未加 min-h-0 时，scroll 常落在外层 document，与瀑布流默认的 window 恢复混用会一起把位置顶回顶部。
 */
const getWorksScrollRoot = (el) => {
  let node = el;
  while (node) {
    const oy = getComputedStyle(node).overflowY;
    if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && node.scrollHeight > node.clientHeight) {
      return node;
    }
    node = node.parentElement;
  }
  const doc = document.scrollingElement;
  if (doc && doc.scrollHeight > doc.clientHeight) return doc;
  return el;
};

/** 与瀑布流 redraw 使用同一滚动根，避免错绑到 window 或子元素 */
const worksMasonryScrollTarget = computed(() => {
  const start = homeScrollEl.value;
  if (!start) return null;
  return getWorksScrollRoot(start);
});

/**
 * items 追加触发 masonry 重排后，恢复滚动位置避免“回弹/跳动”。
 * 优先采用“距离底部的 gap”恢复（对瀑布流多列重排更稳）；其次才退回高度差法。
 */
let pendingWorksScrollRestore = null;
let worksRestoreObserver = null;
let worksRestoreRafId = 0;
let worksRestoreStableTimerId = 0;

/**
 * 安排一次恢复（rAF 合并多次高度变化）。
 * @param {string} reason
 */
const scheduleWorksScrollRestore = (reason = 'unknown') => {
  if (!pendingWorksScrollRestore) return;
  if (worksRestoreRafId) cancelAnimationFrame(worksRestoreRafId);
  worksRestoreRafId = requestAnimationFrame(() => {
    worksRestoreRafId = 0;
    applyPendingWorksScrollRestore(reason);
  });
};

/**
 * 将滚动条恢复到“追加前看到的内容位置”。
 * @param {string} reason
 */
const applyPendingWorksScrollRestore = () => {
  const pending = pendingWorksScrollRestore;
  if (!pending) return;
  pendingWorksScrollRestore = null;
  const {el, prevTop, prevScrollH, prevBottomGap} = pending;
  if (!el?.isConnected) return;
  if (prevBottomGap >= 0) {
    const nextTop = el.scrollHeight - el.clientHeight - prevBottomGap;
    el.scrollTop = Math.max(0, nextTop);
  } else {
    el.scrollTop = prevTop + (el.scrollHeight - prevScrollH);
  }

  // 进入“稳定窗口”：若后续高度继续变化（masonry 二次测量/图片解码），observer 会触发再次收敛
  if (worksRestoreStableTimerId) clearTimeout(worksRestoreStableTimerId);
  worksRestoreStableTimerId = window.setTimeout(() => {
    worksRestoreStableTimerId = 0;
    if (worksRestoreObserver) {
      worksRestoreObserver.disconnect();
      worksRestoreObserver = null;
    }
  }, 220);
};

const onWorksMasonryRedraw = () => {
  nextTick(() => scheduleWorksScrollRestore('masonry-redraw'));
};
const searchText = ref('');
const {
  isComposing,
  placeholderIndex,
  rotatingPlaceholder,
  handleCompositionStart: handleTextareaCompositionStart,
  handleCompositionEnd: handleTextareaCompositionEnd,
} = useRotatingPlaceholder(() => t('home.placeholders'));
const modelStore = useModelStore();
const modelPopoverVisible = ref(false);
const isAudioPointsInsufficient = computed(() => {
  if (!isNotEmpty(uploadedAudioEstimatedPoints.value)) return false;
  if (!isNotEmpty(userStore.pointsBalance)) return false;
  return uploadedAudioEstimatedPoints.value > userStore.pointsBalance;
});
const handleSelectModel = (value) => {
  modelStore.setSelectedModel(value);
  modelPopoverVisible.value = false;
};

const resolutionPopoverVisible = ref(false);

/** 当前选中模型的展示名（如 Seedance 2.0 Fast）。 */
const selectedModelLabel = computed(() => modelStore.modelOptions.find((item) => item.value === modelStore.selectedModel)?.label);

/** 当前选中分辨率选项（含清晰度标签 tag 与展示名 label）。 */
const selectedResolutionOption = computed(() => RESOLUTION_OPTIONS.find((item) => item.value === modelStore.selectedResolution));

const handleSelectResolution = (value) => {
  modelStore.setSelectedResolution(value);
  resolutionPopoverVisible.value = false;
};

const fileInputRef = ref(null);
const uploadedFile = ref({fileId: '', fileName: ''});
const uploadedAudioTrimDuration = ref(0);
const uploadedAudioEstimatedPoints = ref(null);

// 图片上传状态
const uploadedImages = ref([]); // { id, fileId, fileName, previewUrl, intention }
const MAX_IMAGES = 6;

// intention 枚举选项（跟随当前语言动态变化）
const intentionOptions = computed(() => [
  {label: t('creation.intention.character'), value: 'CHARACTER', icon: 'gy-character'},
  {label: t('creation.intention.costume'), value: 'COSTUME', icon: 'gy-clothes'},
  {label: t('creation.intention.environment'), value: 'ENVIRONMENT', icon: 'gy-environment'},
  {label: t('creation.intention.prop'), value: 'PROP', icon: 'gy-prop'},
  {label: t('creation.intention.style'), value: 'STYLE', icon: 'gy-style'},
]);

// 是否有任何附件
const hasAttachments = computed(() => uploadedImages.value.length > 0 || !!uploadedFile.value.fileId);
const isAudioUploading = computed(() => String(uploadedFile.value.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX));

const {showTrimmer, rawFile, audioDuration, isPreparingAudio, openWithFile, uploadTrimmedBlob} = useAudioTrimUpload();

const ensureLoggedInForUpload = () => {
  if (userStore.isLoggedIn) return true;
  ElMessage.warning(t('common.loginRequired'));
  return false;
};

/**
 * 文件选择后根据类型分流处理
 */
const handleFileChange = async (e) => {
  const files = Array.from(e.target.files || []);
  e.target.value = '';
  if (!files.length) return;
  if (!ensureLoggedInForUpload()) return;
  await processUploadFiles(files, {
    maxImages: MAX_IMAGES,
    currentImagesCount: uploadedImages.value.length,
    onImageFile: handleIncomingFile,
    onAudioFile: handleIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
    onMultipleAudio: () => ElMessage.warning(t('home.audioLimit')),
  });
};

/**
 * 处理所有入口的文件（点击上传 / 粘贴）
 */
const handleIncomingFile = async (file) => {
  // 图片文件
  if (isSupportedImageUpload(file)) {
    if (uploadedImages.value.length >= MAX_IMAGES) {
      ElMessage.warning(t('home.uploadLimit', {count: MAX_IMAGES}));
      return;
    }
    if (file.size / 1024 / 1024 >= 10) {
      ElMessage.error(t('home.imageTooLarge'));
      return;
    }
    await handleUploadImage(file);
    return;
  }

  // 音频文件
  if (isSupportedAudioUpload(file)) {
    await openWithFile(file);
    return;
  }

  ElMessage.error(t('home.uploadTypeError'));
};

/**
 * 处理粘贴上传（图片或 mp3/wav）
 */
const handlePasteUpload = async (event) => {
  if (!ensureLoggedInForUpload()) return;
  await handlePasteUploadFiles(event, {
    maxImages: MAX_IMAGES,
    currentImagesCount: uploadedImages.value.length,
    onImageFile: handleIncomingFile,
    onAudioFile: handleIncomingFile,
    onOverLimit: (maxImages) => {
      ElMessage.warning(t('home.uploadLimit', {count: maxImages}));
    },
  });
};

const dragEnterCount = ref(0);

const handleDrop = async (event) => {
  dragEnterCount.value = 0;
  if (!ensureLoggedInForUpload()) return;
  await handleDropUploadFiles(event, {
    maxImages: MAX_IMAGES,
    currentImagesCount: uploadedImages.value.length,
    onImageFile: handleIncomingFile,
    onAudioFile: handleIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
    onMultipleAudio: () => ElMessage.warning(t('home.audioLimit')),
  });
};

/**
 * 上传图片文件
 */
const handleUploadImage = async (file) => {
  try {
    const compressedFile = await compressImageBeforeUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile, compressedFile.name);
    const result = await uploadFile(formData);
    uploadedImages.value.push({
      id: Date.now(),
      fileId: result.fileId,
      fileName: compressedFile.name,
      previewUrl: URL.createObjectURL(compressedFile),
      fileUrl: result.fileUrl || '',
      intention: 'CHARACTER',
      uiExpanded: false,
    });
  } catch (error) {
    console.error('图片上传失败:', error);
    ElMessage.error(error?.message || t('home.imageUploadFail'));
  }
};

/** 图片意图下拉显示状态变更 */
const handleImageIntentionVisibleChange = (img, visible) => {
  img.uiExpanded = visible;
};

/** 删除已上传图片 */
const handleRemoveImage = (idx) => {
  const img = uploadedImages.value[idx];
  if (img?.previewUrl) URL.revokeObjectURL(img.previewUrl);
  uploadedImages.value.splice(idx, 1);
};


/**
 * 裁剪确认后上传，写入 uploadedFile
 */
const TEMP_AUDIO_FILE_ID_PREFIX = 'temp_audio_';
const DEFAULT_IMAGE_COUNT = 7;
const IMAGE_POINTS_PER_UNIT = 28;
const POINTS_UPLIFT_RATE = 0.15;

const clearUploadedAudio = () => {
  uploadedFile.value = {fileId: '', fileName: '', fileUrl: ''};
  uploadedAudioTrimDuration.value = 0;
  uploadedAudioEstimatedPoints.value = null;
};

const estimateUploadedAudioPoints = async () => {
  // 首页为全新项目（无 sessionId），分辨率由用户当前选择，后端无 session 可取，需显式带上
  const mvPoints = await getPointsPrice({
    modelName: modelStore.selectedModel,
    taskType: 'MAKE_MV',
    duration: uploadedAudioTrimDuration.value,
    resolution: modelStore.selectedResolution,
  });
  const imagePoints = DEFAULT_IMAGE_COUNT * IMAGE_POINTS_PER_UNIT;
  return Math.ceil((mvPoints + imagePoints) * (1 + POINTS_UPLIFT_RATE));
};

const handleTrimmerConfirm = async (payload) => {
  const sourceFile = rawFile.value;
  const tempFileId = `${TEMP_AUDIO_FILE_ID_PREFIX}${Date.now()}`;
  const tempFileName = sourceFile?.name
      ? `${sourceFile.name.replace(/\.[^/.]+$/, '')}_trimmed_${payload?.timeRange || ''}.mp3`
      : 'audio_trimmed.mp3';

  uploadedFile.value = {fileId: tempFileId, fileName: tempFileName, fileUrl: ''};
  uploadedAudioTrimDuration.value = payload?.durationSeconds || 0;
  uploadedAudioEstimatedPoints.value = null;
  showTrimmer.value = false;

  try {
    const result = await uploadTrimmedBlob(payload, {silent: true, showSuccess: false, sourceFile});
    if (result) {
      uploadedFile.value = {fileId: result.fileId, fileName: result.fileName, fileUrl: result.fileUrl || ''};
      uploadedAudioEstimatedPoints.value = await estimateUploadedAudioPoints();
    }
  } catch {
    clearUploadedAudio();
  }
};

watch(() => modelStore.selectedModel, async (newModel, oldModel) => {
  if (!oldModel || newModel === oldModel) return;
  if (!uploadedFile.value.fileId || String(uploadedFile.value.fileId).startsWith(TEMP_AUDIO_FILE_ID_PREFIX)) return;
  if (uploadedAudioTrimDuration.value <= 0) return;

  try {
    const latestPoints = await estimateUploadedAudioPoints();
    const previousPoints = uploadedAudioEstimatedPoints.value;
    uploadedAudioEstimatedPoints.value = latestPoints;
    if (isNotEmpty(previousPoints) && previousPoints === latestPoints) return;
    ElMessage.warning(t('creation.musicTrimmer.pointsRecalculatedByModel', {points: latestPoints}));
  } catch {
    // ignore estimate errors on model switch hint
  }
});

/**
 * 处理文本域 Enter 键事件
 * Shift+Enter 或 Ctrl+Enter 换行，单独 Enter 发送
 * @param {KeyboardEvent} event
 */
const handleTextareaEnter = (event) => {
  if (event.shiftKey || event.ctrlKey) return;
  event.preventDefault();
  handleStartCreation();
};

/**
 * 跳转创作页，携带 prompt、model 及可选音频/图片参数
 */
const handleStartCreation = () => {
  if (!searchText.value.trim()) {
    ElMessage.warning(t('home.createPromptEmpty'));
    return;
  }
  const query = {prompt: searchText.value.trim()};
  if (String(uploadedFile.value.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX)) {
    ElMessage.warning(t('home.audioUploading'));
    return;
  }
  if (isAudioPointsInsufficient.value) {
    ElMessage.warning(t('account.points.insufficientForSend'));
    return;
  }
  if (uploadedFile.value.fileId) {
    query.audioFileId = uploadedFile.value.fileId;
    query.fileName = uploadedFile.value.fileName;
    query.audioUrl = uploadedFile.value.fileUrl || '';
  }
  if (uploadedImages.value.length > 0) {
    // 将图片信息序列化传递
    query.images = JSON.stringify(uploadedImages.value.map(img => ({
      fileId: img.fileId,
      fileName: img.fileName,
      intention: img.intention,
      fileUrl: img.fileUrl || '',
    })));
  }
  // 校验全通过、即将跳创作页 = 用户确实在发起一次新对话，打 CREATE_NEW_PROJECT 埋点
  saveUserTracking({target: 'CREATE_NEW_PROJECT'}).catch((error) => {
    console.error('首页发起新对话埋点上报失败:', error);
  });

  const lang = router.currentRoute.value?.params?.lang || 'zh';
  router.push({name: 'creation', params: {lang}, query});
};

// ── 作品展示 ──────────────────────────────────────────────────────────────
let latestScrollSnapshot = null;
const {
  worksList,
  worksLoading,
  worksHasMore,
  loadWorksList,
  handleWorksScroll: handleHomeWorksScroll,
} = useHomeWorksWall({
  pageSize: 999,
  scrollBottomOffset: 80,
  onBeforeLoad: ({reset}) => {
    if (reset) {
      pendingWorksScrollRestore = null;
      latestScrollSnapshot = null;
      if (worksRestoreObserver) {
        worksRestoreObserver.disconnect();
        worksRestoreObserver = null;
      }
      return;
    }
    if (!homeScrollEl.value) return;
    const el = getWorksScrollRoot(homeScrollEl.value);
    const prevBottomGap = el.scrollHeight - (el.scrollTop + el.clientHeight);
    latestScrollSnapshot = {el, prevTop: el.scrollTop, prevScrollH: el.scrollHeight, prevBottomGap};
    pendingWorksScrollRestore = latestScrollSnapshot;

    if (worksSectionEl.value && !worksRestoreObserver) {
      worksRestoreObserver = new ResizeObserver(() => {
        if (pendingWorksScrollRestore || worksRestoreStableTimerId) scheduleWorksScrollRestore('resize-observer');
      });
      worksRestoreObserver.observe(worksSectionEl.value);
    }
  },
  onError: (error) => {
    pendingWorksScrollRestore = null;
    latestScrollSnapshot = null;
    console.error(error);
    ElMessage.error(error?.message || t('home.worksLoadingFail'));
  },
  onFinally: ({reset}) => {
    if (reset) return;
    if (latestScrollSnapshot && pendingWorksScrollRestore === latestScrollSnapshot) {
      scheduleWorksScrollRestore('request-finally');
    }
    latestScrollSnapshot = null;
  },
});

/** 滚动超过一屏后展示「回到顶部」按钮 */
const showBackToTop = ref(false);

/** 平滑滚回顶部 */
const scrollToTop = () => {
  homeScrollEl.value.scrollTo({top: 0, behavior: 'smooth'});
};

/** 页面滚动：加载下一页 + 控制回到顶部按钮显隐 */
const handleWorksScroll = (e) => {
  handleHomeWorksScroll(e);
  showBackToTop.value = e.target.scrollTop > e.target.clientHeight;
};

/**
 * 预览视频全局静音状态：默认静音，用户在任意一张卡片上切换后所有卡片同步。
 * 用 ref + `:muted="isPreviewMuted"` 绑定，Vue 会在状态变化时同步到所有 <video> 的 muted IDL 属性，
 * 正在播放的视频也能立刻应用新状态。
 */
const isPreviewMuted = ref(true);

/** 切换静音：仅翻转状态，绑定到 video 的 :muted 会自动同步 DOM */
const handleTogglePreviewMute = () => {
  isPreviewMuted.value = !isPreviewMuted.value;
};

/**
 * 卡片悬停：从卡片 DOM 中找到 .mv-preview-video，从头播放。
 * 用 querySelector 而不是 ref 数组：MasonryWall 列表更新时不用维护 ref，更简单。
 * play() 在浏览器可能因策略 reject（已 muted + playsinline 通常没事），catch 静默处理。
 */
const handleCardEnter = (event) => {
  const video = event.currentTarget.querySelector('.mv-preview-video');
  if (!video) return;
  video.currentTime = 0;
  video.play().catch(() => {});
};

/** 卡片移开：暂停并归零，避免下次悬停接着上次进度播放 */
const handleCardLeave = (event) => {
  const video = event.currentTarget.querySelector('.mv-preview-video');
  if (!video) return;
  video.pause();
  video.currentTime = 0;
};

/**
 * 点击瀑布流卡片：调 shareLink 拿到 shareId，跳到分享页。
 * 分享页路由是 `name: 'share'`、`path: /:lang(en|zh)/share`，参数：
 *   - params.lang：当前语言前缀（缺省 zh）
 *   - query.shareId：shareLink 接口返回值（字符串）
 * useSharePage 里通过 route.query.shareId 拉详情，所以 query 名必须就是 shareId。
 */
const handleOpenShare = async (item) => {
  try {
    const shareId = await shareLink({projectId: item.projectId});
    const lang = router.currentRoute.value?.params?.lang || 'zh';
    router.push({name: 'share', params: {lang}, query: {shareId}});
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.message || t('home.shareFailed'));
  }
};

const reportHomePageTracking = async () => {
  try {
    await saveUserTracking({
      target: 'HOME_PAGE',
    });
  } catch (error) {
    console.error('首页埋点上报失败:', error);
  }
};

onMounted(() => {
  reportHomePageTracking();
  loadWorksList(true);

  // 邀请码 query 自动填入登录表单并打开登录弹窗
  const inviteCodeFromQuery = typeof route.query.inviteCode === 'string' ? route.query.inviteCode.trim() : '';
  if (!userStore.isLoggedIn && inviteCodeFromQuery) {
    referralCode.value = inviteCodeFromQuery;
    loginVisible.value = true;
  }
  consumeLoginQuery();

  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo();
    userStore.fetchUserPlan();
  }
});

watch(() => route.query.login, () => consumeLoginQuery());

onBeforeUnmount(() => {
  if (worksRestoreObserver) {
    worksRestoreObserver.disconnect();
    worksRestoreObserver = null;
  }
  if (worksRestoreRafId) {
    cancelAnimationFrame(worksRestoreRafId);
    worksRestoreRafId = 0;
  }
  if (worksRestoreStableTimerId) {
    clearTimeout(worksRestoreStableTimerId);
    worksRestoreStableTimerId = 0;
  }
});
</script>

<style lang="scss">
@use '@/styles/modelSelectDropdown.scss' as *;


// 上传提示气泡
.upload-tip-popover {
  @apply py-[6px] px-3 min-w-0;
  width: max-content !important;
  background: rgba(27, 31, 12, 0.1) !important;
  border: 1px solid #B3B3B3 !important;
  @apply backdrop-blur-[8px];
  border-radius: 10px !important;
  outline: none !important;
  --el-popover-border-color: transparent !important;
  box-shadow: none !important;
}

.upload-tip-content {
  @apply text-[14px] font-normal text-white leading-[22px] whitespace-nowrap;

  .upload-tip-title {
    @apply text-[#C2FF00];
  }
}

</style>

<style lang="scss" scoped>
.home-hero-screen {
  min-height: 100vh;
  min-height: max(100svh, 860px);
}

// 背景视频舞台：按 16:9 固定占位，切换/加载下一个视频时高度不塌陷
.bg-video-stage {
  aspect-ratio: 16 / 9;
}

// 背景视频：两个常驻视频叠放，靠 B 的 opacity 交叉淡化切换，避免换源黑闪（A 始终不透明垫底）
.bg-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.8s ease;
}

.bg-video.is-hidden {
  opacity: 0;
}

.home-search-inner {
  @apply relative;
  background: rgba(216, 216, 216, 0.1);
  transition: background 0.2s;
  width: 1046px;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(90deg, rgba(161, 255, 0, 1), rgba(130, 255, 121, 1));
    mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask: linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    pointer-events: none;
  }

  &.is-drag-over {
    background: rgba(194, 255, 0, 0.15);
  }
}

.placeholder-slide-enter-active,
.placeholder-slide-leave-active {
  transition: transform 0.4s ease, opacity 0.4s ease;
}

.placeholder-slide-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.placeholder-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.home-textarea {
  &::placeholder {
    @apply text-white/60;
  }

  /* 滚动条整体样式 */
  &::-webkit-scrollbar {
    width: 4px;
  }

  /* 滚动条轨道 */
  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.1);
    @apply rounded-[2px];
  }

  /* 滚动条滑块 */
  &::-webkit-scrollbar-thumb {
    background: rgba(194, 255, 0, 0.4);
    @apply rounded-[2px];

    &:hover {
      background: rgba(194, 255, 0, 0.6);
    }
  }
}

.audio-upload-loading {
  display: inline-flex;
  align-items: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  line-height: 16px;
  gap: 4px;
}

.audio-upload-loading__spinner {
  width: 12px;
  height: 12px;
  border-radius: 9999px;
  border: 2px solid rgba(194, 255, 0, 0.2);
  border-top-color: #c2ff00;
  animation: audio-upload-spin 0.8s linear infinite;
}

@keyframes audio-upload-spin {
  to {
    transform: rotate(360deg);
  }
}

.home-scroll-container {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.home-scroll-container::-webkit-scrollbar {
  display: none;
}

// 回到顶部按钮：半透明深色底 + 青柠箭头，与顶部功能按钮同色系；默认透明不可点，is-visible 时淡入
.back-to-top-btn {
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.25s ease, background 0.2s, box-shadow 0.2s;

  &.is-visible {
    opacity: 1;
    pointer-events: auto;
  }
}
</style>
