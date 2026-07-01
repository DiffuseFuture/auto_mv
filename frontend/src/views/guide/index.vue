<!--
  OhYesAI 使用指南页。
  数据驱动：所有文案从 guide-data.js 按 locale 取，不在模板里硬编中文。
  实现优先 element-plus：el-card / el-timeline / el-alert / el-image / el-divider。
  TOC 自己实现（el-anchor 在嵌套滚动容器下无效）。
-->
<template>
  <div ref="containerRef" class="guide-page relative w-full h-full overflow-y-auto bg-[#0a0a0a] text-[#eff1ea]" :class="{'min-screen': !isMobile}">
    <!-- 返回按钮：固定在视口左上角，滚动时常驻（独立页面无主壳，自带返回入口） -->
    <button class="fixed left-4 top-12 z-20 w-10 h-10 rounded-full flex-center hover:opacity-90 cursor-pointer" @click="safeBack()">
      <svg-icon name="gy-return" size="34" color="#FFFFFF"></svg-icon>
    </button>

    <div class="flex justify-center px-8 pt-12 pb-12 max-w-[1200px] mx-auto">
      <!-- 左侧 TOC -->
      <nav class="guide-toc shrink-0 w-[230px] sticky top-12 self-start hidden lg:block">
        <a v-for="item in guide.toc"
           :key="item.id"
           :href="`#${item.id}`"
           :class="['toc-link', item.sub && 'is-sub', activeId === item.id && 'is-active']"
           @click.prevent="scrollToSection(item.id)"
        >{{ item.title }}</a>
      </nav>

      <main class="flex-1 max-w-[760px] ml-0 lg:ml-12 min-w-0">
        <!-- HERO -->
        <section id="hero" class="pb-12">
          <h1 class="text-[40px] leading-[1.15] font-bold mb-4">
            {{ guide.hero.titlePrefix }} <span class="text-[#a8b09f] italic font-normal">{{ guide.hero.titleEm }}</span>
          </h1>
          <p class="text-[15px] leading-[1.85] text-[#a8b09f]">{{ guide.hero.intro }}</p>
        </section>

        <!-- S1 -->
        <section id="s1">
          <h2 class="guide-h2">{{ guide.s1.title }}</h2>
          <p class="guide-intro">{{ guide.s1.intro }}</p>

          <div id="s1-1" class="mb-10">
            <h3 class="guide-h3"><span class="h3-num">{{ guide.s1_1.num }}</span>{{ guide.s1_1.title }}</h3>
            <p class="guide-p">{{ guide.s1_1.desc }}</p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
              <el-card v-for="m in guide.models" :key="m.name" shadow="never"
                       class="model-card" :body-style="{padding: '14px 16px'}">
                <div class="text-[10px] tracking-[0.04em] font-bold text-[#C2FF00] mb-1.5">{{ m.tag }}</div>
                <div class="text-[15px] font-bold text-[#eff1ea] mb-1">{{ m.name }}</div>
                <div class="text-[12.5px] text-[#a8b09f] leading-[1.6] min-h-[36px] mb-2">{{ m.desc }}</div>
                <div class="flex gap-10">
                  <div v-for="p in m.prices" :key="p.res">
                    <div class="text-[11px] text-[#a8b09f]/60 mb-1">{{ p.res }}</div>
                    <div class="text-[11.5px] text-[#a8b09f]">
                      <span class="text-[#C2FF00] text-[18px] font-extrabold mr-1">{{ p.credit }}</span>{{ guide.pointsUnit }}
                    </div>
                  </div>
                </div>
              </el-card>
            </div>
            <el-alert :closable="false" class="guide-alert">
              <span v-html="guide.s1_1.operationHtml"></span>
            </el-alert>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-2.5 mt-4">
              <el-image :src="imgs[1]" :alt="guide.imageAlts[1]" :preview-src-list="[imgs[1], imgs[2]]" :initial-index="0" preview-teleported fit="cover" class="guide-img" />
              <el-image :src="imgs[2]" :alt="guide.imageAlts[2]" :preview-src-list="[imgs[1], imgs[2]]" :initial-index="1" preview-teleported fit="cover" class="guide-img" />
            </div>
          </div>

          <div id="s1-2" class="mb-10">
            <h3 class="guide-h3"><span class="h3-num">{{ guide.s1_2.num }}</span>{{ guide.s1_2.title }}</h3>
            <p class="guide-p">{{ guide.s1_2.desc }}</p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
              <el-card v-for="(r, idx) in guide.ratios" :key="r.title" shadow="never" class="ratio-card"
                       :body-style="{padding: '12px 14px', display: 'flex', alignItems: 'center', gap: '14px'}">
                <div :class="['ratio-vis', idx === 0 ? 'land' : 'port']"></div>
                <div>
                  <h4 class="text-[14px] font-bold mb-1">{{ r.title }}</h4>
                  <p class="text-[12px] text-[#a8b09f]">{{ r.desc }}</p>
                </div>
              </el-card>
            </div>
            <el-image :src="imgs[3]" :alt="guide.imageAlts[3]" :preview-src-list="[imgs[3]]" preview-teleported fit="cover" class="guide-img w-full" />
          </div>
        </section>

        <el-divider class="my-12" />

        <!-- S2 -->
        <section id="s2">
          <h2 class="guide-h2">{{ guide.s2.title }}</h2>

          <div id="s2-1" class="mb-10">
            <h3 class="guide-h3"><span class="h3-num">{{ guide.s2_1.num }}</span>{{ guide.s2_1.title }}</h3>
            <p class="guide-p">{{ guide.s2_1.desc }}</p>
            <ul class="guide-list">
              <li v-html="guide.s2_1.uploadHtml"></li>
            </ul>
            <el-image :src="imgs[4]" :alt="guide.imageAlts[4]" :preview-src-list="[imgs[4]]" preview-teleported fit="cover" class="guide-img w-full mt-3" />

            <ul class="guide-list mt-5">
              <li v-html="guide.s2_1.aiGenHtml"></li>
            </ul>
            <div class="flex flex-col gap-2.5 mt-3">
              <el-image v-for="(src, i) in [imgs[5], imgs[6], imgs[7], imgs[8]]" :key="src" :src="src"
                        :alt="guide.imageAlts[5 + i]"
                        :preview-src-list="[imgs[5], imgs[6], imgs[7], imgs[8]]" :initial-index="i"
                        preview-teleported fit="cover" class="guide-img w-full" />
            </div>

            <el-alert type="success" :closable="false" show-icon class="guide-alert mt-5">
              <span v-html="guide.s2_1.tipHtml"></span>
            </el-alert>
          </div>

          <div id="s2-2" class="mb-10">
            <h3 class="guide-h3"><span class="h3-num">{{ guide.s2_2.num }}</span>{{ guide.s2_2.title }}</h3>
            <p class="guide-p" v-html="guide.s2_2.descHtml"></p>
            <el-image :src="imgs[9]" :alt="guide.imageAlts[9]" :preview-src-list="[imgs[9]]" preview-teleported fit="cover" class="guide-img w-full" />
            <p class="guide-p mt-4">{{ guide.s2_2.notice }}</p>
            <ul class="guide-list">
              <li v-html="guide.s2_2.rule1Html"></li>
              <li v-html="guide.s2_2.rule2Html"></li>
            </ul>
          </div>
        </section>

        <el-divider class="my-12" />

        <!-- S3 -->
        <section id="s3">
          <h2 class="guide-h2">{{ guide.s3.title }}</h2>
          <p class="guide-intro" v-html="guide.s3.introHtml"></p>

          <el-timeline class="guide-timeline">
            <el-timeline-item
                v-for="step in guide.steps"
                :id="step.id"
                :key="step.id"
                :timestamp="step.timestamp"
                placement="top"
                type="primary"
                size="large"
                hollow
            >
              <h3 class="step-title flex items-center gap-2 flex-wrap">
                {{ step.title }}
                <el-tag v-if="step.badge" size="small" class="key-tag">{{ step.badge }}</el-tag>
              </h3>

              <p v-if="step.bodyHtml" class="guide-p" v-html="step.bodyHtml"></p>

              <el-alert v-if="step.alertHtml" :type="step.alertType || 'info'" :closable="false" show-icon class="guide-alert">
                <span v-html="step.alertHtml"></span>
              </el-alert>

              <ul v-if="step.substeps" class="substep-list">
                <li v-for="(sub, i) in step.substeps" :key="i">
                  <span class="s-icon">{{ sub.icon }}</span>
                  <div v-html="sub.textHtml"></div>
                </li>
              </ul>

              <!-- 步骤 4 的两种方法卡片 + 列表 -->
              <template v-if="step.methods">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3 my-3">
                  <el-card v-for="m in step.methods" :key="m.title" shadow="never"
                           class="model-card" :body-style="{padding: '14px 16px'}">
                    <h4 class="text-[14px] font-bold mb-1 flex items-center justify-between gap-2">
                      <span>{{ m.title }}</span>
                      <el-tag v-if="m.recommendTag" size="small" class="rec-tag">{{ m.recommendTag }}</el-tag>
                    </h4>
                    <p class="text-[12.5px] text-[#a8b09f] leading-[1.65]">{{ m.desc }}</p>
                    <p v-if="m.note" class="text-[12px] italic text-[#a8b09f] mt-1">{{ m.note }}</p>
                  </el-card>
                </div>
                <p v-if="step.methodsHint" class="guide-p">{{ step.methodsHint }}</p>
              </template>

              <ul v-if="step.bulletsHtml" class="guide-list">
                <li v-for="(html, i) in step.bulletsHtml" :key="i" v-html="html"></li>
              </ul>

              <!-- 分条带配图：每条要点下方紧跟它对应的截图（如步骤 5 的下载 / 分享） -->
              <template v-if="step.bulletGroups">
                <div v-for="(group, gi) in step.bulletGroups" :key="gi" class="mb-4">
                  <ul class="guide-list">
                    <li v-html="group.html"></li>
                  </ul>
                  <div :class="['grid gap-2.5 mt-3', group.imageIdx.length === 2 ? 'md:grid-cols-2' : 'grid-cols-1']">
                    <el-image v-for="(idx, i) in group.imageIdx" :key="idx" :src="imgs[idx]" :alt="guide.imageAlts[idx]"
                              :preview-src-list="group.imageIdx.map(j => imgs[j])" :initial-index="i"
                              preview-teleported fit="cover" class="guide-img" />
                  </div>
                </div>
              </template>

              <!-- 图片：1 张铺满 / 2 张并排 / 3+ 纵排，靠 grid-cols 切换；alt 来自 guide.imageAlts 提升 SEO -->
              <div v-if="step.imageIdx?.length" :class="['grid gap-2.5 mt-3', step.imageIdx.length === 2 ? 'md:grid-cols-2' : 'grid-cols-1']">
                <el-image v-for="(idx, i) in step.imageIdx" :key="idx" :src="imgs[idx]" :alt="guide.imageAlts[idx]"
                          :preview-src-list="step.imageIdx.map(j => imgs[j])" :initial-index="i"
                          preview-teleported fit="cover" class="guide-img" />
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted, onBeforeUnmount} from 'vue';
import {useI18nText} from '@/i18n';
import {useSeo, removeJsonLd} from '@/composables/useSeo.js';
import {useSafeBack} from '@/composables/useSafeBack.js';
import {isMobileClient} from '@/utils/index.js';
import {GUIDE_MAP} from './guide-data.js';
import img1 from '@/assets/guide/image1.jpeg';
import img2 from '@/assets/guide/image2.jpeg';
import img3 from '@/assets/guide/image3.jpeg';
import img4 from '@/assets/guide/image4.jpeg';
import img5 from '@/assets/guide/image5.jpeg';
import img6 from '@/assets/guide/image6.jpeg';
import img7 from '@/assets/guide/image7.jpeg';
import img8 from '@/assets/guide/image8.jpeg';
import img9 from '@/assets/guide/image9.jpeg';
import img10 from '@/assets/guide/image10.jpeg';
import img11 from '@/assets/guide/image11.jpeg';
import img12 from '@/assets/guide/image12.jpeg';
import img13 from '@/assets/guide/image13.jpeg';
import img14 from '@/assets/guide/image14.jpeg';
import img15 from '@/assets/guide/image15.jpeg';
import img16 from '@/assets/guide/image16.jpeg';
import img17 from '@/assets/guide/image17.jpeg';
import img18 from '@/assets/guide/image18.jpeg';
import img19 from '@/assets/guide/image19.jpeg';
import img20 from '@/assets/guide/image20.jpeg';
import img21 from '@/assets/guide/image21.jpeg';
import img22 from '@/assets/guide/image22.jpeg';

