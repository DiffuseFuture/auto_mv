<template>
  <div
    class="flex flex-col text-white h-full bg-[radial-gradient(120%_75%_at_50%_34%,_rgba(167,243,38,0.28)_0%,_rgba(167,243,38,0.08)_36%,_rgba(0,0,0,0)_74%),_linear-gradient(180deg,_#050706_0%,_#0a1208_42%,_#070707_100%)]"
  >
    <!-- 顶部栏：返回、会话标题、积分入口、历史入口 -->
    <header
      class="shrink-0 px-4 pt-3 pb-1.5 bg-[linear-gradient(180deg,rgba(7,10,8,0.96)_0%,rgba(7,10,8,0.72)_68%,rgba(7,10,8,0)_100%)] backdrop-blur-[2px]"
    >
      <div class="flex items-center justify-between gap-2">
        <div class="min-w-12 flex-1 flex items-center">
          <button @click="handleBack">
            <svg-icon class="rotate-180" name="gy-enter" size="16" color="#FFFFFF"></svg-icon>
          </button>
          <div class="ml-2 text-[16px] leading-[23px] truncate">{{ chatTitle }}</div>
        </div>
        <div class="flex items-center gap-1 shrink-0">
          <el-popover
            v-model:visible="pointsPopoverVisible"
            placement="bottom-end"
            trigger="click"
            :width="330"
            :show-arrow="false"
            popper-class="creation-mobile-points-popover"
            :teleported="true"
            @show="handlePointsPopoverShow"
          >
            <template #reference>
              <button
                type="button"
                class="h-7 min-w-[74px] px-2 rounded-[7px] bg-[#2a3720] text-[#C2FF00] text-[14px] flex items-center justify-center gap-1.5 active:opacity-80 transition-opacity"
              >
                <svg-icon name="gy-integral" size="16" color="#C2FF00"></svg-icon>
                <span>{{ userPoints }}</span>
              </button>
            </template>

            <div class="flex justify-between items-center mb-2">
              <span class="text-[15px] font-bold text-[#000000]">{{ t('account.points.detail') }}</span>
              <span v-if="sessionPointsUpdateTime" class="text-[12px] text-[#A7A7A6]">
                {{ t('creation.topBar.updatedAt') }}{{ sessionPointsUpdateTime }}
              </span>
            </div>
            <div class="border-t border-[#A7A7A6]/30 mb-2"></div>

            <div class="max-h-[200px] overflow-y-auto no-scrollbar">
              <div v-if="sessionPointsLoading && !sessionPointsList.length" class="py-4 text-center text-[#A7A7A6] text-[13px]">
                {{ t('common.loading') }}
              </div>
              <div v-else-if="!sessionPointsList.length" class="py-4 text-center text-[#A7A7A6] text-[13px]">
                {{ t('account.points.empty') }}
              </div>
              <div
                v-else
                v-for="item in sessionPointsList"
                :key="item.id"
                class="flex justify-between items-center py-[6px] text-[13px] text-[#141414]"
              >
                <span class="flex-1 min-w-0 truncate">{{ item.description }}</span>
                <span class="shrink-0 pl-3 text-right tabular-nums">{{ item.amount }}</span>
              </div>
            </div>

            <div class="border-t border-[#A7A7A6]/30 pt-2 mt-2">
              <span class="text-[12px] text-[#A7A7A6]">{{ t('creation.topBar.pointsTip') }}</span>
            </div>
          </el-popover>
        </div>
      </div>
    </header>

    <div class="creation-mobile-body relative flex-1 min-h-0 flex flex-col">
      <!-- 主消息区：承载历史消息、流式回复以及音视频卡片 -->
      <main ref="scrollRef" class="flex-1 min-h-0 overflow-y-auto px-4 pt-2" @scroll="handleScroll">
        <CreationMessageList
          ref="messageListRef"
          :loading-messages="loadingMessages"
          :loading="loading"
          :messages="messages"
          :current-ai-message-id="currentAIMessageId"
          :player-state="playerState"
          :current-user-audio-message-id="currentUserAudioMessageId"
          :can-edit-creation="canEditCreation"
          :show-feedback="showFeedback"
          :subject-continue-msg-id="subjectContinueMsgId"
          :subject-continue-disabled="subjectContinueDisabled"
          :scene-script-continue-msg-id="sceneScriptContinueMsgId"
          :scene-script-continue-disabled="sceneScriptContinueDisabled"
          :scene-continue-msg-id="sceneContinueMsgId"
          :scene-continue-disabled="sceneContinueDisabled"
          @audio-open="handleAudioOpen"
          @user-audio-play="handleUserAudioPlay"
          @subject-edit="handleSubjectEditOpen"
          @scene-edit="handleSceneEditOpen"
          @scene-play="handleScenePlayOpen"
          @subject-version-change="handleSubjectVersionChange"
          @scene-version-change="handleSceneVersionChange"
          @scene-script-change="handleSceneScriptChange"
          @action-required-submit="handleActionRequiredSubmit"
          @subject-continue="handleSubjectContinue"
          @scene-script-continue="handleSceneScriptContinue"
          @scene-continue="handleSceneContinue"
          @scene-lipsync-confirm="handleSceneLipsyncConfirm"
        />
      </main>

      <!-- 底部输入区：上传附件、模型选择、文本输入与发送 -->
      <footer
        class="shrink-0 px-[15px] pb-[calc(env(safe-area-inset-bottom,0px)+21px)] pt-2"
      >
        <div
          class="rounded-[10px] p-[10px] bg-[linear-gradient(180deg,rgba(33,51,19,0.9)_0%,rgba(22,31,16,0.9)_100%)] border border-[rgba(184,255,26,0.62)] shadow-[0_0_18px_rgba(184,255,26,0.3),inset_0_0_16px_rgba(184,255,26,0.1)]"
        >
          <input
            ref="fileInputRef"
            type="file"
            class="hidden"
            :accept="UPLOAD_ACCEPT"
            multiple
            @change="handleFileChange"
          />

          <!-- 已选附件预览：图片缩略图 + 已上传音频条目 -->
          <div v-if="hasAttachments" class="mb-2 flex flex-wrap items-start gap-2">
            <div v-for="(img, idx) in uploadedImages" :key="img.id" class="intention-select-host relative h-[40px] w-[40px] shrink-0 overflow-hidden rounded-[6px]">
              <el-image
                :src="img.previewUrl"
                :preview-src-list="uploadedImages.map((item) => item.previewUrl)"
                :initial-index="idx"
                :hide-on-click-modal="true"
                preview-teleported
                fit="cover"
                class="h-full w-full"
              />
              <div
                v-if="img.uploading"
                class="absolute inset-0 z-[3] flex-center bg-black/45 backdrop-blur-[1px]"
              >
                <div class="h-4 w-4 animate-spin rounded-full border-2 border-[#C2FF00]/30 border-t-[#C2FF00]"></div>
              </div>
              <button
                type="button"
                class="absolute right-0 top-0 flex h-[16px] w-[16px] items-center justify-center rounded-bl-[6px] bg-black/70 text-white"
                :disabled="img.uploading"
                :class="img.uploading ? 'pointer-events-none opacity-40' : ''"
                @click="handleRemoveImage(idx)"
              >
                <svg-icon name="gy-closure" size="10" color="#FFFFFF"></svg-icon>
              </button>
              <el-select
                v-model="img.intention"
                size="small"
                class="mobile-intention-select"
                popper-class="intention-dropdown"
                :teleported="true"
                :show-arrow="false"
                @click.stop
              >
                <template #prefix>
                  <svg-icon :name="getIntentionIcon(img.intention)" size="10" color="#C2FF00"></svg-icon>
                </template>
                <el-option v-for="option in intentionOptions" :key="`${img.id}-${option.value}`" :label="option.label" :value="option.value">
                  <div class="intention-option">
                    <svg-icon :name="option.icon" size="12" color="currentColor"></svg-icon>
                    <span>{{ option.label }}</span>
                  </div>
                </el-option>
              </el-select>
            </div>

            <div v-if="uploadedAudio" class="relative flex h-[30px] min-w-[40px] max-w-full items-center gap-1 rounded-[6px] bg-white/10 px-2 pr-6">
              <svg-icon name="gy-audiofiles" size="16" color="#C2FF00"></svg-icon>
              <span class="max-w-[120px] truncate text-[11px] leading-[16px] text-white/88">
                {{ uploadedAudio.fileName || t('creation.uploadedAudioFallback') }}
              </span>
              <div
                v-if="isAudioUploading"
                class="absolute inset-0 z-[3] flex-center rounded-[6px] bg-black/45 backdrop-blur-[1px]"
              >
                <div class="h-4 w-4 animate-spin rounded-full border-2 border-[#C2FF00]/30 border-t-[#C2FF00]"></div>
              </div>
              <button
                type="button"
                class="absolute right-0 top-0 flex-center"
                :disabled="isAudioUploading"
                :class="isAudioUploading ? 'pointer-events-none opacity-40' : ''"
                @click="handleRemoveAudio"
              >
                <svg-icon name="gy-closure" size="10" color="#FFFFFF"></svg-icon>
              </button>
            </div>
          </div>

          <textarea
            v-model="draftText"
            :disabled="!canEditCreation"
            rows="2"
            class="w-full resize-none bg-transparent outline-none text-[14px] leading-[22px] text-white/85 placeholder:text-white/40"
            :placeholder="canEditCreation ? placeholderText : readonlyPlaceholderText"
          ></textarea>

          <!-- 输入区底部操作栏：上传、模型选择、发送 -->
          <div class="mt-2 flex items-center justify-between">
            <div class="flex items-center gap-[6px]">
              <button class="flex-center" type="button" :disabled="!canEditCreation" @click="handleUploadClick">
                <svg-icon name="gy-upload" size="25" color="#ADE300"></svg-icon>
              </button>
              <button
                class="flex-center"
                type="button"
                :disabled="!canEditCreation"
                @click="modelPopoverVisible = true"
              >
                <svg-icon name="gy-model" size="25" color="#ADE300"></svg-icon>
              </button>
            </div>
            <div class="flex items-center gap-2">
              <div
                v-if="isNotEmpty(uploadedAudio?.fileId) && isNotEmpty(uploadedAudioEstimatedPoints)"
                class="inline-flex h-5 items-center rounded-[10px] bg-[#121800]/50 px-[5px] text-[9px] text-[#C2FF00]"
              >
                {{ t('creation.messageArea.estimatedPoints', {points: uploadedAudioEstimatedPoints}) }}
              </div>
              <button
                class="rounded-full flex-center"
                type="button"
                :disabled="!canEditCreation || loading || isAudioUploading || !draftText.trim()"
                @click="handleSendClick"
              >
                <svg-icon name="gy-fasong" size="25" :color="(!canEditCreation || loading || isAudioUploading || !draftText.trim()) ? 'rgba(194,255,0,0.35)' : '#C2FF00'"></svg-icon>
              </button>
            </div>
          </div>
        </div>
      </footer>

      <!-- history panel removed -->
    </div>

    <!-- 音乐播放弹层：用于查看歌词、控制播放并继续制作 MV -->
    <AudioPlayerSheet
      :visible="audioPlayerVisible"
      :can-edit-creation="canEditCreation"
      :player-state="playerState"
      @close="audioPlayerVisible = false"
      @toggle-play="handlePlayerTogglePlay"
      @prepare-mv="handlePrepareMv"
      @download="handleDownloadCurrentAudio"
      @seek="handleAudioSeek"
    />

    <SubjectEditorSheet
      :visible="subjectEditorVisible"
      :source-msg="subjectEditMessage"
      :target-subject="subjectEditTarget"
      :subject-index="activeSubjectEditIndex"
      @close="closeSubjectEditor"
      @edit-context-change="handleEditContextChange"
    />
    <SceneEditorSheet
      :visible="sceneEditorVisible"
      :source-msg="sceneEditMessage"
      :target-scene="sceneEditTarget"
      :scene-index="activeSceneEditIndex"
      @close="closeSceneEditor"
      @edit-context-change="handleEditContextChange"
    />

    <!-- 全局唯一音频播放器：由页面统一维护播放状态 -->
    <audio
      ref="audioPlayerRef"
      @timeupdate="onAudioTimeUpdate"
      @loadedmetadata="onAudioLoadedMetadata"
      @play="onAudioPlay"
      @playing="onAudioPlaying"
      @pause="onAudioPause"
      @ended="onAudioEnded"
      @error="onAudioError"
    />

    <!--
      音频裁剪弹层：上传音频时复用首页同款裁剪器。
      绑 ref + play-start：跨媒体互斥需要双向打通——
      1) 试听开始时通知本页暂停其它播放器（play-start）
      2) 其它播放器开始时调用 trimmerRef.stopPreview() 反向停掉试听（含 WebAudio 路径）
    -->
    <MobileMusicTrimmer
      ref="trimmerRef"
      v-model="showTrimmer"
      :raw-file="rawFile"
      :duration="audioDuration"
      :preparing="isPreparingAudio || isUploadingTrimmedAudio"
      @play-start="pauseAllMediaExcept(null)"
      @confirm="handleTrimmerConfirm"
      @cancel="showTrimmer = false"
    />

    <!-- 模型选择底部抽屉：与首页同款，从底部上滑、宽度占满 -->
    <el-drawer
        v-model="modelPopoverVisible"
        direction="btt"
        :show-close="false"
        :with-header="false"
        size="auto"
        class="creation-mobile-model-drawer"
    >
      <div class="px-[18px] pt-[20px] pb-[calc(env(safe-area-inset-bottom)+18px)]">
        <div class="mb-3 text-[15px] font-bold leading-5 text-white">{{ t('home.model') }}</div>
        <div
            v-for="item in modelStore.modelOptions"
            :key="item.value"
            class="mb-2 flex h-12 items-center rounded-[10px] px-3 transition-colors"
            :class="item.value === modelStore.selectedModel ? 'bg-[linear-gradient(299deg,rgba(190,250,0,0.5)_0%,rgba(130,255,121,0.5)_100%)]' : 'bg-transparent active:bg-[rgba(164,228,60,0.2)]'"
            @click="handleSelectModel(item.value)"
        >
          <span class="text-[14px] font-semibold leading-5 text-white">{{ item.label }}</span>
          <span class="ml-auto text-[12px] font-medium leading-4 text-white/55">{{ item.costText }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- 口型同步确认弹窗 -->
    <LipSyncConfirmDialog
        :visible="showLipSyncDialog"
        :messages="messages"
        mobile
        width="92%"
        @confirm="handleLipSyncDialogConfirm"
        @cancel="handleLipSyncDialogCancel"
    />
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useSafeBack} from '@/composables/useSafeBack.js';
import {ElMessage} from 'element-plus';
import {useUserStore} from '@/store/user';
import {useModelStore} from '@/store/model';
import CreationMessageList from '@/views/creation-mobile/components/CreationMessageList.vue';
import AudioPlayerSheet from '@/views/creation-mobile/components/AudioPlayerSheet.vue';
import SceneEditorSheet from '@/views/creation-mobile/components/SceneEditorSheet.vue';
import SubjectEditorSheet from '@/views/creation-mobile/components/SubjectEditorSheet.vue';
import LipSyncConfirmDialog from '@/views/creation/components/LipSyncConfirmDialog.vue';
import MobileMusicTrimmer from '@/views/home/components/MobileMusicTrimmer.vue';
import {creationBus} from '@/views/creation/creationBus';
import {useCreationChatSse} from '@/views/creation/composables/useCreationChatSse';
import {useCreationAudioController} from '@/views/creation/composables/useCreationAudioController';
import {useCreationPendingDirectEditTasks} from '@/views/creation/composables/useCreationPendingDirectEditTasks';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {convertMessageChunks} from '@/views/creation/utils/creationMessageChunks';
import {extractUserAttachmentsFromChunks} from '@/views/creation/utils/creationUserAttachments';
import {appendBlock} from '@/views/creation/utils/creationMessageBlocks';
import {createAiMessage} from '@/views/creation/utils/creationAiMessage';
import {createCreationSseEventApplier} from '@/views/creation/utils/creationSseEventApplier';
import {applyCreationVersionChange} from '@/views/creation/utils/creationVersionSwitch';
import {getHistoryMessage, getSessionPoints, uploadFile, getPointsPrice} from '@/api/creation';
import {saveUserTracking} from '@/api/tracking';
import {formatDate, isNotEmpty} from '@/utils';
import {useI18nText} from '@/i18n';
import {useAudioTrimUpload} from '@/composables/useAudioTrimUpload.js';
import {useCreationEditContextStore} from '@/store/creationEditContext';
import {
  compressImageBeforeUpload,
  CREATION_UPLOAD_ACCEPT,
  isSupportedAudioUpload,
  isSupportedImageUpload,
  processUploadFiles,
} from '@/views/creation/utils/upload.js';

