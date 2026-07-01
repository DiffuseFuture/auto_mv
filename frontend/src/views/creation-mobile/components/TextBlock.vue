<!-- 文本消息 -->
<template>
  <div
    v-if="hasText"
    class="mb-3 w-fit max-w-full break-words rounded-[10px] rounded-tl-none bg-[#5D634A]/50 p-[10px] text-[15px] leading-[21px] text-white"
  >
    <div
      class="mobile-markdown-body markdown-body markdown-body-chat w-full max-w-full"
      v-html="renderCreationMarkdown(block.text)"
    ></div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {isNotEmpty} from '@/utils/index.js';
import {renderCreationMarkdown} from '@/views/creation/utils/creationMarkdown.js';

/**
 * 移动端文本消息块：渲染普通文本 / Markdown 内容。
 * 「正在生成中」的三点动画已抽到 CreationMessageList 顶层独立气泡，
 * 避免 SCENE_SCRIPT 等非 TEXT 块成为消息末块时动画消失。
 */
const props = defineProps({
  /** 当前消息块，约定使用 `block.text` 作为正文。 */
  block: {type: Object, required: true},
});

/** 当前文本块是否存在可展示的正文内容。 */
const hasText = computed(() => isNotEmpty(props.block?.text));
</script>

