<!--
  PC 首页「六大核心优势」板块：严格按设计稿（首页优化/page_v7.8.html #why）还原。
  六行左右交错，每行右侧是一个产品示意（流水线 / 对话 / 卡点波形 / 模型切换 / 时间线 / 风格网格）。
  - 标题、每条优势的 kicker/title/desc/tags 走 i18n（home.features.*）。
  - 示意图内部的演示文字属装饰性产品示意，按设计稿硬编码中文。
  - 卡点波形条与模型卡轮播两处动效用 setInterval 驱动，卸载时清理。
-->
<template>
  <section class="home-features">
    <!-- 板块标题 -->
    <div class="feat-head">
      <div class="feat-head-kicker">{{ t('home.features.kicker') }}</div>
      <h2 class="feat-head-title">{{ t('home.features.title') }}</h2>
    </div>

    <div class="feature-rows">
      <!-- 01 一键化生成：生成流水线 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">01</div>
          <div class="feat-kicker">{{ t('home.features.f1Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f1Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f1Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(1)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-pipeline">
            <div class="fv-step">
              <div class="fv-step-icon"><svg viewBox="0 0 24 24"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/></svg></div>
              <div class="fv-step-text"><div class="fv-step-name">上传音乐</div><div class="fv-step-sub">MP3 · 3:24</div></div>
              <span class="fv-step-badge">INPUT</span>
            </div>
            <div class="fv-step">
              <div class="fv-step-icon"><svg viewBox="0 0 24 24"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></div>
              <div class="fv-step-text"><div class="fv-step-name">节奏分析</div><div class="fv-step-sub">BPM · 段落 · Drop</div></div>
              <span class="fv-step-badge">ANALYZE</span>
            </div>
            <div class="fv-step">
              <div class="fv-step-icon"><svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div>
              <div class="fv-step-text"><div class="fv-step-name">生成分镜脚本</div><div class="fv-step-sub">10 个镜头 · AI 规划</div></div>
              <span class="fv-step-badge">PLAN</span>
            </div>
            <div class="fv-step">
              <div class="fv-step-icon"><svg viewBox="0 0 24 24"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg></div>
              <div class="fv-step-text"><div class="fv-step-name">生成镜头视频</div><div class="fv-step-sub">逐镜渲染</div></div>
              <span class="fv-step-badge">RENDER</span>
            </div>
            <div class="fv-step done">
              <div class="fv-step-icon"><svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg></div>
              <div class="fv-step-text"><div class="fv-step-name">合成完整 MV</div><div class="fv-step-sub">可下载 · 去水印</div></div>
              <span class="fv-step-badge">DONE</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 02 对话式创作：对话界面 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">02</div>
          <div class="feat-kicker">{{ t('home.features.f2Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f2Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f2Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(2)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-chat">
            <div class="fv-chat-screen">
              <div class="fv-msg user">把第 8 个分镜改成雨夜霓虹街道，仰拍视角</div>
              <div class="fv-msg ai"><strong>已更新镜头 08：</strong><br>雨夜霓虹街道 · 仰拍 · 慢推。反光路面已加入画面，氛围与高潮段情绪匹配。</div>
              <div class="fv-msg user">第 3 镜的颜色改冷一点，更有新海诚感</div>
              <div class="fv-typing"><span class="fv-dot"></span><span class="fv-dot"></span><span class="fv-dot"></span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 03 毫秒级卡点：BPM 波形 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">03</div>
          <div class="feat-kicker">{{ t('home.features.f3Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f3Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f3Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(3)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-bpm">
            <div class="fv-bpm-bar-row">
              <div
                  v-for="(h, i) in BPM_HEIGHTS"
                  :key="i"
                  class="fv-bbar"
                  :class="{ beat: isBeat(i) }"
                  :style="{ height: h + 'px' }"
              ></div>
            </div>
            <div class="fv-bpm-labels">
              <span>0:00</span><span class="hi">DROP ▼</span><span>1:30</span><span>3:00</span><span>4:20</span>
            </div>
            <div class="fv-beat-chips">
              <span class="fv-beat-chip">128 BPM</span>
              <span class="fv-beat-chip">4/4 拍</span>
              <span class="fv-beat-chip">8 段落</span>
              <span class="fv-beat-chip dim">3 个 Drop</span>
              <span class="fv-beat-chip dim">副歌 ×4</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 04 模型自由切换：模型卡轮播 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">04</div>
          <div class="feat-kicker">{{ t('home.features.f4Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f4Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f4Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(4)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-models">
            <div
                v-for="(m, i) in MODELS"
                :key="i"
                class="fv-model-card"
                :class="{ active: i === activeModel }"
                @click="activeModel = i"
            >
              <div class="fv-model-dot"></div>
              <div class="fv-model-info">
                <div class="fv-model-name">{{ m.name }}</div>
                <div class="fv-model-desc">{{ m.desc }}</div>
              </div>
              <span class="fv-model-badge">精选</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 05 5 分钟完整叙事：时间线 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">05</div>
          <div class="feat-kicker">{{ t('home.features.f5Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f5Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f5Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(5)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-timeline">
            <div class="fv-tl-header">
              <span>TIMELINE</span>
              <strong>5:00 完整成片</strong>
            </div>
            <div class="fv-tl-bar"><div class="fv-tl-fill"></div></div>
            <div class="fv-scenes">
              <div class="fv-scene"><div class="fv-scene-art art a-cinema"></div><span class="fv-scene-num">01</span></div>
              <div class="fv-scene"><div class="fv-scene-art art a-shinkai"></div><span class="fv-scene-num">02</span></div>
              <div class="fv-scene"><div class="fv-scene-art art a-ghibli"></div><span class="fv-scene-num">03</span></div>
              <div class="fv-scene"><div class="fv-scene-art art a-cyber"></div><span class="fv-scene-num">04</span></div>
              <div class="fv-scene"><div class="fv-scene-art art a-vangogh"></div><span class="fv-scene-num">05</span></div>
            </div>
            <div class="fv-tl-footer"><span>前奏</span><span>主歌</span><span>副歌</span><span>间奏</span><span>尾奏</span></div>
          </div>
        </div>
      </div>

      <!-- 06 多样视觉风格：风格网格 -->
      <div class="feat-row">
        <div class="feat-text">
          <div class="feat-n">06</div>
          <div class="feat-kicker">{{ t('home.features.f6Kicker') }}</div>
          <h3 class="feat-title">{{ t('home.features.f6Title') }}</h3>
          <p class="feat-desc">{{ t('home.features.f6Desc') }}</p>
          <div class="feat-tags"><span v-for="(tag, i) in tags(6)" :key="i">{{ tag }}</span></div>
        </div>
        <div class="feat-visual">
          <div class="fv-stylegrid">
            <div class="fv-scard"><div class="fv-scard-art art a-ghibli"></div><div class="fv-scard-label">宫崎骏治愈风</div></div>
            <div class="fv-scard"><div class="fv-scard-art art a-vangogh"></div><div class="fv-scard-label">梵高油画风</div></div>
            <div class="fv-scard"><div class="fv-scard-art art a-shinkai"></div><div class="fv-scard-label">新海诚风格</div></div>
            <div class="fv-scard"><div class="fv-scard-art art a-cyber"></div><div class="fv-scard-label">赛博朋克霓虹</div></div>
            <div class="fv-scard"><div class="fv-scard-art art a-inkcn"></div><div class="fv-scard-label">中国水墨风</div></div>
            <div class="fv-scard"><div class="fv-scard-art art a-dream"></div><div class="fv-scard-label">超现实梦核</div></div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();

/** 取第 n 条优势的标签数组（i18n 里以英文逗号分隔）。 */
const tags = (n) => t(`home.features.f${n}Tags`).split(',');

// ==== 03 卡点波形：固定高度条 + 高亮位每 480ms 右移 3 格 ====
const BPM_HEIGHTS = [18, 26, 34, 52, 38, 62, 44, 80, 58, 92, 74, 88, 66, 96, 70, 84, 56, 72, 48, 60, 42, 74, 86, 64, 50, 36, 78, 90, 54, 68];
const BPM_BEATS = [3, 6, 9, 12, 15, 18, 21, 24, 27];
const bpmShift = ref(0);
/** 第 i 根波形条当前是否为高亮（卡点）位。 */
const isBeat = (i) => BPM_BEATS.includes((i + bpmShift.value) % BPM_HEIGHTS.length);

// ==== 04 模型卡：每 1800ms 自动切换高亮，点击也可手动切 ====
const MODELS = [
  {name: 'Vidu Q2', desc: '性价比之王。光影细腻，适合绝大多数风格。'},
  {name: 'Kling V3 Omni Pro', desc: '运镜大师。擅长处理大范围肢体动作和写实场景。'},
  {name: 'Seedance 2.0 Fast', desc: '极速预览。适合快速迭代创意，验证想法。'},
  {name: 'Seedance 2.0', desc: '画质巅峰。细节表现力极强，适合追求极致成片质感的创作者。'},
];
const activeModel = ref(0);

let bpmTimer = null;
let modelTimer = null;

onMounted(() => {
  bpmTimer = window.setInterval(() => {
    bpmShift.value = (bpmShift.value + 3) % BPM_HEIGHTS.length;
  }, 480);
  modelTimer = window.setInterval(() => {
    activeModel.value = (activeModel.value + 1) % MODELS.length;
  }, 1800);
});

onBeforeUnmount(() => {
  if (bpmTimer) clearInterval(bpmTimer);
  if (modelTimer) clearInterval(modelTimer);
});
</script>

<style lang="scss" scoped>
.home-features {
  --lime: #BEFA00;
  width: 100%;
}

// 板块标题
.feat-head {
  text-align: center;
  padding: 80px 40px 56px;
  background: #000;
}

.feat-head-kicker {
  color: var(--lime);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: .14em;
}

.feat-head-title {
  font-size: 42px;
  letter-spacing: -.02em;
  margin-top: 10px;
  font-weight: 900;
  color: #fff;
}

// 交错行骨架
.feature-rows { width: 100%; overflow: hidden; }

.feat-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 500px;
  position: relative;

  &:nth-child(odd) { background: #000; }
  &:nth-child(even) { background: #080808; }
  &:nth-child(even) .feat-text { order: 2; }
  &:nth-child(even) .feat-visual { order: 1; }
}

.feat-text {
  padding: 90px 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.feat-n {
  font-size: 120px;
  font-weight: 950;
  line-height: 1;
  letter-spacing: -.05em;
  margin-bottom: 4px;
  color: rgba(190, 250, 0, .07);

  .feat-row:nth-child(1) & { color: rgba(190, 250, 0, .14); }
}

.feat-kicker { color: var(--lime); font-size: 11px; font-weight: 950; letter-spacing: .14em; margin-bottom: 10px; }
.feat-title { font-size: 36px; font-weight: 950; line-height: 1.06; letter-spacing: -.02em; margin-bottom: 16px; color: #fff; }
.feat-desc { color: #8a8a8a; font-size: 16px; line-height: 1.72; max-width: 420px; margin-bottom: 24px; }
.feat-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.feat-tags span {
  display: inline-flex; align-items: center;
  height: 28px; padding: 0 12px; border-radius: 999px;
  color: #b0b8a8; background: rgba(255, 255, 255, .03);
  font-size: 12px; font-weight: 900; letter-spacing: .03em;
}

.feat-visual {
  position: relative;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; padding: 52px 40px;
  background: rgba(255, 255, 255, .016);
}

// ── 01 流水线 ──
.fv-pipeline { width: 100%; max-width: 360px; display: flex; flex-direction: column; gap: 12px; }
.fv-step {
  display: flex; align-items: center; gap: 14px; padding: 14px 18px; border-radius: 14px;
  background: rgba(255, 255, 255, .03);
  animation: fv-step-in .5s ease both;

  &:nth-child(1) { animation-delay: .1s; }
  &:nth-child(2) { animation-delay: .3s; }
  &:nth-child(3) { animation-delay: .5s; }
  &:nth-child(4) { animation-delay: .7s; }
  &:nth-child(5) { animation-delay: .9s; }
}
@keyframes fv-step-in { from { opacity: 0; transform: translateX(-12px); } to { opacity: 1; transform: none; } }
.fv-step-icon {
  width: 38px; height: 38px; border-radius: 10px; background: rgba(190, 250, 0, .09);
  display: grid; place-items: center; flex-shrink: 0;

  svg { width: 18px; height: 18px; fill: none; stroke: var(--lime); stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
}
.fv-step-text { flex: 1; }
.fv-step-name { font-size: 14px; font-weight: 900; color: #dde8d0; }
.fv-step-sub { font-size: 11px; color: #666; margin-top: 2px; }
.fv-step-badge { font-size: 10px; font-weight: 950; padding: 3px 8px; border-radius: 999px; background: rgba(190, 250, 0, .09); color: var(--lime); letter-spacing: .06em; }
.fv-step.done .fv-step-icon { background: rgba(190, 250, 0, .16); }
.fv-step.done .fv-step-badge { background: var(--lime); color: #101010; }

// ── 02 对话 ──
.fv-chat { width: 100%; max-width: 360px; display: flex; flex-direction: column; gap: 10px; }
.fv-chat-screen { background: #0a0d0a; border-radius: 18px; padding: 18px; display: flex; flex-direction: column; gap: 10px; }
.fv-msg { max-width: 82%; padding: 10px 14px; border-radius: 12px; font-size: 13px; line-height: 1.55; }
.fv-msg.user { align-self: flex-end; background: rgba(190, 250, 0, .1); color: #d8ecc8; border-radius: 12px 12px 2px 12px; }
.fv-msg.ai { align-self: flex-start; background: rgba(255, 255, 255, .05); color: #bfc8b5; border-radius: 12px 12px 12px 2px; }
.fv-msg.ai strong { color: var(--lime); }
.fv-typing { align-self: flex-start; display: flex; gap: 4px; padding: 10px 14px; background: rgba(255, 255, 255, .04); border-radius: 12px; }
.fv-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #555; animation: fv-type 1.2s ease-in-out infinite;
  &:nth-child(2) { animation-delay: .2s; }
  &:nth-child(3) { animation-delay: .4s; }
}
@keyframes fv-type { 0%, 80%, 100% { transform: translateY(0); background: #555; } 40% { transform: translateY(-6px); background: var(--lime); } }

// ── 03 卡点波形 ──
.fv-bpm { width: 100%; max-width: 360px; }
.fv-bpm-bar-row { display: flex; align-items: flex-end; gap: 3px; height: 80px; margin-bottom: 12px; }
.fv-bbar { flex: 1; min-width: 0; border-radius: 2px 2px 0 0; background: rgba(190, 250, 0, .18); transition: background .1s; }
.fv-bbar.beat { background: var(--lime); box-shadow: 0 0 10px rgba(190, 250, 0, .5); }
.fv-bpm-labels { display: flex; justify-content: space-between; margin-bottom: 18px; }
.fv-bpm-labels span { font-size: 10px; font-weight: 900; letter-spacing: .06em; color: #555; }
.fv-bpm-labels span.hi { color: var(--lime); }
.fv-beat-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.fv-beat-chip { padding: 5px 11px; border-radius: 999px; font-size: 11px; font-weight: 950; letter-spacing: .05em; color: var(--lime); background: rgba(190, 250, 0, .07); }
.fv-beat-chip.dim { color: #666; background: transparent; }

// ── 04 模型切换 ──
.fv-models { width: 100%; max-width: 360px; display: flex; flex-direction: column; gap: 10px; }
.fv-model-card {
  display: flex; align-items: center; gap: 14px; padding: 14px 18px;
  border-radius: 14px; background: rgba(255, 255, 255, .03);
  cursor: pointer; transition: background .2s;

  &.active { background: rgba(190, 250, 0, .06); }
}
.fv-model-dot { width: 10px; height: 10px; border-radius: 50%; background: rgba(255, 255, 255, .15); flex-shrink: 0; }
.fv-model-card.active .fv-model-dot { background: var(--lime); box-shadow: 0 0 8px rgba(190, 250, 0, .5); }
.fv-model-info { flex: 1; }
.fv-model-name { font-size: 14px; font-weight: 900; color: #e0e8d8; }
.fv-model-card:not(.active) .fv-model-name { color: #777; }
.fv-model-desc { font-size: 11px; color: #555; margin-top: 2px; }
.fv-model-card.active .fv-model-desc { color: #8a9880; }
.fv-model-badge { font-size: 10px; font-weight: 950; padding: 2px 8px; border-radius: 999px; background: rgba(190, 250, 0, .1); color: var(--lime); letter-spacing: .06em; }
.fv-model-card:not(.active) .fv-model-badge { opacity: 0; }

// ── 05 时间线 ──
.fv-timeline { width: 100%; max-width: 380px; }
.fv-tl-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.fv-tl-header span { font-size: 12px; font-weight: 900; color: #666; letter-spacing: .06em; }
.fv-tl-header strong { color: var(--lime); font-size: 13px; }
.fv-tl-bar { height: 6px; background: rgba(255, 255, 255, .07); border-radius: 99px; overflow: hidden; margin-bottom: 16px; }
.fv-tl-fill { height: 100%; background: linear-gradient(90deg, var(--lime), rgba(190, 250, 0, .5)); border-radius: 99px; animation: fv-fill 4s ease-in-out infinite alternate; }
@keyframes fv-fill { 0% { width: 20%; } 100% { width: 92%; } }
.fv-scenes { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.fv-scene { aspect-ratio: 16/9; border-radius: 6px; overflow: hidden; position: relative; }
.fv-scene-art { position: absolute; inset: 0; }
.fv-scene-num { position: absolute; bottom: 4px; right: 5px; font-size: 9px; font-weight: 900; color: rgba(255, 255, 255, .6); }
.fv-tl-footer { margin-top: 12px; display: flex; justify-content: space-between; font-size: 11px; color: #555; font-weight: 900; letter-spacing: .04em; }

// ── 06 风格网格 ──
.fv-stylegrid { width: 100%; max-width: 360px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.fv-scard {
  aspect-ratio: 4/3; border-radius: 12px; overflow: hidden; position: relative; cursor: pointer;
  transition: transform .2s;

  &:hover { transform: scale(1.04); }
  &:hover .fv-scard-label { opacity: 1; }
}
.fv-scard-art { position: absolute; inset: 0; }
.fv-scard-label {
  position: absolute; inset: 0; display: flex; align-items: flex-end; justify-content: flex-start; padding: 8px 9px;
  background: linear-gradient(transparent 40%, rgba(0, 0, 0, .72));
  font-size: 11px; font-weight: 900; color: #e8f0e0; opacity: 0; transition: opacity .2s;
}

// ── 渐变美术占位（与设计稿一致）──
.art { position: absolute; inset: 0; }
.a-ghibli { background: radial-gradient(ellipse at 62% 18%, rgba(220, 190, 90, .42), transparent 44%), radial-gradient(circle at 18% 78%, rgba(80, 150, 70, .34), transparent 44%), linear-gradient(155deg, #060e06, #1c3810, #4a6a20); }
.a-cinema { background: radial-gradient(circle at 70% 18%, rgba(255, 172, 56, .4), transparent 28%), linear-gradient(145deg, #060504, #2c180a, #5a3818); }
.a-vangogh { background: radial-gradient(ellipse at 68% 20%, rgba(242, 208, 34, .46), transparent 40%), radial-gradient(circle at 16% 68%, rgba(18, 56, 186, .38), transparent 44%), linear-gradient(145deg, #04062a, #0e1e72, #c89008); }
.a-cyber { background: radial-gradient(ellipse at 72% 16%, rgba(255, 0, 240, .4), transparent 36%), radial-gradient(circle at 20% 76%, rgba(0, 220, 255, .3), transparent 40%), linear-gradient(135deg, #08001a, #260040, #12002a); }
.a-shinkai { background: radial-gradient(ellipse at 50% 0%, rgba(255, 140, 55, .44), transparent 44%), radial-gradient(circle at 20% 82%, rgba(20, 40, 165, .38), transparent 46%), linear-gradient(180deg, #060c26, #183060, #4a2c0c); }
.a-dream { background: radial-gradient(circle at 60% 18%, rgba(0, 200, 220, .36), transparent 38%), radial-gradient(circle at 24% 72%, rgba(140, 0, 220, .34), transparent 42%), linear-gradient(135deg, #03000c, #0e0026, #001216); }
.a-inkcn { background: radial-gradient(ellipse at 38% 28%, rgba(198, 188, 166, .32), transparent 50%), linear-gradient(165deg, #eeead8, #b0a898, #2c2018); }

// 响应式：窄屏单列，图文恢复正常顺序
@media (max-width: 1100px) {
  .feat-row { grid-template-columns: 1fr; }
  .feat-row:nth-child(even) .feat-text { order: 1; }
  .feat-row:nth-child(even) .feat-visual { order: 2; }
  .feat-visual { min-height: 340px; }
}
</style>
