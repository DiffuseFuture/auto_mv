<template>
  <el-dialog
      v-model="visible"
      width="1024px"
      append-to-body
      align-center
      :show-close="false"
      :modal-class="'scene-edit-modal'"
      class="scene-direct-edit-dialog"
  >
    <div class="scene-direct-edit-shell relative overflow-hidden rounded-[22px] border border-white/10 bg-[#070707]">
      <div class="flex h-[620px]">
        <div class="flex-1 p-6">
          <div class="relative h-full w-full overflow-hidden rounded-[20px] bg-[#0f0f0f] shadow-[0_20px_60px_rgba(0,0,0,0.55)]">
            <div class="absolute left-3 top-3 z-[3]">
              <el-select
                  size="small"
                  :title="formatCreationVersionLabel(editTargetScene?.activeVersion)"
                  class="dialog-version-select"
                  popper-class="dialog-version-select-popper"
                  :show-arrow="false"
                  :model-value="editTargetScene?.activeVersion"
                  @update:model-value="handleSceneVersionChange"
              >
                <el-option
                    v-for="v in (editTargetScene?.versions || [])"
                    :key="v.version"
                    :label="formatCreationVersionLabel(v.version)"
                    :value="v.version"
                />
              </el-select>
            </div>

            <video
                v-if="activeVersion?.videoUrl"
                :src="activeVersion.videoUrl"
                :poster="activeVersion.coverUrl"
                controls
                class="h-full w-full object-contain bg-black"
            ></video>
            <div v-else class="h-full w-full flex-center text-white/60">{{ t('creation.sceneEditDialog.noVideo') }}</div>
          </div>
        </div>

        <div class="w-[380px] shrink-0 border-l border-white/10 bg-[#0b0b0b] flex flex-col">
          <div class="shrink-0 flex items-center justify-between px-6 pt-6 pb-4">
            <div class="text-white text-[20px] leading-[28px] font-semibold">{{ t('creation.sceneEditDialog.title') }}</div>
            <button
                class="rounded-full cursor-pointer"
                aria-label="close"
                @click="handleClose"
            >
              <svg-icon name="gy-closure" :size="26" color="#FFFFFF"></svg-icon>
            </button>
          </div>

          <div class="flex-1 min-h-0 overflow-y-auto custom-scrollbar px-6 pb-6">
            <div class="mb-6">
              <div class="mb-3 text-[13px] leading-5 text-white/60">{{ t('creation.sceneEditDialog.referenceImages') }}</div>
              <div class="min-h-[60px] flex flex-wrap items-start content-start gap-3">
                <label
                    v-if="sceneSubjects.length < MAX_SCENE_SUBJECTS"
                    :class="[
                      'relative inline-flex h-[60px] w-[60px] items-center justify-center rounded-[14px] border border-dashed border-white/20 bg-white/[0.03] text-[22px] text-white/80 transition-colors',
                      uploadingRefImage ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:bg-white/[0.05]'
                    ]"
                    :title="uploadingRefImage ? t('creation.sceneEditDialog.uploading') : t('creation.sceneEditDialog.uploadReferenceImage')"
                >
                  <input
                      type="file"
                      accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                      class="hidden"
                      :disabled="uploadingRefImage"
                      @change="handleSceneRefUpload"
                  />
                  <template v-if="uploadingRefImage">
                    <i class="el-icon is-loading text-[18px]"><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 64a32 32 0 0 1 32 32v96a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32zm0 672a32 32 0 0 1 32 32v160a32 32 0 0 1-64 0V768a32 32 0 0 1 32-32zM192 480a32 32 0 0 1 0 64H96a32 32 0 0 1 0-64h96zm736 0a32 32 0 0 1 0 64h-160a32 32 0 0 1 0-64h160zM237.255 237.255a32 32 0 0 1 45.255 0l67.882 67.882a32 32 0 0 1-45.255 45.255l-67.882-67.882a32 32 0 0 1 0-45.255zm481.608 481.608a32 32 0 0 1 45.255 0l113.137 113.137a32 32 0 0 1-45.255 45.255L718.863 764.118a32 32 0 0 1 0-45.255zM237.255 786.745a32 32 0 0 1 45.255 45.255L169.373 945.137a32 32 0 0 1-45.255-45.255l113.137-113.137zm594.745-549.49a32 32 0 0 1 45.255 45.255L764.118 395.647a32 32 0 0 1-45.255-45.255l113.137-113.137z"></path></svg></i>
                  </template>
                  <template v-else>+</template>
                </label>

                <div v-for="(img, idx) in sceneSubjects" :key="img.imageFileId" class="relative group">
                  <el-image
                      :src="img.imageUrl"
                      :preview-src-list="sceneSubjects.map(item => item.imageUrl)"
                      :hide-on-click-modal="true"
                      :initial-index="idx"
                      preview-teleported
                      fit="cover"
                      class="h-[60px] w-[60px] rounded-[14px] cursor-pointer"
                  />
                  <button
                      class="absolute -right-1.5 -top-1.5 h-[18px] w-[18px] rounded-full bg-black/70 text-[12px] text-white/90 opacity-0 group-hover:opacity-100 transition-opacity"
                      aria-label="remove"
                      @click="removeSceneSubject(idx)"
                  >×</button>
                </div>
              </div>
            </div>

            <div class="mb-2 text-[13px] leading-5 text-white/60">{{ t('creation.sceneEditDialog.promptLabel') }}</div>
            <el-mention
                v-model="promptDisplay"
                type="textarea"
                prefix="@"
                :options="mentionOptions"
                :autosize="false"
                :placeholder="t('creation.sceneEditDialog.promptPlaceholder')"
                class="scene-prompt-input"
                popper-class="scene-mention-popper"
            />

          </div>

          <div class="shrink-0 px-6 pb-6 pt-4">
            <el-popover
                v-model:visible="modelPopoverVisible"
                placement="top-start"
                trigger="click"
                width="354"
                popper-class="scene-model-select-dropdown"
                :show-arrow="false"
                :offset="10"
            >
              <template #reference>
                <button class="scene-model-trigger mb-3" type="button">
                  <span class="scene-model-trigger__label">模型：</span>
                  <span class="scene-model-trigger__value">{{ getModelLabel(sceneModel) }}</span>
                  <el-icon class="scene-model-trigger__arrow"><ArrowDown /></el-icon>
                </button>
              </template>
              <div class="el-select-dropdown__header">{{ t('home.model') }}</div>
              <div
                  v-for="item in modelOptions"
                  :key="item.value"
                  class="el-select-dropdown__item"
                  :class="{ 'is-selected': item.value === sceneModel }"
                  @click="handleSelectModel(item.value)"
              >
                <span class="scene-model-item-main">{{ item.label }}</span>
                <span class="scene-model-item-meta">{{ item.costText }}</span>
              </div>
            </el-popover>
            <div class="flex items-center justify-between gap-3">
              <button class="h-[44px] w-[110px] shrink-0 rounded-[14px] bg-white/10 hover:bg-white/15 text-white/80 transition-colors cursor-pointer" @click="handleClose">{{ t('common.cancel') }}</button>
              <button
                  class="h-[44px] flex-1 rounded-[14px] bg-[#c2ff00] hover:opacity-90 text-black font-semibold transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="editing || uploadingRefImage"
                  @click="handleGenerateSceneVersion"
              >
                <span v-if="editing">{{ t('creation.sceneEditDialog.generating') }}</span>
                <span v-else class="inline-flex items-center justify-center">
                  <span>{{ t('creation.sceneEditDialog.generateVersion', {version: nextSceneVersionText}) }}</span>
                  <el-divider direction="vertical" class="scene-generate-divider" />
                  <span>{{ scenePointsPrice != null ? t('creation.sceneEditDialog.pointsAmount', {points: scenePointsPrice}) : t('creation.sceneEditDialog.pointsUnknown') }}</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
