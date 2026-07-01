<template>
  <div class="flex-1 flex flex-col bg-[#141414]">
    <div ref="chatCenterRef" class="flex-1 flex justify-center overflow-auto custom-scrollbar" @scroll="handleChatScroll">
      <CreationMessageArea
          :content-width-class="contentWidthClass"
          :loading-messages="loadingMessages"
          :loading="loading"
          :messages="messages"
          :current-ai-message-id="currentAiMessageId"
          :can-edit-creation="canEditCreation"
          :show-feedback="showFeedback"
          :player-state="playerState"
          :current-user-audio-message-id="currentUserAudioMessageId"
          :subject-continue-msg-id="subjectContinueMsgId"
          :subject-continue-disabled="subjectContinueDisabled"
          :scene-script-continue-msg-id="sceneScriptContinueMsgId"
          :scene-script-continue-disabled="sceneScriptContinueDisabled"
          :scene-continue-msg-id="sceneContinueMsgId"
          :scene-continue-disabled="sceneContinueDisabled"
          @user-audio-play="handleUserAudioPlay"
          @audio-select="handleAudioSelect"
          @audio-toggle-play="handleTogglePlay"
          @video-play="handleVideoPlay"
          @open-storyboard="(video) => emit('open-storyboard', video)"
          @subject-continue="handleSubjectContinue"
          @scene-script-continue="handleSceneScriptContinue"
          @scene-continue="handleSceneContinue"
          @action-required-submit="handleActionRequiredSubmit"
          @edit-context-change="handleEditContextChange"
      />
    </div>

    <!-- 右侧分区导航：fixed 覆盖在聊天区右侧，不占布局 -->
    <div
        v-if="showRightNav"
        ref="chatNavRef"
        class="fixed z-[100] -translate-y-1/2 pointer-events-none"
        :style="navStyle"
    >
      <div
          class="flex flex-col gap-[22px] px-4 py-[14px] rounded-[20px] pointer-events-auto backdrop-blur-[12px]"
          :class="{ 'bg-[#2C2C2C] shadow-[0px_0px_6px_0px_rgba(127,168,0,0.3)]': navExpanded }"
          @mouseenter="navExpanded = true"
          @mouseleave="navExpanded = false"
      >
        <template v-for="item in navItems" :key="item.id">
          <div
              v-if="(item.id === 'ref' && hasRefSection) || (item.id === 'script' && hasScriptSection) || (item.id === 'mv' && hasMvSection)"
              class="group flex items-center justify-end gap-[10px] cursor-pointer"
              @click="scrollToSection(item.target)"
          >
          <span
              class="text-[16px] text-[#949494] group-hover:text-white whitespace-nowrap
                     opacity-0 max-w-0 overflow-hidden transition-all duration-[250ms]"
              :class="{'opacity-100 max-w-[80px]': navExpanded}"
          >{{ item.label }}</span>
          <div
              class="w-4 h-[6px] rounded-[8px] bg-[#6F8234] transition-all duration-[250ms] shrink-0
                     group-hover:bg-[#C2FF00]"
              :class="{'bg-[#C2FF00] shadow-[0_0_6px_rgba(194,255,0,0.5)]': item.active}"
          ></div>
          </div>
        </template>
      </div>
    </div>

    <div class="shrink-0 flex justify-center pt-[10px] pb-[30px]">
      <div :class="contentWidthClass" class="flex flex-col items-center gap-3">
        <MusicTrimmer
            v-model="trimmerVisible"
            :raw-file="activeTrimmerContext.rawFile"
            :duration="activeTrimmerContext.duration"
            :preparing="activeTrimmerContext.preparing"
            :audio-url="activeTrimmerContext.audioUrl"
            @play-start="handleTrimmerPlayStart"
            @confirm="handleTrimmerConfirm"
            @cancel="handleTrimmerCancel"
        />

        <div
            ref="composerRef"
            class="creation-composer w-full relative rounded-[10px] px-[14px] py-[10px] shadow-[0px_0px_10px_0px_#C2FF00]"
            :class="[hasCreationAttachments ? 'h-auto min-h-[100px]' : 'h-[100px]', { 'is-drag-over': chatDragEnterCount > 0 }]"
            @dragenter.prevent="chatDragEnterCount++"
            @dragleave="chatDragEnterCount = Math.max(0, chatDragEnterCount - 1)"
            @dragover.prevent
            @drop.prevent="handleChatDrop"
        >
          <input ref="chatFileInputRef" type="file" :accept="CREATION_UPLOAD_ACCEPT" multiple class="hidden" @change="handleChatFileChange"/>

          <div class="h-full flex flex-col">
            <div v-if="hasCreationAttachments" class="mb-2">
              <div v-if="isNotEmpty(uploadedFile.fileId)" class="relative h-[36px] max-w-[176px] inline-flex items-center px-2 rounded-[10px] border border-[#A9D936] bg-[rgba(216,216,216,0.3)] mb-2">
                <svg-icon class="shrink-0" name="gy-audiofiles" size="20" color="#C2FF00"></svg-icon>
                <span class="ml-2 text-white text-[14px] leading-[20px] truncate pr-2">{{ uploadedFile.fileName }}</span>
                <span v-if="isAudioUploading" class="audio-upload-loading shrink-0 ml-1">
                  <i class="audio-upload-loading__spinner"></i>
                  {{ t('common.loading') }}
                </span>
                <svg-icon
                    name="gy-closure"
                    size="14"
                    color="white"
                    class="cursor-pointer shrink-0 absolute -top-[5px] -right-[5px]"
                    @click="handleRemoveAudioFile"
                ></svg-icon>
              </div>

              <div v-if="uploadedCreationImages.length" class="flex flex-wrap items-start gap-2">
                <div v-for="(img, idx) in uploadedCreationImages" :key="img.id" class="intention-select-host relative group w-[88px] h-[88px] shrink-0">
                  <el-image
                      :src="img.previewUrl"
                      :preview-src-list="uploadedCreationImages.map(i => i.previewUrl)"
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
                      @click="handleRemoveCreationImage(idx)"
                  ></svg-icon>
                  <el-select
                      v-model="img.intention"
                      size="small"
                      class="intention-select"
                      :class="{ 'is-expanded': img.uiExpanded }"
                      popper-class="intention-dropdown"
                      :teleported="true"
                      @visible-change="(visible) => handleCreationIntentionVisibleChange(img, visible)"
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

            <textarea
                v-model="inputText"
                class="creation-textarea w-full flex-1 bg-transparent text-white text-lg outline-none resize-none"
                :class="hasCreationAttachments ? 'pt-1' : ''"
                :placeholder="composerPlaceholder"
                :disabled="loading || !canEditCreation"
                @keydown="handleKeyDown"
                @paste="handleChatPasteUpload"
            ></textarea>

            <div class="flex items-center justify-between shrink-0 pt-2 gap-2">
              <div class="flex items-center gap-2 flex-shrink-0">
                <div v-if="canEditCreation" class="flex items-center gap-2 flex-shrink-0">
                  <el-popover placement="top-start" trigger="hover" width="240" popper-class="upload-tip-popover" :show-arrow="false" :offset="10">
                    <template #reference>
                      <div class="bg-black w-7 h-7 rounded-full flex-center cursor-pointer" @click="handleRequestPickFile">
                        <svg-icon name="gy-upload" size="24" color="#ADE300"></svg-icon>
                      </div>
                    </template>
                    <div class="upload-tip-content">
                      <div class="upload-tip-title">{{ t('home.upload.title') }}</div>
                      <div class="upload-tip-desc">{{ t('home.upload.imageLimit') }}</div>
                      <div class="upload-tip-desc">{{ t('home.upload.audioTypes') }}</div>
                    </div>
                  </el-popover>
                </div>

                <el-popover
                    v-model:visible="modelPopoverVisible"
                    placement="top-start"
                    trigger="click"
                    popper-class="scene-model-select-dropdown"
                    :show-arrow="false"
                    :offset="10"
                >
                  <template #reference>
                    <div class="inline-flex h-8 items-center gap-1.5 rounded-[6px] bg-[#D8D8D8]/10 px-[10px] cursor-pointer text-[13px] text-[#BEFA00] shrink-0">
                      <svg-icon name="gy-model" size="18" color="#ADE300"></svg-icon>
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

                <!-- 分辨率：仅会话首条消息前可选，发送后锁定（由后端记录），故此时隐藏 -->
                <el-popover
                    v-if="canChooseResolution"
                    v-model:visible="resolutionPopoverVisible"
                    placement="top-start"
                    trigger="click"
                    popper-class="resolution-select-dropdown"
                    :show-arrow="false"
                    :offset="10"
                >
                  <template #reference>
                    <div class="inline-flex h-8 items-center gap-1 rounded-[6px] bg-[#D8D8D8]/10 px-[10px] cursor-pointer text-[13px] shrink-0">
                      <span class="text-[#BEFA00]">{{ selectedResolutionOption?.tag }}</span>
                      <span class="text-[#BEFA00]">{{ selectedResolutionOption?.label }}</span>
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

              <div class="flex items-center gap-2 flex-shrink-0">
                <div v-if="isNotEmpty(uploadedFile.fileId) && isNotEmpty(uploadedAudioEstimatedPoints)" class="inline-flex h-8 items-center rounded-[16px] bg-[#22251A] px-[16px] text-[14px] text-[#C2FF00] shadow-[inset_0_0_0_1px_rgba(190,250,0,0.08)]">
                  {{ t('creation.messageArea.estimatedPoints', {points: uploadedAudioEstimatedPoints}) }}
                </div>

                <el-button circle class="!bg-black !w-8 !h-8 disabled:!opacity-50 disabled:!cursor-not-allowed flex-shrink-0 cursor-pointer" :disabled="!canSend" @click="submitMessage()">
                  <svg-icon name="gy-send-copy" size="32" color="#C2FF00"></svg-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, reactive, ref, onMounted, onUnmounted, watch, nextTick} from 'vue';
