import {defineStore} from 'pinia';
import {ref, computed, watch} from 'vue';

const MODEL_STORAGE_KEY = 'ohyesai_selected_model';
const DEFAULT_MODEL = 'VIDUQ2';
const RESOLUTION_STORAGE_KEY = 'ohyesai_selected_resolution';
const DEFAULT_RESOLUTION = 'P720';

// 模型选项按分辨率分两套：后端枚举 value 不变，仅展示名 / 单价 / 可选项随分辨率变化。
export const MODEL_OPTIONS_BY_RESOLUTION = {
  P720: [
    {label: 'Vidu Q2', value: 'VIDUQ2', costText: '45积分/秒（720P）'},
    {label: 'Kling V3 Omni Std', value: 'KLING_V3_OMNI', costText: '70积分/秒（720P）'},
    {label: 'Seedance 2.0 Fast', value: 'SEEDANCE_2_FAST', costText: '90积分/秒（720P）'},
    {label: 'Seedance 2.0', value: 'SEEDANCE_2', costText: '120积分/秒（720P）'},
  ],
  P1080: [
    {label: 'Vidu Q2', value: 'VIDUQ2', costText: '90积分/秒（1080P）'},
    {label: 'Kling V3 Omni Pro', value: 'KLING_V3_OMNI', costText: '90积分/秒（1080P）'},
    {label: 'Seedance 2.0', value: 'SEEDANCE_2', costText: '280积分/秒（1080P）'},
  ],
};

// 分辨率选项：label 展示名，value 后端枚举值，tag 清晰度标签（720P=HD，1080P=FHD）。
export const RESOLUTION_OPTIONS = [
  {label: '720P', value: 'P720', tag: 'HD'},
  {label: '1080P', value: 'P1080', tag: 'FHD'},
];

/** 按分辨率取模型选项；未知分辨率回退默认分辨率。 */
export const getModelOptions = (resolution) => MODEL_OPTIONS_BY_RESOLUTION[resolution] || MODEL_OPTIONS_BY_RESOLUTION[DEFAULT_RESOLUTION];

export const useModelStore = defineStore('model', () => {
  const selectedModel = ref(localStorage.getItem(MODEL_STORAGE_KEY) || DEFAULT_MODEL);
  const selectedResolution = ref(localStorage.getItem(RESOLUTION_STORAGE_KEY) || DEFAULT_RESOLUTION);
  // 当前会话锁定的分辨率：新会话首条消息发送时锁定为当时所选、已有会话来自 history-message；
  // null 表示尚未发送首条消息的新会话 / 首页，此时模型选项跟随 selectedResolution。
  const conversationResolution = ref(null);

  // 当前生效分辨率：已有会话用其锁定值，否则用用户所选。
  const effectiveResolution = computed(() => conversationResolution.value || selectedResolution.value);
  // 当前分辨率下的模型选项列表，供所有模型选择框使用。
  const modelOptions = computed(() => getModelOptions(effectiveResolution.value));

  const setSelectedModel = (value) => {
    selectedModel.value = value || DEFAULT_MODEL;
  };

  const setSelectedResolution = (value) => {
    selectedResolution.value = value || DEFAULT_RESOLUTION;
  };

  const setConversationResolution = (value) => {
    conversationResolution.value = value || null;
  };

  // 生效分辨率变化后，若当前所选模型在新分辨率下不存在（如 720P 的 Seedance Fast 切到 1080P），回退到该分辨率首个模型。
  watch(
    effectiveResolution,
    () => {
      if (!modelOptions.value.some((item) => item.value === selectedModel.value)) {
        selectedModel.value = modelOptions.value[0].value;
      }
    },
    {immediate: true},
  );

  watch(
    selectedModel,
    (value) => {
      localStorage.setItem(MODEL_STORAGE_KEY, value || DEFAULT_MODEL);
    },
    {immediate: true},
  );

  watch(
    selectedResolution,
    (value) => {
      localStorage.setItem(RESOLUTION_STORAGE_KEY, value || DEFAULT_RESOLUTION);
    },
    {immediate: true},
  );

  return {
    selectedModel,
    setSelectedModel,
    selectedResolution,
    setSelectedResolution,
    conversationResolution,
    setConversationResolution,
    effectiveResolution,
    modelOptions,
  };
});