/**
 * SCENE 直连编辑弹窗：预览与提示词来自当前 `activeVersion` 对应版本的 `videoUrl/coverUrl/visualPrompt/subjects`。
 * 生成新视频版本后 upsert `msg.sceneList`。若仅切换版本未编辑，以 `versions[].visualPrompt` 为真源。
 */
import {computed, ref, watch} from 'vue';
import {ArrowDown} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import {isNotEmpty} from '@/utils/index.js';
import {directEditSceneSubmit, getPointsPrice, uploadFile} from '@/api/creation';
import {replaceMentionLabelsToValues, replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {applyCreationVersionChange, formatCreationVersionLabel} from '@/views/creation/utils/creationVersionSwitch';
import {upsertSceneItem} from '@/views/creation/utils/creationMessageChunks';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {useCreationDirectEditTask} from '@/views/creation/composables/useCreationDirectEditTask';
import {useUserStore} from '@/store/user';
import {useModelStore} from '@/store/model';
import {useI18nText} from '@/i18n';

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  sourceMsg: {type: Object, default: null},
  targetScene: {type: Object, default: null},
});

const emit = defineEmits(['update:modelValue', 'edit-context-change']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const editing = ref(false);
const editMessage = ref(null);
const editTargetScene = ref(null);
const promptRaw = ref('');
const promptDisplay = ref('');
const sceneSubjects = ref([]);
const scenePointsPrice = ref(null);
const modelPopoverVisible = ref(false);
const modelStore = useModelStore();
// 场景模型选项跟随会话分辨率（已有会话锁定其分辨率）
const modelOptions = computed(() => modelStore.modelOptions);
const defaultSceneModel = computed(() => modelOptions.value[0].value);
const sceneModel = ref(defaultSceneModel.value);
const MAX_SCENE_SUBJECTS = 6;
const sceneEditStore = useCreationSubjectEditStore();
const userStore = useUserStore();
const uploadingRefImage = ref(false);
const {t} = useI18nText();

const mentionOptions = computed(() => sceneSubjects.value.map((img, idx) => ({
  label: t('creation.sceneEditDialog.imageLabel', {index: idx + 1}),
  value: t('creation.sceneEditDialog.imageLabel', {index: idx + 1}),
})));

/**
 * 解析分镜时长秒数。
 * SCENE 字段契约：startTime / endTime / duration 后端皆 number。
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
 * 取分镜当前激活版本。
 * SCENE 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} scene
 * @returns {any|null}
 */
const getActiveSceneVersion = (scene) => {
  const versions = scene?.versions || [];
  if (!versions.length) return null;
  return versions.find((version) => version.version === scene?.activeVersion) || versions[0];
};

const activeVersion = computed(() => getActiveSceneVersion(editTargetScene.value));

const nextSceneVersionLabel = computed(() => {
  const versions = editTargetScene.value?.versions || [];
  if (!versions.length) return 1;
  // versions[].version 后端 number
  return versions.reduce((max, item) => Math.max(max, item.version), 0) + 1;
});

const nextSceneVersionText = computed(() => formatCreationVersionLabel(nextSceneVersionLabel.value));
/** 分镜重生成是否积分不足。scenePointsPrice / pointsBalance 后端皆 number。 */
const isScenePointsInsufficient = computed(() => {
  const estimated = scenePointsPrice.value;
  const balance = userStore.pointsBalance;
  if (estimated == null || estimated <= 0) return false;
  if (balance == null || balance < 0) return false;
  return estimated > balance;
});

const sceneDuration = computed(() => {
  return Math.max(0, Math.round(resolveSceneDurationSeconds(editTargetScene.value)));
});

const resolveSceneModel = (modelValue) => {
  return isNotEmpty(modelValue) ? modelValue : defaultSceneModel.value;
};
const resolveSceneModelByPriority = (version) => {
  return resolveSceneModel(version?.model);
};

/** 关闭弹窗时重置本地状态。 */
const resetLocalState = () => {
  editing.value = false;
  editMessage.value = null;
  editTargetScene.value = null;
  promptRaw.value = '';
  promptDisplay.value = '';
  sceneSubjects.value = [];
  scenePointsPrice.value = null;
};

/**
 * 从当前分镜 `activeVersion` 填充提示词与参考图列表。
 * @returns {void}
 */
const initFromProps = () => {
  if (!props.sourceMsg || !props.targetScene) return;

  editMessage.value = props.sourceMsg;
  editTargetScene.value = props.targetScene;

  const active = getActiveSceneVersion(props.targetScene);
  sceneModel.value = resolveSceneModelByPriority(active);
  promptRaw.value = active?.visualPrompt ?? '';
  // SCENE versions[].subjects 后端契约 array；subjects[].id 后端 string（不是 subjectId）
  sceneSubjects.value = (active?.subjects || []).map((item) => ({
    id: item.id,
    imageFileId: item.imageFileId,
    imageUrl: item.imageUrl,
  }));
  promptDisplay.value = replaceMentionValuesToLabels(promptRaw.value, sceneSubjects.value, {
    idFields: ['id'],
  });
};

/**
 * 按当前模型与时长拉取积分预估（弹窗打开或参数变化时）。
 * @returns {Promise<void>}
 */
const queryScenePointsPrice = async () => {
  if (!visible.value) return;
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
  } catch (error) {
    scenePointsPrice.value = null;
  }
};

