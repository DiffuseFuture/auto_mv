<template>
  <el-dialog
      v-model="visible"
      width="1024px"
      append-to-body
      align-center
      :show-close="false"
      :modal-class="'subject-edit-modal'"
      class="subject-edit-dialog"
  >
    <div class="subject-edit-shell relative overflow-hidden rounded-[22px] border border-white/10 bg-[#070707]">
      <div class="flex h-[560px]">
        <!-- 左侧：预览区域 -->
        <div class="flex-1 p-6">
          <div class="relative h-full w-full overflow-hidden rounded-[20px] bg-[#0f0f0f] shadow-[0_20px_60px_rgba(0,0,0,0.55)]">
            <div class="absolute left-3 top-3 z-[3]">
              <el-select
                  size="small"
                  :title="formatCreationVersionLabel(editTargetSubject?.activeVersion)"
                  class="dialog-version-select"
                  popper-class="dialog-version-select-popper"
                  :show-arrow="false"
                  :model-value="editTargetSubject?.activeVersion"
                  @update:model-value="handleSubjectVersionChange"
              >
                <el-option
                    v-for="v in (editTargetSubject?.versions || [])"
                    :key="v.version"
                    :label="formatCreationVersionLabel(v.version)"
                    :value="v.version"
                />
              </el-select>
            </div>

            <el-image
                v-if="activeVersion?.imgUrl"
                :src="activeVersion.imgUrl"
                fit="contain"
                class="h-full w-full"
            />
            <div v-else class="h-full w-full flex-center text-white/60">{{ t('creation.subjectEditDialog.noImage') }}</div>
          </div>
        </div>

        <!-- 右侧：编辑面板 -->
        <div class="w-[380px] shrink-0 border-l border-white/10 bg-[#0b0b0b] flex flex-col">
          <div class="shrink-0 flex items-center justify-between px-6 pt-6 pb-4">
            <div class="text-white text-[20px] leading-[28px] font-semibold">{{ t('creation.subjectEditDialog.title') }}</div>
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
              <div class="mb-3 text-[13px] leading-5 text-white/60">{{ t('creation.subjectEditDialog.referenceImages') }}</div>
              <div class="min-h-[60px] flex flex-wrap items-start content-start gap-3">
                <label
                    v-if="refImages.length < MAX_REF_IMAGES"
                    :class="[
                      'relative inline-flex h-[60px] w-[60px] items-center justify-center rounded-[14px] border border-dashed border-white/20 bg-white/[0.03] text-[22px] text-white/80 transition-colors',
                      uploadingRefImage ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:bg-white/[0.05]'
                    ]"
                    :title="uploadingRefImage ? t('creation.subjectEditDialog.uploading') : t('creation.subjectEditDialog.uploadReferenceImage')"
                >
                  <input
                      type="file"
                      accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                      class="hidden"
                      :disabled="uploadingRefImage"
                      @change="handleSubjectRefUpload"
                  />
                  <template v-if="uploadingRefImage">
                    <i class="el-icon is-loading text-[18px]"><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 64a32 32 0 0 1 32 32v96a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32zm0 672a32 32 0 0 1 32 32v160a32 32 0 0 1-64 0V768a32 32 0 0 1 32-32zM192 480a32 32 0 0 1 0 64H96a32 32 0 0 1 0-64h96zm736 0a32 32 0 0 1 0 64h-160a32 32 0 0 1 0-64h160zM237.255 237.255a32 32 0 0 1 45.255 0l67.882 67.882a32 32 0 0 1-45.255 45.255l-67.882-67.882a32 32 0 0 1 0-45.255zm481.608 481.608a32 32 0 0 1 45.255 0l113.137 113.137a32 32 0 0 1-45.255 45.255L718.863 764.118a32 32 0 0 1 0-45.255zM237.255 786.745a32 32 0 0 1 45.255 45.255L169.373 945.137a32 32 0 0 1-45.255-45.255l113.137-113.137zm594.745-549.49a32 32 0 0 1 45.255 45.255L764.118 395.647a32 32 0 0 1-45.255-45.255l113.137-113.137z"></path></svg></i>
                  </template>
                  <template v-else>+</template>
                </label>

                <!-- 已选择/上传的参考图片缩略图 -->
                <div v-for="(img, idx) in refImages" :key="img.fileId" class="relative group">
                  <el-image
                      :src="img.url"
                      :preview-src-list="refImages.map(item => item.url)"
                      :hide-on-click-modal="true"
                      :initial-index="idx"
                      preview-teleported
                      fit="cover"
                      class="h-[60px] w-[60px] rounded-[14px] cursor-pointer"
                  />
                  <button
                      class="absolute -right-1.5 -top-1.5 h-[18px] w-[18px] rounded-full bg-black/70 text-[12px] text-white/90 opacity-0 group-hover:opacity-100 transition-opacity"
                      aria-label="remove"
                      @click="removeSubjectRefImage(idx)"
                  >×</button>
                </div>
              </div>
            </div>

            <div class="mb-2 text-[13px] leading-5 text-white/60">{{ t('creation.subjectEditDialog.promptLabel') }}</div>
            <textarea
                v-model="promptRaw"
                class="subject-prompt-input w-full"
                :placeholder="t('creation.subjectEditDialog.promptPlaceholder')"
                :disabled="editing"
            ></textarea>
          </div>

          <div class="shrink-0 px-6 pb-6 pt-4">
            <div class="flex items-center justify-between gap-3">
              <button class="h-[44px] w-[110px] shrink-0 rounded-[14px] bg-white/10 hover:bg-white/15 text-white/80 transition-colors cursor-pointer" @click="handleClose">{{ t('common.cancel') }}</button>
              <button
                  class="h-[44px] flex-1 rounded-[14px] bg-[#c2ff00] hover:opacity-90 text-black font-semibold transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="editing || uploadingRefImage"
                  @click="handleGenerateSubjectVersion"
              >
                <span v-if="editing">{{ t('creation.subjectEditDialog.generating') }}</span>
                <span v-else class="inline-flex items-center justify-center">
                  <span>{{ t('creation.subjectEditDialog.generateVersion', {version: nextSubjectVersionText}) }}</span>
                  <el-divider direction="vertical" class="subject-generate-divider" />
                  <span>{{ subjectPointsPrice != null ? t('creation.subjectEditDialog.pointsAmount', {points: subjectPointsPrice}) : t('creation.subjectEditDialog.pointsUnknown') }}</span>
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
 * SUBJECT 直连编辑弹窗：读取 `targetSubject` 当前 `activeVersion` 对应条目的 `prompt`、refImages。
 * 生成新任务完成后 upsert 回 `msg.subjectList`（见 upsertSubjectItem）。
 * 须与 `CreationMessageArea` 卡片展示一致：均以 `versions[].prompt` 为准，description 仅兜底。
 */
