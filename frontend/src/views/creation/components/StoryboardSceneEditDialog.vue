<template>
  <el-dialog
      :model-value="modelValue"
      width="936px"
      top="50px"
      :show-close="false"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      class="scene-edit-dialog"
      @update:model-value="(val) => emit('update:modelValue', val)"
  >
    <!-- 视频预览区：占据弹窗剩余高度 -->
    <div class="relative w-full h-[504px] shrink-0 rounded-[12px] overflow-hidden">
      <video v-if="scene?.videoFile?.fileUrl" :src="scene.videoFile.fileUrl" :poster="scene?.coverFile?.fileUrl" controls class="w-full h-full object-contain bg-black"></video>
      <img v-else-if="scene?.coverFile?.fileUrl" :src="scene.coverFile.fileUrl" class="w-full h-full object-cover"/>
      <div v-else class="w-full h-full flex-center text-white/30 text-sm">暂无预览</div>
      <div class="absolute top-3 left-4 text-white text-[18px] font-semibold drop-shadow-lg">
        分镜{{ sceneIndex + 1 }}
      </div>
    </div>

    <!-- 参考素材 + 分镜描述：左右分栏 -->
    <div class="flex-1 flex gap-10 mt-[10px]">
      <!-- 左侧：参考素材 -->
      <div class="w-[286px] shrink-0">
        <div class="text-white text-[18px] leading-[26px] mb-[10px]">参考素材</div>
        <div class="flex flex-wrap gap-2">
          <div v-for="(s, idx) in editingSubjects" :key="s.fileId || idx" class="relative w-[90px] aspect-square rounded-[8px] group">
            <el-image
                :src="s.fileUrl"
                :preview-src-list="editingSubjects.map(item => item.fileUrl)"
                :hide-on-click-modal="true"
                :initial-index="idx"
                preview-teleported
                fit="cover"
                class="w-full h-full rounded-[8px] cursor-pointer"
            />
            <a
                class="absolute bottom-1 right-1 w-[18px] h-[18px] rounded-[6px] flex-center bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity z-10"
                :href="s.fileUrl"
                download
                title="下载"
            >
              <svg-icon name="gy-download" size="12" color="#C2FF00"></svg-icon>
            </a>
            <button class="absolute top-1 right-1 w-[14px] h-[14px] rounded-full flex-center z-10 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity" @click="handleRemoveSubject(idx)">
              <svg-icon name="gy-closure" size="14" color="#C2FF00"></svg-icon>
            </button>
          </div>
          <!-- 上传按钮：最多 6 张参考图，满 6 张时隐藏 -->
          <label v-if="editingSubjects.length < 6" class="bg-[rgba(255,255,255,0.2)] w-[90px] aspect-square rounded-[10px] flex-center cursor-pointer">
            <input type="file" :accept="IMAGE_UPLOAD_ACCEPT" class="hidden" @change="handleAddSubject"/>
            <el-icon v-if="sceneSubjectUploading" class="animate-spin" color="#9C9C9C" :size="20">
              <Loading/>
            </el-icon>
            <svg-icon v-else name="gy-Add" size="20" color="#9C9C9C"></svg-icon>
          </label>
        </div>
      </div>

      <!-- 右侧：分镜描述 -->
      <div class="flex-1 flex flex-col">
        <div class="text-white text-[18px] leading-[26px] mb-[10px]">分镜描述</div>
        <el-mention
            v-model="editingPromptDisplay"
            type="textarea"
            :options="sceneMentionOptions"
            prefix="@"
            :autosize="false"
            placeholder="编辑你的分镜脚本..."
            class="scene-prompt-textarea"
            @select="handleSceneMentionSelect"
        />
        <div class="scene-action-row mt-[10px] shrink-0">
          <div class="scene-action-row__model">
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
                <button type="button" class="scene-model-trigger">
                  <span class="scene-model-trigger__label">模型：</span>
                  <span class="scene-model-trigger__value">{{ getModelLabel(sceneModel) }}</span>
                  <el-icon class="scene-model-trigger__arrow">
                    <ArrowDown/>
                  </el-icon>
                </button>
              </template>
              <div class="el-select-dropdown__header">模型</div>
              <div
                  v-for="item in modelStore.modelOptions"
                  :key="item.value"
                  class="el-select-dropdown__item"
                  :class="{ 'is-selected': item.value === sceneModel }"
                  @click="handleSelectModel(item.value)"
              >
                <span class="scene-model-item-main">{{ item.label }}</span>
                <span class="scene-model-item-meta">{{ item.costText }}</span>
              </div>
            </el-popover>
          </div>
          <button
              class="scene-action-row__save bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] rounded-[20px] flex-center text-black text-[16px] font-semibold transition-colors cursor-pointer"
              @click="handleSave"
          >
            保存{{ scenePointsPrice != null ? ` ｜${scenePointsPrice}积分` : '' }}
          </button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
