import {createApp} from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import {createPinia} from 'pinia';
import router from './router/index';
import './style.css';
import App from './App.vue';
import './assets/iconfont/symbol.js';
import SvgIcon from './components/SvgIcon.vue';
import {ensureLocaleLoaded, getCurrentLocale, setupI18n} from './i18n';
import {isMobileClient} from './utils/index.js';
import {captureBdVid} from './utils/baiduTrack.js';

// 模块顶层立即抓取 bd_vid：在任何 await / vue-router 重定向（会丢 query）发生之前
// 锁住裸落地 URL。后续 router.beforeEach 还会再兜底一次。
captureBdVid();

// 关掉浏览器对 reload 的滚动位置自动恢复（SPA 内路由切换的滚动由 router.scrollBehavior 接管）。
// 登录后 reload 的"页面不在顶部"问题由 useLogin.js 的 reloadFromTop 在 unload 前归零处理；
// 这里只是确保浏览器不在新页面里又把旧 scrollY 写回去。
if ('scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

/**
 * 判断当前页面是否处于微信 OAuth 回跳态（code + state）。
 * @returns {boolean}
 */
const isWechatOAuthCallback = () => {
  const params = new URLSearchParams(window.location.search || '');
  return params.has('code') && params.has('state');
};

/**
 * 判断当前 referrer 是否应写入 last touch。
 * @param {string} referrerUrl
 * @returns {boolean}
 */
const shouldUpdateReferer = (referrerUrl) => {
  if (!referrerUrl) return false;
  if (isWechatOAuthCallback()) return false;
  try {
    const referrer = new URL(referrerUrl);
    const currentHost = window.location.hostname;
    if (referrer.hostname === currentHost) return false;
    return true;
  } catch {
    return false;
  }
};

const bootstrap = async () => {
  await ensureLocaleLoaded(getCurrentLocale());

  // 仅用“能力特征/UA”判断移动端，不用屏幕宽度。
  // 用于：给 html 打标记，启用移动端 rem 基准（px->rem 仅对移动端文件生效）。
  try {
    document.documentElement.classList.toggle('is-mobile', isMobileClient());
  } catch (error) {
    console.error('[bootstrap] set is-mobile failed:', error);
  }

  const app = createApp(App);
  const referrer = document.referrer || '';
  if (shouldUpdateReferer(referrer)) {
    sessionStorage.setItem('refererUrl', referrer);
  } else if (!sessionStorage.getItem('refererUrl')) {
    sessionStorage.setItem('refererUrl', '');
  }

  // 捕获 URL 查询参数中的渠道来源标记（如 utm_source），优先级高于 document.referrer。
  // 适用场景：外部站点通过 rel="noreferrer" 跳转时，referrer 为空，
  // 但对方可以在链接上带 ?utm_source=xxx 来标识来源。
  const urlParams = new URLSearchParams(window.location.search || '');
  const utmSource = urlParams.get('utm_source');
  if (utmSource) {
    sessionStorage.setItem('refererUrl', utmSource);
  }

  app.component('SvgIcon', SvgIcon);
  app.use(createPinia());
  app.use(router);
  app.use(ElementPlus);
  setupI18n(app);
  app.mount('#app');
};

bootstrap();