const {locale} = useI18nText();
const safeBack = useSafeBack();

/** PC 端套用全局 1280px 最小宽度（.min-screen）；移动端不限制，保持响应式。 */
const isMobile = isMobileClient();

/** 按 locale 取一份完整 guide 数据；找不到回退 zh-CN。 */
const guide = computed(() => GUIDE_MAP[String(locale.value)] || GUIDE_MAP['zh-CN']);

/** 图片下标对照表（guide-data.js 里 imageIdx 用 1-22，直接索引取真实 import）。 */
const imgs = [null, img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20, img21, img22];

const containerRef = ref(null);
const activeId = ref('hero');
// 点击目录后短暂锁定高亮的截止时间戳：平滑滚动期间不让 scroll-spy 把高亮抢到相邻章节
let scrollSpyLockUntil = 0;

const scrollToSection = (id) => {
  const el = document.getElementById(id);
  const container = containerRef.value;
  if (!el || !container) return;
  activeId.value = id;
  scrollSpyLockUntil = Date.now() + 700;
  container.scrollTo({top: Math.max(0, el.offsetTop - 80), behavior: 'smooth'});
};

const updateActiveFromScroll = () => {
  // 点击目录后的锁定期内，保持点击选中的高亮，不被平滑滚动过程覆盖
  if (Date.now() < scrollSpyLockUntil) return;
  const container = containerRef.value;
  if (!container) return;
  // 已滚到底部：最后一个章节一定是当前章节（末节下方空间不足时，threshold 永远够不到它）
  if (container.scrollTop + container.clientHeight >= container.scrollHeight - 2) {
    activeId.value = guide.value.toc[guide.value.toc.length - 1].id;
    return;
  }
  const threshold = container.scrollTop + 120;
  let current = 'hero';
  for (const item of guide.value.toc) {
    const el = document.getElementById(item.id);
    if (el && el.offsetTop <= threshold) current = item.id;
  }
  activeId.value = current;
};