import {ArrowDown, Loading} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import {getPointsPrice, uploadFile} from '@/api/creation';
import {useModelStore} from '@/store/model';
import {replaceMentionLabelsToValues, replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {compressImageBeforeUpload, IMAGE_UPLOAD_ACCEPT, isSupportedImageUpload} from '@/views/creation/utils/upload.js';

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  scene: {type: Object, default: null},
  sceneIndex: {type: Number, default: -1},
  canEditCreation: {type: Boolean, default: false},
});

const emit = defineEmits(['update:modelValue', 'save']);

const editingPrompt = ref('');
const editingPromptDisplay = ref('');
const editingSubjects = ref([]);
const sceneSubjectUploading = ref(false);
const scenePointsPrice = ref(null);
const modelPopoverVisible = ref(false);
const DEFAULT_SCENE_MODEL = 'VIDUQ2';
const sceneModel = ref(DEFAULT_SCENE_MODEL);

const sceneMentionOptions = computed(() => editingSubjects.value.map((s, idx) => ({
  label: `图片${idx + 1}`,
  value: `图片${idx + 1}`,
})));

/**
 * 将数字/时间字符串解析为秒数。
 * 支持：`12`、`"12"`、`"00:12"`、`"00:00:12"`。
 * @param {unknown} value
 * @returns {number}
 */
const parseDurationSeconds = (value) => {
  const direct = Number(value);
  if (Number.isFinite(direct) && direct > 0) return direct;
  if (typeof value !== 'string') return 0;
  const text = value.trim();
  if (!text) return 0;
  if (/^\d+(\.\d+)?$/.test(text)) return Number(text);
  if (!text.includes(':')) return 0;
  const parts = text.split(':').map((part) => Number(part));
  if (!parts.length || parts.some((part) => !Number.isFinite(part) || part < 0)) return 0;
  const padded = parts.length > 3 ? parts.slice(-3) : parts;
  if (padded.length === 3) return padded[0] * 3600 + padded[1] * 60 + padded[2];
  if (padded.length === 2) return padded[0] * 60 + padded[1];
  return padded[0];
};

/**
 * 解析分镜时长秒数，优先区间差值，再尝试 duration/timeRange 等字段。
 * @param {any} scene
 * @returns {number}
 */
const resolveSceneDurationSeconds = (scene) => {
  const start = Number(scene?.startTime ?? NaN);
  const end = Number(scene?.endTime ?? NaN);
  if (Number.isFinite(start) && Number.isFinite(end) && end > start) {
    return end - start;
  }

  const fromDuration = parseDurationSeconds(scene?.duration);
  if (fromDuration > 0) return fromDuration;

  const timeRangeText = typeof scene?.timeRange === 'string' ? scene.timeRange.trim() : '';
  if (!timeRangeText || !timeRangeText.includes('-')) return 0;
  const [startText = '', endText = ''] = timeRangeText.split('-', 2);
  const startSec = parseDurationSeconds(startText);
  const endSec = parseDurationSeconds(endText);
  if (endSec > startSec) return endSec - startSec;
  return 0;
};

const sceneDuration = computed(() => {
  return Math.max(0, Math.round(resolveSceneDurationSeconds(props.scene)));
});

/**
 * 从 props.scene 同步本地编辑态（提示词 + 参考图列表）。
 * @returns {void}
 */
const initializeByScene = () => {
  editingPrompt.value = props.scene?.visualPrompt || '';
  editingSubjects.value = [...(props.scene?.subject || [])];
  editingPromptDisplay.value = replaceMentionValuesToLabels(editingPrompt.value, editingSubjects.value);
  sceneModel.value = props.scene?.model || DEFAULT_SCENE_MODEL;
  scenePointsPrice.value = null;
};

/** 弹窗打开且 scene 可用时：同步本地编辑态（不包含积分查询）。 */
const syncLocalStateIfReady = () => {
  if (!props.scene) return;
  initializeByScene();
};

/**
 * 按分镜时长与模型查询保存所需积分预估。
 * @returns {Promise<void>}
 */
