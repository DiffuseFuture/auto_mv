<template>
  <div class="rounded-[22px] border border-white/10 bg-[#141414] overflow-hidden">
    <div class="flex items-center justify-between px-6 py-4 bg-white/5">
      <div class="text-[#C2FF00] text-[18px] font-semibold leading-[26px]">分镜脚本</div>
      <div class="text-white/60 text-[14px]">共{{ scenesCount }}个分镜</div>
    </div>

    <div class="divide-y divide-white/10">
      <div
          v-for="(scene, sceneIdx) in normalizedScenes"
          :key="scene.id"
          class="px-6 py-5"
      >
        <!-- 顶部：标题/时段/时长 + 编辑按钮 -->
        <div class="flex items-center justify-between gap-4">
          <div class="flex items-end gap-4 min-w-0">
            <div class="text-[#C2FF00] text-[22px] font-semibold leading-none">{{ scene.id }}</div>
            <div class="text-white/60 text-[13px] whitespace-nowrap">{{ formatRange(scene.startTime, scene.endTime) }}</div>
            <div class="h-[22px] px-3 rounded-full bg-white/10 text-[#C2FF00] text-[14px] whitespace-nowrap inline-flex items-center">
              {{ Math.round(scene.duration) }}s
            </div>
          </div>

          <button
              v-if="canEdit"
              class="h-9 w-9 rounded-[10px] hover:bg-white/10 flex-center transition-colors cursor-pointer shrink-0"
              title="编辑"
              aria-label="edit-scene-script"
              @click="startEdit(scene)"
          >
            <svg-icon name="gy-edit" size="16" color="rgba(255,255,255,0.75)"></svg-icon>
          </button>
        </div>

        <!-- 下方：左（提示词）右（参考图，约200宽） -->
        <div class="mt-4 flex items-start gap-6">
          <div class="flex-1 min-w-0">
            <div v-if="!isEditing(scene.id)" class="text-white/85 text-[14px] leading-[22px] whitespace-pre-wrap">
              {{ formatScenePromptDisplay(scene) }}
            </div>

            <div v-else class="rounded-[16px] bg-black/20 border border-white/25 px-4 py-4">
              <el-mention
                  v-model="editingDraft"
                  type="textarea"
                  prefix="@"
                  :options="getSceneMentionOptions(scene)"
                  :autosize="{minRows: 5, maxRows: 8}"
                  placeholder="编辑分镜提示词（输入 @ 可提及参考图）"
                  class="scene-script-mention-input"
                  popper-class="scene-script-mention-popper"
              />

              <div class="mt-4 flex items-center justify-end gap-3">
                <button
                    class="h-[34px] px-6 rounded-[10px] bg-white/10 hover:bg-white/15 text-white/80 transition-colors cursor-pointer"
                    @click="cancelEdit"
                >
                  取消
                </button>
                <button
                    class="h-[34px] px-6 rounded-[10px] bg-[#C2FF00] hover:opacity-90 text-black font-semibold transition-opacity cursor-pointer"
                    @click="saveEdit(scene, sceneIdx)"
                >
                  保存
                </button>
              </div>
            </div>
          </div>

          <div class="shrink-0 w-[200px] flex items-start justify-end">
            <div class="scene-ref-stack">
              <div
                  v-for="(ref, idx) in scene.subjectRefs"
                  :key="ref.fileId || idx"
                  class="scene-ref-card group"
                  :style="{zIndex: idx + 1}"
              >
                <el-image
                    :src="ref.url"
                    :preview-src-list="getSceneRefPreviewList(scene)"
                    :hide-on-click-modal="true"
                    :initial-index="idx"
                    preview-teleported
                    fit="cover"
                    class="h-full w-full cursor-pointer"
                />
                <button
                    v-if="canEdit"
                    class="absolute top-1 right-1 z-[60] inline-flex rounded-full opacity-0 pointer-events-none transition-opacity duration-200 group-hover:opacity-100 group-hover:pointer-events-auto cursor-pointer"
                    type="button"
                    title="删除参考图"
                    aria-label="remove-scene-ref"
                    @click.stop="removeSceneRef(scene, idx, sceneIdx)"
                >
                  <svg-icon name="gy-closure" :size="14" color="#FFFFFF"></svg-icon>
                </button>
              </div>
              <label
                  v-if="canEdit && scene.subjectRefs.length < MAX_SCENE_SCRIPT_REFS"
                  class="scene-ref-card scene-ref-add-card"
                  :class="{'is-uploading': isUploadingScene(scene)}"
                  title="上传参考图"
              >
                <input
                    type="file"
                    accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                    class="hidden"
                    :disabled="isUploadingScene(scene)"
                    @change="(event) => handleSceneRefUpload(scene, event, sceneIdx)"
                />
                <span v-if="isUploadingScene(scene)" class="text-[11px] leading-none">上传中</span>
                <span v-else>+</span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * SCENE_SCRIPT 分镜脚本表：数据来自消息 `blocks` 中 type=SCENE_SCRIPT 的 `scenes[]` 行。
 *
 * - 与 SUBJECT/SCENE 不同：每行只有一份 `visualPrompt` 与参考图列表，无多版本模型。
 * - 编辑：内联改 `scene.raw.visualPrompt` / subjectRefs，经 `scene-script-change` 通知父层拼入 `editContext`。
 */