import {ElMessage} from 'element-plus';
import {useCreationEditContextStore} from '@/store/creationEditContext';
import {useUserStore} from '@/store/user';
import {useModelStore, RESOLUTION_OPTIONS} from '@/store/model';
import {getPointsPrice, uploadFile} from '@/api/creation';
import {isNotEmpty} from '@/utils/index.js';
import {
  CREATION_UPLOAD_ACCEPT,
  compressImageBeforeUpload,
  handlePasteUploadFiles,
  handleDropUploadFiles,
  processUploadFiles,
  isSupportedAudioUpload,
  isSupportedImageUpload,
} from '@/views/creation/utils/upload.js';
import MusicTrimmer from './MusicTrimmer.vue';
import CreationMessageArea from './CreationMessageArea.vue';
import {useAudioTrimUpload} from '@/composables/useAudioTrimUpload';
import {useCreationAudioController} from '@/views/creation/composables/useCreationAudioController';
import {creationBus} from '../creationBus';
import {useI18nText} from '@/i18n';

const props = defineProps({ // 父组件传入属性
  sessionId: {type: [String, Number], default: ''},
  contentWidthClass: {type: String, default: 'w-[800px]'},
  loadingMessages: {type: Boolean, default: false},
  loading: {type: Boolean, default: false},
  messages: {type: Array, default: () => []},
  // messageId 后端契约为 string，本地占位也已统一为 string；未发起请求时为 null
  currentAiMessageId: {type: String, default: null},
  canEditCreation: {type: Boolean, default: false},
  showFeedback: {type: Boolean, default: true},
  playerState: {type: Object, required: true},
  audioPlayerRef: {type: Object, default: null},
  pauseAllVideos: {type: Function, required: true},
  showMusicTrimmer: {type: Boolean, default: false},
  mvRawFile: {type: [Object, File], default: null},
});

const emit = defineEmits([ // 对外事件
  'chat-scroll',
  'open-storyboard',
  'trimmer-play-start',
  'trimmer-confirm',
  'send',
  'action-required-clarify',
  'update:showMusicTrimmer',
]);

const userStore = useUserStore(); // 用户状态 store
const {t} = useI18nText(); // 多语言翻译函数
const chatCenterRef = ref(null); // 聊天区滚动容器 DOM 引用

// ─────────────────────────────────────────────
// 右侧分区导航
// ─────────────────────────────────────────────
const chatNavRef = ref(null); // 导航面板 DOM 引用
const navExpanded = ref(false); // 鼠标悬浮时展开标签文字
const navStyle = ref({}); // fixed 定位样式，由 updateNavPosition 写入
const navItems = ref([ // 导航项：target 为 chatCenterRef 内对应分区的 CSS 选择器
  { id: 'ref', label: '参考图', active: false, target: '.reference-images-section' },
  { id: 'script', label: '分镜脚本', active: false, target: '.scene-script-section' },
  { id: 'mv', label: 'mv分镜', active: false, target: '.mv-scenes-section' },
]);

// 参考图：有 subjectList 的消息
const hasRefSection = computed(() =>
  props.messages.some((msg) => msg.subjectList && msg.subjectList.length > 0),
);
// 分镜脚本：有 SCENE_SCRIPT block 且 scenes 非空的消息
const hasScriptSection = computed(() =>
  props.messages.some((msg) =>
    (msg.blocks || []).some((b) => b.type === 'SCENE_SCRIPT' && b.scenes && b.scenes.length > 0),
  ),
);
// mv分镜：有 sceneList 的消息
const hasMvSection = computed(() =>
  props.messages.some((msg) => msg.sceneList && msg.sceneList.length > 0),
);

const showRightNav = computed(() => hasRefSection.value || hasScriptSection.value || hasMvSection.value);

/**
 * 根据 chatCenterRef 视口位置计算导航面板的 fixed 定位：
 * top 取聊天区垂直中心，right 贴聊天区右边缘。
 * 布局变化（窗口 resize、右边栏开闭）时需重新调用。
 * @returns {void}
 */
const updateNavPosition = () => {
  const chatEl = chatCenterRef.value;
  if (!chatEl) return;
  const rect = chatEl.getBoundingClientRect();
  navStyle.value = {
    top: `${rect.top + rect.height / 2}px`,
    right: `${window.innerWidth - rect.right}px`,
  };
};

/**
 * 根据 chatCenterRef 当前滚动位置更新导航激活态：
 * 视口垂直中心落在哪个分区内，对应导航项高亮。
 * @returns {void}
 */
const updateNavActive = () => {
  const container = chatCenterRef.value;
  if (!container) return;
  const containerRect = container.getBoundingClientRect();
  const mid = containerRect.top + containerRect.height / 2;
  navItems.value.forEach((item) => {
    const sections = container.querySelectorAll(item.target);
    item.active = false;
    sections.forEach((section) => {
      const rect = section.getBoundingClientRect();
      if (mid >= rect.top - 50 && mid <= rect.bottom + 50) {
        item.active = true;
      }
    });
  });
};

/**
 * 点击导航项时滚动到对话中该类型分区的最后一次出现位置。
 * 消息块按时间从上到下排列，取 querySelectorAll 返回的最后一个元素即为最新一次。
 * @param {string} selector 分区 CSS 选择器
 * @returns {void}
 */
const scrollToSection = (selector) => {
  const container = chatCenterRef.value;
  if (!container) return;
  const sections = container.querySelectorAll(selector);
  if (!sections.length) return;

  const target = sections[sections.length - 1];
  const containerRect = container.getBoundingClientRect();
  const sectionRect = target.getBoundingClientRect();
  const scrollTarget = container.scrollTop + (sectionRect.top - containerRect.top) - 20;
  container.scrollTo({ top: scrollTarget, behavior: 'smooth' });
};

/** 聊天区滚动：通知父组件 + 更新导航激活态。 */
const handleChatScroll = () => {
  emit('chat-scroll');
  updateNavActive();
};

// 右边栏切换导致内容宽度变化时，原本贴底的视图保持贴底
watch(() => props.contentWidthClass, async () => {
  const el = chatCenterRef.value;
  if (!el) return;
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight <= 50;
  await nextTick();
  if (nearBottom) el.scrollTop = el.scrollHeight - el.clientHeight;
  updateNavPosition();
});

