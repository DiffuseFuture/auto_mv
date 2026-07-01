<!-- 常见问题页面：Hero + 顶部 Tab 跳转 + 三组手风琴问答（基于 el-collapse） -->
<template>
  <div ref="pageRef" class="faq-page w-full h-full overflow-y-auto text-gray-100 bg-[#050d03]" :class="{'min-screen': !isMobile}">
    <!-- 返回按钮：固定在视口左上角、与标题垂直对齐，滚动时常驻（独立页面无主壳，自带返回入口） -->
    <button class="fixed left-4 top-[84px] z-40 w-10 h-10 rounded-full flex-center hover:opacity-90 cursor-pointer" @click="handleBack">
      <svg-icon name="gy-return" size="34" color="#FFFFFF"></svg-icon>
    </button>

    <!-- HERO -->
    <section
        class="relative overflow-hidden pt-20 pb-14 px-4"
        style="background: radial-gradient(ellipse 120% 70% at 50% -10%, #2a4a10 0%, #0f2508 35%, #060d04 70%, #050d03 100%);"
    >
      <div class="relative z-10 max-w-2xl mx-auto text-center">
        <h1 class="text-[48px] font-extrabold tracking-tight leading-none bg-gradient-to-br from-[#e3ff80] via-[#C2FF00] to-[#7fae00] bg-clip-text text-transparent">
          {{ faqData.heroTitle }}
        </h1>
        <p class="text-lg text-gray-400 mt-4 max-w-lg mx-auto leading-relaxed">
          {{ faqData.heroSubtitle }}
        </p>
      </div>
    </section>

    <!-- 顶部 Tab 跳转条（粘性） -->
    <div class="sticky top-0 z-30 backdrop-blur-[10px] bg-[rgba(5,13,3,0.9)] border-b border-white/[0.07] px-4">
      <div class="max-w-2xl mx-auto flex">
        <button
            v-for="cat in faqData.categories"
            :key="cat.id"
            class="bg-transparent border-0 border-b-2 border-solid cursor-pointer text-[0.9rem] font-medium px-4 py-[0.65rem] whitespace-nowrap transition-colors"
            :class="activeTab === cat.id
              ? 'text-[#C2FF00] border-[#C2FF00]'
              : 'text-[#777] border-transparent hover:text-[#ccc]'"
            @click="handleTabClick(cat.id)"
        >
          {{ cat.tabLabel }}
        </button>
      </div>
    </div>

    <!-- FAQ 主体：每个分组单独一个 el-collapse(accordion)，分组之间独立展开 -->
    <section class="py-16 px-4 bg-[#080f06]">
      <div class="max-w-2xl mx-auto">
        <div
            v-for="cat in faqData.categories"
            :id="`faq-section-${cat.id}`"
            :key="cat.id"
            class="mb-10"
        >
          <p class="text-[1.05rem] font-bold text-[#C2FF00] border-l-[3px] border-[#C2FF00] pl-3 mb-3">
            {{ cat.title }}
          </p>

          <el-collapse
              v-model="openMap[cat.id]"
              accordion
              class="faq-collapse"
          >
            <el-collapse-item
                v-for="(item, idx) in cat.items"
                :key="idx"
                :name="idx"
            >
              <template #title>
                <!--
                  自己包一层 flex 容器：不依赖 el-collapse-item__header 内部布局/字号。
                  字号直接写在 question span 上（text-[18px]）：EP 内部会把 slot 包到 .el-collapse-item__header__title 里，
                  cascade 下来的 font-size 仍会被外层若干 EP 节点的 var(--el-collapse-header-font-size) 覆盖；
                  直接在叶子节点声明是最稳的——浏览器 cascade 末端不会再被任何 EP 内部规则改写。
                -->
                <div class="flex items-center justify-between gap-4 w-full">
                  <span class="flex-1 min-w-0 text-[18px] font-semibold leading-[1.5]">{{ item.q }}</span>
                  <span
                      class="shrink-0 w-5 h-5 rounded-full border-[1.5px] flex items-center justify-center text-[0.75rem] transition-all"
                      :class="openMap[cat.id] === idx
                        ? 'border-[#C2FF00] text-[#C2FF00] bg-[rgba(194,255,0,0.1)] rotate-45'
                        : 'border-white/20 text-[#888]'"
                  >＋</span>
                </div>
              </template>
              <div class="faq-answer-content text-[16px] leading-[1.75] text-[#9ca3af]" v-html="item.a"></div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import {computed, reactive, ref} from 'vue';
import {useI18nText} from '@/i18n';
import {useSafeBack} from '@/composables/useSafeBack.js';
import {isMobileClient} from '@/utils/index.js';
import {FAQ_MAP} from './faq-data.js';