import {computed, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {updateSceneScript, uploadFile} from '@/api/creation';
import {replaceMentionLabelsToValues, replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {compressImageBeforeUpload, isSupportedImageUpload} from '@/views/creation/utils/upload.js';

/**
 * SCENE_SCRIPT 字段契约（数据结构.txt）：
 * - chunkId(=messageChunkId):   string
 * - scenes[].id:                string ⭐ ("1"/"2"/... 或 "scene_1"/...，**绝不可 Number()**)
 * - scenes[].startTime / endTime / duration: number
 * - scenes[].subjectRefs:       array
 * - scenes[].subjectRefs[].subjectId / fileId / url: string
 * - scenes[].description / visualPrompt: string
 */
const props = defineProps({
  scenes: {type: Array, default: () => []},
  canEdit: {type: Boolean, default: false},
  messageChunkId: {type: String, default: ''},
});
const emit = defineEmits(['scene-script-change']);
const MAX_SCENE_SCRIPT_REFS = 6;

const scenesCount = computed(() => props.scenes.length);

const normalizedScenes = computed(() =>
  props.scenes.map((s) => ({
    id: s.id,
    startTime: s.startTime,
    endTime: s.endTime,
    duration: s.duration,
    subjectRefs: s.subjectRefs,
    visualPrompt: s.visualPrompt,
    raw: s,
  })),
);

const editingId = ref(null);
const editingDraft = ref('');
const uploadingMap = ref({});

/**
 * @param {string} id 脚本行 id（SCENE_SCRIPT.id 后端 string）
 * @returns {boolean}
 */
const isEditing = (id) => editingId.value === id;

/** @param {number} n */
const pad2 = (n) => String(n).padStart(2, '0');
/**
 * @param {number} seconds startTime/endTime 后端 number
 * @returns {string}
 */
const formatTime = (seconds) => {
  const s = Math.max(0, Math.floor(seconds || 0));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${pad2(m)}:${pad2(sec)}`;
};
/**
 * @param {number} start
 * @param {number} end
 * @returns {string}
 */
const formatRange = (start, end) => `${formatTime(start)}-${formatTime(end)}`;

/**
 * 供 mention 替换使用：直接复用后端 subjectRefs。
 * subjectRefs[].subjectId 后端 string，作为提及 token 的规范字段。
 * @param {any} scene
 * @returns {any[]}
 */
const getSceneMentionSubjects = (scene) => scene.subjectRefs;

/**
 * 只读展示：把 visualPrompt 中的 @subjectId 替换成「@图片N」。
 * @param {any} scene normalized 行
 * @returns {string}
 */
const formatScenePromptDisplay = (scene) => {
  return replaceMentionValuesToLabels(scene.visualPrompt, getSceneMentionSubjects(scene), {idFields: ['subjectId']});
};

/**
 * el-mention 下拉选项（图片1…）。
 * @param {any} scene
 * @returns {{label:string, value:string}[]}
 */
const getSceneMentionOptions = (scene) => {
  return getSceneMentionSubjects(scene).map((_, idx) => ({
    label: `图片${idx + 1}`,
    value: `图片${idx + 1}`,
  }));
};

/**
 * 把 subjectRefs 浅克隆为 emit/上报用的快照副本（每个 item 是新对象，避免 splice/push 污染 emit payload）。
 * subjectRefs[].subjectId / fileId / url 后端皆 string。
 * @param {any[]} refs
 * @returns {{subjectId:string, fileId:string, url:string}[]}
 */
const cloneSubjectRefs = (refs) => (refs || []).map((item) => ({
  subjectId: item.subjectId,
  fileId: item.fileId,
  url: item.url,
}));

/**
 * 从提示词中提取 @图片N 对应的 subjectId 列表（去重，按出现顺序）。
 * @param {string} text 后端 visualPrompt 是 string
 * @param {any[]} subjects
 * @returns {string[]}
 */
const extractMentionSubjectIds = (text, subjects) => {
  const ids = [];
  const seen = new Set();
  const mentionRegexp = /@图片(\d+)/g;
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
 * 获取脚本行索引（优先 raw.scriptIdx，缺失时回退当前渲染下标）。
 * scriptIdx 是前端附加 number 字段，未必存在；不存在时 `>= 0` 自动 false 走回退。
 * @param {any} scene
 * @param {number} sceneIdx
 * @returns {number}
 */
const getSceneScriptIdx = (scene, sceneIdx) => {
  const rawIdx = scene.raw?.scriptIdx;
  return rawIdx >= 0 ? rawIdx : sceneIdx;
};

/**
 * 当前行 visualPrompt 解包（@图片N -> @subjectId）。
 * @param {any} scene
 * @param {string} prompt
 * @returns {string}
 */
const getUnwrappedPrompt = (scene, prompt) => {
  return (replaceMentionLabelsToValues(prompt || '', getSceneMentionSubjects(scene), {valueField: 'subjectId'}) || '').trim();
};

/**
 * 将当前脚本行最新内容同步给后端记录。
 * 注意：chunkId 后端契约 **string**（曾因 Number() 转换被 "scene_1" 打死，绝对不要再转）。
 * @param {any} scene
 * @param {number} sceneIdx 当前 v-for 渲染下标（number）
 * @param {{visualPrompt?: string, subjectRefs?: any[]}} payload
 * @returns {Promise<void>}
 */
const persistSceneScriptLine = async (scene, sceneIdx, payload = {}) => {
  const scriptIdx = getSceneScriptIdx(scene, sceneIdx);
  if (!props.messageChunkId || scriptIdx < 0) return;
  await updateSceneScript({
    chunkId: props.messageChunkId,
    scriptIdx,
    visualPrompt: payload.visualPrompt,
    subjectRefs: cloneSubjectRefs(payload.subjectRefs),
  });
};

/**
 * @param {any} scene
 * @returns {void}
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

/** @returns {void} */
const cancelEdit = () => {
  editingId.value = null;
  editingDraft.value = '';
};

/**
 * 保存编辑：保持提示词为面向人的包装文案（如 @图片N）。
 * @param {any} scene
 * @returns {void}
 */
const saveEdit = async (scene, sceneIdx = -1) => {
  if (!props.canEdit) return;

  // visualPrompt 后端 string；scene.id 后端 string
  const previousPrompt = scene.visualPrompt;
  const nextPrompt = (editingDraft.value || '').trim();
  if (!nextPrompt) return;

  const subjects = getSceneMentionSubjects(scene);
  const previousSubjectRefs = cloneSubjectRefs(scene.subjectRefs);
  const previousPromptUnwrapped = (replaceMentionLabelsToValues(previousPrompt, subjects, {valueField: 'subjectId'}) || '').trim();
  const nextPromptUnwrapped = (replaceMentionLabelsToValues(nextPrompt, subjects, {valueField: 'subjectId'}) || '').trim();

  if (scene.raw) {
    // 本地始终存储去包装后的提示词，与后端保持一致
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
  try {
    await persistSceneScriptLine(scene, sceneIdx, {
      visualPrompt: nextPromptUnwrapped,
      subjectRefs: scene.subjectRefs,
    });
  } catch (error) {
    ElMessage.warning(error?.message || '脚本保存已生效，后端记录同步失败');
  }
  cancelEdit();
};

/**
 * @param {any} scene
 * @returns {boolean}
 */
const isUploadingScene = (scene) => !!uploadingMap.value[scene.id];

/**
 * @param {any} scene
 * @param {boolean} uploading
 * @returns {void}
 */
const setUploadingScene = (scene, uploading) => {
  uploadingMap.value = {
    ...uploadingMap.value,
    [scene.id]: uploading,
  };
};

/**
 * 确保 `raw.subjectRefs` 为数组并返回引用。
 * @param {any} scene
 * @returns {any[]}
 */
const ensureSceneRefs = (scene) => {
  if (!scene.raw) return [];
  scene.raw.subjectRefs ||= [];
  return scene.raw.subjectRefs;
};

/**
 * 右侧叠图预览列表 URL。
 * subjectRefs 后端 array；subjectRefs[].url 后端 string。
 * @param {any} scene
 * @returns {string[]}
 */
const getSceneRefPreviewList = (scene) => scene.subjectRefs.map((item) => item.url);

/**
 * 删除指定分镜的单张参考图，并向父层上报 fileId 列表差异。
 * @param {any} scene
 * @param {number} idx
 * @returns {void}
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
    ElMessage.warning(error?.message || '参考图已更新，后端记录同步失败');
  }
};

/**
 * 上传参考图并追加到 `raw.subjectRefs`，emit 前后 fileId 列表。
 * @param {any} scene
 * @param {Event} event
 * @returns {Promise<void>}
 */
const handleSceneRefUpload = async (scene, event, sceneIdx = -1) => {
  if (!props.canEdit) return;
  const file = event?.target?.files?.[0];
  if (event?.target) event.target.value = '';
  if (!file) return;
  if (!isSupportedImageUpload(file)) {
    ElMessage.warning('仅支持 jpg/png 图片');
    return;
  }

  if (file.size / 1024 / 1024 >= 10) {
    ElMessage.warning('图片大小不能超过 10MB');
    return;
  }

  try {
    setUploadingScene(scene, true);
    const compressedFile = await compressImageBeforeUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile, compressedFile.name);
    const result = await uploadFile(formData);

    const refs = ensureSceneRefs(scene);
    const previousSubjectRefs = cloneSubjectRefs(refs);
    const createSubjectId = () => `subject_${Date.now()}_${Math.floor(Math.random() * 1000000)}`;
    refs.push({
      subjectId: createSubjectId(),
      fileId: result.fileId,
      url: result.fileUrl || URL.createObjectURL(compressedFile),
    });
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
    ElMessage.error(error?.message || '上传失败，请重试');
  } finally {
    setUploadingScene(scene, false);
  }
};
</script>

<style lang="scss">
@use '@/views/creation/styles/creationMention.scss';

.scene-ref-stack {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  padding-right: 2px;
}

.scene-ref-card {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
  margin-left: -30px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

  &:first-child {
    margin-left: 0;
  }

  &:hover {
    z-index: 50 !important;
    transform: translateY(-1px) scale(1.04);
    box-shadow: 0 10px 18px rgba(0, 0, 0, 0.35);
    border-color: rgba(194, 255, 0, 0.5);
  }
}


.scene-ref-add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.62);
  font-size: 20px;
  border-style: dashed;
  border-color: rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.1);
  z-index: 10;
  cursor: pointer;

  &.is-uploading {
    font-size: 12px;
    background: rgba(194, 255, 0, 0.16);
    border-color: rgba(194, 255, 0, 0.55);
    color: rgba(194, 255, 0, 0.95);
  }

}

.scene-script-textarea {
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

</style>