const composerRef = ref(null); // 聊天输入区 DOM 引用
const chatFileInputRef = ref(null); // 隐藏文件 input DOM 引用
const currentUserAudioMessageId = ref(null); // 当前正在播放的用户音频所属消息 ID，用于脉冲动画
const MAX_CREATION_IMAGES = 6; // 单次创作最多上传的参考图数量
const intentionOptions = computed(() => [ // 参考图意图下拉选项
  {label: t('creation.intention.character'), value: 'CHARACTER', icon: 'gy-character'},
  {label: t('creation.intention.costume'), value: 'COSTUME', icon: 'gy-clothes'},
  {label: t('creation.intention.environment'), value: 'ENVIRONMENT', icon: 'gy-environment'},
  {label: t('creation.intention.prop'), value: 'PROP', icon: 'gy-prop'},
  {label: t('creation.intention.style'), value: 'STYLE', icon: 'gy-style'},
]);
const inputText = ref(''); // 聊天输入框文本
const uploadedCreationImages = ref([]); // 已上传的参考图列表
const uploadedFile = reactive({fileId: '', fileName: '', fileUrl: '', audioLyrics: ''}); // 已上传的音频文件信息
const uploadedAudioTrimDuration = ref(0); // 裁剪后音频时长（秒）
const uploadedAudioEstimatedPoints = ref(null); // 预估所需积分
const TEMP_AUDIO_FILE_ID_PREFIX = 'temp_audio_'; // 临时音频文件 ID 前缀，用于区分上传中状态
const DEFAULT_IMAGE_COUNT = 7; // MV 制作默认参考图数量
const IMAGE_POINTS_PER_UNIT = 28; // 每张参考图消耗积分
const POINTS_UPLIFT_RATE = 0.15; // 积分上浮比例
const isAudioUploading = computed(() => String(uploadedFile.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX)); // 音频是否正在上传
const ACTION_REQUIRED_CUSTOM_OPTION_ID = '__ACTION_REQUIRED_CUSTOM_OPTION__'; // ACTION_REQUIRED 自定义选项 ID
const editContextStore = useCreationEditContextStore(); // 编辑上下文 store
const editContextSessionKey = computed(() => String(props.sessionId || '')); // 当前会话的编辑上下文 key
const hasEditContextSession = computed(() => isNotEmpty(editContextSessionKey.value)); // 是否存在有效会话
const editContextState = computed(() => ( // 当前会话的编辑上下文状态
  hasEditContextSession.value
    ? editContextStore.getState(editContextSessionKey.value)
    : null
));

const {
  showTrimmer: uploadTrimmerVisible,
  rawFile: uploadRawFile,
  audioDuration: uploadAudioDuration,
  isPreparingAudio: uploadPreparingAudio,
  openWithFile: openUploadTrimmerWithFile,
  uploadTrimmedBlob: uploadTrimmedBlobForChat,
  clearUploadState,
} = useAudioTrimUpload();

const audioController = useCreationAudioController({ // 音频播放控制器
  audioPlayerRef: props.audioPlayerRef,
  // 避免 setup 时 props.audioPlayerRef 还没就绪导致捕获 null：
  // 每次操作时动态读取最新的 audio DOM。
  getAudioEl: () => props.audioPlayerRef?.value ?? props.audioPlayerRef,
  playerState: props.playerState,
  pauseAllVideos: props.pauseAllVideos,
  onOpenPanel: () => creationBus.emit('detail:open-audio'),
});

const musicTrimmerVisible = computed({ // MV 裁剪器弹窗显隐
  get: () => props.showMusicTrimmer,
  set: (value) => emit('update:showMusicTrimmer', value),
});

const activeTrimmerContext = computed(() => { // 裁剪器统一上下文：upload 为本地上传，mv 为播放器音频
  if (uploadTrimmerVisible.value) {
    return {
      type: 'upload',
      rawFile: uploadRawFile.value,
      duration: uploadAudioDuration.value,
      preparing: uploadPreparingAudio.value,
      audioUrl: '',
    };
  }

  return {
    type: 'mv',
    rawFile: props.mvRawFile,
    duration: props.playerState.duration || 180,
    preparing: false,
    audioUrl: props.playerState.currentAudio?.audioUrl || '',
  };
});

const trimmerVisible = computed({ // 裁剪器弹窗显隐（上传裁剪 + MV 裁剪统一入口）
  get: () => uploadTrimmerVisible.value || musicTrimmerVisible.value,
  set: (value) => {
    if (value) return;
    uploadTrimmerVisible.value = false;
    musicTrimmerVisible.value = false;
  },
});

const modelStore = useModelStore(); // 模型选择状态 store
const modelPopoverVisible = ref(false); // 模型选择弹窗显隐
const resolutionPopoverVisible = ref(false); // 分辨率选择弹窗显隐

const selectedModelLabel = computed(() => modelStore.modelOptions.find((item) => item.value === modelStore.selectedModel)?.label); // 当前选中模型的展示名

const selectedResolutionOption = computed(() => RESOLUTION_OPTIONS.find((item) => item.value === modelStore.selectedResolution)); // 当前选中分辨率选项

const canChooseResolution = computed(() => !isNotEmpty(props.sessionId) && props.messages.length === 0); // 分辨率仅首条消息前可选，发送后锁定

const handleSelectModel = (value) => { // 切换当前模型
  modelStore.setSelectedModel(value);
  modelPopoverVisible.value = false;
};

const handleSelectResolution = (value) => { // 切换分辨率
  modelStore.setSelectedResolution(value);
  resolutionPopoverVisible.value = false;
};

/** 裁剪器开始播放时，仅 MV 裁剪场景通知外层暂停其它播放器。 */
const handleTrimmerPlayStart = () => {
  if (activeTrimmerContext.value.type !== 'mv') return;
  emit('trimmer-play-start');
};

/**
 * 裁剪器确认回调：
 * - upload 场景：上传并回填聊天附件
 * - mv 场景：透传给父级继续制作 MV
 */
const handleTrimmerConfirm = async (payload) => {
  if (activeTrimmerContext.value.type === 'upload') {
    await handleUploadTrimmerConfirm(payload);
    return;
  }
  emit('trimmer-confirm', payload);
};

/** 裁剪器取消：按当前上下文分别回收状态。 */
const handleTrimmerCancel = () => {
  if (activeTrimmerContext.value.type === 'upload') {
    clearUploadState();
    return;
  }
  emit('update:showMusicTrimmer', false);
};

const hasCreationAttachments = computed(() => uploadedCreationImages.value.length > 0 || isNotEmpty(uploadedFile.fileId)); // 输入区是否有附件
/**
 * ACTION_REQUIRED 是否已完成选择。
 * 规则：selectedOptionId（string）有值即视为已完成（包含 custom）。
 * @param {any} block
 * @returns {boolean}
 */
const isActionRequiredResolved = (block) => !!block?.data?.selectedOptionId;
/**
 * 仅在“最后一条 AI 消息”的 ACTION_REQUIRED 且未选择时，返回该 block。
 * 这样在出现新消息后，输入框会立即恢复默认 placeholder。
 * @returns {any|null}
 */
const getActiveActionRequiredBlock = () => {
  const messages = props.messages || [];
  const messageCount = messages.length;
  if (!messageCount) return null;
  const lastMsg = messages[messageCount - 1];
  if (!lastMsg || lastMsg.senderType === 'USER') return null;
  const blocks = lastMsg.blocks || [];
  for (let i = blocks.length - 1; i >= 0; i -= 1) {
    const block = blocks[i];
    if (block?.type !== 'ACTION_REQUIRED') continue;
    if (isActionRequiredResolved(block)) return null;
    return block;
  }
  return null;
};
const activeActionRequiredBlock = computed(() => ( // 当前需要用户响应的 ACTION_REQUIRED block
  props.loading ? null : getActiveActionRequiredBlock()
));
const composerPlaceholder = computed(() => { // 输入框占位文案
  return activeActionRequiredBlock.value
      ? t('creation.chatPanel.actionRequiredPlaceholder')
      : t('creation.chatPanel.inputPlaceholder');
});
const canSend = computed(() => ( // 是否允许发送消息
  isNotEmpty(inputText.value)
  && !props.loading
  && props.canEditCreation
  && !isAudioUploading.value
));
const isAudioPointsInsufficient = computed(() => { // 音频积分是否不足
  if (!isNotEmpty(uploadedAudioEstimatedPoints.value)) return false;
  if (!isNotEmpty(userStore.pointsBalance)) return false;
  return uploadedAudioEstimatedPoints.value > userStore.pointsBalance;
});
const hasWarnedInsufficientPoints = ref(false); // 是否已弹过积分不足提示，避免重复弹
const resetInsufficientPointsWarning = () => { // 重置积分不足警告标记
  hasWarnedInsufficientPoints.value = false;
};

