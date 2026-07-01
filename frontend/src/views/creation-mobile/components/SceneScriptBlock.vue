<template>
  <!-- SCENE_SCRIPT 移动端展示块：复用 PC 端脚本行编辑/参考图维护逻辑 -->
  <div v-if="normalizedScenes.length" class="mt-3 mb-4">
    <section class="rounded-[0_10px_10px_10px] pl-[21px] pr-[10px] pt-[10px] pb-[12px] bg-[#5D634A]/50">
      <header class="mb-[6px] text-[18px] leading-[24px] font-bold text-white">{{ t('creation.sceneScript.title') }}</header>
      <div class="relative border-l border-dashed border-[#6A7454]">
        <div class="absolute -left-1 bottom-0 h-2 w-2 rounded-full bg-[#6A7454]"></div>
        <article
          v-for="(scene, index) in normalizedScenes"
          :key="scene.id || index"
          class="pl-[5px] pr-0 pt-0 pb-[25px]"
        >
          <div class="-ml-4 mb-[4px] flex items-center gap-[6px]">
            <div class="flex-center gap-[6px] rounded-[20px] bg-[#222719] p-[6px_8px]">
              <span class="text-[12px] text-[#C2FF00]">{{ index + 1 }}</span>
              <span class="text-[12px] text-[#C2FF00]">SCENE</span>
              <button type="button" class="h-3 w-3 flex-center" :disabled="!canEdit" @click="startEdit(scene)">
                <svg-icon name="gy-edit" size="12" color="#C2FF00"></svg-icon>
              </button>
            </div>
            <span class="text-[12px] text-[#C0C0C0]">{{ formatRange(scene.startTime, scene.endTime) }}</span>
            <span class="text-[12px] text-[#C2FF00]">{{ formatDurationLabel(scene) }}</span>
          </div>

          <template v-if="isEditing(scene.id)">
            <div
              class="mobile-scene-script__mention-input mb-[6px] rounded-[8px] border border-white/30 px-[8px] py-[6px]"
              @click.stop="focusEditingDraftInput"
              @touchstart.stop="focusEditingDraftInput"
              @touchend.stop="focusEditingDraftInput"
            >
              <el-mention
                ref="editingMentionRef"
                v-model="editingDraft"
                type="textarea"
                prefix="@"
                :options="getSceneMentionOptions(scene)"
                :autosize="{minRows: 3, maxRows: 6}"
                popper-class="scene-script-mention-popper"
                :placeholder="t('creation.sceneScript.mentionPlaceholder')"
              />
            </div>
            <div class="mb-[8px] flex items-center justify-end gap-2">
              <button type="button" class="h-[30px] rounded-[8px] bg-white/10 px-[12px] text-[12px] text-white" @click="cancelEdit">{{ t('common.cancel') }}</button>
              <button
                type="button"
                class="h-[30px] rounded-[8px] bg-[#C2FF00] px-[12px] text-[12px] text-black disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="isSavingScene(scene)"
                @click="saveEdit(scene, index)"
              >
                {{ isSavingScene(scene) ? t('common.saving') : t('common.save') }}
              </button>
            </div>
          </template>
          <p v-else class="mobile-scene-script__prompt-text mb-[5px]">{{ formatScenePromptDisplay(scene) || '--' }}</p>

          <div v-if="canEdit || scene.subjectRefs.length" class="flex items-center gap-[6px] flex-wrap">
            <div
              v-for="(ref, refIdx) in scene.subjectRefs.slice(0, 6)"
              :key="ref.fileId || ref.url || refIdx"
              class="group relative h-[57px] w-[57px] bg-black/20"
            >
              <el-image
                :src="ref.url"
                :preview-src-list="scene.subjectRefs.map((item) => item.url).filter(Boolean)"
                :initial-index="refIdx"
                :hide-on-click-modal="true"
                preview-teleported
                fit="cover"
                class="h-full w-full rounded-[6px]"
              />
              <button
                v-if="canEdit"
                type="button"
                class="absolute -right-[3px] -top-[3px] flex-center"
                @click.stop="removeSceneRef(scene, refIdx, index)"
              >
                <svg-icon name="gy-closure" size="10" color="#C2FF00"></svg-icon>
              </button>
            </div>
            <label
              v-if="canEdit && scene.subjectRefs.length < MAX_SCENE_SCRIPT_REFS"
              class="flex h-[57px] w-[57px] shrink-0 items-center justify-center rounded-[6px] border border-dashed border-white/40 text-[14px] text-white/80"
              :class="isUploadingScene(scene) ? 'pointer-events-none opacity-60' : 'cursor-pointer'"
            >
              <input
                type="file"
                accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                class="hidden"
                multiple
                :disabled="isUploadingScene(scene)"
                @change="(event) => handleSceneRefUpload(scene, event, index)"
              />
              <span v-if="isUploadingScene(scene)" class="text-[10px]">...</span>
              <span v-else>+</span>
            </label>
          </div>
        </article>
      </div>
    </section>

  </div>