const queryScenePointsPrice = async () => {
  if (!props.modelValue || !props.scene) return;
  if (!sceneDuration.value || !sceneModel.value) {
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

const modelStore = useModelStore();

const getModelLabel = (modelValue) => {
  const matched = modelStore.modelOptions.find((item) => item.value === modelValue);
  return matched?.label || 'Vidu Q2';
};

const handleSelectModel = (modelValue) => {
  sceneModel.value = modelValue;
  modelPopoverVisible.value = false;
};

/**
 * 上传参考图并追加到 `editingSubjects`。
 * @param {Event} e
 * @returns {Promise<void>}
 */
const handleAddSubject = async (e) => {
  if (!props.canEditCreation) return;
  const file = e.target.files?.[0];
  e.target.value = '';
  if (!file) return;
  if (!isSupportedImageUpload(file)) {
    ElMessage.error('参考图仅支持 jpg/png 格式');
    return;
  }
  sceneSubjectUploading.value = true;
  try {
    const compressedFile = await compressImageBeforeUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile, compressedFile.name);
    const res = await uploadFile(formData);
    const localSubjectId = `SUB_${Date.now().toString(36).slice(-4).toUpperCase()}_${Math.random().toString(36).slice(2, 5).toUpperCase()}`;
    editingSubjects.value.push({subjectId: localSubjectId, fileId: res.fileId, fileUrl: res.fileUrl});
    editingPromptDisplay.value = replaceMentionValuesToLabels(editingPromptDisplay.value, editingSubjects.value);
  } catch (error) {
    ElMessage.error(error?.message || '上传失败，请重试');
  } finally {
    sceneSubjectUploading.value = false;
  }
};

/**
 * @param {number} index
 * @returns {void}
 */
const handleRemoveSubject = (index) => {
  if (!props.canEditCreation) return;
  editingSubjects.value.splice(index, 1);
  editingPromptDisplay.value = replaceMentionValuesToLabels(editingPromptDisplay.value, editingSubjects.value);
};

/**
 * mention 选中后刷新展示层（与占位 subjectId 对齐）。
 * @param {any} option
 * @returns {void}
 */
const handleSceneMentionSelect = (option) => {
  if (!option?.value) return;
  editingPromptDisplay.value = replaceMentionValuesToLabels(editingPromptDisplay.value, editingSubjects.value);
};

/**
 * 将编辑结果 emit 给父组件并关闭弹窗。
 * @returns {void}
 */
const handleSave = () => {
  if (!props.canEditCreation || !props.scene) return;
  editingPrompt.value = replaceMentionLabelsToValues(editingPromptDisplay.value, editingSubjects.value);
  emit('save', {
    sceneId: props.scene.sceneId,
    visualPrompt: editingPrompt.value,
    subject: [...editingSubjects.value],
    model: sceneModel.value,
  });
  emit('update:modelValue', false);
};

watch(
    () => props.modelValue,
    async (visible) => {
      if (!visible || !props.scene) return;
      syncLocalStateIfReady();
    },
    {immediate: true},
);

watch(
    () => props.scene,
    (scene) => {
      if (!props.modelValue || !scene) return;
      syncLocalStateIfReady();
    },
);

watch(
    () => [props.modelValue, props.scene?.sceneId, sceneModel.value, sceneDuration.value],
    async ([visible]) => {
      if (!visible) return;
      await queryScenePointsPrice();
    },
);
</script>

<style lang="scss">
@use '@/styles/modelSelectDropdown.scss' as *;

.scene-edit-dialog {
  background: #383838;
  border-radius: 20px;
  padding: 20px;

  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 0;
  }
}

.el-mention {
  flex: 1;
}

.scene-prompt-textarea {
  .el-mention__editor,
  .el-textarea__inner,
  textarea {
    background: rgba(255, 255, 255, 0.1) !important;
    border: none !important;
    border-radius: 10px !important;
    color: #FFFFFF !important;
    font-size: 14px !important;
    line-height: 22px !important;
    padding: 12px !important;
    resize: none !important;
    box-shadow: none !important;
    height: 100% !important;

    &::placeholder {
      color: rgba(255, 255, 255, 0.4) !important;
    }

    &:focus {
      box-shadow: none !important;
    }

    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.06);
      border-radius: 9999px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(194, 255, 0, 0.42);
      border-radius: 9999px;
      border: 2px solid transparent;
      background-clip: padding-box;

      &:hover {
        background: rgba(194, 255, 0, 0.66);
        background-clip: padding-box;
      }
    }
  }

  .el-mention__editor {
    width: 100% !important;
  }

  &.el-mention,
  &.el-textarea {
    height: 100% !important;
  }

  .el-textarea__inner,
  textarea {
    min-height: 100% !important;
  }
}

.scene-action-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .scene-action-row__model {
    flex: 0 0 36%;
    min-width: 0;
  }

  .scene-action-row__save {
    flex: 1;
    height: 44px;
  }
}
</style>