// ─────────────────────────────────────────────
// SUBJECT 一次性“确认并继续”状态（通过 creationBus 与 index 解耦）
// ─────────────────────────────────────────────
const createContinueFlow = (prompt) => { // 创建一次性「确认并继续」流程，通过 creationBus 与 index 解耦
  const msgId = ref(null); // 触发流程的目标消息 ID
  const disabled = ref(false); // 是否已禁用（避免重复提交）

  const onReceived = ({msgId: nextMsgId} = {}) => {
    msgId.value = nextMsgId ?? null;
    disabled.value = false;
  };

  const onReset = () => {
    msgId.value = null;
    disabled.value = false;
  };

  /**
   * @param {string|number} targetMsgId
   * @param {object} [extras] 仅 SCENE「完成制作」用：{subtitle, lipSync}；其他 flow 传 undefined
   */
  const submitIfMatch = (targetMsgId, extras) => {
    if (!props.canEditCreation) return;
    if (disabled.value) return;
    if (msgId.value !== targetMsgId) return;

    disabled.value = true;
    creationBus.emit('chat:draft:seed', {prompt});
    // extras 通过 chat:submit payload 透传给 submitMessage → useCreationChatSse → sendChatMessageSSE data
    creationBus.emit('chat:submit', extras ? {finalMakeExtras: extras} : undefined);
  };

  const lock = () => {
    if (isNotEmpty(msgId.value) && !disabled.value) {
      disabled.value = true;
    }
  };

  return {msgId, disabled, onReceived, onReset, submitIfMatch, lock};
};

const subjectContinueFlow = createContinueFlow('确认并继续'); // SUBJECT 阶段继续流程
const sceneScriptContinueFlow = createContinueFlow('确认并生成'); // SCENE_SCRIPT 阶段继续流程
const sceneContinueFlow = createContinueFlow('完成制作'); // SCENE 阶段继续流程

const subjectContinueMsgId = subjectContinueFlow.msgId; // SUBJECT 流程目标消息 ID
const subjectContinueDisabled = subjectContinueFlow.disabled; // SUBJECT 流程禁用态
const sceneScriptContinueMsgId = sceneScriptContinueFlow.msgId; // SCENE_SCRIPT 流程目标消息 ID
const sceneScriptContinueDisabled = sceneScriptContinueFlow.disabled; // SCENE_SCRIPT 流程禁用态
const sceneContinueMsgId = sceneContinueFlow.msgId; // SCENE 流程目标消息 ID
const sceneContinueDisabled = sceneContinueFlow.disabled; // SCENE 流程禁用态

/**
 * 发送下一条用户消息前，把「相对会话初始态的修改」汇总进 composer 文案（buildEditContext）。
 *
 * - SUBJECT / SCENE：相对首屏切换 `activeVersion` 时，`editContext` 中 Subject/Scene 行使用约定模板（imgFileId / videoFileId + 版本号）。
 * - SCENE_SCRIPT：脚本表编辑合并进 sceneScriptInitial / sceneScriptCurrent，由 buildEditContext 追加「SCENE_SCRIPT 修改」段落。
 * - 初始态记录规则：仅在“首次编辑”时快照初始值；发送后统一清空，进入下一轮监控。
 *
 * 隐患：clearEditContextState 在提交成功后需调用，否则下一轮发送会重复附带旧修改说明。
 */
// 当点击用户音频时，`setCurrentAudio` 会先把 `playerState.isPlaying` 置为 `false`，
// 期间 watch 可能会清掉 `currentUserAudioMessageId`，导致“脉冲动画”来不及出现。
// 这里用 pendingId 缓冲“即将开始播放的用户音频”，避免动画丢失。
const pendingUserAudioMessageId = ref(null); // 缓冲「即将播放的用户音频」目标消息 ID，避免脉冲动画在 isPlaying 切换间隙丢失

/**
 * 用户点击「确认并继续」：种子草稿并触发提交（由 index 走 SSE）。
 * @param {string|number} msgId
 * @returns {void}
 */
const handleSubjectContinue = (msgId) => subjectContinueFlow.submitIfMatch(msgId);

/**
 * 用户点击「确认并生成」（分镜脚本阶段）。
 * @param {string|number} msgId
 * @returns {void}
 */
const handleSceneScriptContinue = (msgId) => sceneScriptContinueFlow.submitIfMatch(msgId);

/**
 * 用户点击「完成制作」（分镜卡阶段）。
 * @param {string|number} msgId
 * @returns {void}
 */
/**
 * SCENE 阶段「完成制作」：extras 携带最终合成的两个开关（subtitle / lipSync），
 * 经由 chat:submit 事件继续向下，最终由 useCreationChatSse 注入到 SSE 请求体。
 */
const handleSceneContinue = (msgId, extras) => sceneContinueFlow.submitIfMatch(msgId, extras);

/** 清空并立刻进入下一轮记录；仅在存在有效 sessionKey 时执行。 */
const clearEditContextState = (sessionKey = editContextSessionKey.value) => {
  const key = String(sessionKey || '');
  if (!isNotEmpty(key)) return null;
  // resetState 返回新对象，确保“清空”与“下一轮开始”在同一同步链路完成
  return editContextStore.resetState(key);
};

const pendingEditContextClearOriginKey = ref(null); // 发送成功后需要清空 editContext 的起点 sessionKey，新对话暂存为 __draft__

/**
 * 发送成功后清空本次 edit-context（失败保留，下一次成功再清）。
 * @param {{originSessionId?: string, sessionId?: string}} [payload]
 * @returns {void}
 */
const onChatSubmitSuccess = (payload = {}) => {
  const originSessionId = payload?.originSessionId ?? '';
  const originKey = String(originSessionId || '');
  if (!pendingEditContextClearOriginKey.value) return;
  if (!isNotEmpty(originKey)) return;
  if (pendingEditContextClearOriginKey.value !== originKey) return;
  clearEditContextState(originKey);
  pendingEditContextClearOriginKey.value = null;
};

/**
 * SCENE_SCRIPT：subjectRefs 浅克隆（每个 item 是新对象，避免引用污染）。
 * subjectRefs[].subjectId / fileId / url 后端皆 string。
 */
const cloneSubjectRefs = (refs) => (refs || []).map((item) => ({
  subjectId: item.subjectId,
  fileId: item.fileId,
  url: item.url,
}));

/** SCENE_SCRIPT：两组 subjectRefs 是否一致（顺序敏感）。 */
const isSameSubjectRefs = (a, b) => JSON.stringify(a) === JSON.stringify(b);

/**
 * 分镜脚本表编辑时合并进 sceneScriptInitial / sceneScriptCurrent。
 * messageId / lineKey(=SCENE_SCRIPT.id) 后端皆 string；previousPromptUnwrapped / nextPromptUnwrapped 是 string。
 * @param {Record<string, any>} payload
 * @returns {void}
 */
