<!--
  PC 首页页脚：参照设计稿 footer 视觉，Tailwind 优先实现。
  左品牌 + 社交图标，右四列导航，底部 lime 版权条。
  导航链接已对接本项目真实路由（设计稿里链接指向不存在的静态页，已替换为应用内页面）；
  社交为外链。文案走 i18n（home.footer.*）。
-->
<template>
  <footer class="text-[#6f7867] text-[12px]">
    <div class="grid grid-cols-1 lg:grid-cols-[1fr_2fr] border-t border-white/[0.08]">
      <!-- 左：品牌 + 社交 -->
      <div class="flex flex-col justify-between gap-8 px-6 lg:px-[56px] py-16 border-b lg:border-b-0 lg:border-r border-white/[0.08]">
        <div>
          <div class="text-[40px] font-black tracking-[-0.04em] leading-none text-white">OhYesAI</div>
          <div class="text-[#BEFA00] text-[14px] font-bold mt-1.5">{{ t('home.footer.tagline') }}</div>
        </div>
        <!-- 社交外链仅海外（英文）版展示，中文版隐藏 -->
        <div v-if="lang === 'en'" class="flex flex-wrap gap-2.5">
          <a
              v-for="s in socials"
              :key="s.title"
              :href="s.href"
              target="_blank"
              rel="noopener"
              :title="s.title"
              class="w-[38px] h-[38px] rounded-[9px] border border-white/10 flex items-center justify-center text-white/45 transition hover:border-[#BEFA00]/60 hover:text-[#BEFA00] hover:bg-[#BEFA00]/[0.07]"
          >
            <svg viewBox="0 0 24 24" class="w-[18px] h-[18px] fill-current"><path :d="s.d"/></svg>
          </a>
        </div>
      </div>

      <!-- 右：四列导航 -->
      <nav class="grid grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-10 px-6 lg:px-[56px] py-16 content-start">
        <div v-for="col in cols" :key="col.title">
          <h4 class="text-[14px] font-black tracking-[0.12em] uppercase text-white mb-[18px] pb-3 border-b border-white/[0.07]">{{ col.title }}</h4>
          <template v-for="link in col.links" :key="link.label">
            <router-link
                v-if="link.to"
                :to="link.to"
                class="block text-[#72786c] text-[14px] mb-[11px] leading-snug transition hover:text-white"
            >{{ link.label }}</router-link>
            <a
                v-else
                :href="link.href"
                target="_blank"
                rel="noopener"
                class="block text-[#72786c] text-[14px] mb-[11px] leading-snug transition hover:text-white"
            >{{ link.label }}</a>
          </template>
        </div>
      </nav>
    </div>
  </footer>
</template>

<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import {useI18nText} from '@/i18n';

const route = useRoute();
const {t} = useI18nText();

/** 当前语言前缀，用于拼应用内路由。 */
const lang = computed(() => (route.params.lang === 'en' ? 'en' : 'zh'));

/** 社交外链 + 图标路径（单 path，取自设计稿）。 */
const socials = [
  {title: 'Discord', href: 'https://discord.gg/ohyesai', d: 'M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z'},
  {title: 'YouTube', href: 'https://youtube.com/@ohyesai', d: 'M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z'},
  {title: 'X / Twitter', href: 'https://x.com/ohyesai', d: 'M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-4.714-6.231-5.401 6.231H2.748l7.73-8.835L1.254 2.25H8.08l4.259 5.631 5.905-5.631zm-1.161 17.52h1.833L7.084 4.126H5.117z'},
  {title: 'TikTok', href: 'https://tiktok.com/@ohyesai', d: 'M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-2.88 2.5 2.89 2.89 0 0 1-2.89-2.89 2.89 2.89 0 0 1 2.89-2.89c.28 0 .54.04.79.1V9.01a6.33 6.33 0 0 0-.79-.05 6.34 6.34 0 0 0-6.34 6.34 6.34 6.34 0 0 0 6.34 6.34 6.34 6.34 0 0 0 6.33-6.34V8.69a8.17 8.17 0 0 0 4.78 1.52V6.75a4.85 4.85 0 0 1-1.01-.06z'},
];

/** 导航列：仅保留产品(定价) / 资源(常见问题·使用指南) / 法务(隐私·条款)，均对接应用内真实页面。 */
const cols = computed(() => [
  {
    title: t('home.footer.colProduct'),
    links: [
      {label: t('home.footer.lPricing'), to: `/${lang.value}/subscribe`},
    ],
  },
  {
    title: t('home.footer.colResources'),
    links: [
      {label: t('home.footer.lFaq'), to: `/${lang.value}/faq`},
      {label: t('home.footer.lGuide'), to: `/${lang.value}/guide`},
    ],
  },
  {
    title: t('home.footer.colLegal'),
    links: [
      {label: t('home.footer.lPrivacy'), to: `/${lang.value}/policy/privacy`},
      {label: t('home.footer.lTerms'), to: `/${lang.value}/policy/terms`},
    ],
  },
]);
</script>
