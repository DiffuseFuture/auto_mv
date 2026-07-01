<!--
  PC 首页「制作流程」板块：
  - 左侧标题 + 五步列表，点击切换高亮步骤（当前步高亮卡片 + 主题绿竖条/序号，其余置灰）
  - 右侧展示对应步骤的产品演示图，五张交叉淡入
  视觉沿用首页主题绿 #C2FF00。
-->
<template>
  <section class="home-workflow w-full bg-[#0a0a0a] px-8 py-24">
    <div class="wf-grid max-w-[1280px] mx-auto">
      <!-- 左：标题 + 步骤列表 -->
      <div>
        <div class="text-[12px] font-extrabold tracking-[0.14em] text-[#C2FF00] mb-3">{{ t('home.workflow.kicker') }}</div>
        <h2 class="text-[42px] font-black leading-[1.05] text-white mb-8">{{ t('home.workflow.title') }}</h2>

        <div class="flex flex-col">
          <button
              v-for="(step, i) in steps"
              :key="i"
              type="button"
              class="flow-step"
              :class="{ 'is-active': i === activeStep }"
              @mouseenter="activeStep = i"
          >
            <span class="flow-bar"></span>
            <b class="flow-num">{{ step.no }}</b>
            <div class="flow-text">
              <strong>{{ step.title }}</strong>
              <span>{{ step.sub }}</span>
            </div>
          </button>
        </div>
      </div>

      <!-- 右：演示图（5 张交叉淡入） -->
      <div class="flow-art">
        <img
            v-for="(step, i) in steps"
            :key="i"
            :src="step.art"
            :alt="step.title"
            class="flow-art-img"
            :class="{ 'is-hidden': i !== activeStep }"
            decoding="async"
            loading="lazy"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import {ref, computed} from 'vue';
import {useI18nText} from '@/i18n';
import art1 from '@/assets/home/workflow/step-1.png';
import art2 from '@/assets/home/workflow/step-2.png';
import art3 from '@/assets/home/workflow/step-3.png';
import art4 from '@/assets/home/workflow/step-4.png';
import art5 from '@/assets/home/workflow/step-5.png';

const {t} = useI18nText();
const arts = [art1, art2, art3, art4, art5];

/** 当前高亮步骤下标。 */
const activeStep = ref(0);

/** 五步数据：序号 + 文案（i18n）+ 演示图，与 arts 一一对应。 */
const steps = computed(() => arts.map((art, i) => ({
  no: String(i + 1).padStart(2, '0'),
  title: t(`home.workflow.s${i + 1}Title`),
  sub: t(`home.workflow.s${i + 1}Sub`),
  art,
})));
</script>

<style lang="scss" scoped>
.wf-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 44px;
  align-items: center; // 右侧演示图相对左侧步骤列垂直居中

  @media (min-width: 1024px) {
    // 左侧步骤列收窄、右侧演示图加宽，让图更大更突出
    grid-template-columns: minmax(300px, 0.46fr) 1.3fr;
  }
}

.flow-step {
  display: grid;
  grid-template-columns: 56px 1fr;
  align-items: center;
  gap: 0 16px;
  width: 100%;
  text-align: left;
  position: relative;
  padding: 20px 18px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: transparent;
  cursor: pointer;
  transition: background .26s, border-color .26s;

  // 非激活项之间的细分隔线
  &:not(.is-active)::after {
    content: "";
    position: absolute;
    left: 18px;
    right: 12px;
    bottom: 0;
    height: 1px;
    background: rgba(255, 255, 255, 0.06);
  }

  &.is-active {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(194, 255, 0, 0.18);
  }
}

// 左侧高亮竖条（仅激活态显示）
.flow-bar {
  position: absolute;
  left: 0;
  top: 16px;
  bottom: 16px;
  width: 4px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, #C2FF00, rgba(194, 255, 0, 0.3));
  box-shadow: 0 0 14px rgba(194, 255, 0, 0.5);
  opacity: 0;
  transition: opacity .26s;

  .flow-step.is-active & {
    opacity: 1;
  }
}

.flow-num {
  font-size: 44px;
  font-weight: 900;
  line-height: 1;
  text-align: center;
  color: rgba(194, 255, 0, 0.1);
  transition: color .3s, text-shadow .3s;

  .flow-step.is-active & {
    color: #C2FF00;
    text-shadow: 0 0 26px rgba(194, 255, 0, 0.3);
  }
}

.flow-text {
  min-width: 0;

  strong {
    display: block;
    font-size: 18px;
    font-weight: 800;
    letter-spacing: -0.01em;
    color: rgba(208, 220, 196, 0.32);
    transition: color .26s;
  }

  span {
    display: block;
    margin-top: 4px;
    font-size: 14px;
    line-height: 1.5;
    color: rgba(138, 150, 128, 0.3);
    transition: color .26s;
  }

  .flow-step.is-active & strong {
    color: #eef8e8;
  }

  .flow-step.is-active & span {
    color: #98a28f;
  }
}

// 右侧演示图容器：保持 1672:941 比例，叠放五张图交叉淡入
.flow-art {
  position: relative;
  aspect-ratio: 1672 / 941;
  border-radius: 22px;
  overflow: hidden;
  background: #090e09;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.07),
    0 52px 130px rgba(0, 0, 0, 0.76),
    0 18px 50px rgba(0, 0, 0, 0.38);
}

.flow-art-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 1;
  transform: scale(1);
  transition: opacity .34s ease, transform .48s ease;

  &.is-hidden {
    opacity: 0;
    transform: scale(1.025);
    pointer-events: none;
  }
}
</style>