const updateSceneScriptEditContext = (payload = {}) => {
  if (!hasEditContextSession.value || !editContextState.value) return;

  const {messageId, lineKey} = payload;
  if (!messageId || !lineKey) return;
  const key = `${messageId}::${lineKey}`;

  const prevPrompt = payload.previousPromptUnwrapped;
  const nextPrompt = payload.nextPromptUnwrapped;
  const hasPromptPayload = prevPrompt !== undefined && nextPrompt !== undefined;
  const prevSubjectRefs = payload.previousSubjectRefs;
  const nextSubjectRefs = payload.nextSubjectRefs;
  const hasSubjectRefsPayload = prevSubjectRefs !== undefined && nextSubjectRefs !== undefined;

  if (!hasPromptPayload && !hasSubjectRefsPayload) return;

  if (!editContextState.value.sceneScriptInitial[key]) {
    editContextState.value.sceneScriptInitial[key] = {};
  }
  if (!editContextState.value.sceneScriptCurrent[key]) {
    editContextState.value.sceneScriptCurrent[key] = {messageId, lineKey};
  }

  const initial = editContextState.value.sceneScriptInitial[key];
  const current = editContextState.value.sceneScriptCurrent[key];

  if (hasPromptPayload) {
    if (initial.visualPrompt === undefined) {
      initial.visualPrompt = prevPrompt || '';
    }
    if ((nextPrompt || '') === (initial.visualPrompt || '')) {
      delete current.visualPrompt;
    } else {
      current.visualPrompt = nextPrompt || '';
    }
  }

  if (hasSubjectRefsPayload) {
    if (initial.subjectRefs === undefined) {
      initial.subjectRefs = cloneSubjectRefs(prevSubjectRefs);
    }
    const nextRefs = cloneSubjectRefs(nextSubjectRefs);
    if (isSameSubjectRefs(nextRefs, initial.subjectRefs)) {
      delete current.subjectRefs;
    } else {
      current.subjectRefs = nextRefs;
    }
  }

  if (current.visualPrompt === undefined && current.subjectRefs === undefined) {
    delete editContextState.value.sceneScriptCurrent[key];
  }
};

/**
 * CreationMessageArea 上报：SUBJECT/SCENE 版本切换或 SCENE_SCRIPT 行编辑。
 * @param {Record<string, any>} payload
 * @returns {void}
 */
const handleEditContextChange = (payload = {}) => {
  // type 由调用方以常量字符串传入（'SUBJECT'/'SCENE'/'SCENE_SCRIPT'），无需转换
  const type = payload.type;
  if (type === 'SUBJECT' || type === 'SCENE') {
    // 守卫保留在调用处：避免在没有会话时往 store 里建空状态
    if (!hasEditContextSession.value) return;
    editContextStore.recordVersionChange(editContextSessionKey.value, type, payload);
    return;
  }
  if (type === 'SCENE_SCRIPT') {
    updateSceneScriptEditContext(payload);
  }
};

/**
 * SSE 自动替换旧版本后，清除该对象既有修改记录，并以最新版本重置基线。
 * messageId / itemKey(=chunkId) 后端 string；activeVersion 后端 number。
 * @param {Record<string, any>} payload
 * @returns {void}
 */
const handleVersionResetFromSSE = (payload = {}) => {
  const {type, messageId, itemKey, activeVersion} = payload;
  if (type !== 'SUBJECT' && type !== 'SCENE') return;
  if (!messageId || !itemKey) return;

  const initialKey = type === 'SUBJECT' ? 'subjectInitial' : 'sceneInitial';
  const currentKey = type === 'SUBJECT' ? 'subjectCurrent' : 'sceneCurrent';
  const key = `${messageId}::${itemKey}`;

  delete editContextState.value[currentKey][key];
  editContextState.value[initialKey][key] = activeVersion;
};

/**
 * 取 SUBJECT/SCENE 项的 itemKey（即 chunkId，后端 string）。
 * @param {any} item
 * @returns {string}
 */
const getVersionItemKey = (item = {}) => item.messageChunkId || '';

/**
 * SUBJECT：指定版本条目上的 imgFileId（后端 string）。version 后端 number。
 * @param {{ versions: Array<{ version: number, imgFileId: string }> }} subject
 * @param {number} versionNum
 */
const getSubjectImgFileIdForVersion = (subject, versionNum) => {
  const v = subject.versions.find((x) => x.version === versionNum);
  return v?.imgFileId || '';
};

/**
 * SCENE：指定版本条目上的 videoFileId（后端 string）。version 后端 number。
 * @param {{ versions: Array<{ version: number, videoFileId: string }> }} scene
 * @param {number} versionNum
 */
const getSceneVideoFileIdForVersion = (scene, versionNum) => {
  const v = scene.versions.find((x) => x.version === versionNum);
  return v?.videoFileId || '';
};

/** 按 messageId + itemKey 在指定 listKey 下定位版本条目。messageId / chunkId 后端皆 string。 */
const findVersionItemByMessageAndKey = (messageId, itemKey, listKey) => {
  const msg = props.messages.find((m) => m.messageId === messageId);
  if (!msg) return null;
  return (msg[listKey] || []).find((item) => getVersionItemKey(item) === itemKey) || null;
};

/** 按 messageId + itemKey 定位一条 subject。 */
const findSubjectByMessageAndKey = (messageId, itemKey) => findVersionItemByMessageAndKey(messageId, itemKey, 'subjectList');

/** 按 messageId + itemKey 定位一条 scene。 */
const findSceneByMessageAndKey = (messageId, itemKey) => findVersionItemByMessageAndKey(messageId, itemKey, 'sceneList');

/** 构建 Subject/Scene 版本切换的发送说明行。 */
const buildVersionEditLines = ({
  items,
  initialMap,
  sectionTitle,
  fieldName,
  findItem,
  getFileIdForVersion,
}) => {
  const detailLines = [];
  items.forEach((item) => {
    const key = `${item.messageId}::${item.itemKey}`;
    const initialVer = initialMap[key];
    const finalVer = item.activeVersion;
    if (!isNotEmpty(initialVer) || !isNotEmpty(finalVer)) return;
    const target = findItem(item.messageId, item.itemKey);
    const fromId = target ? getFileIdForVersion(target, initialVer) : '';
    const toId = target ? getFileIdForVersion(target, finalVer) : '';
    const vx = `V${finalVer + 1}`;
    detailLines.push(`${fieldName} 由"${fromId}"改为"${toId}"，最终选择版本${vx}`);
  });
  if (!detailLines.length) return [];
  return [sectionTitle, ...detailLines];
};

/**
 * 基于“本轮版本变更记录 + 当前消息 activeVersion”计算最终修改集合。
 * 说明：`currentMap` 同时包含用户手动切换与 SSE 回写触发的版本变化；
 * 只要发生版本变化（主动/被动），都会进入本轮记录，并在下次发送成功建链后清空。
 */
const collectVersionCurrentItems = (initialMap, currentMap, listKey) => {
  const result = [];
  const items = Object.values(currentMap || {});

  // messageId / itemKey(=chunkId) 后端皆 string；activeVersion 后端 number
  items.forEach((item) => {
    const {messageId, itemKey} = item;
    if (!messageId || !itemKey) return;

    const key = `${messageId}::${itemKey}`;
    const initialVersion = initialMap?.[key];
    if (!isNotEmpty(initialVersion)) return;

    const target = findVersionItemByMessageAndKey(messageId, itemKey, listKey);
    if (!target) return;

    const finalVersion = target.activeVersion;
    if (!isNotEmpty(finalVersion)) return;
    if (initialVersion === finalVersion) return;

    result.push({messageId, itemKey, activeVersion: finalVersion});
  });

  return result;
};