const {locale} = useI18nText();
const safeBack = useSafeBack();

/** PC 端套用全局 1280px 最小宽度（.min-screen）；移动端不限制，保持响应式。 */
const isMobile = isMobileClient();

/** 当前 locale 对应的 FAQ 数据；找不到时回退到 zh-CN */
const faqData = computed(() => FAQ_MAP[String(locale.value)] || FAQ_MAP['zh-CN']);

/** 当前激活的分类 Tab，默认第一个 */
const activeTab = ref(faqData.value.categories[0]?.id);

/**
 * 每个分组当前展开的 item 索引（accordion 模式下值为 number 或 ''）。
 * 用 reactive 让 v-model="openMap[cat.id]" 对动态 key 生效。
 */
const openMap = reactive({});

const pageRef = ref(null);

const handleBack = () => safeBack();

/** Tab 点击：高亮当前 + 在页面滚动容器中平滑滚到对应分组 */
const handleTabClick = (catId) => {
  activeTab.value = catId;
  const target = document.getElementById(`faq-section-${catId}`);
  const container = pageRef.value;
  if (!target || !container) return;
  // sticky 顶部条 + Hero 余量，避免标题被顶栏遮挡
  const offset = 55;
  const top = target.getBoundingClientRect().top - container.getBoundingClientRect().top + container.scrollTop - offset;
  container.scrollTo({top, behavior: 'smooth'});
};
</script>

<!--
  SCSS 只承担两件 Tailwind 无法表达的事：
  1. el-collapse 内部样式覆写（必须用 :deep() 穿透 scoped）
  2. v-html 渲染出的富文本内部元素样式
  3. 自定义滚动条 ::-webkit-scrollbar
-->
<style lang="scss" scoped>
.faq-page {
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background-color: rgba(255, 255, 255, 0.05);
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 3px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.3);
    }
  }
}

// el-collapse 主题覆写：去除默认边框/箭头/底色，匹配深色 + 主绿主题
.faq-collapse {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  border-bottom: none;
  --el-collapse-border-color: rgba(255, 255, 255, 0.06);
  --el-collapse-header-bg-color: transparent;
  --el-collapse-header-text-color: #e5e7eb;
  --el-collapse-content-bg-color: transparent;
  --el-collapse-content-text-color: #9ca3af;

  :deep(.el-collapse-item) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  :deep(.el-collapse-item__header) {
    background: transparent;
    border-bottom: none;
    color: #e5e7eb;
    font-size: 18px;
    height: auto;
    padding: 1.1rem 0;
    transition: color 0.15s;

    &:hover {
      color: #ffffff;
    }

    &.is-active {
      color: #c2ff00;
    }
  }

  :deep(.el-collapse-item__arrow) {
    display: none;
  }

  :deep(.el-collapse-item__wrap) {
    background: transparent;
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 0 0 1.1rem;
    color: #9ca3af;
    font-size: 16px;
    line-height: 1.75;
  }
}

// v-html 渲染的富文本：用 :deep() 命中内部 ul/li/a/strong 等
.faq-answer-content {
  :deep(p + p),
  :deep(p + ul),
  :deep(ul + p) {
    margin-top: 0.5rem;
  }

  :deep(ul) {
    list-style: disc;
    padding-left: 1.25rem;
    margin-top: 0.4rem;
  }

  :deep(li) {
    margin-bottom: 0.25rem;
  }

  :deep(a) {
    color: #c2ff00;
    text-decoration: underline;
    word-break: break-all;
  }

  :deep(strong) {
    color: #e5e7eb;
  }

  :deep(.faq-answer-paragraph) {
    margin-top: 0.5rem;
  }

  :deep(.faq-answer-highlight) {
    color: #c2ff00;
  }

  // 积分单价表：复用订阅 FAQ 同款表格样式
  :deep(.faq-answer-table) {
    margin-top: 0.75rem;
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    table {
      width: 100%;
      min-width: 480px;
      border-collapse: collapse;
      background: transparent;
    }

    th, td {
      padding: 10px 14px;
      text-align: left;
      border: 1px solid rgba(255, 255, 255, 0.15);
      white-space: nowrap;
      line-height: 22px;
    }

    th {
      background: rgba(255, 255, 255, 0.08);
      color: #ffffff;
      font-weight: 600;
      font-size: 16px;
    }

    td {
      color: rgba(255, 255, 255, 0.82);
      font-size: 16px;
    }

    tr:nth-child(even) {
      background: rgba(255, 255, 255, 0.03);
    }
  }
}
</style>