onMounted(() => {
  containerRef.value?.addEventListener('scroll', updateActiveFromScroll, {passive: true});
  updateActiveFromScroll();
});

onBeforeUnmount(() => {
  containerRef.value?.removeEventListener('scroll', updateActiveFromScroll);
  removeJsonLd('guide-howto');
});

/**
 * SEO 注入：HowTo 结构化数据 + hreflang 替代语言链接 + canonical URL。
 * router.afterEach 已写入 title/description/og/twitter 基础 meta；这里追加结构化数据
 * 让搜索引擎识别"教程文章"（HowTo schema），收录效果更好。
 * watch locale 变化重新写入，保证 zh/en 切换时 JSON-LD 与页面文案一致。
 */
const buildHowToJsonLd = () => {
  const g = guide.value;
  return {
    '@context': 'https://schema.org',
    '@type': 'HowTo',
    name: `${g.hero.titlePrefix} ${g.hero.titleEm}`,
    description: g.hero.intro,
    inLanguage: String(locale.value),
    publisher: {
      '@type': 'Organization',
      name: 'OhYesAI',
      url: 'https://www.ohyesai.com',
    },
    tool: g.models.map((m) => ({'@type': 'HowToTool', name: m.name})),
    step: g.steps.map((s, idx) => ({
      '@type': 'HowToStep',
      position: idx + 1,
      name: s.title,
      // 富文本字段去 HTML 标签，schema 要纯文本
      text: String(s.bodyHtml || '').replace(/<[^>]+>/g, '').trim(),
    })),
  };
};

