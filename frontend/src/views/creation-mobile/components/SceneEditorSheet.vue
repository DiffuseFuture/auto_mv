<template>
  <!-- 移动端分镜编辑全屏页：仅保留横屏预览，交互风格贴近视频卡片 -->
  <div v-if="visible" class="fixed inset-0 z-[70] flex flex-col bg-[linear-gradient(90deg,#081100_0%,#243d15_100%)] text-white">
    <!-- 顶部标题区 -->
    <div class="shrink-0 px-[15px] pb-[10px] pt-[18px] text-center">
      <div class="text-[20px] font-semibold leading-[28px]">{{ t('creation.sceneEditDialog.title') }}</div>
    </div>

    <!-- 主内容区：视频预览 + 参考图 + 提示词 -->
    <div class="min-h-0 flex-1 overflow-y-auto px-[15px]">
      <!-- 分镜标题行：左侧分镜序号，右侧版本下拉（与 PC 端 SceneEditDialog 样式一致） -->
      <div class="mb-[5px] flex items-center justify-between gap-2">
        <div class="text-[15px] font-bold leading-[21px] text-white">
          {{ t('creation.messageArea.sceneLabel', {index: displaySceneIndex}) }}
        </div>
        <el-select
          v-if="versionList.length > 0"
          size="small"
          :title="formatCreationVersionLabel(editTargetScene?.activeVersion)"
          class="dialog-version-select"
          popper-class="dialog-version-select-popper"
          :show-arrow="false"
          :model-value="editTargetScene?.activeVersion"
          @update:model-value="handleSceneVersionChange"
        >
          <el-option
            v-for="v in versionList"
            :key="v.version"
            :label="formatCreationVersionLabel(v.version)"
            :value="v.version"
          />
        </el-select>
      </div>

      <!-- 分镜预览区：始终按横屏比例展示，并支持前后版本切换 -->
      <div class="relative aspect-[16/9] w-full overflow-hidden rounded-[5px] bg-black">
        <video
          v-if="activeVersion?.videoUrl && !scenePreviewingImage"
          :key="`scene-video-${activeVersion?.videoUrl}`"
          :ref="setSceneVideoRef"
          :src="activeVersion.videoUrl"
          :poster="activeVersion.coverUrl || undefined"
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
          preload="metadata"
          class="w-full h-full object-contain"
          @loadedmetadata="handleSceneVideoLoadedMetadata"
          @timeupdate="handleSceneVideoTimeUpdate"
          @play="handleSceneVideoPlay"
          @pause="handleSceneVideoPause"
          @ended="handleSceneVideoEnded"
          @click.stop="handleSceneVideoSurfaceTap"
        ></video>
        <div
          v-else
          :key="'scene-video-empty'"
          class="absolute inset-0 flex-center bg-black/40 text-white/55"
        >
          {{ t('creation.sceneEditDialog.noVideo') }}
        </div>

        <!-- 覆盖层：中部播放按钮 + 底部时间/进度/全屏 -->
        <div
          class="absolute inset-0 z-[1] flex flex-col justify-between p-[12px]"
          :class="shouldShowSceneControls ? 'bg-[linear-gradient(180deg,rgba(0,0,0,0)_0%,rgba(0,0,0,0.08)_52%,rgba(0,0,0,0.42)_100%)]' : ''"
          @click="handleSceneVideoSurfaceTap"
        >
          <div></div>
          <div v-if="shouldShowSceneControls" class="flex flex-1 items-center justify-center">
            <button type="button" class="flex h-[54px] w-[54px] items-center justify-center rounded-full" @click.stop="toggleSceneVideoPlay">
              <svg-icon :name="sceneVideoPlaying ? 'gy-pause' : 'gy-play2'" size="32" color="#C2FF00"></svg-icon>
            </button>
          </div>
          <div v-else class="flex-1"></div>
          <div v-if="shouldShowSceneControls" class="flex items-center gap-[8px] text-[10px] leading-[14px] text-white">
            <span class="shrink-0">{{ formatDuration(sceneVideoCurrentTime) }}</span>
            <button class="min-w-0 flex-1" type="button" @click.stop="handleSceneSeek">
              <div class="h-[4px] rounded-full bg-[rgba(255,255,255,0.28)]">
                <div class="h-full rounded-full bg-[#C2FF00]" :style="{width: `${sceneProgressPercent}%`}"></div>
              </div>
            </button>
            <span class="shrink-0">{{ formatDuration(sceneVideoDuration) }}</span>
            <button
              type="button"
              class="flex h-[18px] w-[18px] shrink-0 items-center justify-center text-white/92"
              @click.stop="handleSceneFullscreen"
            >
              <svg-icon name="gy-enlarge" size="13"></svg-icon>
            </button>
          </div>
        </div>

      </div>

      <!-- 参考图区：上传和删除分镜参考图 -->
      <div class="mt-3">
        <div class="mb-1 text-[14px] font-medium leading-[20px] text-[#C2FF00]">
          {{ t('creation.sceneEditDialog.referenceImages') }}
        </div>
        <div class="flex flex-wrap gap-3">
          <label
            v-if="sceneSubjects.length < MAX_SCENE_SUBJECTS"
            class="flex h-[50px] w-[50px] items-center justify-center rounded-[10px] border border-[#C2FF00] bg-[linear-gradient(299deg,rgba(190,250,0,0.2)_0%,rgba(130,255,121,0.2)_100%)]"
            :class="uploadingRefImage ? 'pointer-events-none opacity-70' : ''"
          >
            <input
              type="file"
              accept="image/jpeg,image/png,.jpg,.jpeg,.png"
              class="hidden"
              :disabled="uploadingRefImage"
              @change="handleSceneRefUpload"
            />
            <span v-if="uploadingRefImage" class="text-[12px] text-white">{{ t('creation.sceneEditDialog.uploading') }}</span>
            <svg-icon v-else name="gy-upload" size="14" color="#C2FF00"></svg-icon>
          </label>

          <div
            v-for="(img, idx) in sceneSubjects"
            :key="img.imageFileId || `${img.imageUrl}-${idx}`"
            class="group relative h-[50px] w-[50px]"
          >
            <el-image
              :src="img.imageUrl"
              :preview-src-list="sceneSubjects.map((item) => item.imageUrl)"
              :hide-on-click-modal="true"
              :initial-index="idx"
              preview-teleported
              fit="cover"
              class="h-full w-full rounded-[10px]"
              @show="handleSceneImagePreviewShow"
              @close="handleSceneImagePreviewClose"
            />
            <button
              type="button"
              class="absolute right-[-6px] top-[-6px]"
              @click="removeSceneSubject(idx)"
            >
              <svg-icon name="gy-closure" size="14" color="#FFFFFF"></svg-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 提示词区：编辑 visualPrompt 并支持一键复制 -->
      <div class="mt-3">
        <div class="mb-[5px] flex items-center justify-between">
          <div class="text-[14px] font-medium leading-[20px] text-[#C2FF00]">
            {{ t('creation.sceneEditDialog.promptLabel') }}
          </div>
          <button type="button" class="h-[17px] w-[17px] flex-center" @click="handleCopyPrompt">
            <svg-icon name="gy-copy" size="17" color="#C2FF00"></svg-icon>
          </button>
        </div>
        <div
          class="mobile-scene-prompt-input bg-[#F0F6DD]/30 rounded-[10px] border border-white/30 px-[10px] py-[8px]"
          @click.stop="focusPromptInput"
          @touchstart.stop="focusPromptInput"
          @touchend.stop="focusPromptInput"
        >
          <el-mention
            ref="promptMentionRef"
            v-model="promptRaw"
            type="textarea"
            prefix="@"
            :options="sceneMentionOptions"
            :autosize="{minRows: 4, maxRows: 10}"
            popper-class="scene-mention-popper"
            :placeholder="t('creation.sceneEditDialog.promptPlaceholder')"
            :disabled="editing"
          />
        </div>
      </div>

      <!-- 模型选择：复用 PC 端模型候选，控制分镜生成模型 -->
      <div class="mt-3">
        <el-popover
          v-model:visible="modelPopoverVisible"
          placement="top-start"
          trigger="click"
          width="280"
          popper-class="mobile-scene-model-popover"
          :show-arrow="false"
          :offset="8"
        >
          <template #reference>
            <button type="button" class="mobile-scene-model-trigger">
              <span class="mobile-scene-model-trigger__label">{{ t('home.model') }}：</span>
              <span class="mobile-scene-model-trigger__value">{{ getModelLabel(sceneModel) }}</span>
              <el-icon class="mobile-scene-model-trigger__arrow"><ArrowDown /></el-icon>
            </button>
          </template>
          <div class="mobile-scene-model-popover__title">{{ t('home.model') }}</div>
          <button
            v-for="item in modelStore.modelOptions"
            :key="item.value"
            type="button"
            class="mobile-scene-model-popover__item"
            :class="{'is-selected': item.value === sceneModel}"
            @click="handleSelectModel(item.value)"
          >
            <span>{{ item.label }}</span>
            <span class="text-white/65">{{ item.costText }}</span>
          </button>
        </el-popover>
      </div>
    </div>

    <!-- 底部操作区：取消或生成下一个分镜版本 -->
    <div class="shrink-0 px-[22px] pb-[18px] pt-[10px]">
      <div class="flex items-center gap-[15px]">
        <button
          type="button"
          class="h-[40px] flex-1 rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-[15px] text-[#192100]"
          @click="handleClose"
        >
          {{ t('common.back') }}
        </button>
        <button
          type="button"
          class="h-[40px] flex-[1.45] rounded-[10px] bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] px-[12px] text-[15px] text-[#192100] disabled:opacity-55"
          :disabled="editing || uploadingRefImage"
          @click="handleGenerateSceneVersion"
        >
          <span v-if="editing">{{ t('creation.sceneEditDialog.generating') }}</span>
          <span v-else>{{ t('creation.sceneEditDialog.generateVersion', {version: nextSceneVersionText}) }} | {{ pointsText }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue';
import {ArrowDown} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import {directEditSceneSubmit, getPointsPrice, uploadFile} from '@/api/creation';
import {useI18nText} from '@/i18n';
import {useModelStore} from '@/store/model';
import {formatDuration} from '@/utils/index.js';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';
import {upsertSceneItem} from '@/views/creation/utils/creationMessageChunks';
import {applyCreationVersionChange, formatCreationVersionLabel} from '@/views/creation/utils/creationVersionSwitch';
import {replaceMentionLabelsToValues, replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {useCreationDirectEditTask} from '@/views/creation/composables/useCreationDirectEditTask';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {useUserStore} from '@/store/user';
import {isNotEmpty} from '@/utils';

/**
 * 移动端分镜编辑页：
 * - 复用 PC 端 SCENE 直连编辑提交/轮询/回写链路
 * - 页面只展示横屏分镜预览（视频/封面）
 * - 支持版本切换、参考图维护、提示词编辑与生成
 */
const props = defineProps({
  /** 是否显示当前全屏编辑页。 */
  visible: {type: Boolean, default: false},
  /** 当前分镜所属消息对象。 */
  sourceMsg: {type: Object, default: null},
  /** 当前被编辑分镜对象。 */
  targetScene: {type: Object, default: null},
  /** 当前分镜在 sceneList 中的序号。 */
  sceneIndex: {type: Number, default: 0},
});

/** 通知父组件关闭当前编辑页。 */
const emit = defineEmits(['close', 'edit-context-change']);

const {t} = useI18nText();
const sceneEditStore = useCreationSubjectEditStore();
const userStore = useUserStore();

/** 本地编辑状态：目标对象、提示词、参考图、积分和任务态。 */
const editing = ref(false);
const editMessage = ref(null);
const editTargetScene = ref(null);
const promptRaw = ref('');
const promptMentionRef = ref(null);
const sceneSubjects = ref([]);
const uploadingRefImage = ref(false);
const scenePointsPrice = ref(null);
const MAX_SCENE_SUBJECTS = 6;
const modelStore = useModelStore();
// 场景模型选项跟随会话分辨率（已有会话锁定其分辨率）
const defaultSceneModel = computed(() => modelStore.modelOptions[0]?.value || 'VIDUQ2');
const sceneModel = ref(defaultSceneModel.value);
const modelPopoverVisible = ref(false);
const sceneVideoRef = ref(null);
const sceneVideoDuration = ref(0);
const sceneVideoCurrentTime = ref(0);
const sceneVideoPlaying = ref(false);
const sceneControlsVisible = ref(true);
const scenePreviewingImage = ref(false);
const scenePreviewResumeState = ref({
  shouldResumePlay: false,
  currentTime: 0,
});
let sceneControlsTimer = null;
const AUTO_HIDE_SCENE_CONTROLS_MS = 2000;

/**
 * 计算当前分镜时长。SCENE 字段契约：startTime/endTime/duration 后端皆 number。
 * @param {any} scene
 * @returns {number}
 */
const resolveSceneDurationSeconds = (scene) => {
  if (!scene) return 0;
  const {startTime, endTime, duration} = scene;
  if (startTime != null && endTime != null && endTime > startTime) return endTime - startTime;
  return duration || 0;
};

/**
 * 获取分镜当前激活版本。
 * SCENE 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} scene
 * @returns {any|null}
 */
const getActiveSceneVersion = (scene) => {
  const versions = scene?.versions || [];
  if (!versions.length) return null;
  return versions.find((item) => item?.version === scene?.activeVersion) || versions[0];
};

/** 页面展示与提交所依赖的派生状态。 */
const activeVersion = computed(() => getActiveSceneVersion(editTargetScene.value));
const versionList = computed(() => editTargetScene.value?.versions || []);
const displaySceneIndex = computed(() => (props.sceneIndex || 0) + 1);
const sceneDuration = computed(() => Math.max(0, Math.round(resolveSceneDurationSeconds(editTargetScene.value))));
const sceneProgressPercent = computed(() => {
  // sceneVideoDuration / sceneVideoCurrentTime 来自 HTMLMediaElement，元数据未加载时可能 NaN，|| 0 兜底
  const duration = sceneVideoDuration.value || 0;
  if (duration <= 0) return 0;
  return Math.max(0, Math.min(100, ((sceneVideoCurrentTime.value || 0) / duration) * 100));
});
const shouldShowSceneControls = computed(() => sceneControlsVisible.value);

const nextSceneVersionLabel = computed(() => {
  const versions = versionList.value;
  if (!versions.length) return 1;
  // versions[].version 后端 number
  return versions.reduce((max, item) => Math.max(max, item.version), 0) + 1;
});
const nextSceneVersionText = computed(() => formatCreationVersionLabel(nextSceneVersionLabel.value));
const pointsText = computed(() => (
  scenePointsPrice.value != null
    ? t('creation.sceneEditDialog.pointsAmount', {points: scenePointsPrice.value})
    : t('creation.sceneEditDialog.pointsUnknown')
));

/** 生成前积分是否不足。scenePointsPrice / pointsBalance 后端皆 number。 */
const isScenePointsInsufficient = computed(() => {
  const estimated = scenePointsPrice.value;
  const balance = userStore.pointsBalance;
  if (estimated == null || estimated <= 0) return false;
  if (balance == null || balance < 0) return false;
  return estimated > balance;
});

/** 关闭编辑页时清空本地状态，避免残留上次数据。 */
const resetLocalState = () => {
  editing.value = false;
  editMessage.value = null;
  editTargetScene.value = null;
  promptRaw.value = '';
  sceneSubjects.value = [];
  uploadingRefImage.value = false;
  scenePointsPrice.value = null;
  sceneModel.value = defaultSceneModel.value;
  modelPopoverVisible.value = false;
  sceneVideoRef.value = null;
  sceneVideoDuration.value = 0;
  sceneVideoCurrentTime.value = 0;
  sceneVideoPlaying.value = false;
  sceneControlsVisible.value = true;
  scenePreviewingImage.value = false;
  scenePreviewResumeState.value = {shouldResumePlay: false, currentTime: 0};
  clearSceneControlsTimer();
};

const setSceneVideoRef = (el) => {
  sceneVideoRef.value = el || null;
};

/**
 * 当前分镜提示词 @ 提及映射源。
 * SCENE versions[].subjects[].id 后端 string；这里把 id 也 alias 成 subjectId 以便 unwrap 时映射回 @id。
 * @param {Array<any>} subjects
 * @returns {Array<any>}
 */
const getMentionSubjects = (subjects = sceneSubjects.value) => {
  return (subjects || []).map((item) => ({
    subjectId: item.id,
    id: item.id,
    imageFileId: item.imageFileId,
  }));
};

/**
 * 后端存储值（@subject_X） -> 前端展示值（@图片N）。
 * @param {string} prompt 后端 visualPrompt 为 string
 * @param {Array<any>} subjects
 * @returns {string}
 */
const wrapPromptForDisplay = (prompt, subjects = sceneSubjects.value) => {
  return replaceMentionValuesToLabels(prompt || '', getMentionSubjects(subjects), {
    idFields: ['id'],
    fallbackPattern: null,
  });
};

/**
 * 前端展示值（@图片N） -> 后端存储值（@subject_X）。
 * @param {string} prompt
 * @param {Array<any>} subjects
 * @returns {string}
 */
const unwrapPromptForSubmit = (prompt, subjects = sceneSubjects.value) => {
  return (replaceMentionLabelsToValues(prompt || '', getMentionSubjects(subjects), {valueField: 'subjectId'}) || '').trim();
};

const sceneMentionOptions = computed(() => {
  return getMentionSubjects(sceneSubjects.value).map((_, idx) => ({
    label: `图片${idx + 1}`,
    value: `图片${idx + 1}`,
  }));
});

const focusPromptInput = async () => {
  if (editing.value) return;
  const root = promptMentionRef.value?.$el || promptMentionRef.value;
  const textarea = root?.querySelector?.('textarea');
  if (!textarea) return;

  // 移动端某些 WebView 对异步 focus 会拒绝弹键盘：先同步尝试，再做一次 nextTick 兜底。
  textarea.focus({preventScroll: true});

  await nextTick();
  textarea.focus({preventScroll: true});
  setTimeout(() => textarea.focus({preventScroll: true}), 0);
};

/**
 * 与 PC 保持一致：只要模型值非空就沿用，否则回退默认模型。
 * @param {string} value
 * @returns {string}
 */
const resolveSceneModel = (value) => {
  return isNotEmpty(value) ? value : defaultSceneModel.value;
};

/**
 * 模型优先级：当前仅使用 activeVersion.model，与 PC 一致。
 * @param {any} version
 * @returns {string}
 */
const resolveSceneModelByPriority = (version) => {
  return resolveSceneModel(version?.model);
};

/**
 * 获取模型显示文案。
 * @param {string} modelValue
 * @returns {string}
 */
const getModelLabel = (modelValue) => {
  const matched = modelStore.modelOptions.find((item) => item.value === modelValue);
  return matched?.label || modelStore.modelOptions[0]?.label || defaultSceneModel.value;
};

/**
 * 切换分镜模型并关闭弹层。
 * @param {string} modelValue
 * @returns {void}
 */
const handleSelectModel = (modelValue) => {
  sceneModel.value = modelValue;
  modelPopoverVisible.value = false;
};

const clearSceneControlsTimer = () => {
  if (sceneControlsTimer) {
    clearTimeout(sceneControlsTimer);
    sceneControlsTimer = null;
  }
};

const scheduleSceneControlsAutoHide = () => {
  clearSceneControlsTimer();
  sceneControlsTimer = setTimeout(() => {
    sceneControlsVisible.value = false;
    sceneControlsTimer = null;
  }, AUTO_HIDE_SCENE_CONTROLS_MS);
};

const handleSceneVideoLoadedMetadata = (event) => {
  // HTMLMediaElement.duration 元数据未就绪时可能 NaN，|| 0 兜底
  sceneVideoDuration.value = event?.target?.duration || 0;
  // 初始暂停态也遵循“2 秒显示后自动隐藏控件”的规则
  sceneControlsVisible.value = true;
  scheduleSceneControlsAutoHide();
};

const handleSceneVideoTimeUpdate = (event) => {
  // HTMLMediaElement.currentTime 是 number；|| 0 兜底极端 NaN
  sceneVideoCurrentTime.value = event?.target?.currentTime || 0;
};

const handleSceneVideoPlay = () => {
  sceneVideoPlaying.value = true;
  sceneControlsVisible.value = true;
  scheduleSceneControlsAutoHide();
};

const handleSceneVideoPause = () => {
  sceneVideoPlaying.value = false;
  sceneControlsVisible.value = true;
  scheduleSceneControlsAutoHide();
};

const handleSceneVideoEnded = () => {
  sceneVideoPlaying.value = false;
  sceneVideoCurrentTime.value = 0;
  sceneControlsVisible.value = true;
  scheduleSceneControlsAutoHide();
};

const toggleSceneVideoPlay = async () => {
  const el = sceneVideoRef.value;
  if (!el) return;
  if (el.paused) {
    try {
      await el.play();
    } catch {
      ElMessage.warning(t('creation.sceneEditDialog.noVideo'));
    }
    return;
  }
  el.pause();
};

const handleSceneVideoSurfaceTap = () => {
  sceneControlsVisible.value = true;
  scheduleSceneControlsAutoHide();
};

const handleSceneSeek = (event) => {
  const el = sceneVideoRef.value;
  const duration = sceneVideoDuration.value || 0;
  if (!el || duration <= 0) return;
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = (event.clientX - rect.left) / rect.width;
  el.currentTime = Math.max(0, Math.min(duration, duration * Math.max(0, Math.min(1, ratio))));
};

const handleSceneFullscreen = async () => {
  const el = sceneVideoRef.value;
  if (!el) return;
  if (typeof el.requestFullscreen === 'function') {
    await el.requestFullscreen();
    return;
  }
  if (typeof el.webkitEnterFullscreen === 'function') {
    el.webkitEnterFullscreen();
  }
};

/**
 * 打开参考图预览时挂起视频：
 * 1) 记录当前进度与播放状态
 * 2) 暂停视频并卸载 video 节点
 *
 * 说明：部分移动端浏览器会把视频提升到系统播放器层，z-index 无法覆盖，
 * 唯一稳定方案是在预览期间移除 video 元素。
 */
const handleSceneImagePreviewShow = () => {
  const videoEl = sceneVideoRef.value;
  scenePreviewResumeState.value = {
    shouldResumePlay: Boolean(videoEl && !videoEl.paused),
    currentTime: videoEl?.currentTime || 0,
  };
  if (videoEl) videoEl.pause();
  scenePreviewingImage.value = true;
};

/**
 * 关闭参考图预览后恢复视频状态：
 * - 恢复到预览前时间点
 * - 如果预览前是播放态，则自动继续播放
 */
const handleSceneImagePreviewClose = async () => {
  scenePreviewingImage.value = false;
  await nextTick();
  const videoEl = sceneVideoRef.value;
  if (!videoEl) {
    scenePreviewResumeState.value = {shouldResumePlay: false, currentTime: 0};
    return;
  }
  const {currentTime, shouldResumePlay} = scenePreviewResumeState.value;
  if (currentTime > 0) {
    try {
      videoEl.currentTime = currentTime;
    } catch (_) {}
  }
  if (shouldResumePlay) {
    try {
      await videoEl.play();
    } catch (_) {}
  }
  scenePreviewResumeState.value = {shouldResumePlay: false, currentTime: 0};
};

/**
 * 从当前分镜激活版本回填表单。
 * @param {any} scene
 * @returns {void}
 */
const syncFormFromScene = (scene) => {
  const active = getActiveSceneVersion(scene);
  sceneModel.value = resolveSceneModelByPriority(active);
  // SCENE versions[].subjects 后端契约 array；subjects[].id / imageFileId / imageUrl 都是 string
  const nextSubjects = (active?.subjects || []).map((item) => ({
    id: item.id,
    imageFileId: item.imageFileId,
    imageUrl: item.imageUrl,
  }));
  sceneSubjects.value = nextSubjects;
  promptRaw.value = wrapPromptForDisplay(active?.visualPrompt ?? scene?.description ?? '', nextSubjects);
};

/** 打开编辑页时根据父层入参同步初始化上下文。 */
const initFromPropsSync = () => {
  if (!props.sourceMsg || !props.targetScene) return;
  editMessage.value = props.sourceMsg;
  editTargetScene.value = props.targetScene;
  syncFormFromScene(props.targetScene);
};

/**
 * 当外部切换到新的分镜目标时，清理上一次编辑态并重新回填表单。
 * @returns {void}
 */
const syncEditorTargetFromProps = () => {
  if (!props.visible) return;
  editing.value = false;
  initFromPropsSync();
};

/**
 * 按模型与时长拉取分镜生成积分预估。
 * @returns {Promise<void>}
 */
const queryScenePointsPrice = async () => {
  if (!props.visible) return;
  if (!sceneModel.value || sceneDuration.value <= 0) {
    scenePointsPrice.value = null;
    return;
  }
  try {
    const data = await getPointsPrice({
      modelName: sceneModel.value,
      taskType: 'MAKE_MV',
      duration: sceneDuration.value,
      // 始终带上会话生效分辨率，计价口径与生成保持一致
      resolution: modelStore.effectiveResolution,
    });
    scenePointsPrice.value = data;
  } catch (_) {
    scenePointsPrice.value = null;
  }
};

/** sessionId 后端 string */
const getCurrentSessionId = () => editMessage.value?.sessionId || '';
/** chunkId(=messageChunkId) 后端 string，禁止私自转 number */
const getCurrentChunkId = () => editTargetScene.value?.messageChunkId || '';

/**
 * 复用通用 direct-edit 任务能力：
 * - 支持重新进入编辑页时恢复任务状态
 * - 任务完成后回写最新 scene 版本
 */
const {applyTaskResult, resumeTaskIfNeeded} = useCreationDirectEditTask({
  store: sceneEditStore,
  getSessionId: getCurrentSessionId,
  getChunkId: getCurrentChunkId,
  getSourceMsg: () => editMessage.value,
  setEditing: (value) => {
    editing.value = value;
  },
  taskType: 'SCENE',
  expectedType: 'SCENE',
  successMessage: t('creation.sceneEditDialog.newVersionGenerated'),
  upsertItem: upsertSceneItem,
  listKey: 'sceneList',
  emitVersionChange: (payload) => emit('edit-context-change', payload),
  onSuccess: (resultData, sourceMsg, {messageChunkId: submittedChunkId, updatedItem} = {}) => {
    // upsert 与版本变化记录已由 composable 统一处理，这里只负责组件特有副作用：
    // 面板仍打开且就在编辑当前 chunk 时同步刷新预览目标 + 表单。
    if (!submittedChunkId || !updatedItem) return;
    if (editTargetScene.value?.messageChunkId === submittedChunkId) {
      editTargetScene.value = updatedItem;
      syncFormFromScene(updatedItem);
    }
  },
  onError: (message) => ElMessage.error(message),
  onNotifySuccess: (message) => ElMessage.success(message),
});

/** 编辑页打开时初始化并恢复任务，关闭时重置状态。 */
watch(
  () => props.visible,
  async (open) => {
    if (open) {
      initFromPropsSync();
      await Promise.allSettled([
        queryScenePointsPrice(),
        resumeTaskIfNeeded(),
      ]);
    } else {
      resetLocalState();
    }
  },
  {immediate: true},
);

/**
 * 编辑中如果外部切换了目标分镜，清理旧草稿并重新回填。
 * @returns {void}
 */
watch(
  () => [props.visible, props.sourceMsg?.messageId, props.targetScene?.messageChunkId],
  ([open]) => {
    if (!open) return;
    initFromPropsSync();
  },
);

/** 影响积分预估的参数变化时，重新计算预估积分。 */
watch(
  () => [props.visible, editTargetScene.value?.sceneId, sceneModel.value, sceneDuration.value],
  async ([open]) => {
    if (!open) return;
    await queryScenePointsPrice();
  },
);

/**
 * 与 PC 一致：切换 activeVersion 时同步模型，保证积分预估基于当前版本模型。
 */
watch(
  () => [props.visible, activeVersion.value?.version, activeVersion.value?.model],
  ([open]) => {
    if (!open) return;
    sceneModel.value = resolveSceneModelByPriority(activeVersion.value);
  },
);

watch(
  () => activeVersion.value?.videoUrl,
  () => {
    sceneVideoCurrentTime.value = 0;
    sceneVideoDuration.value = 0;
    sceneVideoPlaying.value = false;
    sceneControlsVisible.value = true;
    scenePreviewingImage.value = false;
    scenePreviewResumeState.value = {shouldResumePlay: false, currentTime: 0};
    clearSceneControlsTimer();
  },
);

onBeforeUnmount(() => {
  clearSceneControlsTimer();
});

/** 关闭当前全屏编辑页。 */
const handleClose = () => emit('close');

/**
 * 切换分镜版本并同步本地表单内容。
 * @param {number|string} version
 * @returns {void}
 */
const handleSceneVersionChange = (version) => {
  applyCreationVersionChange({
    version,
    type: 'SCENE',
    target: editTargetScene.value,
    message: editMessage.value,
    emitEditContextChange: (payload) => emit('edit-context-change', payload),
  });
  syncFormFromScene(editTargetScene.value);
};

/** 删除一张参考图。 */
const removeSceneSubject = (idx) => {
  sceneSubjects.value.splice(idx, 1);
};

/**
 * 上传参考图并追加到分镜 subjects 列表。
 * @param {Event} event
 * @returns {Promise<void>}
 */
const handleSceneRefUpload = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  if (uploadingRefImage.value) return;
  if (!isSupportedImageUpload(file)) {
    ElMessage.warning(t('creation.sceneEditDialog.onlyJpgPng'));
    return;
  }
  if (file.size / 1024 / 1024 >= 10) {
    ElMessage.warning(t('creation.sceneEditDialog.imageMaxSize'));
    return;
  }

  try {
    uploadingRefImage.value = true;
    const compressedFile = await compressImageBeforeUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile, compressedFile.name);
    const result = await uploadFile(formData);
    sceneSubjects.value.push({
      id: `subject_${sceneSubjects.value.length + 1}`,
      imageFileId: result.fileId,
      imageUrl: result.fileUrl || URL.createObjectURL(compressedFile),
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.sceneEditDialog.uploadFailed'));
  } finally {
    uploadingRefImage.value = false;
  }
};

/**
 * 复制当前提示词，便于外部复用。
 * @returns {Promise<void>}
 */
const handleCopyPrompt = async () => {
  const text = (promptRaw.value || '').trim();
  if (!text) {
    ElMessage.warning(t('creation.sceneEditDialog.enterPrompt'));
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(t('creation.messageArea.copiedWithLabel', {label: t('creation.sceneEditDialog.promptLabel')}));
  } catch {
    ElMessage.error(t('layout.copyFail'));
  }
};

/**
 * 将当前草稿乐观回写到消息中的对应 scene。
 * sceneList/versions 后端契约 array；chunkId/version 直接 ===。
 * @param {any} sourceMsg
 * @param {string} chunkId 后端 messageChunkId
 * @param {string} visualPrompt
 * @returns {void}
 */
const syncSceneDraftToMessage = (sourceMsg, chunkId, visualPrompt) => {
  const target = (sourceMsg?.sceneList || []).find((item) => item?.messageChunkId === chunkId);
  if (!target) return;

  if (!target.versions) target.versions = [];
  const activeVersionNo = target.activeVersion;
  let active = target.versions.find((item) => item?.version === activeVersionNo);
  if (!active) active = target.versions[0];
  if (!active) {
    active = {version: activeVersionNo > 0 ? activeVersionNo : 1};
    target.versions.push(active);
  }

  active.visualPrompt = visualPrompt;
  active.model = sceneModel.value;
  active.subjects = sceneSubjects.value.map((item) => ({
    id: item.id,
    imageFileId: item.imageFileId,
    imageUrl: item.imageUrl,
  }));
  target.description = visualPrompt;

  if (editTargetScene.value?.messageChunkId === chunkId) {
    editTargetScene.value = target;
  }
};

/**
 * 提交分镜新版本生成：
 * - 校验输入
 * - 乐观回写当前草稿
 * - 调用接口并等待任务结束后回写结果
 * @returns {Promise<void>}
 */
const handleGenerateSceneVersion = async () => {
  const targetScene = editTargetScene.value;
  const sourceMsg = editMessage.value;
  if (!targetScene || !sourceMsg) return;
  const promptDisplay = (promptRaw.value || '').trim();
  if (!promptDisplay) {
    ElMessage.warning(t('creation.sceneEditDialog.enterPrompt'));
    return;
  }
  const visualPrompt = unwrapPromptForSubmit(promptDisplay);
  if (isScenePointsInsufficient.value) {
    ElMessage.warning(t('creation.sceneEditDialog.pointsInsufficient'));
    return;
  }

  const chunkId = targetScene.messageChunkId;
  if (!isNotEmpty(chunkId)) {
    ElMessage.warning(t('creation.sceneEditDialog.missingChunkId'));
    return;
  }
  if (!isNotEmpty(sourceMsg.sessionId)) {
    ElMessage.warning(t('creation.sceneEditDialog.missingSessionInfo'));
    return;
  }

  const submitSessionId = sourceMsg.sessionId;
  syncSceneDraftToMessage(sourceMsg, chunkId, visualPrompt);

  try {
    editing.value = true;
    const taskId = await directEditSceneSubmit({
      sessionId: submitSessionId,
      chunkId,
      model: sceneModel.value,
      visualPrompt,
      subject: sceneSubjects.value.map((item) => ({
        subjectId: item.id,
        fileId: item.imageFileId,
        fileUrl: item.imageUrl,
      })),
    });
    if (!taskId) throw new Error(t('creation.taskSubmitFail'));
    sceneEditStore.startTask({
      sessionId: submitSessionId,
      chunkId,
      taskId,
      taskType: 'SCENE',
      previousVersion: editTargetScene.value?.activeVersion,
      messageId: sourceMsg.messageId,
    });
    const finalTask = await sceneEditStore.waitForTask(submitSessionId, chunkId, {}, 'SCENE');
    await applyTaskResult(finalTask, {
      sessionId: submitSessionId,
      messageChunkId: chunkId,
      sourceMsg,
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.sceneEditDialog.generateFailed'));
  } finally {
    editing.value = false;
  }
};
</script>

<style lang="scss" scoped>
.mobile-scene-model-trigger {
  width: 100%;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(194, 255, 0, 0.28);
  background: rgba(0, 0, 0, 0.22);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  font-size: 12px;
  line-height: 16px;
  color: #ffffff;
}

.mobile-scene-model-trigger__label {
  color: rgba(255, 255, 255, 0.7);
}

.mobile-scene-model-trigger__value {
  flex: 1;
  min-width: 0;
  color: #c2ff00;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-scene-model-trigger__arrow {
  color: rgba(255, 255, 255, 0.8);
}

:global(.mobile-scene-model-popover) {
  padding: 8px !important;
  border: 1px solid rgba(194, 255, 0, 0.3) !important;
  border-radius: 10px !important;
  background: #101a0d !important;
}

.mobile-scene-model-popover__title {
  margin-bottom: 6px;
  padding: 0 2px;
  font-size: 12px;
  line-height: 16px;
  color: #c2ff00;
  font-weight: 600;
}

.mobile-scene-model-popover__item {
  width: 100%;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 8px;
  font-size: 12px;
  line-height: 16px;
}

.mobile-scene-model-popover__item.is-selected {
  background: linear-gradient(299deg, rgba(190, 250, 0, 0.35) 0%, rgba(130, 255, 121, 0.3) 100%);
}
</style>

<style lang="scss">
// 复用 PC 端版本下拉的视觉规范，保持双端一致
@use '@/views/creation/styles/creationVersionSelect.scss';

.mobile-scene-prompt-input {
  textarea {
    min-height: 0 !important;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    outline: none !important;
    border-radius: 0 !important;
    padding: 0 !important;
    color: #ffffff !important;
    font-size: 12px !important;
    line-height: 17px !important;
    font-weight: 400 !important;
    resize: none !important;

    &::placeholder {
      color: rgba(255, 255, 255, 0.5) !important;
    }

    &:focus {
      border: none !important;
      box-shadow: none !important;
      outline: none !important;
    }
  }
}

.scene-mention-popper {
  &.el-mention__popper.el-popper {
    background: #0f140b !important;
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    border-radius: 10px !important;
    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.38) !important;
    overflow: hidden !important;
    padding: 0 !important;

    .el-popper__arrow {
      display: none !important;
    }
  }

  .el-mention-dropdown {
    --el-mention-bg-color: #0f140b;
    --el-mention-border: none;
    --el-mention-shadow: none;
    --el-mention-option-color: rgba(255, 255, 255, 0.9);
    --el-mention-option-hover-background: rgba(194, 255, 0, 0.12);
    --el-mention-option-selected-color: #c2ff00;
    --el-mention-font-size: 12px;
    --el-mention-option-height: 30px;
    --el-mention-max-height: none;
    --el-mention-padding: 6px 0;
  }

  .el-mention-dropdown__item {
    padding: 0 10px !important;
  }
}
</style>
