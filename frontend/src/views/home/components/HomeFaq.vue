<!--
  PC 首页「常见问题」板块：参照设计稿 #faq 的视觉，按本项目 Tailwind 优先风格重写。
  - 居中标题（按需求不要标题上方的小字 kicker）。
  - 每条独立展开/收起：点击只在原地向下展开，不会因为收起别的条目而把页面上推。
  - 展开动画用 grid-template-rows 0fr→1fr，按真实内容高度展开，不写死高度、不裁剪。
  文案走 i18n（home.faq.*）。
-->
<template>
  <section class="bg-[#0a0a0a]">
    <div class="max-w-[1280px] mx-auto px-10 pt-[46px] pb-[84px]">
      <h2 class="text-center text-[42px] font-black tracking-[-0.02em] text-white mb-7">
        {{ t('home.faq.title') }}
      </h2>

      <div class="flex flex-col gap-2.5">
        <div
            v-for="(item, i) in items"
            :key="i"
            class="rounded-[14px] border bg-[#0a0c09]/70 transition-colors"
            :class="open[i] ? 'border-[#BEFA00]/25' : 'border-white/10'"
        >
          <button
              type="button"
              class="w-full flex items-center justify-between gap-3 px-6 py-5 text-left text-[15px] font-extrabold text-[#eef4e6] cursor-pointer"
              @click="toggle(i)"
          >
            <span>{{ item.q }}</span>
            <span
                class="shrink-0 text-[#BEFA00] text-[24px] leading-none transition-transform duration-200"
                :class="{ 'rotate-45': open[i] }"
            >+</span>
          </button>

          <!-- 折叠面板：用 Element Plus 折叠过渡按真实内容高度向下平滑展开（独立切换，不会上推页面） -->
          <el-collapse-transition>
            <div v-show="open[i]">
              <p class="px-6 pb-5 text-[14px] leading-[1.75] text-[#aeb8a3]">{{ item.a }}</p>
            </div>
          </el-collapse-transition>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import {reactive, computed} from 'vue';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();

/** 九条问答（均来自 i18n）。 */
const items = computed(() => Array.from({length: 9}, (_, i) => ({
  q: t(`home.faq.q${i + 1}`),
  a: t(`home.faq.a${i + 1}`),
})));

/** 各条目展开状态（key=下标，各条独立，默认展开第一条）。 */
const open = reactive({0: true});

/** 点击切换某条的展开/收起，不影响其它条目。 */
const toggle = (i) => {
  open[i] = !open[i];
};
</script>
