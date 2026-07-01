<!--
  PC 首页「定价」板块：参照设计稿 #pricing 视觉，按本项目 Tailwind 优先风格实现。
  - 标题区按需求不要小标题（kicker）；左标题、右描述。
  - 四个套餐卡（注册体验 / Basic / Standard / Pro），Standard 高亮边框。
  文案走 i18n（home.pricing.*）；特性以英文逗号分隔后拆数组。
-->
<template>
  <section class="bg-[#0a0a0a]">
    <div class="max-w-[1280px] mx-auto px-10 pt-[46px] pb-[84px]">
      <!-- 标题区（无 kicker 小标题） -->
      <div class="flex flex-col md:flex-row md:items-end md:justify-between gap-6 mb-7">
        <h2 class="text-[42px] font-black tracking-[-0.02em] leading-[1.02] text-white">
          {{ t('home.pricing.title') }}
        </h2>
        <p class="text-[#aeb8a4] text-[15px] leading-[1.6] whitespace-nowrap">{{ t('home.pricing.desc') }}</p>
      </div>

      <!-- 四个套餐卡 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div
            v-for="(plan, i) in plans"
            :key="i"
            class="min-h-[210px] rounded-[22px] border border-white/10 p-[22px] bg-[linear-gradient(180deg,rgba(18,23,17,0.9),rgba(7,8,7,0.96))] cursor-pointer transition-[transform,border-color] duration-200 hover:-translate-y-1 hover:border-[#BEFA00]/35"
            @click="goSubscribe"
        >
          <div class="text-[21px] font-black leading-none" :class="plan.free ? 'text-[#8a948a]' : 'text-[#BEFA00]'">
            {{ plan.price }}<span v-if="plan.unit" class="text-[14px] font-normal text-[#8a948a]">{{ plan.unit }}</span>
          </div>
          <h3 class="mt-3 mb-2 text-[17px] font-bold text-white">{{ plan.name }}</h3>
          <p class="text-[#a7b19d] text-[14px] leading-[1.6]">{{ plan.desc }}</p>
          <ul class="mt-3.5 grid gap-1.5">
            <li
                v-for="(feat, fi) in plan.features"
                :key="fi"
                class="flex items-center gap-2 text-[12px] font-extrabold text-[#d4ddca]"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-[#BEFA00] shrink-0"></span>{{ feat }}
            </li>
          </ul>
        </div>
      </div>

      <p class="text-center mt-4 text-[#8a948a] text-[13px]">{{ t('home.pricing.note') }}</p>
    </div>
  </section>
</template>

<script setup>
import {computed} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();
const router = useRouter();
const route = useRoute();

/** 当前语言前缀。 */
const lang = computed(() => (route.params.lang === 'en' ? 'en' : 'zh'));
/** 套餐卡点击：跳转订阅页。 */
const goSubscribe = () => router.push(`/${lang.value}/subscribe`);

/** 四个套餐：价格/单位/名称/描述/特性（均来自 i18n）；free 控制价格配色（仅 hover 才高亮边框，无默认高亮）。 */
const plans = computed(() => [1, 2, 3, 4].map((n) => ({
  price: t(`home.pricing.p${n}Price`),
  unit: t(`home.pricing.p${n}Unit`),
  name: t(`home.pricing.p${n}Name`),
  desc: t(`home.pricing.p${n}Desc`),
  features: t(`home.pricing.p${n}Features`).split(','),
  free: n === 1,
})));
</script>