/** 将 editContextState 转为随用户消息一并发送给后端的纯文本说明（无修改则返回空串）。 */
const buildEditContext = () => {
  if (!hasEditContextSession.value || !editContextState.value) return '';

  const subject = collectVersionCurrentItems(
      editContextState.value.subjectInitial,
      editContextState.value.subjectCurrent,
      'subjectList',
  );
  const scene = collectVersionCurrentItems(
      editContextState.value.sceneInitial,
      editContextState.value.sceneCurrent,
      'sceneList',
  );
  const sceneScript = Object.values(editContextState.value.sceneScriptCurrent).map((item) => {
    const result = {
      messageId: item.messageId,
      lineKey: item.lineKey,
    };
    if (item.visualPrompt !== undefined) result.visualPrompt = item.visualPrompt;
    if (item.subjectRefs !== undefined) result.subjectRefs = item.subjectRefs;
    return result;
  });

  if (!subject.length && !scene.length && !sceneScript.length) return '';

  const lines = [];

  lines.push(...buildVersionEditLines({
    items: subject,
    initialMap: editContextState.value.subjectInitial,
    sectionTitle: '【Subject 参考图修改】',
    fieldName: 'imgFileId',
    findItem: findSubjectByMessageAndKey,
    getFileIdForVersion: getSubjectImgFileIdForVersion,
  }));

  lines.push(...buildVersionEditLines({
    items: scene,
    initialMap: editContextState.value.sceneInitial,
    sectionTitle: '【Scene 视频修改】',
    fieldName: 'videoFileId',
    findItem: findSceneByMessageAndKey,
    getFileIdForVersion: getSceneVideoFileIdForVersion,
  }));

  if (sceneScript.length) {
    const scriptLines = [];
    sceneScript.forEach((item) => {
      const key = `${item.messageId}::${item.lineKey}`;
      const initial = editContextState.value.sceneScriptInitial[key] || {};

      // visualPrompt 后端 string；initial.visualPrompt 在写入时也保留 string
      const hasPromptChange = item.visualPrompt !== undefined && item.visualPrompt !== (initial.visualPrompt || '');
      const hasRefChange = item.subjectRefs !== undefined
          && !isSameSubjectRefs(item.subjectRefs, cloneSubjectRefs(initial.subjectRefs || []));

      if (!hasPromptChange && !hasRefChange) return;

      const fromPrompt = hasPromptChange ? (initial.visualPrompt || '') : (item.visualPrompt || '');
      const toPrompt = item.visualPrompt || '';
      const toSubjectRefs = hasRefChange
          ? cloneSubjectRefs(item.subjectRefs || [])
          : cloneSubjectRefs(initial.subjectRefs || []);

      scriptLines.push(`脚本行 ${item.lineKey}：visualPrompt 从"${fromPrompt}"改为"${toPrompt}"，subjectRefs 改为"${JSON.stringify(toSubjectRefs)}"`);
    });
    if (scriptLines.length) {
      lines.push('【Scene_script 分镜脚本修改】');
      scriptLines.forEach((line) => lines.push(line));
    }
  }

  return lines.join('\n').trim();
};

/** 触发隐藏 file input，需已登录。 */
const handleRequestPickFile = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  chatFileInputRef.value?.click();
};

/**
 * AI 消息区选择音频：切歌并打开详情面板，清除用户音频脉冲状态。
 * @param {{item:any, index:number}} param0
 * @param {any[]} audioList
 * @returns {void}
 */
const handleAudioSelect = ({item, index}, audioList) => {
  currentUserAudioMessageId.value = null;
  audioController.setCurrentAudio(item, index, audioList, {openPanel: true});
};

/**
 * 播放/暂停 AI 音频列表；可选保留用户消息脉冲用的 `currentUserAudioMessageId`。
 * @param {{item:any, index:number}} param0
 * @param {any[]} audioList
 * @param {{openPanel?: boolean, keepUserMessageId?: boolean}} [options={}]
 * @returns {void}
 */
const handleTogglePlay = ({item, index}, audioList, options = {}) => {
  const {openPanel = true, keepUserMessageId = false} = options;
  const isSame = props.playerState.currentAudio?.audioFileId === item.audioFileId;
  if (isSame) {
    void audioController.togglePlay({item, index, audioList, openPanel});
    return;
  }

  if (!keepUserMessageId) {
    currentUserAudioMessageId.value = null;
  }
  void audioController.togglePlay({item, index, audioList, openPanel});
};

/**
 * 播放用户消息附带音频：关闭详情面板、设置脉冲目标 id、走 togglePlay。
 * @param {any} msg
 * @returns {void}
 */
const handleUserAudioPlay = (msg) => {
  const audioInfo = msg?.attachments?.audio;
  if (!audioInfo?.fileUrl) {
    ElMessage.warning(t('creation.chatPanel.audioUnavailable'));
    return;
  }

  const item = {
    audioFileId: audioInfo.fileId || `user_${msg.messageId}`,
    audioUrl: audioInfo.fileUrl,
    title: audioInfo.fileName || t('creation.uploadedAudioFallback'),
  };

  const willPauseSameUserAudio = props.playerState.currentAudio?.audioFileId === item.audioFileId
      && props.playerState.isPlaying;
  if (willPauseSameUserAudio) {
    // 同一音频正在播放：点击应暂停，脉冲不需要保留。
    currentUserAudioMessageId.value = null;
    pendingUserAudioMessageId.value = null;
  } else {
    // 不同音频或同音频但未播放：保留目标 msg.messageId，直到 playerState.isPlaying 变为 true。
    currentUserAudioMessageId.value = msg?.messageId || null;
    pendingUserAudioMessageId.value = msg?.messageId || null;
  }

  creationBus.emit('detail:close');

  handleTogglePlay({item, index: 0}, [item], {openPanel: false, keepUserMessageId: true});
};

/** 页面内视频开始播放时暂停音频并清除用户音频高亮。 */
const handleVideoPlay = () => {
  audioController.pauseAudio();
  currentUserAudioMessageId.value = null;
};

/**
 * 构造 composer 区一张参考图条目（含展示用 previewUrl 与意图）。
 * @param {object} [params]
 * @returns {{id:number, fileId:string, fileName:string, previewUrl:string, intention:string, uiExpanded:boolean}}
 */
const createCreationImage = ({
  id = Date.now(),
  fileId = '',
  fileName = '',
  previewUrl = '',
  intention = 'CHARACTER',
  uiExpanded = false,
} = {}) => ({id, fileId, fileName, previewUrl, intention, uiExpanded});

/**
 * 写入 composer 区当前用户上传的音频文件元数据。
 * audioLyrics 只在"右侧栏制作 MV → 裁剪 → 上传"路径上有值（来自 AUDIO chunk 的 lyrics 字段），
 * 用户手动选 mp3 上传时为空。发送时直接以 audioLyrics 字段发给后端。
 * @param {{fileId?: string, fileName?: string, fileUrl?: string, audioLyrics?: string}} [params]
 * @returns {void}
 */
const setUploadedAudio = ({fileId = '', fileName = '', fileUrl = '', audioLyrics = ''} = {}) => {
  uploadedFile.fileId = fileId;
  uploadedFile.fileName = fileName;
  uploadedFile.fileUrl = fileUrl;
  uploadedFile.audioLyrics = audioLyrics;
};

/**
 * 清空参考图列表；`preservePreview` 为 true 时不 revoke blob URL（发送成功后仍可能展示缩略图）。
 * @param {boolean} [preservePreview=false]
 * @returns {void}
 */
const clearUploadedImages = (preservePreview = false) => {
  if (!preservePreview) {
    uploadedCreationImages.value.forEach(img => { if (img.previewUrl) URL.revokeObjectURL(img.previewUrl); });
  }
  uploadedCreationImages.value = [];
};

/**
 * 清空 composer 附件与 trimmer 临时状态。
 * @param {{preservePreview?: boolean}} [options={}]
 * @returns {void}
 */
const clearDraft = (options = {}) => {
  const {preservePreview = false} = options;
  resetInsufficientPointsWarning();
  setUploadedAudio();
  uploadedAudioTrimDuration.value = 0;
  uploadedAudioEstimatedPoints.value = null;
  clearUploadedImages(preservePreview);
  clearUploadState();
};

/**
 * 聊天区音频 trim 完成：上传 blob 并写入 `uploadedFile`。
 * @param {any} payload
 * @returns {Promise<void>}
 */
const recalculateUploadedAudioEstimatedPoints = async () => {
  if (!isNotEmpty(uploadedFile.fileId)) return;
  if (uploadedAudioTrimDuration.value <= 0) return;

  const imagePoints = DEFAULT_IMAGE_COUNT * IMAGE_POINTS_PER_UNIT;
  const mvPoints = await getPointsPrice({
    modelName: modelStore.selectedModel,
    taskType: 'MAKE_MV',
    duration: uploadedAudioTrimDuration.value,
    // 始终带上生效分辨率：新会话=用户所选、已有会话=该会话锁定值（effectiveResolution 已统一），与 chat 接口一致
    resolution: modelStore.effectiveResolution,
  });
  uploadedAudioEstimatedPoints.value = Math.ceil((mvPoints + imagePoints) * (1 + POINTS_UPLIFT_RATE));
};

