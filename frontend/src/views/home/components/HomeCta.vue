<!--
  PC 首页「最终 CTA」板块：参照设计稿 #final-cta 视觉，Tailwind 优先实现。
  按钮点击平滑滚回页面顶部的输入区开始创作。文案走 i18n（home.cta.*）。
  仅多层渐变背景用一小段 scoped 样式（Tailwind 不便表达）。
-->
<template>
  <div class="max-w-[1280px] mx-auto px-10 mb-20">
    <div class="cta-box relative overflow-hidden rounded-[30px] border border-[#BEFA00]/30 px-[42px] py-16 text-center shadow-[0_30px_90px_rgba(0,0,0,0.42)]">
      <h2 class="text-[42px] leading-none tracking-[-0.025em] font-black text-white mb-4">
        {{ t('home.cta.title') }}
      </h2>
      <p class="text-[#b9c3ae] text-[14px] mb-6">{{ t('home.cta.desc') }}</p>
      <button
          type="button"
          class="inline-flex h-12 items-center rounded-full bg-[#BEFA00] text-black px-7 font-black cursor-pointer transition hover:brightness-105 hover:shadow-[0_0_24px_rgba(190,250,0,0.5)]"
          @click="scrollToTop"
      >
        {{ t('home.cta.button') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import {useI18nText} from '@/i18n';

const {t} = useI18nText();

/** 滚回首页顶部输入区开始创作。 */
const scrollToTop = () => {
  document.querySelector('.home-scroll-container')?.scrollTo({top: 0, behavior: 'smooth'});
};
</script>

<style scoped>
/* 多层径向 + 线性渐变，Tailwind 表达不便，单独写 */
.cta-box {
  background:
    radial-gradient(circle at 72% 20%, rgba(53, 232, 255, 0.15), transparent 32%),
    radial-gradient(circle at 28% 10%, rgba(191, 255, 0, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(18, 24, 16, 0.92), rgba(7, 8, 7, 0.98));
}
</style>