</template>

<script setup>
import {computed, nextTick, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {useI18nText} from '@/i18n';
import {updateSceneScript, uploadFile} from '@/api/creation';
import {replaceMentionLabelsToValues, replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';

const {t} = useI18nText();

const props = defineProps({
  block: {type: Object, required: true},
  canEdit: {type: Boolean, default: false},
});
const emit = defineEmits(['scene-script-change']);
const MAX_SCENE_SCRIPT_REFS = 6; // 每个分镜行最大参考图数量

/**
 * SCENE_SCRIPT 字段契约（数据结构.txt）：
 * - block.messageChunkId:                 string ⭐（曾因 Number() 转换被打死，绝对不要再转）
 * - scenes[].id:                          string ⭐ ("1"/"scene_1"...)
 * - scenes[].startTime / endTime / duration: number
 * - scenes[].subjectRefs:                 array
 * - scenes[].subjectRefs[].subjectId / fileId / url: string
 * - scenes[].description / visualPrompt:  string
 *
 * 直接展开后端 scenes，不做任何类型转换。
 */
const normalizedScenes = computed(() =>
  (props.block?.scenes || []).map((item) => ({
    id: item.id,
    startTime: item.startTime,
    endTime: item.endTime,
    duration: item.duration,
    visualPrompt: item.visualPrompt,
    subjectRefs: item.subjectRefs,
    raw: item,
  })),
);

const editingId = ref(null); // 当前编辑中的分镜行 id（string）
const editingDraft = ref(''); // 编辑中的草稿文本
const editingMentionRef = ref(null); // el-mention 组件引用
const uploadingMap = ref({}); // 分镜行的上传中状态映射
const savingMap = ref({}); // 分镜行的保存中状态映射

const isSavingScene = (scene) => !!savingMap.value[scene.id];
const setSavingScene = (scene, saving) => {
  savingMap.value = {...savingMap.value, [scene.id]: saving};
};

// padStart 入参必须是字符串，String() 是 JS 语言层必需
const pad2 = (num) => String(Math.max(0, Math.floor(num || 0))).padStart(2, '0');
const formatTime = (seconds) => {
  const s = Math.max(0, Math.floor(seconds || 0));
  return `${pad2(Math.floor(s / 60))}:${pad2(s % 60)}`;
};
const formatRange = (start, end) => `${formatTime(start)}-${formatTime(end)}`;
const formatDurationLabel = (scene) => `${Math.max(0, Math.round(scene.duration || 0))}s`;
const isEditing = (id) => editingId.value === id;

const isUploadingScene = (scene) => !!uploadingMap.value[scene.id];
const setUploadingScene = (scene, uploading) => {
  uploadingMap.value = {...uploadingMap.value, [scene.id]: uploading};
};

/**
 * 当前分镜行的 mention 主体来源：直接复用后端 subjectRefs。
 * subjectRefs[].subjectId 后端 string，作为提及 token 的规范字段。
 * @param {Object} scene 分镜行数据
 * @returns {Array} 主体列表
 */
const getSceneMentionSubjects = (scene) => scene.subjectRefs;

/**
 * 将 visualPrompt 中的 @subjectId 转为 @图片N 展示文本。
 * @param {Object} scene 分镜行数据
 * @returns {string} 替换后的展示文本
 */
const formatScenePromptDisplay = (scene) => {
  return replaceMentionValuesToLabels(scene.visualPrompt, getSceneMentionSubjects(scene), {idFields: ['subjectId']});
};

/**
 * 生成 el-mention 下拉选项列表（@图片1, @图片2...）。
 * @param {Object} scene 分镜行数据
 * @returns {Array<{label: string, value: string}>}
 */
const getSceneMentionOptions = (scene) => {
  const subjects = getSceneMentionSubjects(scene);
  return subjects.map((_, idx) => {
    const label = t('creation.sceneScript.mentionLabel', {index: idx + 1});
    return {label, value: label};
  });
};

/**
 * 从 i18n 提取 mention 前缀生成匹配正则（中文 @图片1 / 英文 @image1）。
 * @returns {RegExp}
 */
const getMentionRegex = () => {
  const label = t('creation.sceneScript.mentionLabel');
  const prefix = label.replace(/\{index\}/, '');
  return new RegExp(`@${prefix}(\\d+)`, 'g');
};

/**
 * subjectRefs 浅克隆为 emit/上报用的快照副本（每个 item 都是新对象，避免 splice/push 污染 emit payload）。
 * subjectRefs[].subjectId / fileId / url 后端皆 string。
 * @param {Array} refs 原始参考图列表
 * @returns {Array<{subjectId:string, fileId:string, url:string}>}
 */
const cloneSubjectRefs = (refs) => (refs || []).map((item) => ({
  subjectId: item.subjectId,
  fileId: item.fileId,
  url: item.url,
}));

/**
 * 从提示词文本中提取 @图片N 引用的主体 id 列表。
 * @param {string} text 后端 visualPrompt 是 string
 * @param {Array} subjects 主体列表
 * @returns {string[]} 去重后的主体 id 列表
 */
const extractMentionSubjectIds = (text, subjects) => {
  const ids = [];
  const seen = new Set();
  const mentionRegexp = getMentionRegex();
  (text || '').replace(mentionRegexp, (_match, num) => {
    // 正则捕获组本身为 string，需转 number 做索引（JS 语言层必需）
    const subjectId = subjects[Number(num) - 1]?.subjectId;
    if (!subjectId || seen.has(subjectId)) return _match;
    seen.add(subjectId);
    ids.push(subjectId);
    return _match;
  });
  return ids;
};

/**
 * 获取分镜行在后端脚本中的索引位置。
 * scriptIdx 是前端附加 number 字段，未必存在；不存在时 `>= 0` 自动 false 走回退。
 * @param {Object} scene 分镜行数据
 * @param {number} sceneIdx 当前 v-for 渲染下标
 * @returns {number}
 */
const getSceneScriptIdx = (scene, sceneIdx) => {
  const rawIdx = scene.raw?.scriptIdx;
  return rawIdx >= 0 ? rawIdx : sceneIdx;
};

/**
 * 将提示词中的 @图片N 转回 @subjectId 格式（后端存储用）。
 * @param {Object} scene 分镜行数据
 * @param {string} prompt 提示词文本
 * @returns {string}
 */
const getUnwrappedPrompt = (scene, prompt) => {
  return (replaceMentionLabelsToValues(prompt || '', getSceneMentionSubjects(scene), {valueField: 'subjectId'}) || '').trim();
};

/**
 * 调用后端接口持久化一条分镜脚本行。
 * 注意：chunkId 后端契约 **string**（曾因 Number() 转换被 "scene_1" 打死，绝对不要再转）。
 * @param {Object} scene 分镜行数据
 * @param {number} sceneIdx 行索引
 * @param {Object} [payload] 持久化数据
 * @param {string} [payload.visualPrompt] 视觉提示词
 * @param {Array} [payload.subjectRefs] 参考图列表
 */
const persistSceneScriptLine = async (scene, sceneIdx, payload = {}) => {
  const scriptIdx = getSceneScriptIdx(scene, sceneIdx);
  if (!props.block?.messageChunkId || scriptIdx < 0) return;
  await updateSceneScript({
    chunkId: props.block.messageChunkId,
    scriptIdx,
    visualPrompt: payload.visualPrompt,
    subjectRefs: cloneSubjectRefs(payload.subjectRefs),
  });
};

/**
 * 进入分镜行编辑态，填充初始提示词草稿。
 * @param {Object} scene 分镜行数据
 */
const startEdit = (scene) => {
  if (!props.canEdit) return;
  editingId.value = scene.id;
  editingDraft.value = replaceMentionValuesToLabels(
    scene.visualPrompt,
    getSceneMentionSubjects(scene),
    {idFields: ['subjectId']},
  );
};

/**
 * 强制聚焦编辑区 textarea（兼容移动端多次尝试）。
 */
const focusEditingDraftInput = async () => {
  const root = editingMentionRef.value?.$el || editingMentionRef.value;
  const textarea = root?.querySelector?.('textarea');
  if (!textarea) return;

  textarea.focus({preventScroll: true});
  await nextTick();
  textarea.focus({preventScroll: true});
  setTimeout(() => textarea.focus({preventScroll: true}), 0);
};

/**
 * 取消编辑，清空编辑态和草稿。
 */
const cancelEdit = () => {
  editingId.value = null;
  editingDraft.value = '';
};

/**
 * 保存分镜脚本行编辑：
 * 1. 先乐观更新前端状态 + emit scene-script-change
 * 2. 再调用后端接口持久化
 * 3. 使用时 savingMap 防止重复提交
 * @param {Object} scene 分镜行数据
 * @param {number} [sceneIdx=-1] 行索引
 */
const saveEdit = async (scene, sceneIdx = -1) => {
  if (!props.canEdit) return;
  if (isSavingScene(scene)) return;
  // visualPrompt 后端 string；scene.id 后端 string
  const previousPrompt = scene.visualPrompt;
  const nextPrompt = (editingDraft.value || '').trim();
  if (!nextPrompt) return;

  const subjects = getSceneMentionSubjects(scene);
  const previousSubjectRefs = cloneSubjectRefs(scene.subjectRefs);
  const previousPromptUnwrapped = (replaceMentionLabelsToValues(previousPrompt, subjects, {valueField: 'subjectId'}) || '').trim();
  const nextPromptUnwrapped = (replaceMentionLabelsToValues(nextPrompt, subjects, {valueField: 'subjectId'}) || '').trim();

  if (scene.raw) {
    scene.raw.visualPrompt = nextPromptUnwrapped;
  }
  emit('scene-script-change', {
    lineKey: scene.id,
    previousPrompt,
    nextPrompt,
    previousPromptUnwrapped,
    nextPromptUnwrapped,
    previousSubjectRefs,
    nextSubjectRefs: cloneSubjectRefs(scene.subjectRefs),
    previousMentionSubjectIds: extractMentionSubjectIds(previousPrompt, subjects),
    nextMentionSubjectIds: extractMentionSubjectIds(nextPrompt, subjects),
  });
  setSavingScene(scene, true);
  try {
    await persistSceneScriptLine(scene, sceneIdx, {
      visualPrompt: nextPromptUnwrapped,
      subjectRefs: scene.subjectRefs,
    });
  } catch (error) {
    ElMessage.warning(error?.message || t('creation.sceneScript.saveFallback'));
  } finally {
    setSavingScene(scene, false);
  }
  cancelEdit();
};

/**
 * 确保分镜行的 raw 对象上有 subjectRefs 数组并返回。
 * @param {Object} scene 分镜行数据
 * @returns {Array} 参考图列表
 */
const ensureSceneRefs = (scene) => {
  if (!scene.raw) return [];
  scene.raw.subjectRefs ||= [];
  return scene.raw.subjectRefs;
};

/**
 * 删除指定索引的参考图，并在前端乐观更新后同步后端。
 * @param {Object} scene 分镜行数据
 * @param {number} idx 要删除的参考图索引
 * @param {number} [sceneIdx=-1] 行索引
 */
const removeSceneRef = async (scene, idx, sceneIdx = -1) => {
  if (!props.canEdit) return;
  const refs = ensureSceneRefs(scene);
  if (idx < 0 || idx >= refs.length) return;
  const previousSubjectRefs = cloneSubjectRefs(refs);
  refs.splice(idx, 1);
  const nextSubjectRefs = cloneSubjectRefs(refs);
  emit('scene-script-change', {
    lineKey: scene.id,
    previousSubjectRefs,
    nextSubjectRefs,
  });
  try {
    await persistSceneScriptLine(scene, sceneIdx, {
      visualPrompt: getUnwrappedPrompt(scene, scene.visualPrompt),
      subjectRefs: nextSubjectRefs,
    });
  } catch (error) {
    ElMessage.warning(error?.message || t('creation.sceneScript.refRemovedFallback'));
  }
};

/**
 * 上传参考图到分镜行（支持批量）：
 * 1. 校验文件类型和大小
 * 2. 按剩余可用槽位裁剪文件列表
 * 3. 逐个压缩并上传
 * 4. 添加 ref 并同步后端
 * @param {Object} scene 分镜行数据
 * @param {Event} event 文件选择事件
 * @param {number} [sceneIdx=-1] 行索引
 */
const handleSceneRefUpload = async (scene, event, sceneIdx = -1) => {
  if (!props.canEdit) return;
  const files = Array.from(event?.target?.files || []);
  if (event?.target) event.target.value = '';
  if (!files.length) return;

  const refs = ensureSceneRefs(scene);
  const available = MAX_SCENE_SCRIPT_REFS - refs.length;
  if (available <= 0) return;

  const validFiles = [];
  for (const file of files) {
    if (!isSupportedImageUpload(file)) {
      ElMessage.warning(t('creation.sceneEditDialog.onlyJpgPng'));
      continue;
    }
    if (file.size / 1024 / 1024 >= 10) {
      ElMessage.warning(t('creation.sceneEditDialog.imageMaxSize'));
      continue;
    }
    if (validFiles.length >= available) break;
    validFiles.push(file);
  }

  if (!validFiles.length) return;

  try {
    setUploadingScene(scene, true);
    const previousSubjectRefs = cloneSubjectRefs(refs);
    const createSubjectId = () => `subject_${Date.now()}_${Math.floor(Math.random() * 1000000)}`;

    for (const file of validFiles) {
      const compressedFile = await compressImageBeforeUpload(file);
      const formData = new FormData();
      formData.append('file', compressedFile, compressedFile.name);
      const result = await uploadFile(formData);
      refs.push({
        subjectId: createSubjectId(),
        fileId: result.fileId,
        url: result.fileUrl || URL.createObjectURL(compressedFile),
      });
    }

    const nextSubjectRefs = cloneSubjectRefs(refs);
    emit('scene-script-change', {
      lineKey: scene.id,
      previousSubjectRefs,
      nextSubjectRefs,
    });
    await persistSceneScriptLine(scene, sceneIdx, {
      visualPrompt: getUnwrappedPrompt(scene, scene.visualPrompt),
      subjectRefs: nextSubjectRefs,
    });
  } catch (error) {
    ElMessage.error(error?.message || t('creation.sceneEditDialog.uploadFailed'));
  } finally {
    setUploadingScene(scene, false);
  }
};
</script>

<style scoped lang="scss">
.mobile-scene-script__prompt-text {
  color: #ffffff;
  font-size: 12px;
  line-height: 17px;
  font-weight: 400;
  white-space: pre-wrap;
}
</style>

<style lang="scss">
.mobile-scene-script__mention-input {
  .el-mention__editor,
  .el-textarea__inner,
  textarea {
    width: 100% !important;
    min-height: 51px !important;
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

.scene-script-mention-popper {
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