/**
 * 移动端创作页：
 * - 承载创作会话消息流、历史会话切换、积分面板
 * - 复用 PC 端 SSE/消息块转换逻辑
 * - 补充移动端专属的音频播放器、附件上传与输入区交互
 */
const {t} = useI18nText();
const route = useRoute();
const router = useRouter();
const safeBack = useSafeBack();
const userStore = useUserStore();
const modelStore = useModelStore();
const editContextStore = useCreationEditContextStore();
const {resumeAllPendingDirectEditTasks} = useCreationPendingDirectEditTasks();
const subjectEditStore = useCreationSubjectEditStore();

const placeholderText = ref(t('creation.chatPanel.inputPlaceholder'));
const readonlyPlaceholderText = '仅创作者可继续编辑此对话';

const chatTitle = ref(t('creation.defaultChatTitle'));
const sessionId = ref('');
const editContextSessionKey = computed(() => {
  // 有 sessionId 时用 sessionId，新对话时用 __draft__ 作为 fallback（与 PC 端一致）
  const sid = String(sessionId.value || '');
  return isNotEmpty(sid) ? sid : '__draft__';
});
const hasEditContextSession = computed(() => isNotEmpty(editContextSessionKey.value));
const editContextState = computed(() => editContextStore.getState(editContextSessionKey.value));
const creatorUserId = ref('');

let pointsRefreshTimer = null;

const clearPointsRefreshTimer = () => {
  if (!pointsRefreshTimer) return;
  clearTimeout(pointsRefreshTimer);
  pointsRefreshTimer = null;
};

const schedulePointsRefresh = () => {
  if (!userStore.isLoggedIn) return;
  clearPointsRefreshTimer();
  pointsRefreshTimer = setTimeout(async () => {
    try {
      await userStore.fetchUserPlan();
    } catch (error) {
      ElMessage.error(error?.message || t('creation.pointsRefreshFail'));
      console.error(error);
    } finally {
      pointsRefreshTimer = null;
    }
  }, 800);
};