const applyGuideSeo = () => {
  const g = guide.value;
  const langPrefix = String(locale.value) === 'en-US' ? '/en' : '/zh';
  useSeo({
    title: `${g.hero.titlePrefix} ${g.hero.titleEm}`,
    description: g.hero.intro,
    keywords: String(locale.value) === 'en-US'
      ? 'OhYesAI user guide, AI MV tutorial, video models, storyboard, one-click generation'
      : 'OhYesAI 使用指南, AI MV 教程, 视频模型, 分镜脚本, 一键生成, Vidu Q2, Seedance, Kling',
    url: `${langPrefix}/guide`,
    type: 'article',
    alternates: {
      'zh-CN': '/zh/guide',
      'en-US': '/en/guide',
      'x-default': '/zh/guide',
    },
    jsonLd: {
      id: 'guide-howto',
      data: buildHowToJsonLd(),
    },
  });
};

onMounted(() => applyGuideSeo());
watch(locale, () => applyGuideSeo());
</script>

<style scoped lang="scss">
.guide-page {
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background-color: rgba(255,255,255,0.05); }
  &::-webkit-scrollbar-thumb { background-color: rgba(255,255,255,0.2); border-radius: 3px; }
}

// TOC
.guide-toc {
  border-left: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
}
.toc-link {
  display: block;
  color: #a8b09f;
  font-size: 14px;
  line-height: 24px;
  padding: 3px 0 3px 12px;
  border-left: 2px solid transparent;
  margin-left: -1px;
  font-weight: 600;
  transition: color .15s, border-color .15s;
  cursor: pointer;
  &:hover { color: #fff; }
  &.is-sub {
    font-size: 13px;
    font-weight: 400;
    color: #5c6358;
    padding-left: 24px;
    &:hover { color: #a8b09f; }
  }
  &.is-active {
    color: #C2FF00;
    border-left-color: #C2FF00;
    &.is-sub { color: #C2FF00; }
  }
}

// 章节标题
.guide-h2 {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 12px;
}
.guide-h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  .h3-num {
    color: #C2FF00;
    font-size: 14px;
    font-weight: 800;
  }
}
.guide-intro, .guide-p {
  font-size: 14px;
  line-height: 1.85;
  color: #a8b09f;
  margin-bottom: 14px;
  :deep(em) { font-style: normal; }
  :deep(strong) { color: #eff1ea; }
}
.guide-intro { margin-bottom: 24px; }

// 列表
.guide-list {
  list-style: disc;
  padding-left: 1.25rem;
  font-size: 13.5px;
  color: #a8b09f;
  line-height: 1.85;
  li { margin-bottom: 4px; }
  :deep(strong) { color: #eff1ea; }
  :deep(em) { font-style: normal; }
  :deep(code) {
    background: rgba(255,255,255,0.06);
    padding: 1px 6px;
    border-radius: 4px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
    color: #C2FF00;
  }
}

.substep-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 14px 0;
  li {
    display: flex;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 10px;
    background: rgba(255,255,255,0.025);
    border: 1px solid rgba(255,255,255,0.06);
    font-size: 13px;
    line-height: 1.7;
    color: #a8b09f;
    .s-icon { flex-shrink: 0; font-size: 16px; }
    :deep(strong) { color: #eff1ea; }
    :deep(em) { font-style: normal; }
  }
}

// 关键步骤 / 推荐徽章：统一主题绿
.key-tag, .rec-tag {
  background: rgba(194,255,0,0.1) !important;
  border-color: rgba(194,255,0,0.25) !important;
  color: #C2FF00 !important;
}

.model-card, .ratio-card {
  background-color: rgba(255,255,255,0.025) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  border-radius: 12px !important;
  color: #eff1ea;
}

.ratio-vis {
  flex-shrink: 0;
  border-radius: 4px;
  background: linear-gradient(135deg, rgba(194,255,0,0.25), rgba(130,255,121,0.18));
  border: 1px solid rgba(194,255,0,0.35);
  &.land { width: 64px; height: 36px; }
  &.port { width: 36px; height: 64px; }
}

.guide-img {
  // 截图统一 16:9，提前用 aspect-ratio 占位：图片未加载完也不会塌成 0 高，
  // 避免逐张加载撑高页面导致锚点位置漂移、电梯导航（尤其靠底部的 3.5/3.6）跳错。
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.09);
  box-shadow: 0 6px 22px rgba(0,0,0,0.4);
  overflow: hidden;
  :deep(img) { width: 100%; height: 100%; object-fit: cover; display: block; }
}

.guide-timeline {
  padding-left: 6px;
  --el-timeline-node-color: rgba(194,255,0,0.5);
  // 节点圆圈：背景用页面底色（不透明）而不是半透明绿——这样圆圈"内部"看起来像空心，
  // 但 absolute 定位的 tail 在节点下层被节点挡住，不会从圆圈内部透出。
  :deep(.el-timeline-item__node) {
    background: #0a0a0a;
    border: 2px solid rgba(194,255,0,0.55);
  }
  :deep(.el-timeline-item__tail) {
    border-left-color: rgba(194,255,0,0.18);
  }
  :deep(.el-timeline-item__timestamp) {
    color: #C2FF00 !important;
    font-size: 18px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }
}
.step-title {
  font-size: 17px;
  font-weight: 700;
  color: #eff1ea;
  margin: 6px 0 12px;
}

.guide-alert {
  margin: 12px 0;
  background: rgba(255,255,255,0.03) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  border-radius: 10px;
  :deep(.el-alert__content) { padding: 0; }
  :deep(.el-alert__description) {
    font-size: 13px;
    line-height: 1.7;
    color: #a8b09f;
    margin: 0;
    strong { color: #eff1ea; }
  }
  &.el-alert--success, &.el-alert--warning {
    border-color: rgba(194,255,0,0.22) !important;
    background: linear-gradient(90deg, rgba(194,255,0,0.06), rgba(194,255,0,0.015)) !important;
    :deep(.el-alert__icon) { color: #C2FF00; }
  }
}

.el-divider { border-top-color: rgba(255,255,255,0.08) !important; }
</style>
