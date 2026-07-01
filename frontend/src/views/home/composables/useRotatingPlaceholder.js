import {computed, onBeforeUnmount, onMounted, ref} from 'vue';

/**
 * 首页输入框占位词轮播逻辑（PC / Mobile 复用）。
 * @param {() => string[]} getPlaceholders - 返回占位词列表的方法（可响应语言切换）
 * @param {number} [rotationMs=3000] - 轮播间隔（毫秒）
 */
export const useRotatingPlaceholder = (getPlaceholders, rotationMs = 3000) => {
  const placeholderIndex = ref(0);
  const isComposing = ref(false);
  let placeholderTimerId = 0;

  const rotatingPlaceholders = computed(() => {
    const list = getPlaceholders?.();
    return Array.isArray(list) ? list : [];
  });

  const rotatingPlaceholder = computed(() => {
    const list = rotatingPlaceholders.value;
    return list[placeholderIndex.value] || list[0] || '';
  });

  /** 中文输入法组合输入开始：隐藏滚动占位词，避免与拼音预编辑重叠。 */
  const handleCompositionStart = () => {
    isComposing.value = true;
  };

  /** 中文输入法组合输入结束：恢复占位词显示判定。 */
  const handleCompositionEnd = () => {
    isComposing.value = false;
  };

  onMounted(() => {
    placeholderTimerId = window.setInterval(() => {
      const list = rotatingPlaceholders.value;
      if (!list.length) return;
      placeholderIndex.value = (placeholderIndex.value + 1) % list.length;
    }, rotationMs);
  });

  onBeforeUnmount(() => {
    if (placeholderTimerId) {
      clearInterval(placeholderTimerId);
      placeholderTimerId = 0;
    }
  });

  return {
    placeholderIndex,
    rotatingPlaceholder,
    isComposing,
    handleCompositionStart,
    handleCompositionEnd,
  };
};