/** sessionId 后端契约 string */
const getCurrentSessionId = () => editMessage.value?.sessionId || '';

const getCurrentChunkId = () => editTargetScene.value?.messageChunkId || '';

const {applyTaskResult: applySceneEditTaskResult, resumeTaskIfNeeded: resumeSceneEditTask} = useCreationDirectEditTask({
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
    // 弹窗仍打开且就在编辑当前 chunk 时同步刷新预览目标。
    if (!submittedChunkId || !updatedItem) return;
    if (editTargetScene.value?.messageChunkId === submittedChunkId) {
      editTargetScene.value = updatedItem;
    }
  },
  onError: (message) => ElMessage.error(message),
  onNotifySuccess: (message) => ElMessage.success(message),
});

watch(
    () => props.modelValue,
    async (open) => {
      if (open) {
        initFromProps();
        // 弹窗打开时若该 chunk 已有进行中的任务，回显生成中状态并续上等待
        resumeSceneEditTask().catch(() => {});
      } else {
        resetLocalState();
      }
    },
    {immediate: true},
);

watch(
    () => [visible.value, editTargetScene.value?.sceneId, sceneModel.value, sceneDuration.value],
    async ([open]) => {
      if (!open) return;
      await queryScenePointsPrice();
    },
);

watch(
    () => [visible.value, activeVersion.value?.version, activeVersion.value?.model],
    ([open]) => {
      if (!open) return;
      sceneModel.value = resolveSceneModelByPriority(activeVersion.value);
    },
);

