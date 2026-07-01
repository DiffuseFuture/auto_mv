<template>
  <nav class="fixed inset-x-0 z-40 h-[62px] flex-center gap-[75px] bg-[rgba(48,50,41,0.7)]" style="bottom: env(safe-area-inset-bottom, 0px);">
    <!--
      为什么每个 tab 图标都自带一份内联 <svg> + 本地 <defs>：
      iOS Safari WebKit 有两条已知 bug（Bugzilla #41952 / #189499）：
        (1) <use> 跨 SVG 引用外部 <defs> 里的 paint server 时，缓存键挂在被引用 symbol 的 shadow tree 上
        (2) <use fill> 在 "url(#id)" → 普通颜色 → 再切回 "url(#id)" 时不会重新解析 paint server
      首页是初始路由：fill 走 url → #FFFFFF → url 这条路径，正好踩中 (2)；项目/订阅只走 #FFFFFF → url 一次，绕开。
      iOS 17/18 至今仍存在。修法（同时绕开 (1) 和 (2)）：
        - 每个 tab 的 <linearGradient> 定义在和 <use> 同一个 <svg> 内 → 不跨 SVG 引用
        - <use fill> 永远是同一个本地 url 字符串 → 不发生 url↔hex 切换
        - active 切换改的是 stop-color → 同 svg 内的 attr 变更，iOS 重绘是正确的
    -->
    <RouterLink :to="homeTo">
      <div class="flex-col-center gap-1 text-[12px]">
        <svg width="24" height="24" viewBox="0 0 1024 1024" aria-hidden="true">
          <defs>
            <linearGradient id="mobile-tab-icon-home-fill" x1="0%" y1="0%" x2="100%" y2="100%" gradientTransform="rotate(299)">
              <stop offset="0%" :stop-color="isHome ? '#BEFA00' : '#FFFFFF'" />
              <stop offset="100%" :stop-color="isHome ? '#82FF79' : '#FFFFFF'" />
            </linearGradient>
          </defs>
          <use xlink:href="#gy-home" fill="url(#mobile-tab-icon-home-fill)" />
        </svg>
        <div :class="isHome ? activeTextClass : 'text-white'">首页</div>
      </div>
    </RouterLink>
    <button type="button" @click="handleResourceTabClick">
      <div class="flex-col-center gap-1 text-[12px]">
        <svg width="24" height="24" viewBox="0 0 1024 1024" aria-hidden="true">
          <defs>
            <linearGradient id="mobile-tab-icon-resource-fill" x1="0%" y1="0%" x2="100%" y2="100%" gradientTransform="rotate(299)">
              <stop offset="0%" :stop-color="isResource ? '#BEFA00' : '#FFFFFF'" />
              <stop offset="100%" :stop-color="isResource ? '#82FF79' : '#FFFFFF'" />
            </linearGradient>
          </defs>
          <use xlink:href="#gy-project" fill="url(#mobile-tab-icon-resource-fill)" />
        </svg>
        <div :class="isResource ? activeTextClass : 'text-white'">项目</div>
      </div>
    </button>
    <RouterLink :to="subscribeTo" @click="handleSubscribeTabClick">
      <div class="flex-col-center gap-1 text-[12px]">
        <svg width="24" height="24" viewBox="0 0 1024 1024" aria-hidden="true">
          <defs>
            <linearGradient id="mobile-tab-icon-subscription-fill" x1="0%" y1="0%" x2="100%" y2="100%" gradientTransform="rotate(299)">
              <stop offset="0%" :stop-color="isSubscribe ? '#BEFA00' : '#FFFFFF'" />
              <stop offset="100%" :stop-color="isSubscribe ? '#82FF79' : '#FFFFFF'" />
            </linearGradient>
          </defs>
          <use xlink:href="#gy-Homepage" fill="url(#mobile-tab-icon-subscription-fill)" />
        </svg>
        <div :class="isSubscribe ? activeTextClass : 'text-white'">订阅</div>
      </div>
    </RouterLink>
  </nav>

  <MobileLoginSheet v-model="loginVisible"/>
</template>

<script setup>
import {computed, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {saveUserTracking} from '@/api/tracking.js';
import {useUserStore} from '@/store/user.js';
import MobileLoginSheet from '@/components/mobile/MobileLoginSheet.vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

/** 未登录点击受保护 tab 时打开本地登录覆盖层，登录后保留在当前页面。 */
const loginVisible = ref(false);

/**
 * 点击订阅 Tab 进入订阅页时上报 HOME_CLICK_UPGRADE 埋点。
 * 跟 PC 端「升级」按钮、移动端积分详情升级按钮共用同一个 target，方便后端按"升级入口点击"聚合。
 * RouterLink 自身的导航行为不打断（不调 preventDefault）。
 */
const handleSubscribeTabClick = () => {
  saveUserTracking({target: 'HOME_CLICK_UPGRADE'}).catch((error) => {
    console.error('移动端订阅 Tab 点击埋点上报失败:', error);
  });
};

/** 项目 Tab：未登录拦截弹登录覆盖层，已登录正常跳转 resource 页。 */
const handleResourceTabClick = () => {
  if (!userStore.isLoggedIn) {
    loginVisible.value = true;
    return;
  }
  router.push(resourceTo.value);
};

const lang = computed(() => route.params?.lang || 'zh');
const homeTo = computed(() => ({name: 'home', params: {lang: lang.value}}));
const resourceTo = computed(() => ({name: 'resource', params: {lang: lang.value}}));
const subscribeTo = computed(() => ({name: 'subscribe', params: {lang: lang.value}}));

const isHome = computed(() => route.name === 'home');
const isResource = computed(() => route.name === 'resource');
const isSubscribe = computed(() => route.name === 'subscribe');
const activeTextClass = 'bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] bg-clip-text text-transparent';
</script>