const loadingMessages = ref(false);
const loading = ref(false);
const currentAIMessageId = ref(null);
const draftText = ref('');

const messages = ref([]);
const ACTION_REQUIRED_CUSTOM_OPTION_ID = '__ACTION_REQUIRED_CUSTOM_OPTION__';

/**
 * 语义澄清（ACTION_REQUIRED）当前待选择 block：
 * - 只看最后一条 AI 消息
 * - 找到最后一个 ACTION_REQUIRED 且 selectedOptionId 为空
 */
const getActiveActionRequiredBlock = () => {
  const list = messages.value;
  if (!list.length) return null;
  const lastMsg = list[list.length - 1];
  if (!lastMsg || lastMsg.senderType === 'USER') return null;

  // blocks 由 createAiMessage 初始化为数组；selectedOptionId 后端 string
  const blocks = lastMsg.blocks || [];
  for (let i = blocks.length - 1; i >= 0; i -= 1) {
    const block = blocks[i];
    if (block?.type !== 'ACTION_REQUIRED') continue;
    if (block?.data?.selectedOptionId) return null; // 已解析完成（锁定）
    return block;
  }
  return null;
};

const activeActionRequiredBlock = computed(() => (loading.value ? null : getActiveActionRequiredBlock()));

watch(
  activeActionRequiredBlock,
  (block) => {
    placeholderText.value = block
      ? t('creation.chatPanel.actionRequiredPlaceholder')
      : t('creation.chatPanel.inputPlaceholder');
  },
  {immediate: true},
);
const scrollRef = ref(null);
const fileInputRef = ref(null);

/** 页面级共享音频播放器与弹层状态。 */
const audioPlayerRef = ref(null);
const audioPlayerVisible = ref(false);
const userAudioPlayer = ref(null);
const currentUserAudioMessageId = ref(null);
const subjectEditorVisible = ref(false);
const subjectEditMessage = ref(null);
const subjectEditTarget = ref(null);
const activeSubjectEditIndex = ref(0);
const sceneEditorVisible = ref(false);
const sceneEditMessage = ref(null);
const sceneEditTarget = ref(null);
const activeSceneEditIndex = ref(0);
const playerState = reactive({
  currentAudio: null,
  currentIndex: -1,
  audioList: [],
  isPlaying: false,
  currentTime: 0,
  duration: 0,
});

/** 当前输入区附件状态：图片列表与裁剪上传后的音频。 */
const uploadedImages = ref([]);
const uploadedAudio = ref(null);
const hasAttachments = computed(() => uploadedImages.value.length > 0 || !!uploadedAudio.value);
const UPLOAD_ACCEPT = CREATION_UPLOAD_ACCEPT;
const TEMP_AUDIO_FILE_ID_PREFIX = 'temp_audio_';
const TEMP_IMAGE_FILE_ID_PREFIX = 'temp_img_';
const MAX_CREATION_IMAGES = 6;
const isAudioUploading = computed(() => (
  isUploadingTrimmedAudio.value
  || String(uploadedAudio.value?.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX)
));

/** 预估积分相关常量：与 PC 端保持一致 */
const DEFAULT_IMAGE_COUNT = 7;
const IMAGE_POINTS_PER_UNIT = 28;
const POINTS_UPLIFT_RATE = 0.15;

/** 音频裁剪后的时长与预估积分 */
const uploadedAudioTrimDuration = ref(0);
const uploadedAudioEstimatedPoints = ref(null);

const intentionOptions = computed(() => [
  {value: 'CHARACTER', label: t('creation.intention.character'), icon: 'gy-character'},
  {value: 'COSTUME', label: t('creation.intention.costume'), icon: 'gy-clothes'},
  {value: 'ENVIRONMENT', label: t('creation.intention.environment'), icon: 'gy-environment'},
  {value: 'PROP', label: t('creation.intention.prop'), icon: 'gy-prop'},
  {value: 'STYLE', label: t('creation.intention.style'), icon: 'gy-style'},
]);

const getIntentionIcon = (value) => intentionOptions.value.find((i) => i.value === value)?.icon || 'gy-character';

/** 裁剪器组件引用，用于跨媒体互斥时反向调用 stopPreview()（含 WebAudio 路径）。 */
const trimmerRef = ref(null);

/** 复用首页的音频裁剪上传能力。 */
const {
  showTrimmer,
  rawFile,
  audioDuration,
  isPreparingAudio,
  isUploadingTrimmedAudio,
  openWithFile,
  uploadTrimmedBlob,
  clearUploadState,
} = useAudioTrimUpload();

const userPoints = computed(() => userStore.pointsBalance ?? 0);

const pointsPopoverVisible = ref(false);
const modelPopoverVisible = ref(false);
const sessionPointsList = ref([]); // 当前会话(session)积分流水，移动端与 PC 保持一致。
const sessionPointsLoading = ref(false); // 当前会话积分流水加载态。
const sessionPointsUpdateTime = ref(''); // 当前会话积分流水最近更新时间。

/**
 * 处理积分弹层打开：
 * - 未登录时阻止打开并提示登录
 * - 无会话时清空会话积分明细
 * - 有会话时拉取当前会话积分流水
 * @returns {void}
 */
const handlePointsPopoverShow = () => {
  if (!userStore.isLoggedIn) {
    pointsPopoverVisible.value = false;
    ElMessage.warning(t('resource.loginFirst'));
    return;
  }
  if (!sessionId.value) {
    sessionPointsList.value = [];
    sessionPointsUpdateTime.value = '';
    return;
  }
  fetchSessionPoints();
};

/**
 * 获取当前会话积分流水（与 PC 端口径一致）。
 * @returns {Promise<void>}
 */
const fetchSessionPoints = async () => {
  if (!sessionId.value || sessionPointsLoading.value) return;
  sessionPointsLoading.value = true;
  try {
    const data = await getSessionPoints({sessionId: sessionId.value});
    sessionPointsList.value = Array.isArray(data) ? data : [];
    const latestTime = sessionPointsList.value[0]?.createTime;
    sessionPointsUpdateTime.value = latestTime ? formatDate(latestTime, 'YYYY/MM/DD HH:mm:ss') : '';
  } catch (error) {
    console.error(error);
    sessionPointsList.value = [];
    sessionPointsUpdateTime.value = '';
    ElMessage.error(error?.message || t('account.points.logFetchFail'));
  } finally {
    sessionPointsLoading.value = false;
  }
};

/**
 * 切换创作模型。
 * 模型值直接写入 Pinia，首页与创作页保持同步。
 * @param {string} value
 * @returns {void}
 */
const handleSelectModel = (value) => {
  modelStore.setSelectedModel(value);
  modelPopoverVisible.value = false;
};

/**
 * 监听模型切换：
 * - 当模型变更时，若已有音频附件则重新计算预消耗积分
 * - 提示用户预估积分已更新
 */