const handleUploadTrimmerConfirm = async (payload) => { // 裁剪器确认：上传裁剪结果并写入 uploadedFile
  resetInsufficientPointsWarning();
  const sourceFile = uploadRawFile.value;
  const tempFileId = `${TEMP_AUDIO_FILE_ID_PREFIX}${Date.now()}`;
  const tempFileName = sourceFile?.name
    ? `${sourceFile.name.replace(/\.[^/.]+$/, '')}_trimmed_${payload?.timeRange || ''}.mp3`
    : 'audio_trimmed.mp3';

  setUploadedAudio({fileId: tempFileId, fileName: tempFileName, fileUrl: ''});
  uploadedAudioTrimDuration.value = payload?.durationSeconds || 0;
  uploadedAudioEstimatedPoints.value = null;
  clearUploadState();

  try {
    const result = await uploadTrimmedBlobForChat(payload, {silent: true, showSuccess: false, sourceFile});
    if (result) {
      setUploadedAudio({fileId: result.fileId, fileName: result.fileName, fileUrl: result.fileUrl || ''});
      await recalculateUploadedAudioEstimatedPoints();
    }
  } catch {
    setUploadedAudio();
    uploadedAudioTrimDuration.value = 0;
    uploadedAudioEstimatedPoints.value = null;
  }
};

watch(() => modelStore.selectedModel, async (newModel, oldModel) => {
  if (!oldModel || newModel === oldModel) return;
  resetInsufficientPointsWarning();
  if (!isNotEmpty(uploadedFile.fileId) || isAudioUploading.value) return;
  if (uploadedAudioTrimDuration.value <= 0) return;

  try {
    const previous = uploadedAudioEstimatedPoints.value;
    await recalculateUploadedAudioEstimatedPoints();
    if (isNotEmpty(previous) && previous === uploadedAudioEstimatedPoints.value) return;
    ElMessage.warning(t('creation.musicTrimmer.pointsRecalculatedByModel', {points: uploadedAudioEstimatedPoints.value}));
  } catch {
    // ignore estimate errors on model switch notification
  }
});

/**
 * 单文件入口：图片走压缩上传追加列表；音频打开 trimmer；其它格式提示错误。
 * @param {File} file
 * @returns {Promise<void>}
 */
const handleChatIncomingFile = async (file) => {
  if (isSupportedImageUpload(file)) {
    if (uploadedCreationImages.value.length >= MAX_CREATION_IMAGES) {
      ElMessage.warning(t('home.uploadLimit', {count: MAX_CREATION_IMAGES}));
      return;
    }
    if (file.size / 1024 / 1024 >= 10) {
      ElMessage.error(t('home.imageTooLarge'));
      return;
    }
    try {
      const compressedFile = await compressImageBeforeUpload(file);
      const formData = new FormData();
      formData.append('file', compressedFile, compressedFile.name);
      const result = await uploadFile(formData);
      uploadedCreationImages.value.push(createCreationImage({
        id: Date.now(),
        fileId: result.fileId,
        fileName: compressedFile.name,
        previewUrl: URL.createObjectURL(compressedFile),
        intention: 'CHARACTER',
      }));
    } catch (error) {
      ElMessage.error(error?.message || t('home.imageUploadFail'));
    }
    return;
  }

  if (isSupportedAudioUpload(file)) {
    await openUploadTrimmerWithFile(file);
    return;
  }

  ElMessage.error(t('home.uploadTypeError'));
};

/**
 * 隐藏 input 选择文件后的回调。
 * @param {Event} event
 * @returns {Promise<void>}
 */
const handleChatFileChange = async (event) => {
  const files = Array.from(event.target.files || []);
  event.target.value = '';
  if (!files.length) return;
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  await processUploadFiles(files, {
    maxImages: MAX_CREATION_IMAGES,
    currentImagesCount: uploadedCreationImages.value.length,
    onImageFile: handleChatIncomingFile,
    onAudioFile: handleChatIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
    onMultipleAudio: () => ElMessage.warning(t('home.audioLimit')),
  });
};

/**
 * 粘贴剪贴板中的文件/图片，复用 `handleChatIncomingFile`。
 * @param {ClipboardEvent} event
 * @returns {Promise<void>}
 */
const handleChatPasteUpload = async (event) => {
  if (!props.canEditCreation) return;
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  await handlePasteUploadFiles(event, {
    maxImages: MAX_CREATION_IMAGES,
    currentImagesCount: uploadedCreationImages.value.length,
    onImageFile: handleChatIncomingFile,
    onAudioFile: handleChatIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
  });
};

const chatDragEnterCount = ref(0); // 拖拽进入计数，>0 时显示拖放高亮

const handleChatDrop = async (event) => { // 聊天区拖拽文件上传
  chatDragEnterCount.value = 0;
  if (!props.canEditCreation) return;
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  await handleDropUploadFiles(event, {
    maxImages: MAX_CREATION_IMAGES,
    currentImagesCount: uploadedCreationImages.value.length,
    onImageFile: handleChatIncomingFile,
    onAudioFile: handleChatIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
    onMultipleAudio: () => ElMessage.warning(t('home.audioLimit')),
  });
};

/**
 * 意图下拉展开时略放大卡片（由 class `is-expanded` 控制样式）。
 * @param {any} img
 * @param {boolean} visible
 * @returns {void}
 */
const handleCreationIntentionVisibleChange = (img, visible) => {
  img.uiExpanded = visible;
};

/**
 * 移除指定参考图并 revoke 本地预览 URL。
 * @param {number} idx
 * @returns {void}
 */
const handleRemoveCreationImage = (idx) => {
  const img = uploadedCreationImages.value[idx];
  if (img?.previewUrl) URL.revokeObjectURL(img.previewUrl);
  uploadedCreationImages.value.splice(idx, 1);
};

/** 移除已选用户音频。 */
const handleRemoveAudioFile = () => setUploadedAudio();

/**
 * 组装 `emit('send')` 所需 payload（含 editContext 文本）。
 * @returns {{userMessage: string, audioFileId: string, currentAttachments: object, imageFiles: any[], editContext: string}}
 */
const buildSendPayload = (forcedUserMessage = '') => {
  const userMessage = forcedUserMessage ? forcedUserMessage : inputText.value.trim();
  const audioFileId = isAudioUploading.value ? '' : uploadedFile.fileId;
  const currentAttachments = {
    audio: isNotEmpty(audioFileId)
        ? {fileId: audioFileId, fileName: uploadedFile.fileName, fileUrl: uploadedFile.fileUrl}
        : null,
    images: uploadedCreationImages.value.map(img => ({
      fileId: img.fileId,
      fileUrl: img.previewUrl,
      previewUrl: img.previewUrl,
      intention: img.intention,
    })),
  };
  const imageFiles = uploadedCreationImages.value.map(img => ({fileId: img.fileId, intention: img.intention}));
  const editContext = buildEditContext();
  return {userMessage, audioFileId, currentAttachments, imageFiles, editContext};
};

/**
 * 校验登录与内容后向父组件发送，并清空输入与编辑上下文。
 * @param {object} [options={}]
 * @returns {void}
 */