import {computed, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {isNotEmpty} from '@/utils/index.js';
import {directEditSubjectSubmit, getPointsPrice, uploadFile} from '@/api/creation';
import {upsertSubjectItem} from '@/views/creation/utils/creationMessageChunks';
import {applyCreationVersionChange, formatCreationVersionLabel} from '@/views/creation/utils/creationVersionSwitch';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {useCreationDirectEditTask} from '@/views/creation/composables/useCreationDirectEditTask';
import {useUserStore} from '@/store/user';
import {useI18nText} from '@/i18n';

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  /** 编辑目标：包含 subjectList 的消息对象（会被原地 upsert） */
  sourceMsg: {type: Object, default: null},
  /** 编辑目标 subject：来自 sourceMsg.subjectList 的某个元素 */
  targetSubject: {type: Object, default: null},
});

const emit = defineEmits(['update:modelValue', 'edit-context-change']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const editing = ref(false);
const editMessage = ref(null);
const editTargetSubject = ref(null);
const promptRaw = ref('');
const refImages = ref([]);
const MAX_REF_IMAGES = 6;
const subjectEditStore = useCreationSubjectEditStore();
const userStore = useUserStore();
const uploadingRefImage = ref(false);
const subjectPointsPrice = ref(null);
const {t} = useI18nText();

/** 关闭弹窗或切换目标时重置本地表单状态。 */
const resetLocalState = () => {
  editing.value = false;
  editMessage.value = null;
  editTargetSubject.value = null;
  promptRaw.value = '';
  refImages.value = [];
  subjectPointsPrice.value = null;
};

/**
 * SUBJECT 字段契约：versions=array、versions[].version=number、activeVersion=number；
 * 命中失败兜回首版。
 * @param {any} subject
 * @returns {any|null}
 */
const getActiveSubjectVersion = (subject) => {
  const versions = subject?.versions || [];
  if (!versions.length) return null;
  return versions.find((v) => v.version === subject?.activeVersion) || versions[0];
};

const activeVersion = computed(() => getActiveSubjectVersion(editTargetSubject.value));


const nextSubjectVersionLabel = computed(() => {
  const versions = editTargetSubject.value?.versions || [];
  if (!versions.length) return 1;
  // versions[].version 后端 number
  return versions.reduce((max, item) => Math.max(max, item.version), 0) + 1;
});

const nextSubjectVersionText = computed(() => formatCreationVersionLabel(nextSubjectVersionLabel.value));

/** 主体重生成是否积分不足。subjectPointsPrice / pointsBalance 后端契约 number。 */
const isSubjectPointsInsufficient = computed(() => {
  const estimated = subjectPointsPrice.value;
  const balance = userStore.pointsBalance;
  if (estimated == null || estimated <= 0) return false;
  if (balance == null || balance < 0) return false;
  return estimated > balance;
});

/**
 * 按当前模型拉取主体图片生成积分预估（弹窗打开或模型变化时）。
 * @returns {Promise<void>}
 */
const querySubjectPointsPrice = async () => {
  if (!visible.value) return;
  try {
    const data = await getPointsPrice({
      modelName: 'NONE',
      taskType: 'MAKE_IMAGE',
    });
    subjectPointsPrice.value = data;
  } catch (error) {
    subjectPointsPrice.value = null;
  }
};

/**
 * 打开弹窗时从 `targetSubject` 当前版本填充提示词与参考图列表。
 * @returns {void}
 */
const initFromProps = () => {
  if (!props.sourceMsg || !props.targetSubject) return;

  editMessage.value = props.sourceMsg;
  editTargetSubject.value = props.targetSubject;

  const active = getActiveSubjectVersion(props.targetSubject);
  promptRaw.value = active?.prompt ?? '';

  // versions[].refImages 后端契约为 array
  refImages.value = (active?.refImages || []).map((item) => ({
    fileId: item.fileId,
    url: item.url,
  }));
};

watch(
    () => props.modelValue,
    async (open) => {
      if (open) {
        initFromProps();
        // 弹窗打开时若该 chunk 已有进行中的任务，回显生成中状态并续上等待
        resumeSubjectEditTask().catch(() => {});
        await querySubjectPointsPrice();
      } else {
        resetLocalState();
      }
    },
    {immediate: true},
);

/** sessionId 后端契约为 string */
const getCurrentSessionId = () => editMessage.value?.sessionId || '';

const getCurrentChunkId = () => editTargetSubject.value?.messageChunkId || '';

const {applyTaskResult: applySubjectEditTaskResult, resumeTaskIfNeeded: resumeSubjectEditTask} = useCreationDirectEditTask({
  store: subjectEditStore,
  getSessionId: getCurrentSessionId,
  getChunkId: getCurrentChunkId,
  getSourceMsg: () => editMessage.value,
  setEditing: (value) => {
    editing.value = value;
  },
  taskType: 'SUBJECT',
  expectedType: 'SUBJECT',
  successMessage: t('creation.subjectEditDialog.newVersionGenerated'),
  upsertItem: upsertSubjectItem,
  listKey: 'subjectList',
  emitVersionChange: (payload) => emit('edit-context-change', payload),
  onSuccess: (resultData, sourceMsg, {messageChunkId: submittedChunkId, updatedItem} = {}) => {
    // upsert 与版本变化记录已由 composable 统一处理，这里只负责组件特有副作用：
    // 弹窗仍打开且就在编辑当前 chunk 时同步刷新预览目标。
    if (!submittedChunkId || !updatedItem) return;
    if (editTargetSubject.value?.messageChunkId === submittedChunkId) {
      editTargetSubject.value = updatedItem;
    }
  },
  onError: (message) => ElMessage.error(message),
  onNotifySuccess: (message) => ElMessage.success(message),
});

/** @returns {void} */
const handleClose = () => {
  visible.value = false;
};

/**
 * 弹窗内切换 SUBJECT 版本：行为与外部卡片一致（切换 activeVersion + 调用 switchVersion + 上抛 edit-context-change）。
 * @param {number|string} version
 * @returns {void}
 */
const handleSubjectVersionChange = (version) => {
  applyCreationVersionChange({
    version,
    type: 'SUBJECT',
    target: editTargetSubject.value,
    message: editMessage.value,
    emitEditContextChange: (payload) => emit('edit-context-change', payload),
  });
};

/**
 * 删除一张参考图并刷新展示用 @ 提及文案。
 * @param {number} idx
 * @returns {void}
 */
const removeSubjectRefImage = (idx) => {
  refImages.value.splice(idx, 1);
};

/**
 * 本地上传参考图并追加到列表。
 * @param {Event} event
 * @returns {Promise<void>}
 */
const handleSubjectRefUpload = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  if (uploadingRefImage.value) return;
  if (!isSupportedImageUpload(file)) {
    ElMessage.warning(t('creation.subjectEditDialog.onlyJpgPng'));
    return;
  }

  if (file.size / 1024 / 1024 >= 10) {
    ElMessage.warning(t('creation.subjectEditDialog.imageMaxSize'));
    return;
  }

  try {
    uploadingRefImage.value = true;
    const compressedFile = await compressImageBeforeUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile, compressedFile.name);
    const result = await uploadFile(formData);

    refImages.value.push({
      fileId: result.fileId,
      url: result.fileUrl || URL.createObjectURL(compressedFile),
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.subjectEditDialog.uploadFailed'));
  } finally {
    uploadingRefImage.value = false;
  }
};

/**
 * 将当前编辑中的 prompt / 参考图立即回写到消息列表里的对应 subject（本地乐观更新）。
 * 这样重新打开弹窗时会先看到最新草稿，任务完成后再由服务端结果覆盖。
 * @param {any} sourceMsg
 * @param {string} chunkId   后端 messageChunkId
 * @param {string} prompt
 * @returns {void}
 */
const syncSubjectDraftToMessage = (sourceMsg, chunkId, prompt) => {
  const target = (sourceMsg?.subjectList || []).find((item) => item?.messageChunkId === chunkId);
  if (!target) return;

  if (!target.versions) target.versions = [];

  const activeVersionNo = target.activeVersion;
  let active = target.versions.find((v) => v?.version === activeVersionNo);
  if (!active) active = target.versions[0];
  if (!active) {
    active = {version: activeVersionNo > 0 ? activeVersionNo : 1};
    target.versions.push(active);
  }

  active.prompt = prompt;
  active.refImages = refImages.value.map((item) => ({
    fileId: item.fileId,
    url: item.url,
  }));
  target.description = prompt;

  if (editTargetSubject.value?.messageChunkId === chunkId) {
    editTargetSubject.value = target;
  }
};

/**
 * 触发生成新的 subject 版本：
 * - 校验输入（subject/消息/提示词）
 * - 先本地回写当前草稿到消息列表
 * - 调用提交接口拿到 taskId
 * - 将任务交给 Pinia store 管理并后台轮询
 * - 弹窗关闭/离开页面后仍可继续轮询，重新打开可续上状态并回写
 * @returns {Promise<void>}
 */
const handleGenerateSubjectVersion = async () => {
  const subject = editTargetSubject.value;
  const sourceMsg = editMessage.value;
  if (!subject || !sourceMsg) return;

  const trimmed = (promptRaw.value || '').trim();
  if (!trimmed) {
    ElMessage.warning(t('creation.subjectEditDialog.enterPrompt'));
    return;
  }
  if (isSubjectPointsInsufficient.value) {
    ElMessage.warning(t('creation.subjectEditDialog.pointsInsufficient'));
    return;
  }

  const chunkId = subject.messageChunkId;
  if (!isNotEmpty(chunkId)) {
    ElMessage.warning(t('creation.subjectEditDialog.missingChunkId'));
    return;
  }

  if (!isNotEmpty(sourceMsg.sessionId)) {
    ElMessage.warning(t('creation.subjectEditDialog.missingSessionInfo'));
    return;
  }

  const submitSessionId = sourceMsg.sessionId;

  // 先写入本地草稿，确保下次打开展示的是当前编辑内容
  syncSubjectDraftToMessage(sourceMsg, chunkId, trimmed);

  try {
    editing.value = true;
    const taskId = await directEditSubjectSubmit({
      sessionId: submitSessionId,
      chunkId,
      prompt: trimmed,
      refImageFileIds: refImages.value.map(item => item.fileId).filter(Boolean),
    });

    if (!taskId) throw new Error(t('creation.taskSubmitFail'));
    subjectEditStore.startTask({
      sessionId: submitSessionId,
      chunkId,
      taskId,
      taskType: 'SUBJECT',
      previousVersion: editTargetSubject.value?.activeVersion,
      messageId: sourceMsg.messageId,
    });
    const finalTask = await subjectEditStore.waitForTask(submitSessionId, chunkId, {}, 'SUBJECT');
    await applySubjectEditTaskResult(finalTask, {
      sessionId: submitSessionId,
      messageChunkId: chunkId,
      sourceMsg,
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.subjectEditDialog.generateFailed'));
  } finally {
    // editing 状态在 applySubjectEditTaskResult 中统一收尾；这里兜底
    editing.value = false;
  }
};
</script>

<style lang="scss">
@use '@/views/creation/styles/creationVersionSelect.scss';

.subject-edit-modal {
  background: rgba(0, 0, 0, 0.68) !important;
  @apply backdrop-blur-[2px];
}

.subject-edit-dialog {
  padding: 0;

  .el-dialog {
    margin-top: 6vh !important;
    background: transparent !important;
    box-shadow: none !important;
    border-radius: 22px !important;
    overflow: hidden !important; // 防止四角露出默认背景

    &__header {
      display: none;
    }

    &__body {
      padding: 0 !important;
      background: transparent !important;
    }
  }
}

// 兼容 Element Plus 可能把 class 直接挂在 .el-dialog 上的场景
.subject-edit-dialog.el-dialog {
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


.subject-generate-divider {
  margin: 0 8px !important;
  height: 16px !important;
  border-left: 2px solid rgba(0, 0, 0, 0.45) !important;
}

.subject-prompt-input {
  min-height: 160px !important;
  height: 220px !important;
  color: rgba(255, 255, 255, 0.96) !important;
  font-size: 14px !important;
  line-height: 22px !important;
  background: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid rgba(255, 255, 255, 0.14) !important;
  border-radius: 12px !important;
  padding: 10px 12px !important;
  box-shadow: none !important;
  resize: none !important;

  &::placeholder {
    color: rgba(255, 255, 255, 0.5) !important;
  }

  // 提示词输入框滚动条：深色轨道 + 主题绿滑块
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.06);
    border-radius: 9999px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(194, 255, 0, 0.42);
    border-radius: 9999px;

    &:hover {
      background: rgba(194, 255, 0, 0.66);
    }
  }
}

.subject-mention-popper {
  // Mention 下拉：深色主题 + 荧光绿高亮
  &.el-mention__popper.el-popper {
    background: #070707 !important;
    border: 1px solid rgba(255, 255, 255, 0.10) !important;
    border-radius: 12px !important;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45) !important;
    overflow: hidden !important;
    padding: 0 !important;

    // 去掉箭头，避免浅色边缘不统一
    .el-popper__arrow {
      display: none !important;
    }
  }

  .el-mention-dropdown {
    --el-mention-bg-color: #070707;
    --el-mention-border: 1px solid rgba(255, 255, 255, 0.10);
    --el-mention-shadow: none;
    --el-mention-option-color: rgba(255, 255, 255, 0.86);
    --el-mention-option-hover-background: rgba(184, 255, 26, 0.12);
    --el-mention-option-selected-color: #C2FF00;
    --el-mention-option-disabled-color: rgba(255, 255, 255, 0.35);
    --el-mention-font-size: 12px;
    --el-mention-option-height: 30px;
    --el-mention-max-height: 180px;
    --el-mention-padding: 6px 0;
  }

  .el-mention-dropdown__item {
    padding: 0 12px !important;

    &.is-selected {
      font-weight: 700;
    }
  }
}
</style>