watch(() => modelStore.selectedModel, async (newModel, oldModel) => {
  if (!oldModel || newModel === oldModel) return;
  if (!isNotEmpty(uploadedAudio.value?.fileId) || isAudioUploading.value) return;
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

/** 释放图片本地预览 URL，避免 blob 地址泄漏。 */
const revokeImagePreview = (img) => {
  if (img?.previewUrl?.startsWith?.('blob:')) URL.revokeObjectURL(img.previewUrl);
};

/** 清空当前已选音频附件。 */
const clearUploadedAudio = () => {
  uploadedAudio.value = null;
  uploadedAudioTrimDuration.value = 0;
  uploadedAudioEstimatedPoints.value = null;
};

/** 移除指定图片附件。 */
const handleRemoveImage = (idx) => {
  const img = uploadedImages.value[idx];
  revokeImagePreview(img);
  uploadedImages.value.splice(idx, 1);
};

/** 移除当前音频附件。 */
const handleRemoveAudio = () => {
  clearUploadedAudio();
};

/**
 * 点击上传按钮：
 * - 先校验当前是否可编辑
 * - 未登录时沿用创作页上传登录提示
 * - 通过隐藏 input 拉起系统文件选择器
 */
const handleUploadClick = () => {
  if (!canEditCreation.value) return;
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  fileInputRef.value?.click();
};

/**
 * 单文件入口：
 * - 图片：压缩后立即上传并加入附件列表
 * - 音频：进入裁剪器，裁剪完成后再上传
 * - 其它格式：提示不支持
 * @param {File} file
 * @returns {Promise<void>}
 */
const handleIncomingFile = async (file) => {
  if (isSupportedImageUpload(file)) {
    if (uploadedImages.value.length >= MAX_CREATION_IMAGES) {
      ElMessage.warning(t('home.uploadLimit', {count: MAX_CREATION_IMAGES}));
      return;
    }
    if (file.size / 1024 / 1024 >= 10) {
      ElMessage.error(t('home.imageTooLarge'));
      return;
    }
    try {
      const compressedFile = await compressImageBeforeUpload(file);
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const tempFileId = `${TEMP_IMAGE_FILE_ID_PREFIX}${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
      const placeholder = {
        id,
        fileId: tempFileId,
        fileName: compressedFile.name,
        previewUrl: URL.createObjectURL(compressedFile),
        fileUrl: '',
        intention: 'CHARACTER',
        uploading: true,
      };
      uploadedImages.value.push(placeholder);

      // 后台上传，成功后回填 fileId；如果用户已删除占位，则静默丢弃结果。
      const formData = new FormData();
      formData.append('file', compressedFile, compressedFile.name);
      uploadFile(formData)
        .then((result) => {
          const target = uploadedImages.value.find((img) => img.id === id);
          if (!target) return;
          target.fileId = result.fileId;
          target.fileUrl = result.fileUrl || '';
          target.uploading = false;
        })
        .catch((error) => {
          const idx = uploadedImages.value.findIndex((img) => img.id === id);
          if (idx >= 0) {
            const [removed] = uploadedImages.value.splice(idx, 1);
            revokeImagePreview(removed);
          }
          ElMessage.error(error?.message || t('home.imageUploadFail'));
        });
    } catch (error) {
      ElMessage.error(error?.message || t('home.imageUploadFail'));
    }
    return;
  }

  if (isSupportedAudioUpload(file)) {
    const opened = await openWithFile(file);
    if (opened) showTrimmer.value = true;
    return;
  }

  ElMessage.error(t('home.uploadTypeError'));
};

/**
 * 文件选择后的批量入口。
 * 上传数量限制、图片/音频分流策略与首页保持一致。
 * @param {Event} event
 * @returns {Promise<void>}
 */
const handleFileChange = async (event) => {
  const files = Array.from(event.target.files || []);
  event.target.value = '';
  if (!files.length) return;
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('creation.chatPanel.uploadNeedLogin'));
    return;
  }
  await processUploadFiles(files, {
    maxImages: MAX_CREATION_IMAGES,
    currentImagesCount: uploadedImages.value.length,
    onImageFile: handleIncomingFile,
    onAudioFile: handleIncomingFile,
    onOverLimit: (maxImages) => ElMessage.warning(t('home.uploadLimit', {count: maxImages})),
    onMultipleAudio: () => ElMessage.warning(t('home.audioLimit')),
  });
};

/**
 * 音频裁剪确认后上传音频，并回填真实 fileId。
 * 上传期间先使用临时 fileId 占位，避免用户误以为没有响应。
 * @param {{ blob: Blob, timeRange: string, durationSeconds: number }} payload
 * @returns {Promise<void>}
 */
const handleTrimmerConfirm = async (payload) => {
  const sourceFile = rawFile.value;
  const tempFileId = `${TEMP_AUDIO_FILE_ID_PREFIX}${Date.now()}`;
  const tempFileName = sourceFile?.name || t('creation.uploadedAudioFallback');
  uploadedAudio.value = {fileId: tempFileId, fileName: tempFileName, fileUrl: ''};
  uploadedAudioTrimDuration.value = payload?.durationSeconds || 0;
  uploadedAudioEstimatedPoints.value = null;
  showTrimmer.value = false;
  // 裁剪弹窗关闭后立即退出音乐详情弹层，让用户能看到底部输入区的附件 loading（isAudioUploading）。
  // 上传/积分预估在后台继续，不阻塞退出动作。
  resetAudioPlayer();
  try {
    const result = await uploadTrimmedBlob(payload, {silent: true, showSuccess: false, sourceFile});
    uploadedAudio.value = {
      fileId: result.fileId,
      fileName: result.fileName,
      fileUrl: result.fileUrl || '',
    };
    // 计算预估积分
    await recalculateUploadedAudioEstimatedPoints();
  } catch (error) {
    clearUploadedAudio();
    uploadedAudioTrimDuration.value = 0;
    uploadedAudioEstimatedPoints.value = null;
    ElMessage.error(error?.message || t('creation.makeMvFail'));
  } finally {
    clearUploadState();
  }
};

/**
 * 计算当前音频在所选模型下的预消耗积分。
 * 与 PC 端逻辑保持一致：MV 时长积分 + 图片积分，上浮 15%。
 * @returns {Promise<void>}
 */
const recalculateUploadedAudioEstimatedPoints = async () => {
  if (!isNotEmpty(uploadedAudio.value?.fileId)) return;
  if (uploadedAudioTrimDuration.value <= 0) return;

  try {
    const imagePoints = DEFAULT_IMAGE_COUNT * IMAGE_POINTS_PER_UNIT;
    const mvPoints = await getPointsPrice({
      modelName: modelStore.selectedModel,
      taskType: 'MAKE_MV',
      duration: uploadedAudioTrimDuration.value,
      // 始终带上生效分辨率：已有会话=该会话锁定值（来自 history-message），与 chat 接口一致
      resolution: modelStore.effectiveResolution,
    });
    uploadedAudioEstimatedPoints.value = Math.ceil((mvPoints + imagePoints) * (1 + POINTS_UPLIFT_RATE));
  } catch (error) {
    console.error('预消耗积分预估失败:', error);
    uploadedAudioEstimatedPoints.value = null;
  }
};

/**
 * 打开/关闭历史面板。
 * 首次打开时加载历史会话列表。
 * @returns {Promise<void>}
 */
// history panel removed

const currentUserId = computed(() => userStore.userInfo?.id);
const hasShareCreatorContext = computed(() => isNotEmpty(creatorUserId.value));
const isCreator = computed(() =>
  hasShareCreatorContext.value
  && isNotEmpty(currentUserId.value)
  && String(currentUserId.value) === String(creatorUserId.value),
);
const canEditCreation = computed(() => {
  return !hasShareCreatorContext.value || (userStore.isLoggedIn && isCreator.value);
});

/**
 * 反馈显示规则（与 PC 对齐）：
 * - 非分享上下文：始终显示
 * - 分享上下文：仅创作者可见反馈
 */
const showFeedback = computed(() => {
  if (!hasShareCreatorContext.value) return true;
  return userStore.isLoggedIn && isCreator.value;
});

/**
 * 全局媒体互斥：保证创作页同一时刻只有一个媒体源在播。
 *
 * 三种来源各自怎么进互斥：
 * - DOM 内 <audio>/<video>（含 Teleport 到 body 的弹窗）→ document 级 capture 抓 `play` 事件
 *   （`play` bubbles=false，必须 capture 才能在顶层接住）
 * - 游离的 new Audio()（userAudioPlayer）→ 自挂 `play` 监听手动转发到 handleGlobalMediaPlay
 * - 裁剪器 WebAudio（不发原生事件）→ 裁剪器 emit `play-start`，反向调 stopPreview 兜底
 *
 * 裁剪器 dialog 走 appendToBody，`$el` 只是占位注释，contains 判不出，靠 dialog class 识别。
 */
const TRIMMER_DIALOG_SELECTOR = '.mobile-music-trimmer-dialog';
const isInTrimmer = (el) => Boolean(el && el.closest(TRIMMER_DIALOG_SELECTOR));

/**
 * 暂停 target 之外的所有媒体源。target=null 表示外部源（裁剪器是源场景）。
 * 裁剪器内部元素始终跳过：要不要停裁剪器统一交给 stopPreview，同时覆盖 <audio> 和 WebAudio 两条路径。
 */
const pauseAllMediaExcept = (target) => {
  document.querySelectorAll('audio, video').forEach((el) => {
    if (el === target || isInTrimmer(el)) return;
    if (!el.paused) el.pause();
  });
  const userAudio = userAudioPlayer.value;
  if (userAudio && userAudio !== target && !userAudio.paused) userAudio.pause();
  // 源不是裁剪器内部时才停裁剪器；stopPreview 幂等，没在试听就是 no-op
  if (!isInTrimmer(target)) trimmerRef.value?.stopPreview?.();
};

const handleGlobalMediaPlay = (event) => {
  const target = event.target;
  if (!(target instanceof HTMLMediaElement)) return;
  pauseAllMediaExcept(target);
};

/**
 * 复用 PC 端音频播放控制器。
 * 不传 pauseAllVideos：移动端的全局媒体互斥（document 级 play 监听）已经覆盖视频暂停，
 * controller 内部 safePauseAllVideos 在 callback 缺失时为 no-op。
 */
const audioController = useCreationAudioController({
  audioPlayerRef,
  getAudioEl: () => audioPlayerRef.value,
  playerState,
  onOpenPanel: () => {
    audioPlayerVisible.value = true;
  },
});

// history panel related functions removed

/** 中止当前所有进行中的 SSE / clarify / resume 任务。 */
const abortAllTasks = () => {
  ['send', 'resume', 'clarify'].forEach((k) => {
    const ctrl = taskControllers[k];
    if (ctrl?.abort) ctrl.abort();
    taskControllers[k] = null;
  });
};

/** 重置全局音频播放器与播放面板状态。 */
const resetAudioPlayer = () => {
  const audio = audioPlayerRef.value;
  if (audio) {
    audio.pause();
    audio.removeAttribute('src');
    audio.load();
  }
  audioPlayerVisible.value = false;
  playerState.currentAudio = null;
  playerState.currentIndex = -1;
  playerState.audioList = [];
  playerState.isPlaying = false;
  playerState.currentTime = 0;
  playerState.duration = 0;
};

/** 重置“用户附件音频”播放态。 */
const resetUserAudioPlayer = () => {
  const audio = userAudioPlayer.value;
  if (audio) {
    audio.pause();
    audio.src = '';
  }
  currentUserAudioMessageId.value = null;
};

/**
 * 点击用户消息中的音频方块：
 * - 同一条消息再次点击时做播放/暂停切换
 * - 切到另一条消息时切歌并进入播放态
 * @param {any} msg
 * @returns {Promise<void>}
 */
const handleUserAudioPlay = async (msg) => {
  // 用户消息附件 attachments.audio：fileUrl / previewUrl 都是 string
  const audioUrl = msg?.attachments?.audio?.fileUrl || msg?.attachments?.audio?.previewUrl || '';
  const messageId = msg?.messageId;
  if (!audioUrl || !messageId) return;

  if (!userAudioPlayer.value) {
    const audio = new Audio();
    audio.preload = 'metadata';
    // 任意原因导致的暂停（用户点击 / 全局互斥 pause / 结束）都把 currentUserAudioMessageId 清掉，
    // 否则下次再点同一条用户音频时 isSameMessage 判定会错乱。
    audio.addEventListener('pause', () => {
      currentUserAudioMessageId.value = null;
    });
    // userAudio 是游离元素，document capture 抓不到它的 play 事件，必须自行转发互斥
    audio.addEventListener('play', handleGlobalMediaPlay);
    userAudioPlayer.value = audio;
  }

  const audio = userAudioPlayer.value;
  const isSameMessage = currentUserAudioMessageId.value === messageId;
  if (isSameMessage && !audio.paused) {
    audio.pause();
    return;
  }

  // 不再手动 pauseAllVideos / 暂停 audioPlayerRef：全局媒体互斥监听器会处理
  if (!isSameMessage || audio.src !== audioUrl) {
    audio.src = audioUrl;
  }
  try {
    await audio.play();
    currentUserAudioMessageId.value = messageId;
  } catch (error) {
    currentUserAudioMessageId.value = null;
    ElMessage.error(error?.message || '音频播放失败');
  }
};

/** 打开主体编辑全屏伪页面，后续设计稿会直接落在该容器内。 */
const handleSubjectEditOpen = ({message, subject, subjectIndex}) => {
  subjectEditMessage.value = message || null;
  subjectEditTarget.value = subject || null;
  activeSubjectEditIndex.value = Number.isFinite(subjectIndex) ? subjectIndex : 0;
  subjectEditorVisible.value = true;
};

/** 关闭主体编辑页，并清空本次编辑上下文。 */
const closeSubjectEditor = () => {
  subjectEditorVisible.value = false;
  subjectEditMessage.value = null;
  subjectEditTarget.value = null;
  activeSubjectEditIndex.value = 0;
};

const handleSubjectVersionChange = ({message, subject, version}) => {
  applyCreationVersionChange({
    version,
    type: 'SUBJECT',
    target: subject,
    message,
    emitEditContextChange: handleEditContextChange,
  });
};

const handleSceneEditOpen = ({message, scene, sceneIndex}) => {
  sceneEditMessage.value = message || null;
  sceneEditTarget.value = scene || null;
  activeSceneEditIndex.value = Number.isFinite(sceneIndex) ? sceneIndex : 0;
  sceneEditorVisible.value = true;
};

const handleScenePlayOpen = ({message, scene, sceneIndex}) => {
  sceneEditMessage.value = message || null;
  sceneEditTarget.value = scene || null;
  activeSceneEditIndex.value = Number.isFinite(sceneIndex) ? sceneIndex : 0;
  sceneEditorVisible.value = true;
};

const closeSceneEditor = () => {
  sceneEditorVisible.value = false;
  sceneEditMessage.value = null;
  sceneEditTarget.value = null;
  activeSceneEditIndex.value = 0;
};

const handleSceneVersionChange = ({message, scene, version}) => {
  applyCreationVersionChange({
    version,
    type: 'SCENE',
    target: scene,
    message,
    emitEditContextChange: handleEditContextChange,
  });
};

// history rename/delete/select/remove functions removed

/**
 * 用户离底超过该阈值即视为「主动上滑查看历史」，流式更新不再强制贴底。
 * 与 PC 端 `AUTO_SCROLL_THRESHOLD` 同值。
 */
const AUTO_SCROLL_THRESHOLD = 80;
/** 默认 true：初次渲染、切会话、用户主动发消息时应该贴底；用户上滑后置 false。 */
const shouldAutoScrollToBottom = ref(true);

/** 当前滚动位置是否仍在底部阈值内。 */
const isNearMobileBottom = () => {
  const el = scrollRef.value;
  if (!el) return true;
  return el.scrollHeight - el.scrollTop - el.clientHeight <= AUTO_SCROLL_THRESHOLD;
};

/** 监听消息区滚动：仅按"是否贴底"更新 shouldAutoScrollToBottom，不做其它副作用。 */
const handleScroll = () => {
  shouldAutoScrollToBottom.value = isNearMobileBottom();
};

/**
 * 滚动消息区到底部。
 * @param {boolean} [force=false] - true 时忽略「用户已上滑」状态，用于切会话 / 用户刚发消息等强同步场景。
 */
const scrollToBottom = (force = false) => {
  nextTick(() => {
    const el = scrollRef.value;
    if (!el) return;
    // 用户主动上滑离开底部时（且非强制），不抢用户视图，避免流式过程中把屏幕拽回底部
    if (!force && !shouldAutoScrollToBottom.value) return;
    el.scrollTop = el.scrollHeight;
    shouldAutoScrollToBottom.value = true;
  });
};

/** 当前轮 AI 回复结束后统一收口 loading 状态。 */
const handleChatFinished = () => {
  loading.value = false;
  currentAIMessageId.value = null;
  schedulePointsRefresh();
};

const handleSSEErrorEvent = (msg, payload = {}) => {
  const errorText = String(payload?.text || t('creation.requestFailRetry'));
  if (!msg.content) {
    // 错误能在 AI 气泡里展示就不再 toast，避免重复打扰
    appendBlock(msg, 'TEXT', errorText);
    msg.content = errorText;
  } else {
    // 已有流式内容时，气泡里看不到错误，必须 toast 提示
    ElMessage.error(errorText);
  }
  handleChatFinished();
};

let sseTypewriterBlock = null;

/**
 * 移动端 SSE 事件应用器与 PC 对齐：
 * - 覆盖 TEXT/AUDIO/VIDEO/SUBJECT/SCENE/SCENE_SCRIPT 等结构化事件
 * - 保证 continue 触发与 edit-context 重置事件按统一规则广播
 */
const applySSEEvent = createCreationSseEventApplier({
  appendBlock,
  flushTypewriter: () => {
    sseTypewriterBlock = null;
  },
  enqueueTypewriterText: (text) => {
    const chunk = String(text ?? '');
    if (!chunk) return;
    if (sseTypewriterBlock) sseTypewriterBlock.text = `${String(sseTypewriterBlock.text || '')}${chunk}`;
    // 流式追加文本同样不能 force，否则用户上滑查看历史会被拽回底部
    scrollToBottom();
  },
  setTypewriterBlock: (block) => {
    sseTypewriterBlock = block || null;
  },
  creationBus,
  // 注意：这里不能 force=true。SSE 流式过程中每个事件都贴底，会把用户上滑查看历史的视图拽回来。
  // 与 PC 一致：传非强制版本，受 shouldAutoScrollToBottom 控制；用户主动发消息等强同步场景由 useCreationChatSse 内部按需用 force=true。
  scrollToBottom,
  handleChatFinished,
  handleSSEErrorEvent,
  messages,
  onAssetGenerated: schedulePointsRefresh,
});

/** 最小 AbortController 管理：与 PC 行为一致（同类任务启动前先中止旧任务）。 */
const taskControllers = reactive({send: null, resume: null, clarify: null});

const createContinueFlow = (prompt) => {
  const msgId = ref(null);
  const disabled = ref(false);
  const resetState = () => {
    disabled.value = false;
  };
  const onReceived = ({msgId: nextMsgId} = {}) => {
    const normalizedMsgId = nextMsgId ?? null;
    if (msgId.value !== normalizedMsgId) {
      msgId.value = normalizedMsgId;
      resetState();
      return;
    }
    msgId.value = normalizedMsgId;
    resetState();
  };
  const onReset = () => {
    msgId.value = null;
    resetState();
  };
  const submitIfMatch = async (targetMsgId, extras = null) => {
    if (!canEditCreation.value) return;
    if (disabled.value) return;
    if (msgId.value !== targetMsgId) return;
    disabled.value = true;
    await submitMessageWithPrompt(prompt, extras);
  };
  const lock = () => {
    if (isNotEmpty(msgId.value) && !disabled.value) disabled.value = true;
  };
  return {msgId, disabled, onReceived, onReset, submitIfMatch, lock};
};

const subjectContinueFlow = createContinueFlow('确认并继续');
const sceneScriptContinueFlow = createContinueFlow('确认并生成');
const sceneContinueFlow = createContinueFlow('完成制作');
const subjectContinueMsgId = subjectContinueFlow.msgId;
const subjectContinueDisabled = subjectContinueFlow.disabled;
const sceneScriptContinueMsgId = sceneScriptContinueFlow.msgId;
const sceneScriptContinueDisabled = sceneScriptContinueFlow.disabled;
const sceneContinueMsgId = sceneContinueFlow.msgId;
const sceneContinueDisabled = sceneContinueFlow.disabled;

const syncContinueStateByMessages = () => {
  const aiMessages = Array.isArray(messages.value)
    ? messages.value.filter((item) => item?.senderType !== 'USER')
    : [];
  const lastSubjectMsg = [...aiMessages].reverse().find((item) => Array.isArray(item?.subjectList) && item.subjectList.length > 0);
  if (lastSubjectMsg) creationBus.emit('subject:received', {msgId: lastSubjectMsg.messageId});
  else creationBus.emit('subject:reset');

  // blocks 由 createAiMessage 初始化为数组
  const lastSceneScriptMsg = [...aiMessages].reverse().find(
    (item) => item?.blocks?.some((block) => block?.type === 'SCENE_SCRIPT'),
  );
  if (lastSceneScriptMsg) creationBus.emit('scene-script:received', {msgId: lastSceneScriptMsg.messageId});
  else creationBus.emit('scene-script:reset');

  // sceneList 由 createAiMessage 初始化为数组；这里只判 length 即可
  const lastSceneMsg = [...aiMessages].reverse().find((item) => item?.sceneList?.length > 0);
  if (lastSceneMsg) creationBus.emit('scene:received', {msgId: lastSceneMsg.messageId});
  else creationBus.emit('scene:reset');
};

const clearEditContextState = (sessionKey = editContextSessionKey.value) => {
  const key = String(sessionKey || '');
  if (!isNotEmpty(key)) return null;
  return editContextStore.resetState(key);
};
const pendingEditContextClearOriginKey = ref(null);
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
 * SCENE_SCRIPT subjectRefs 浅克隆。
 * subjectRefs[].subjectId / fileId / url 后端皆 string。
 */
const cloneSubjectRefs = (refs) => (refs || []).map((item) => ({
  subjectId: item.subjectId,
  fileId: item.fileId,
  url: item.url,
}));
const isSameSubjectRefs = (a, b) => JSON.stringify(a) === JSON.stringify(b);

const updateSceneScriptEditContext = (payload = {}) => {
  if (!hasEditContextSession.value || !editContextState.value) return;
  // messageId / lineKey(=SCENE_SCRIPT.id) 后端皆 string；previousPromptUnwrapped/nextPromptUnwrapped 是 string
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

  if (!editContextState.value.sceneScriptInitial[key]) editContextState.value.sceneScriptInitial[key] = {};
  if (!editContextState.value.sceneScriptCurrent[key]) editContextState.value.sceneScriptCurrent[key] = {messageId, lineKey};
  const initial = editContextState.value.sceneScriptInitial[key];
  const current = editContextState.value.sceneScriptCurrent[key];

  if (hasPromptPayload) {
    if (initial.visualPrompt === undefined) initial.visualPrompt = prevPrompt || '';
    if ((nextPrompt || '') === (initial.visualPrompt || '')) delete current.visualPrompt;
    else current.visualPrompt = nextPrompt || '';
  }

  if (hasSubjectRefsPayload) {
    if (initial.subjectRefs === undefined) initial.subjectRefs = cloneSubjectRefs(prevSubjectRefs);
    const nextRefs = cloneSubjectRefs(nextSubjectRefs);
    if (isSameSubjectRefs(nextRefs, initial.subjectRefs)) delete current.subjectRefs;
    else current.subjectRefs = nextRefs;
  }

  if (current.visualPrompt === undefined && current.subjectRefs === undefined) {
    delete editContextState.value.sceneScriptCurrent[key];
  }
};

const findVersionItemByMessageAndKey = (messageId, itemKey, listKey) => {
  const msg = messages.value.find((m) => m.messageId === messageId);
  if (!msg) return null;
  return (msg[listKey] || []).find((item) => item?.messageChunkId === itemKey) || null;
};
// versions=array、version=number、imgFileId/videoFileId=string
const getSubjectImgFileIdForVersion = (subject, versionNum) => {
  return (subject?.versions || []).find((x) => x.version === versionNum)?.imgFileId || '';
};
const getSceneVideoFileIdForVersion = (scene, versionNum) => {
  return (scene?.versions || []).find((x) => x.version === versionNum)?.videoFileId || '';
};
const buildVersionEditLines = ({
  items, initialMap, sectionTitle, fieldName, listKey, getFileIdForVersion,
}) => {
  const lines = [];
  items.forEach((item) => {
    const key = `${item.messageId}::${item.itemKey}`;
    const initialVer = initialMap[key];
    const finalVer = item.activeVersion;
    if (!isNotEmpty(initialVer) || !isNotEmpty(finalVer)) return;
    const target = findVersionItemByMessageAndKey(item.messageId, item.itemKey, listKey);
    const fromId = target ? getFileIdForVersion(target, initialVer) : '';
    const toId = target ? getFileIdForVersion(target, finalVer) : '';
    lines.push(`${fieldName} 由"${fromId}"改为"${toId}"，最终选择版本V${finalVer + 1}`);
  });
  if (!lines.length) return [];
  return [sectionTitle, ...lines];
};
const collectVersionCurrentItems = (initialMap, currentMap, listKey) => {
  const result = [];
  // messageId / itemKey(=chunkId) 后端皆 string；activeVersion 后端 number
  Object.values(currentMap || {}).forEach((item) => {
    const {messageId, itemKey} = item;
    if (!messageId || !itemKey) return;
    const key = `${messageId}::${itemKey}`;
    const initialVersion = initialMap?.[key];
    if (!isNotEmpty(initialVersion)) return;
    const target = findVersionItemByMessageAndKey(messageId, itemKey, listKey);
    if (!target) return;
    const finalVersion = target.activeVersion;
    if (!isNotEmpty(finalVersion) || finalVersion === initialVersion) return;
    result.push({messageId, itemKey, activeVersion: finalVersion});
  });
  return result;
};
const buildEditContext = () => {
  if (!hasEditContextSession.value || !editContextState.value) return '';
  const subject = collectVersionCurrentItems(editContextState.value.subjectInitial, editContextState.value.subjectCurrent, 'subjectList');
  const scene = collectVersionCurrentItems(editContextState.value.sceneInitial, editContextState.value.sceneCurrent, 'sceneList');
  const sceneScript = Object.values(editContextState.value.sceneScriptCurrent).map((item) => ({
    messageId: item.messageId,
    lineKey: item.lineKey,
    visualPrompt: item.visualPrompt,
    subjectRefs: item.subjectRefs,
  }));
  if (!subject.length && !scene.length && !sceneScript.length) return '';
  const lines = [];
  lines.push(...buildVersionEditLines({
    items: subject,
    initialMap: editContextState.value.subjectInitial,
    sectionTitle: '【Subject 参考图修改】',
    fieldName: 'imgFileId',
    listKey: 'subjectList',
    getFileIdForVersion: getSubjectImgFileIdForVersion,
  }));
  lines.push(...buildVersionEditLines({
    items: scene,
    initialMap: editContextState.value.sceneInitial,
    sectionTitle: '【Scene 视频修改】',
    fieldName: 'videoFileId',
    listKey: 'sceneList',
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
      const toSubjectRefs = hasRefChange ? cloneSubjectRefs(item.subjectRefs || []) : cloneSubjectRefs(initial.subjectRefs || []);
      scriptLines.push(`脚本行 ${item.lineKey}：visualPrompt 从"${fromPrompt}"改为"${toPrompt}"，subjectRefs 改为"${JSON.stringify(toSubjectRefs)}"`);
    });
    if (scriptLines.length) lines.push('【Scene_script 分镜脚本修改】', ...scriptLines);
  }
  return lines.join('\n').trim();
};

const handleEditContextChange = (payload = {}) => {
  // type 由调用方以常量字符串传入，无需转换
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
const handleSceneScriptChange = (payload = {}) => {
  handleEditContextChange({
    type: 'SCENE_SCRIPT',
    messageId: payload?.message?.messageId,
    ...payload,
  });
};
const handleVersionResetFromSSE = (payload = {}) => {
  // messageId / itemKey(=chunkId) 后端 string；activeVersion 后端 number
  const {type, messageId, itemKey, activeVersion} = payload;
  if (type !== 'SUBJECT' && type !== 'SCENE') return;
  if (!editContextState.value) return;
  if (!messageId || !itemKey) return;
  const initialKey = type === 'SUBJECT' ? 'subjectInitial' : 'sceneInitial';
  const currentKey = type === 'SUBJECT' ? 'subjectCurrent' : 'sceneCurrent';
  const key = `${messageId}::${itemKey}`;
  delete editContextState.value[currentKey][key];
  editContextState.value[initialKey][key] = activeVersion;
};

/**
 * 注册某类任务的 AbortController。
 * 同类型新任务启动前会先中止旧任务。
 * @param {'send'|'resume'|'clarify'} kind
 * @returns {AbortController}
 */
const registerTaskController = (kind) => {
  const prev = taskControllers[kind];
  if (prev?.abort) prev.abort();
  const next = new AbortController();
  taskControllers[kind] = next;
  return next;
};

/** 复用 PC 端 SSE 编排能力，移动端只负责传入状态容器。 */
const {handleSend, handleResumeChat, handleClarifyActionRequired} = useCreationChatSse({
  userStore,
  router,
  creationBus,
  sessionId,
  chatTitle,
  messages,
  currentAIMessageId,
  loading,
  applySSEEvent,
  handleChatFinished,
  registerTaskController,
  taskControllers,
  scrollToBottom,
  createAiMessage,
  appendBlock,
});

/**
 * 拉取并渲染某个会话的历史消息。
 * 同时兼容历史中的用户附件回显，以及未完成会话的 resume。
 * @param {string} sid
 * @returns {Promise<void>}
 */
const loadHistory = async (sid) => {
  if (!sid) return;
  loadingMessages.value = true;
  try {
    const data = await getHistoryMessage(sid);
    // 已有会话锁定其分辨率，模型选项据此过滤可选模型
    modelStore.setConversationResolution(data?.resolution);
    // 后端契约：chatMessages 为 array
    const list = data?.chatMessages || [];
    messages.value = list.map((item) => {
      const {content, audioList, videoList, subjectList, sceneList, blocks} = convertMessageChunks(item.messageChunks);
      const mapped = {
        messageId: item.messageId,
        senderType: item.senderType,
        content,
        audioList,
        videoList,
        subjectList,
        sceneList,
        blocks,
        sessionId: sid,
      };
      if (item.senderType === 'USER') {
        const attachments = extractUserAttachmentsFromChunks(item.messageChunks);
        if (attachments.audio || attachments.images.length > 0) {
          mapped.attachments = attachments;
        }
      }
      return mapped;
    });
    scrollToBottom(true);
    syncContinueStateByMessages();

    // 历史消息加载完后，统一扫描 SUBJECT / SCENE 是否有进行中的生成新版本任务，
    // 命中则后台续上轮询。版本变更记录由 store 在轮询成功时直接处理，无需回调。
    resumeAllPendingDirectEditTasks(sid, messages.value);

    if (data?.resumeChat && data?.lastMessageId != null) {
      handleResumeChat(sid, data.lastMessageId).catch((error) => {
        console.error(error);
      });
    }
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.message || t('creation.historyLoadFail'));
  } finally {
    loadingMessages.value = false;
  }
};

/**
 * 处理从首页携带过来的 prompt/audio/images 查询参数。
 * 若 URL 中有 prompt，则进入页面后自动发送一次创作请求。
 * @returns {Promise<void>}
 */
const consumePromptQueryIfNeeded = async () => {
  const prompt = String(route.query?.prompt || '').trim();
  if (!prompt) return;

  const nextQuery = {};
  if (isNotEmpty(sessionId.value)) nextQuery.sessionId = sessionId.value;
  if (isNotEmpty(creatorUserId.value)) nextQuery.creatorUserId = creatorUserId.value;
  const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
  router.replace({name: 'creation', params: {lang}, query: nextQuery});

  if (!canEditCreation.value) return;

  const audioFileId = String(route.query?.audioFileId || '');
  const audioUrl = String(route.query?.audioUrl || '');
  const audioFileName = String(route.query?.fileName || '');
  let imageFiles = [];
  let currentImages = [];
  if (route.query?.images) {
    try {
      const parsed = JSON.parse(String(route.query.images)) || [];
      imageFiles = Array.isArray(parsed)
        ? parsed.map((img) => ({fileId: img?.fileId || '', intention: img?.intention || 'CHARACTER'}))
        : [];
      currentImages = Array.isArray(parsed)
        ? parsed.map((img) => ({
            fileId: img?.fileId || '',
            fileUrl: img?.fileUrl || '',
            previewUrl: img?.fileUrl || img?.previewUrl || '',
            intention: img?.intention || 'CHARACTER',
          }))
        : [];
    } catch {
      imageFiles = [];
      currentImages = [];
    }
  }

  const currentAttachments = audioFileId
    ? {fileId: audioFileId, fileName: audioFileName, fileUrl: audioUrl}
    : null;

  await handleSend({
    payload: {
      userMessage: prompt,
      audioFileId,
      imageFiles,
      currentAttachments: {audio: currentAttachments, images: currentImages},
      editContext: buildEditContext(),
    },
    options: {},
  });
};

/**
 * 根据路由初始化当前页面上下文：
 * - 读取 sessionId / creatorUserId
 * - 按需加载历史
 * - 消费首页跳转带来的 prompt 等参数
 * @returns {Promise<void>}
 */
const initFromRoute = async () => {
  const sid = String(route.query?.sessionId || '');
  const cid = String(route.query?.creatorUserId || '');
  const sessionName = String(route.query?.sessionName || '').trim();
  closeSubjectEditor();
  closeSceneEditor();
  sessionId.value = sid;
  creatorUserId.value = cid;
  chatTitle.value = sessionName || t('creation.defaultChatTitle');

  if (sid) {
    resetAudioPlayer();
    await loadHistory(sid);
  } else {
    resetAudioPlayer();
    messages.value = [];
    syncContinueStateByMessages();
  }

  await consumePromptQueryIfNeeded();
};

/**
 * 点击发送：
 * - 支持纯文本发送
 * - 支持仅附件发送
 * - 若音频仍在上传中则阻止发送
 * @returns {Promise<void>}
 */
const handleActionRequiredSubmit = async (payload = {}) => {
  if (!canEditCreation.value) return;
  await handleClarifyActionRequired(payload);
};

const handleSendClick = async () => {
  if (!canEditCreation.value) return;
  const text = String(draftText.value || '').trim();
  if (activeActionRequiredBlock.value) {
    if (!text) return;

    handleClarifyActionRequired({
      kind: activeActionRequiredBlock.value.data.kind,
      actionId: activeActionRequiredBlock.value.data.actionId,
      optionId: ACTION_REQUIRED_CUSTOM_OPTION_ID,
      optionLabel: text,
      query: text,
      userMessage: text,
    }).catch((error) => {
      console.error(error);
    });

    // 与 PC 类似：清空输入与已选附件（避免混用旧 draft）
    draftText.value = '';
    uploadedImages.value.forEach(revokeImagePreview);
    uploadedImages.value = [];
    clearUploadedAudio();
    clearUploadState();
    return;
  }
  if (!text) {
    ElMessage.warning(t('home.createPromptEmpty'));
    return;
  }
  if (uploadedImages.value.some((img) => img?.uploading || String(img?.fileId || '').startsWith(TEMP_IMAGE_FILE_ID_PREFIX))) {
    ElMessage.warning('图片上传中，请稍后');
    return;
  }
  if (uploadedAudio.value?.fileId?.startsWith?.(TEMP_AUDIO_FILE_ID_PREFIX) || isUploadingTrimmedAudio.value) {
    ElMessage.warning(t('creation.chatPanel.audioUploading'));
    return;
  }
  subjectContinueFlow.lock();
  sceneScriptContinueFlow.lock();
  sceneContinueFlow.lock();
  pendingEditContextClearOriginKey.value = hasEditContextSession.value ? editContextSessionKey.value : null;
  draftText.value = '';
  const currentAttachments = {
    audio: uploadedAudio.value?.fileId
      ? {fileId: uploadedAudio.value.fileId, fileName: uploadedAudio.value.fileName, fileUrl: uploadedAudio.value.fileUrl || ''}
      : null,
    images: uploadedImages.value.map((img) => ({
      fileId: img.fileId,
      fileUrl: img.fileUrl || img.previewUrl,
      previewUrl: img.previewUrl,
      intention: img.intention || 'CHARACTER',
    })),
  };
  const imageFiles = uploadedImages.value.map((img) => ({fileId: img.fileId, intention: img.intention || 'CHARACTER'}));
  // uploadedAudio.fileId 是上传成功后存的 string，临时占位前缀也是 string
  const audioFileId = uploadedAudio.value?.fileId?.startsWith?.(TEMP_AUDIO_FILE_ID_PREFIX) ? '' : (uploadedAudio.value?.fileId || '');
  handleSend({
    payload: {
      userMessage: text,
      audioFileId,
      imageFiles,
      currentAttachments,
      editContext: buildEditContext(),
    },
    options: {},
  }).catch((error) => {
    console.error(error);
  });
  uploadedImages.value.forEach(revokeImagePreview);
  uploadedImages.value = [];
  clearUploadedAudio();
  clearUploadState();
};

const submitMessageWithPrompt = async (prompt, extras = null) => {
  const text = (prompt || '').trim();
  if (!text || !canEditCreation.value) return;
  pendingEditContextClearOriginKey.value = hasEditContextSession.value ? editContextSessionKey.value : null;
  handleSend({
    payload: {
      userMessage: text,
      audioFileId: '',
      imageFiles: [],
      currentAttachments: {audio: null, images: []},
      editContext: buildEditContext(),
    },
    options: extras ? { finalMakeExtras: extras } : {},
  }).catch((error) => {
    console.error(error);
  });
};

const handleSubjectContinue = async (msgId) => subjectContinueFlow.submitIfMatch(msgId);
const handleSceneScriptContinue = async (msgId) => sceneScriptContinueFlow.submitIfMatch(msgId);
const handleSceneContinue = async (msgId, extras) => sceneContinueFlow.submitIfMatch(msgId, extras);

const messageListRef = ref(null);
const showLipSyncDialog = ref(false);
const pendingLipSyncInfo = ref(null);

const handleSceneLipsyncConfirm = (msg, extras) => {
  pendingLipSyncInfo.value = { msg, extras };
  showLipSyncDialog.value = true;
};

const handleLipSyncDialogConfirm = () => {
  showLipSyncDialog.value = false;
  const info = pendingLipSyncInfo.value;
  pendingLipSyncInfo.value = null;
  if (!info) return;
  messageListRef.value?.dismissSceneGuide(info.msg);
  sceneContinueFlow.submitIfMatch(info.msg?.messageId, info.extras).catch((error) => {
    console.error(error);
  });
};

const handleLipSyncDialogCancel = () => {
  showLipSyncDialog.value = false;
  pendingLipSyncInfo.value = null;
  messageListRef.value?.resetLipSync();
};

/** 点击音频卡片后，切换播放并打开音频播放面板。 */
const handleAudioOpen = ({item, index, audioList}) => {
  audioController.togglePlay({item, index, audioList, openPanel: true}).catch((error) => {
    console.error(error);
  });
};

/** 播放面板里的大按钮：切换当前音频播放/暂停。 */
const handlePlayerTogglePlay = () => {
  if (!playerState.currentAudio) return;
  audioController.togglePlay({
    item: playerState.currentAudio,
    index: playerState.currentIndex,
    audioList: playerState.audioList,
    openPanel: false,
  }).catch((error) => {
    console.error(error);
  });
};

// OPPO / 部分国产浏览器：play / playing / loadedmetadata / timeupdate 事件派发都不可信。
// 用 rAF 在播放期间主动 poll currentTime 和 duration，60fps 平滑刷新。
// 启动条件 watch playerState.isPlaying（见下方 watch）——audio.play() 成功后 controller 主动 set
// 这个 flag，跟事件无关，是最可靠的"播放真的开始了"信号。
let progressRafId = null;
const stopProgressTicker = () => {
  if (progressRafId !== null) {
    cancelAnimationFrame(progressRafId);
    progressRafId = null;
  }
};
const startProgressTicker = () => {
  if (progressRafId !== null) return;
  const tick = () => {
    const audio = audioPlayerRef.value;
    if (!audio) {
      progressRafId = null;
      return;
    }
    // duration 兜底：loadedmetadata 不可信，每帧重读 audio.duration，可用就同步
    const d = audio.duration;
    if (Number.isFinite(d) && d > 0 && playerState.duration !== d) {
      playerState.duration = d;
    }
    playerState.currentTime = audio.currentTime || 0;
    if (audio.paused || audio.ended) {
      progressRafId = null;
      return;
    }
    progressRafId = requestAnimationFrame(tick);
  };
  progressRafId = requestAnimationFrame(tick);
};

// 兜底启动通道：isPlaying 由 controller 在 audio.play() 成功后直接 set，
// 跨任何浏览器都会触发；只要走到 true，就启动 rAF——不再依赖 play/playing 事件能否派发。
watch(() => playerState.isPlaying, (playing) => {
  if (playing) startProgressTicker();
  else stopProgressTicker();
});

/**
 * audio 元素 timeupdate：作为 rAF 漏启时的兜底——只要在播放，就保证 ticker 跑起来。
 * 同时也直接更新一次 currentTime，避免极端情况下 rAF 都不跑时进度卡死。
 */
const onAudioTimeUpdate = () => {
  const audio = audioPlayerRef.value;
  if (!audio) return;
  playerState.currentTime = audio.currentTime;
  if (!audio.paused && progressRafId === null) startProgressTicker();
};

/** audio 元素元数据加载后同步总时长（OPPO 可能不发，rAF 里还有兜底）。 */
const onAudioLoadedMetadata = () => {
  const audio = audioPlayerRef.value;
  if (!audio) return;
  playerState.duration = audio.duration;
};

/** audio 原生 play 事件：同步 isPlaying，开启 rAF 进度刷新。 */
const onAudioPlay = () => {
  playerState.isPlaying = true;
  startProgressTicker();
};

/** audio 原生 playing 事件：play 事件被吞时的二次保险，同样启动 ticker。 */
const onAudioPlaying = () => {
  playerState.isPlaying = true;
  startProgressTicker();
};

/**
 * audio 原生 pause 事件：保证被全局媒体互斥（其它视频/用户音频开始播放时）外部 pause 时也同步 UI 播放态。
 * 不在这里清空 currentAudio，否则面板会跳回空态——只同步 isPlaying + 停 rAF。
 */
const onAudioPause = () => {
  playerState.isPlaying = false;
  stopProgressTicker();
};

/** audio 播放结束后重置当前播放时间。 */
const onAudioEnded = () => {
  playerState.isPlaying = false;
  playerState.currentTime = 0;
  stopProgressTicker();
};

/** audio 播放失败时回退为非播放态。 */
const onAudioError = () => {
  playerState.isPlaying = false;
  stopProgressTicker();
};

/** 播放面板拖动进度条时按比例 seek。 */
const handleAudioSeek = (ratio) => {
  audioController.seekByRatio(ratio);
};

/**
 * 使用当前播放中的音频继续发起“制作 MV”请求。
 * @returns {Promise<void>}
 */
const handlePrepareMv = async () => {
  if (!canEditCreation.value) return;
  const currentAudio = playerState.currentAudio;
  // AUDIO 字段契约：audioUrl / title 都是 string；fileUrl/previewUrl/fileName/name 不在契约里
  const audioUrl = currentAudio?.audioUrl || '';
  if (!audioUrl) {
    ElMessage.warning(t('creation.selectAudioFirst'));
    return;
  }

  const fileName = currentAudio?.title || 'audio.mp3';
  try {
    const response = await fetch(audioUrl);
    if (!response.ok) throw new Error('fetch audio failed');
    const blob = await response.blob();
    const file = new File([blob], fileName.endsWith('.mp3') ? fileName : `${fileName}.mp3`, {
      type: blob.type || 'audio/mpeg',
    });
    const opened = await openWithFile(file);
    if (!opened) return;
    showTrimmer.value = true;
  } catch (error) {
    ElMessage.warning(error?.message || t('creation.makeMvFail'));
  }
};

/**
 * 分享当前音频：
 * - 若音频挂有 projectId，优先请求分享链接
 * - 否则回退为当前页面地址
 * @returns {Promise<void>}
 */
/**
 * 下载当前播放中的音频文件。
 * 与 PC/资产页保持一致：通过临时 <a download> 链接触发浏览器下载。
 */
const handleDownloadCurrentAudio = () => {
  const currentAudio = playerState.currentAudio;
  if (!currentAudio) return;
  // AUDIO 字段契约：audioUrl / title 是 string
  const url = currentAudio?.audioUrl || '';
  if (!url) {
    ElMessage.warning(t('creation.selectAudioFirst'));
    return;
  }

  saveUserTracking({target: 'CREATE_MUSIC_DOWNLOAD_AUDIO'}).catch((error) => {
    console.error('创作页移动端音乐下载埋点上报失败:', error);
  });

  const baseName = currentAudio?.title || 'audio';
  const link = document.createElement('a');
  link.href = url;
  link.download = baseName;
  link.target = '_blank';
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/** 返回上一页：站内无上一页时兜底跳首页，避免刷新后点了没反应。 */
const handleBack = () => {
  safeBack();
};

/** 首次进入页面时初始化用户计划、路由上下文和历史列表。 */
onMounted(async () => {
  saveUserTracking({target: 'ENTER_CREATE_CHAT'}).catch((error) => {
    console.error('进入创作对话页埋点上报失败:', error);
  });
  if (userStore.isLoggedIn) userStore.fetchUserPlan();
  // 全局媒体互斥：保证整页同一时刻只有一个 <audio>/<video> 在播放
  document.addEventListener('play', handleGlobalMediaPlay, true);
  creationBus.on('chat:submit:success', onChatSubmitSuccess);
  creationBus.on('subject:received', subjectContinueFlow.onReceived);
  creationBus.on('subject:reset', subjectContinueFlow.onReset);
  creationBus.on('scene-script:received', sceneScriptContinueFlow.onReceived);
  creationBus.on('scene-script:reset', sceneScriptContinueFlow.onReset);
  creationBus.on('scene:received', sceneContinueFlow.onReceived);
  creationBus.on('scene:reset', sceneContinueFlow.onReset);
  creationBus.on('edit-context:version-reset', handleVersionResetFromSSE);
  await initFromRoute();
});

/** 页面销毁时清理任务、播放器与本地附件预览。 */
onBeforeUnmount(() => {
  document.removeEventListener('play', handleGlobalMediaPlay, true);
  abortAllTasks();
  // 停止后台轮询，避免离开页面后继续烧网络；任务数据保留，下次回到页面会自动 resume
  // 注：SUBJECT 与 SCENE 共用同一个 store（taskType 区分），一次调用即可清理两类任务
  subjectEditStore.stopAllPolling();
  clearPointsRefreshTimer();
  closeSubjectEditor();
  closeSceneEditor();
  resetAudioPlayer();
  resetUserAudioPlayer();
  stopProgressTicker();
  uploadedImages.value.forEach(revokeImagePreview);
  clearUploadedAudio();
  clearUploadState();
  creationBus.off('chat:submit:success', onChatSubmitSuccess);
  creationBus.off('subject:received', subjectContinueFlow.onReceived);
  creationBus.off('subject:reset', subjectContinueFlow.onReset);
  creationBus.off('scene-script:received', sceneScriptContinueFlow.onReceived);
  creationBus.off('scene-script:reset', sceneScriptContinueFlow.onReset);
  creationBus.off('scene:received', sceneContinueFlow.onReceived);
  creationBus.off('scene:reset', sceneContinueFlow.onReset);
  creationBus.off('edit-context:version-reset', handleVersionResetFromSSE);
  // 离开创作页解除会话分辨率锁定，避免泄漏到首页等其它页面的模型选项
  modelStore.setConversationResolution(null);
});

// history search watch removed
</script>

<!-- NOTE: creation-mobile-* / creation-history-* UI 相关样式已迁移到 Tailwind class，避免在 mobile.vue 中堆积 scoped SCSS。 -->
<style lang="scss">
// 模型选择底部抽屉：占满宽度、从底部上滑，顶部圆角 + lime 光晕（与首页同款）
.el-drawer.creation-mobile-model-drawer {
  background: #0A0A0A;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  box-shadow: 0 -8px 30px rgba(194, 255, 0, 0.5);

  .el-drawer__body {
    padding: 0;
  }
}

.creation-mobile-points-popover {
  /* 先 1:1 对齐 PC 端 CreationTopBar 的弹层视觉，再做移动端尺寸微调 */
  padding: 18px 12px 16px !important;
  border: none !important;
  border-radius: 20px !important;
  background: #F0F6DD !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3) !important;
}

.no-scrollbar {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari and Opera */
  }
}
</style>

<!-- history panel related styles removed -->

