<!-- 歌词卡片 -->
<template>
  <div class="mb-3">
    <div class="rounded-[10px] overflow-hidden bg-[#81876E]/50">
      <!-- 顶部 -->
      <div class="h-[32px] flex-between pl-[7px] pr-[12px] bg-[#F0F6DD]">
        <div class="flex items-center gap-[8px] text-[#0b0b0b] text-[15px] font-bold">
          <svg-icon name="gy-lyrics" size="12"></svg-icon>
          <span>{{ t('creation.messageArea.lyricsTitle') }}</span>
        </div>
        <button
          aria-label="copy-lyrics"
          @click="handleCopy"
        >
          <svg-icon name="gy-copy" size="12" color="#000000"></svg-icon>
        </button>
      </div>

      <!-- 内容 -->
      <div class="p-[12px] max-h-[420px] overflow-y-auto lyrics-scrollbar">
        <div class="text-white text-[12px] leading-[17px] whitespace-pre-wrap">{{ block.text }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ElMessage} from 'element-plus';
import {useI18nText} from '@/i18n/index.js';

/**
 * 移动端歌词块：
 * - 展示 AI 返回的歌词内容
 * - 提供一键复制歌词能力
 */
const props = defineProps({
  /** 当前消息块，约定使用 `block.text` 作为歌词正文。 */
  block: {type: Object, required: true},
});

const {t} = useI18nText();

/**
 * 复制歌词文本到剪贴板。
 * block.text 由 appendBlock(msg, 'LYRICS', data.lyrics) 写入，后端契约为 string。
 * 无有效文本时直接返回；复制失败时给出错误提示。
 */
const handleCopy = async () => {
  const text = (props.block?.text || '').trim();
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(t('creation.messageArea.copiedWithLabel', {label: t('creation.messageArea.lyricsTitle')}));
  } catch {
    ElMessage.error(t('share.copyFailed'));
  }
};
</script>

<style scoped lang="scss">
.lyrics-scrollbar {
  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.45);
    border-radius: 999px;
  }
}
</style>

