<template>
  <!-- 移动端主体编辑全屏页：替代 PC 弹窗，承载主体版本预览与重生成操作 -->
  <div v-if="visible" class="fixed inset-0 z-[70] flex flex-col bg-[#000000] text-white">
    <!-- 顶部标题区：保持设计稿的居中标题样式 -->
    <div class="shrink-0 pt-[30px] mb-[11px] text-center">
      <div class="text-[18px] font-bold leading-[25px]">{{ t('creation.messageArea.editReferenceTitle') }}</div>
    </div>

    <!-- 主体内容区：根据图片比例切换横版/竖版预览布局 -->
    <div class="min-h-0 flex-1 overflow-y-auto px-[15px]">
      <div class="text-[15px] font-bold leading-[21px] mb-[5px] text-white">
        {{ t('creation.messageArea.referenceLabel', {index: displaySubjectIndex}) }}
      </div>

<!-- 横版图：大图预览 + 左右切换箭头 -->
      <div v-if="!isImageMetaLoading && isLandscape" class="relative aspect-[16/9] w-full overflow-hidden rounded-[5px] bg-black">
        <el-image
          v-if="activeVersion?.imgUrl"
          :src="activeVersion.imgUrl"
          fit="contain"
          class="w-full h-full"
        />
        <div v-else class="absolute inset-0 flex-center text-white/55">
          {{ t('creation.subjectEditDialog.noImage') }}
        </div>
        <div class="absolute left-[10px] top-[10px] rounded-[8px] bg-black/50 px-[10px] py-[5px] text-[12px] font-semibold leading-[16px] text-[#C2FF00]">
          {{ currentVersionLabel }}
        </div>
        <button
          v-if="versionList.length > 1"
          type="button"
          class="absolute left-0 top-1/2 -translate-y-1/2"
          @click="selectSiblingVersion(-1)"
        >
          <svg-icon name="gy-return" size="20" color="#C2FF00" class="opacity-50"></svg-icon>
        </button>
        <button
          v-if="versionList.length > 1"
          type="button"
          class="absolute right-0 top-1/2 -translate-y-1/2"
          @click="selectSiblingVersion(1)"
        >
          <svg-icon name="gy-return" size="20" color="#C2FF00" class="rotate-180 opacity-50"></svg-icon>
        </button>
      </div>

      <!-- 竖版图：多版本横向卡片列表，单张卡片比例 9:16 -->
      <div v-else-if="!isImageMetaLoading" class="-mx-[2px] overflow-x-auto pb-[2px]">
        <div class="flex gap-[6px] pr-[22px]">
          <button
              v-for="version in versionList"
              :key="version.version"
              type="button"
              class="relative w-[141px] h-[228px] shrink-0 overflow-hidden rounded-[10px] border bg-white/8"
              :class="version.version === editTargetSubject?.activeVersion ? 'border-[#C2FF00]' : 'border-transparent opacity-85'"
              @click="handleSubjectVersionChange(version.version)"
          >
            <el-image
                v-if="version.imgUrl"
                :src="version.imgUrl"
                fit="contain"
            />
            <div v-else class="flex w-[128px] h-[228px] items-center justify-center text-white/55">
              {{ t('creation.subjectEditDialog.noImage') }}
            </div>
            <div class="absolute left-[10px] top-[10px] rounded-[8px] bg-black/50 px-[10px] py-[5px] text-[12px] font-semibold leading-[16px] text-[#C2FF00]">
              {{ formatCreationVersionLabel(version.version) }}
            </div>
          </button>
        </div>
      </div>

      <!-- 图片尺寸加载中：显示占位 -->
      <div v-if="isImageMetaLoading" class="flex h-[228px] w-[128px] items-center justify-center bg-black/40">
        <svg-icon name="gy-loading" size="24" color="#C2FF00" class="animate-spin"></svg-icon>
      </div>

      <!-- 参考图片区：支持追加参考图和删除已选图片 -->
      <div class="mt-3">
        <div class="mb-1 text-[14px] font-medium leading-[20px] text-[#C2FF00]">
          {{ t('creation.subjectEditDialog.referenceImages') }}
        </div>
        <div class="flex flex-wrap gap-3">
          <label
              v-if="refImages.length < MAX_REF_IMAGES"
              class="flex h-[70px] w-[70px] items-center justify-center rounded-[10px] border border-[#C2FF00] bg-[linear-gradient(299deg,rgba(190,250,0,0.2)_0%,rgba(130,255,121,0.2)_100%)]"
              :class="uploadingRefImage ? 'pointer-events-none opacity-70' : ''"
          >
            <input
                type="file"
                accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                class="hidden"
                :disabled="uploadingRefImage"
                @change="handleSubjectRefUpload"
            />
            <span v-if="uploadingRefImage" class="text-[12px] text-white">{{
                t('creation.subjectEditDialog.uploading')
              }}</span>
            <svg-icon v-else name="gy-upload" size="14" color="#C2FF00"></svg-icon>
          </label>

          <div
              v-for="(img, idx) in refImages"
              :key="img.fileId || `${img.url}-${idx}`"
              class="group relative h-[70px] w-[70px]"
          >
            <el-image
                :src="img.url"
                :preview-src-list="refImages.map((item) => item.url)"
                :hide-on-click-modal="true"
                :initial-index="idx"
                preview-teleported
                fit="cover"
                class="h-full w-full rounded-[10px]"
            />
            <button
                type="button"
                class="absolute right-[-6px] top-[-6px]"
                @click="removeSubjectRefImage(idx)"
            >
              <svg-icon name="gy-closure" size="14" color="#FFFFFF"></svg-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 提示词编辑区：支持复制当前提示词并直接修改文案 -->
      <div class="mt-3">
        <div class="mb-[5px] flex items-center justify-between">
          <div class="text-[14px] font-medium leading-[20px] text-[#C2FF00]">
            {{ t('creation.subjectEditDialog.promptLabel') }}
          </div>
          <button type="button" @click="handleCopyPrompt">
            <svg-icon name="gy-copy" size="18" color="#C2FF00"></svg-icon>
          </button>
        </div>

        <div
          @click.stop="focusPromptTextarea"
          @touchstart.stop="focusPromptTextarea"
          @touchend.stop="focusPromptTextarea"
        >
          <textarea
              ref="promptTextareaRef"
              v-model="promptRaw"
              class="mobile-subject-editor__textarea"
              :placeholder="t('creation.subjectEditDialog.promptPlaceholder')"
              :disabled="editing"
          ></textarea>
        </div>
      </div>
    </div>

    <!-- 底部操作区：关闭当前页或提交生成新版本 -->
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
            @click="handleGenerateSubjectVersion"
        >
          <span v-if="editing">{{ t('creation.subjectEditDialog.generating') }}</span>
          <span v-else>{{
              t('creation.subjectEditDialog.generateVersion', {version: nextSubjectVersionText})
            }} | {{ pointsText }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {directEditSubjectSubmit, getPointsPrice, uploadFile} from '@/api/creation';
import {useI18nText} from '@/i18n';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';
import {upsertSubjectItem} from '@/views/creation/utils/creationMessageChunks';
import {applyCreationVersionChange, formatCreationVersionLabel} from '@/views/creation/utils/creationVersionSwitch';
import {useCreationDirectEditTask} from '@/views/creation/composables/useCreationDirectEditTask';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {useUserStore} from '@/store/user';
import {isNotEmpty} from '@/utils';

/**
 * 移动端主体编辑页：
 * - 复用 PC 端主体直连编辑的提交、轮询和结果回写逻辑
 * - 根据当前主体图比例切换横版/竖版展示
 * - 支持上传参考图、编辑提示词、切换版本和生成新版本
 */
const props = defineProps({
  /** 是否显示全屏编辑页。 */
  visible: {type: Boolean, default: false},
  /** 当前被编辑主体所属的消息对象。 */
  sourceMsg: {type: Object, default: null},
  /** 当前被编辑的主体对象。 */
  targetSubject: {type: Object, default: null},
  /** 当前主体在 subjectList 中的展示序号。 */
  subjectIndex: {type: Number, default: 0},
});

/** 向父组件通知关闭编辑页。 */
const emit = defineEmits(['close', 'edit-context-change']);

const {t} = useI18nText();
const subjectEditStore = useCreationSubjectEditStore();
const userStore = useUserStore();

/** 本地编辑态：编辑目标、提示词、参考图、积分预估与图片元信息。 */
const editing = ref(false);
const editMessage = ref(null);
const editTargetSubject = ref(null);
const promptRaw = ref('');
const promptTextareaRef = ref(null);
const refImages = ref([]);
const uploadingRefImage = ref(false);
const subjectPointsPrice = ref(null);
const imageMetaMap = ref({});
const MAX_REF_IMAGES = 6;

const focusPromptTextarea = async () => {
  if (editing.value) return;
  const el = promptTextareaRef.value;
  if (!el?.focus) return;

  el.focus({preventScroll: true});
  await nextTick();
  el.focus({preventScroll: true});
  setTimeout(() => el.focus({preventScroll: true}), 0);
};

/** 编辑页关闭时重置本地状态，避免下次打开残留上一次草稿。 */
const resetLocalState = () => {
  editing.value = false;
  editMessage.value = null;
  editTargetSubject.value = null;
  promptRaw.value = '';
  refImages.value = [];
  uploadingRefImage.value = false;
  subjectPointsPrice.value = null;
  imageMetaMap.value = {};
};

/**
 * 获取主体当前激活版本。
 * SUBJECT 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} subject
 * @returns {any|null}
 */
const getActiveSubjectVersion = (subject) => {
  const versions = subject?.versions || [];
  if (!versions.length) return null;
  return versions.find((item) => item?.version === subject?.activeVersion) || versions[0];
};

/** 当前页面展示所依赖的核心派生状态。 */
const activeVersion = computed(() => getActiveSubjectVersion(editTargetSubject.value));
const versionList = computed(() => editTargetSubject.value?.versions || []);
const currentVersionLabel = computed(() => formatCreationVersionLabel(editTargetSubject.value?.activeVersion ?? 0));
const displaySubjectIndex = computed(() => (props.subjectIndex || 0) + 1);

/** 当前图片是否为横屏（宽高比大于 1）。 */
const isLandscape = computed(() => {
  const key = activeVersion.value?.imgUrl || '';
  const meta = imageMetaMap.value[key];
  if (!meta || !meta.width || !meta.height) return false;
  return meta.width > meta.height;
});

/** 是否正在加载图片尺寸（用于判断横竖屏）。 */
const isImageMetaLoading = computed(() => {
  const key = activeVersion.value?.imgUrl || '';
  if (!key) return false;
  return !imageMetaMap.value[key];
});

const nextSubjectVersionLabel = computed(() => {
  const versions = versionList.value;
  if (!versions.length) return 1;
  // versions[].version 后端 number
  return versions.reduce((max, item) => Math.max(max, item?.version || 0), 0) + 1;
});

const nextSubjectVersionText = computed(() => formatCreationVersionLabel(nextSubjectVersionLabel.value));
const pointsText = computed(() => (
    subjectPointsPrice.value != null
        ? t('creation.subjectEditDialog.pointsAmount', {points: subjectPointsPrice.value})
        : t('creation.subjectEditDialog.pointsUnknown')
));

/** 生成新版本前校验当前用户积分是否足够。subjectPointsPrice / pointsBalance 后端 number。 */
const isSubjectPointsInsufficient = computed(() => {
  const estimated = subjectPointsPrice.value;
  const balance = userStore.pointsBalance;
  if (estimated == null || estimated <= 0) return false;
  if (balance == null || balance < 0) return false;
  return estimated > balance;
});

/**
 * 从当前主体激活版本回填本地表单。
 * @param {any} subject
 * @returns {void}
 */
const syncFormFromSubject = (subject) => {
  const active = getActiveSubjectVersion(subject);
  promptRaw.value = active?.prompt ?? subject?.description ?? '';
  // versions[].refImages 后端契约 array
  refImages.value = (active?.refImages || []).map((item) => ({
    fileId: item.fileId,
    url: item.url,
  }));
};

/**
 * 预加载所有版本图片尺寸，供布局模式判断使用。
 * versions[].imgUrl 后端契约 string，trim 防尾随空白即可。
 * @param {any} subject
 * @returns {Promise<void>}
 */
const collectImageMetas = async (subject) => {
  const versions = subject?.versions || [];
  const entries = await Promise.all(
      versions
          .map((item) => (item?.imgUrl || '').trim())
          .filter(Boolean)
          .map((url) => new Promise((resolve) => {
            const img = new Image();
            img.onload = () => resolve([url, {width: img.naturalWidth, height: img.naturalHeight}]);
            img.onerror = () => resolve([url, {width: 0, height: 0}]);
            img.src = url;
          })),
  );
  imageMetaMap.value = Object.fromEntries(entries);
};

/**
 * 打开编辑页时，根据父层传入的消息与主体初始化本地上下文。
 * 注意：这里优先同步回填基础信息，避免“生成中/按钮禁用”等状态被图片预加载阻塞。
 * @returns {void}
 */
const initFromPropsSync = () => {
  if (!props.sourceMsg || !props.targetSubject) return;
  editMessage.value = props.sourceMsg;
  editTargetSubject.value = props.targetSubject;
  syncFormFromSubject(props.targetSubject);
};

/**
 * 拉取主体图片生成的积分预估。
 * @returns {Promise<void>}
 */
const querySubjectPointsPrice = async () => {
  if (!props.visible) return;
  try {
    const data = await getPointsPrice({
      modelName: 'NONE',
      taskType: 'MAKE_IMAGE',
    });
    subjectPointsPrice.value = data;
  } catch (_) {
    subjectPointsPrice.value = null;
  }
};

/** sessionId 后端 string */
const getCurrentSessionId = () => editMessage.value?.sessionId || '';

/** chunkId(=messageChunkId) 后端 string，禁止私自转 number */
const getCurrentChunkId = () => editTargetSubject.value?.messageChunkId || '';

/**
 * 复用通用直连编辑任务能力：
 * - 若已有运行中任务，重新打开页面时继续等待结果
 * - 成功后把服务端返回的新版本 upsert 回消息列表
 */
const {applyTaskResult, resumeTaskIfNeeded} = useCreationDirectEditTask({
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
  onSuccess: async (resultData, sourceMsg, {messageChunkId: submittedChunkId, updatedItem} = {}) => {
    // upsert 与版本变化记录已由 composable 统一处理，这里只负责组件特有副作用：
    // 面板仍打开且就在编辑当前 chunk 时同步刷新预览目标 + 表单 + 图片尺寸。
    if (!submittedChunkId || !updatedItem) return;
    if (editTargetSubject.value?.messageChunkId === submittedChunkId) {
      editTargetSubject.value = updatedItem;
      syncFormFromSubject(updatedItem);
      await collectImageMetas(updatedItem);
    }
  },
  onError: (message) => ElMessage.error(message),
  onNotifySuccess: (message) => ElMessage.success(message),
});

/** 编辑页打开时初始化数据，关闭时清空状态。 */
watch(
    () => props.visible,
    async (open) => {
      if (open) {
        // 1) 先同步初始化编辑上下文与表单，确保 UI 立即可渲染
        initFromPropsSync();
        // 2) 并发触发：积分预估、图片尺寸预加载、任务恢复（生成中回显）
        await Promise.allSettled([
          querySubjectPointsPrice(),
          collectImageMetas(props.targetSubject),
          resumeTaskIfNeeded(),
        ]);
      } else {
        resetLocalState();
      }
    },
    {immediate: true},
);

/**
 * 编辑中如果外部切换了目标主体，清理旧草稿并重新回填。
 * @returns {void}
 */
watch(
    () => [props.visible, props.sourceMsg?.messageId, props.targetSubject?.messageChunkId],
    ([open]) => {
      if (!open) return;
      initFromPropsSync();
    },
);

/** 关闭当前全屏编辑页。 */
const handleClose = () => {
  emit('close');
};

/**
 * 切换主体版本：
 * - 复用公共版本切换逻辑同步 activeVersion
 * - 然后按新版本重新回填提示词与参考图
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
  syncFormFromSubject(editTargetSubject.value);
};

/**
 * 横版布局下的前后版本切换。
 * @param {number} offset
 * @returns {void}
 */
const selectSiblingVersion = (offset) => {
  const versions = versionList.value;
  if (!versions.length) return;
  const currentIndex = versions.findIndex((item) => item?.version === editTargetSubject.value?.activeVersion);
  const safeIndex = currentIndex >= 0 ? currentIndex : 0;
  const nextIndex = (safeIndex + offset + versions.length) % versions.length;
  handleSubjectVersionChange(versions[nextIndex]?.version);
};

/** 删除一张参考图。 */
const removeSubjectRefImage = (idx) => {
  refImages.value.splice(idx, 1);
};

/**
 * 上传一张新的参考图并追加到当前编辑表单中。
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
 * 复制当前提示词，便于用户外部复用或微调。
 * @returns {Promise<void>}
 */
const handleCopyPrompt = async () => {
  const text = (promptRaw.value || '').trim();
  if (!text) {
    ElMessage.warning(t('creation.subjectEditDialog.enterPrompt'));
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(t('creation.messageArea.copiedWithLabel', {label: t('creation.subjectEditDialog.promptLabel')}));
  } catch {
    ElMessage.error(t('layout.copyFail'));
  }
};

/**
 * 先把当前草稿乐观回写到消息里的目标主体。
 * subjectList/versions 都是 array 契约；chunkId/version 直接用其本身类型 ===。
 * @param {any} sourceMsg
 * @param {string} chunkId 后端 messageChunkId
 * @param {string} prompt
 * @returns {void}
 */
const syncSubjectDraftToMessage = (sourceMsg, chunkId, prompt) => {
  const target = (sourceMsg?.subjectList || []).find((item) => item?.messageChunkId === chunkId);
  if (!target) return;

  if (!target.versions) target.versions = [];
  const activeVersionNo = target.activeVersion;
  let active = target.versions.find((item) => item?.version === activeVersionNo);
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
 * 提交主体新版本生成：
 * - 先做表单与积分校验
 * - 再把当前草稿乐观写回消息
 * - 最后提交任务并等待轮询结果回写
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
  syncSubjectDraftToMessage(sourceMsg, chunkId, trimmed);

  try {
    editing.value = true;
    const taskId = await directEditSubjectSubmit({
      sessionId: submitSessionId,
      chunkId,
      prompt: trimmed,
      refImageFileIds: refImages.value.map((item) => item.fileId).filter(Boolean),
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
    await applyTaskResult(finalTask, {
      sessionId: submitSessionId,
      messageChunkId: chunkId,
      sourceMsg,
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.subjectEditDialog.generateFailed'));
  } finally {
    editing.value = false;
  }
};
</script>

<style lang="scss" scoped>
.mobile-subject-editor__textarea {
  width: 100%;
  min-height: 130px;
  border: none;
  resize: none;
  outline: none;
  border-radius: 10px;
  background: rgba(240,246,221,0.3);
  padding: 10px;
  color: #ffffff;
  font-size: 12px;
  line-height: 17px;

  &::placeholder {
    color: rgba(255, 255, 255, 0.72);
  }
}
</style>