/** @returns {void} */
const handleClose = () => {
  visible.value = false;
};

/**
 * 弹窗内切换 SCENE 版本：行为与外部卡片一致（切换 activeVersion + 调用 switchVersion + 上抛 edit-context-change）。
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
};

const getModelLabel = (modelValue) => {
  const matched = modelOptions.value.find((item) => item.value === modelValue);
  return matched?.label || modelOptions.value[0].label;
};

const handleSelectModel = (modelValue) => {
  sceneModel.value = modelValue;
  modelPopoverVisible.value = false;
};

/**
 * 移除指定参考图并刷新 @ 提及展示。
 * @param {number} idx
 * @returns {void}
 */
const removeSceneSubject = (idx) => {
  sceneSubjects.value.splice(idx, 1);
  promptDisplay.value = replaceMentionValuesToLabels(promptDisplay.value, sceneSubjects.value, {
    idFields: ['id'],
  });
};

/**
 * 上传参考图并追加到分镜 subject 列表。
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
    promptDisplay.value = replaceMentionValuesToLabels(promptDisplay.value, sceneSubjects.value, {
      idFields: ['id'],
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.sceneEditDialog.uploadFailed'));
  } finally {
    uploadingRefImage.value = false;
  }
};

/**
 * 将当前编辑中的 visualPrompt / subject 立即回写到消息列表里的对应 scene（本地乐观更新）。
 * sceneList/versions 后端契约皆 array；chunkId/version 直接 ===。
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
  let active = target.versions.find((v) => v?.version === activeVersionNo);
  if (!active) active = target.versions[0];
  if (!active) {
    active = {version: activeVersionNo > 0 ? activeVersionNo : 1};
    target.versions.push(active);
  }

  active.visualPrompt = visualPrompt;
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
 * 提交直连编辑任务生成新分镜视频版本，并回写 `sceneList`。
 * @returns {Promise<void>}
 */
const handleGenerateSceneVersion = async () => {
  const targetScene = editTargetScene.value;
  const sourceMsg = editMessage.value;
  if (!targetScene || !sourceMsg) return;

  promptRaw.value = replaceMentionLabelsToValues(promptDisplay.value, sceneSubjects.value, {valueField: 'id'});
  const visualPrompt = (promptRaw.value || '').trim();
  if (!visualPrompt) {
    ElMessage.warning(t('creation.sceneEditDialog.enterPrompt'));
    return;
  }
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

  // 先写入本地草稿，确保下次打开展示的是当前编辑内容
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
    await applySceneEditTaskResult(finalTask, {
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

<style lang="scss">
@use '@/views/creation/styles/creationVersionSelect.scss';
@use '@/views/creation/styles/creationMention.scss';
@use '@/styles/modelSelectDropdown.scss' as *;

.scene-edit-modal {
  background: rgba(0, 0, 0, 0.68) !important;
  @apply backdrop-blur-[2px];
}

.scene-direct-edit-dialog {
  padding: 0;

  .el-dialog {
    margin-top: 6vh !important;
    background: transparent !important;
    box-shadow: none !important;
    border-radius: 22px !important;
    overflow: hidden !important;

    &__header {
      display: none;
    }

    &__body {
      padding: 0 !important;
      background: transparent !important;
    }
  }
}

.scene-direct-edit-dialog.el-dialog {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 22px !important;
  overflow: hidden !important;
}

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

.scene-generate-divider {
  margin: 0 8px !important;
  height: 16px !important;
  border-left: 2px solid rgba(0, 0, 0, 0.45) !important;
}

.scene-cost-estimate {
  height: 48px;
  border-radius: 12px;
  border: 1px solid rgba(146, 238, 33, 0.38);
  background: linear-gradient(90deg, rgba(148, 238, 33, 0.18) 0%, rgba(148, 238, 33, 0.06) 55%, rgba(148, 238, 33, 0.02) 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 0 12px;
}

.scene-cost-estimate__left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.scene-cost-estimate__icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid rgba(146, 238, 33, 0.5);
  background: rgba(146, 238, 33, 0.12);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scene-cost-estimate__text {
  color: #a6f34a;
  font-size: 15px;
  line-height: 20px;
  font-weight: 600;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.scene-cost-estimate__dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  background: #9af52f;
  box-shadow: 0 0 8px rgba(154, 245, 47, 0.75);
}
</style>
