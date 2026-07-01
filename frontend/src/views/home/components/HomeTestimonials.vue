<!--
  PC 首页「用户反馈」板块：按设计稿（首页优化/page_v7.8.html #testimonials）还原。
  标题区（kicker + 标题 + 右侧描述）+ 三张评价卡（引号 + 评价 + 五星 + 作者）。
  文案走 i18n（home.testimonials.*）；头像取作者名首字母；五星为装饰按设计稿固定。
-->
<template>
  <section class="home-testimonials section tight">
    <div class="section-head">
      <div>
        <div class="kicker">{{ t('home.testimonials.kicker') }}</div>
        <h2>{{ t('home.testimonials.title') }}</h2>
      </div>
      <p>{{ t('home.testimonials.desc') }}</p>
    </div>

    <div class="testi-grid">
      <div v-for="(item, i) in items" :key="i" class="testi-card">
        <div class="testi-q-mark">“</div>
        <p class="testi-text">{{ item.text }}</p>
        <div class="testi-stars">★★★★★</div>
        <div class="testi-author">
          <div class="testi-avatar">{{ item.name.charAt(0) }}</div>
          <div>
            <strong class="testi-name">{{ item.name }}</strong>
            <span class="testi-role">{{ item.role }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import {computed} from 'vue';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();

/** 三条用户评价：文案 + 作者名 + 角色（均来自 i18n）。 */
const items = computed(() => [1, 2, 3].map((n) => ({
  text: t(`home.testimonials.t${n}Text`),
  name: t(`home.testimonials.t${n}Name`),
  role: t(`home.testimonials.t${n}Role`),
})));
</script>

<style lang="scss" scoped>
.home-testimonials {
  --lime: #BEFA00;
  --muted: #8a948a;
  background: #000;
}

// 板块容器
.section {
  max-width: 1280px;
  margin: 0 auto;
  padding: 84px 40px;

  &.tight { padding-top: 46px; }
}

// 标题区
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 24px;
  margin-bottom: 28px;

  h2 {
    font-size: 42px;
    line-height: 1.02;
    letter-spacing: -.02em;
    color: #fff;
  }

  p {
    max-width: 420px;
    color: #aeb8a4;
    font-size: 15px;
    line-height: 1.6;
  }
}

.kicker {
  color: var(--lime);
  font-size: 12px;
  font-weight: 950;
  letter-spacing: .12em;
  margin-bottom: 9px;
}

// 评价卡
.testi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.testi-card {
  position: relative;
  padding: 28px 24px 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 12px;
  background: rgba(255, 255, 255, .03);
  transition: border-color .25s, transform .25s;

  &:hover {
    border-color: rgba(191, 255, 0, .3);
    transform: translateY(-4px);
  }
}

.testi-q-mark {
  position: absolute;
  top: 8px;
  left: 16px;
  font-size: 80px;
  line-height: 1;
  color: var(--lime);
  opacity: .18;
  font-family: Georgia, serif;
  pointer-events: none;
  user-select: none;
}

.testi-text {
  color: #c7d0bd;
  font-size: 14px;
  line-height: 1.8;
  margin-bottom: 22px;
  padding-top: 12px;
  position: relative;
}

.testi-stars {
  color: var(--lime);
  font-size: 13px;
  letter-spacing: 3px;
  margin-bottom: 14px;
}

.testi-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.testi-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--lime), #40e8c0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #101010;
  flex-shrink: 0;
}

.testi-name {
  display: block;
  font-size: 14px;
  color: #e8eed4;
  font-weight: 600;
}

.testi-role {
  font-size: 12px;
  color: var(--muted);
  font-weight: 400;
}

// 响应式：窄屏标题区竖排、评价卡单列
@media (max-width: 900px) {
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .testi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