const submitMessage = (options = {}, forcedUserMessage = '') => {
  if (!canSend.value && !forcedUserMessage) return;
  if (isAudioUploading.value) {
    ElMessage.warning(t('creation.chatPanel.audioUploading'));
    return;
  }
  if (isAudioPointsInsufficient.value && !hasWarnedInsufficientPoints.value) {
    hasWarnedInsufficientPoints.value = true;
    ElMessage.warning(t('account.points.insufficientForSend'));
    return;
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.sendNeedLogin'));
    return;
  }
  // 提交“手动输入的含义”时必须走 ACTION_REQUIRED 澄清分支，
  // 避免因为 activeActionRequiredBlock 在某些状态下为 null 而误走普通 send 导致插入 USER 气泡。
  const clarifyBlock = getActiveActionRequiredBlock();
  if (clarifyBlock) {
    const customText = forcedUserMessage ? forcedUserMessage.trim() : inputText.value.trim();
    if (!customText) return;
    clarifyBlock.data.selectedOptionId = 'custom';
    emit('action-required-clarify', {
      kind: clarifyBlock.data?.kind,
      actionId: clarifyBlock.data?.actionId,
      optionId: ACTION_REQUIRED_CUSTOM_OPTION_ID,
      optionLabel: customText,
      query: customText,
      userMessage: customText,
    });
    inputText.value = '';
    clearDraft({preservePreview: true});
    return;
  }

  const payload = buildSendPayload(forcedUserMessage);
  if (!isNotEmpty(payload.userMessage)) return;

  // 手动发送时：如果处于 SUBJECT 的等待确认流程，则按钮一次性禁用
  subjectContinueFlow.lock();
  sceneScriptContinueFlow.lock();
  sceneContinueFlow.lock();

  // 仅在 SSE 连接成功建立后清空 editContext；建立失败时保留以便重试
  // 无 sessionId 时不记录清空目标（此时也不会有可编辑消息）。
  pendingEditContextClearOriginKey.value = hasEditContextSession.value ? editContextSessionKey.value : null;

  // 携带音频歌词（仅"右侧栏制作 MV"路径会有值），下游 useCreationChatSse 读 options.audioLyrics 发后端。
  // options 同时承载 chat:submit bus 透传过来的 finalMakeExtras（仅 SCENE「完成制作」路径会有值）。
  const finalOptions = {...options};
  if (isNotEmpty(uploadedFile.audioLyrics) && isNotEmpty(payload.audioFileId)) {
    finalOptions.audioLyrics = uploadedFile.audioLyrics;
  }
  emit('send', {payload, options: finalOptions});
  inputText.value = '';
  clearDraft({preservePreview: true});
};

/**
 * 处理 ACTION_REQUIRED 卡片提交。
 * Why: 卡片选项点选即直接 clarify；自定义描述改走底部统一输入框发送。
 * @param {{kind?:string, actionId?:string|number, optionId?:string, optionLabel?:string, query?:string, userMessage?:string}} payload
 * @returns {void}
 */
const handleActionRequiredSubmit = (payload = {}) => {
  emit('action-required-clarify', payload);
};

/** Enter 发送（Shift/Ctrl+Enter 换行）。 */
const handleKeyDown = (event) => {
  if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey) return;
  event.preventDefault();
  submitMessage();
};

/**
 * 由 creationBus「一键继续」等注入 composer 文案与附件。
 * @param {{prompt?: string, audio?: any, images?: any[]}} [params]
 * @returns {void}
 */
const seedDraft = ({prompt = '', audio = null, images = []} = {}) => {
  inputText.value = prompt || '';
  if (audio) {
    setUploadedAudio(audio);
    const isTempAudio = String(audio.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX);
    if (isTempAudio) {
      uploadedAudioEstimatedPoints.value = null;
    }
    if (isNotEmpty(audio.durationSeconds)) {
      uploadedAudioTrimDuration.value = audio.durationSeconds;
    }
    if (!isTempAudio) {
      recalculateUploadedAudioEstimatedPoints().catch(() => {});
    }
  }
  if (images && images.length) {
    uploadedCreationImages.value = images.map(img => createCreationImage({
      id: Date.now() + Math.random(),
      fileId: img.fileId,
      fileName: img.fileName,
      previewUrl: img.fileUrl || img.previewUrl || '',
      intention: img.intention || 'CHARACTER',
    }));
  }
};
/**
 * 向侧栏等广播当前聊天区 DOM 引用（用于滚动/布局计算）。
 * @returns {void}
 */
const emitChatElements = () => {
  creationBus.emit('chat:elements', {
    chatCenterEl: chatCenterRef.value,
    composerEl: composerRef.value,
  });
};

onMounted(() => {
  emitChatElements();
  updateNavPosition();
  window.addEventListener('resize', updateNavPosition);
  creationBus.on('chat:elements:request', emitChatElements);
  creationBus.on('chat:draft:clear', clearDraft);
  creationBus.on('chat:draft:seed', seedDraft);
  creationBus.on('chat:submit', submitMessage);
  creationBus.on('chat:submit:success', onChatSubmitSuccess);
  creationBus.on('subject:received', subjectContinueFlow.onReceived);
  creationBus.on('subject:reset', subjectContinueFlow.onReset);
  creationBus.on('scene-script:received', sceneScriptContinueFlow.onReceived);
  creationBus.on('scene-script:reset', sceneScriptContinueFlow.onReset);
  creationBus.on('scene:received', sceneContinueFlow.onReceived);
  creationBus.on('scene:reset', sceneContinueFlow.onReset);
  creationBus.on('edit-context:version-reset', handleVersionResetFromSSE);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateNavPosition);
  creationBus.emit('chat:elements', {chatCenterEl: null, composerEl: null});
  creationBus.off('chat:elements:request', emitChatElements);
  creationBus.off('chat:draft:clear', clearDraft);
  creationBus.off('chat:draft:seed', seedDraft);
  creationBus.off('chat:submit', submitMessage);
  creationBus.off('chat:submit:success', onChatSubmitSuccess);
  creationBus.off('subject:received', subjectContinueFlow.onReceived);
  creationBus.off('subject:reset', subjectContinueFlow.onReset);
  creationBus.off('scene-script:received', sceneScriptContinueFlow.onReceived);
  creationBus.off('scene-script:reset', sceneScriptContinueFlow.onReset);
  creationBus.off('scene:received', sceneContinueFlow.onReceived);
  creationBus.off('scene:reset', sceneContinueFlow.onReset);
  creationBus.off('edit-context:version-reset', handleVersionResetFromSSE);
  pendingUserAudioMessageId.value = null;
});

watch(
    () => props.playerState.isPlaying,
    (playing) => {
      if (playing) {
        if (isNotEmpty(pendingUserAudioMessageId.value)) {
          currentUserAudioMessageId.value = pendingUserAudioMessageId.value;
          pendingUserAudioMessageId.value = null;
        }
        return;
      }

      // 正在“等待开始播放”期间，不清掉 currentUserAudioMessageId，避免脉冲动画丢失。
      if (isNotEmpty(pendingUserAudioMessageId.value)) return;

      currentUserAudioMessageId.value = null;
    },
);

// 暴露给父组件：页面级任务恢复（resumeAllPendingDirectEditTasks）自动应用新版本时
// 直接复用此处已有的 editContext 记录逻辑，避免在 index.vue 内重复实现一份。
defineExpose({
  handleEditContextChange,
});
</script>

<style lang="scss">
@use '@/styles/modelSelectDropdown.scss' as *;

.upload-tip-popover {
  background: rgba(27,31,12,0.1) !important;
  border: 1px solid #B3B3B3 !important;
  @apply backdrop-blur-[8px];
  border-radius: 10px !important;
  padding: 6px 12px !important;
  min-width: 0 !important;
  box-shadow: none !important;
}

.upload-tip-content {
  @apply text-[14px] text-white leading-[22px];

  .upload-tip-title {
    @apply text-[#C2FF00];
  }
}
</style>

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

// 创作页输入框容器：渐变边框 + 背景色
.creation-composer {
  position: relative;
  transition: background 0.2s;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 2px;
    background: linear-gradient(90deg, rgba(194, 255, 0, 0.5), rgba(131, 255, 117, 0.5));
    -webkit-mask:
        linear-gradient(#fff 0 0) content-box,
        linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    pointer-events: none;
  }

  &.is-drag-over {
    background: rgba(194, 255, 0, 0.1);
  }
}

// 创作页文本域样式
.creation-textarea {
  background-color: transparent;
  border: none;
  color: white;
  font-size: 1rem;
  line-height: 1.5;
  padding: 0;
  box-shadow: none;
  resize: none;
  outline: none;

  &::placeholder {
    color: rgba(255, 255, 255, 0.6);
  }

  &:focus {
    box-shadow: none;
    border: none;
    outline: none;
  }

  &:disabled {
    background-color: transparent;
    color: rgba(255, 255, 255, 0.6);
  }

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(194, 255, 0, 0.4);
    border-radius: 2px;

    &:hover {
      background: rgba(194, 255, 0, 0.6);
    }
  }
}

.audio-upload-loading {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 16px;
  color: rgba(255, 255, 255, 0.8);
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
</style>
