<!-- 布局 -->
<template>
  <el-container class="relative h-full min-screen overflow-hidden">
    <!-- 首页：菜单栏绝对定位悬浮在全幅内容（含背景视频）之上；其它页：正常占位排版，避免左侧内容被盖住 -->
    <el-aside width="76px" class="flex flex-col px-2 shrink-0" :class="isHomeRoute ? 'absolute left-0 top-0 h-full z-20 overflow-visible' : 'overflow-hidden'">
      <nav class="flex flex-col items-center" aria-label="main-navigation">
        <!-- logo 始终居中、与下方菜单图标对齐（首页站点标题已移入首页滚动内容，随页面一起滚走） -->
        <div class="flex-center w-[48px] h-[48px] mt-[24px] mb-[34px]">
          <el-image :src="logo" alt="OhYesAI Logo" class="w-10"/>
        </div>
        <div @click="handleChange(item)" class="flex-col-center mb-3 w-14 h-14 cursor-pointer" role="button" :aria-label="item.name" :aria-current="isMenuActive(item.routeName) ? 'page' : undefined" v-for="item in menuList" :key="item.name">
          <svg-icon class="mb-[10px]" :name="item.icon" size="24" :color="isMenuActive(item.routeName) ? '#C2FF00' : '#FFFFFF'"></svg-icon>
          <div class="text-white text-[14px] leading-[22px]" :style="{ color: isMenuActive(item.routeName) ? '#C2FF00' : '#FFFFFF' }">
            {{ item.name }}
          </div>
        </div>
      </nav>

    </el-aside>

    <el-main class="bg-white min-h-0" role="main">
      <router-view></router-view>
    </el-main>
  </el-container>

  <!-- 未登录点击受保护菜单（创作 / 资源）时弹出 -->
  <login-dialog v-model="loginVisible"></login-dialog>
</template>

<script setup>
import logo from '@/assets/common/logo2.png';
import {ref, computed} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import LoginDialog from '@/layout/login-dialog.vue';
import {useUserStore} from '@/store/user.js';
import {useI18nText} from '@/i18n';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const isLogin = computed(() => userStore.isLoggedIn);
/** 首页让菜单栏悬浮（绝对定位）在全幅内容之上，其它页菜单栏正常占位。 */
const isHomeRoute = computed(() => route.name === 'home');
const loginVisible = ref(false);
const {t, locale, changeLocale, availableLocales, getLocalePrefix} = useI18nText();

/** 需要登录才能进入的菜单 routeName 列表 */
const PROTECTED_MENU_ROUTES = ['creation', 'resource'];

const lang = computed(() => {
  const p = getLocalePrefix(locale.value);
  return p === 'zh' ? 'zh' : 'en';
});

const getLocaleLabel = (value) => {
  if (value === 'zh-CN') return '中文';
  if (value === 'en-US') return 'EN';
  return value;
};

const menuList = computed(() => [
  {
    name: t('layout.menu.home'),
    icon: 'gy-frontpage',
    routeName: 'home',
    to: {name: 'home', params: {lang: lang.value}},
  },
  {
    name: t('layout.menu.creation'),
    icon: 'gy-creation',
    routeName: 'creation',
    to: {name: 'creation', params: {lang: lang.value}},
  },
  {
    name: t('layout.menu.resource'),
    icon: 'gy-resource',
    routeName: 'resource',
    to: {name: 'resource', params: {lang: lang.value}},
  },
]);

const isMenuActive = (menuRouteName) => route.name === menuRouteName;

const handleLocaleChange = async (value) => {
  await changeLocale(value);
  await router.replace({
    name: route.name || 'home',
    params: {...(route.params || {}), lang: getLocalePrefix(value)},
    query: route.query,
  });
};
/** 菜单点击：受保护菜单（创作 / 资源）未登录时拦截并弹出登录框，其它情况正常跳转。 */
const handleChange = (item) => {
  if (PROTECTED_MENU_ROUTES.includes(item.routeName) && !isLogin.value) {
    loginVisible.value = true;
    return;
  }
  router.push(item.to);
};
</script>

<style lang="scss">
.locale-select {
  .el-select__wrapper {
    @apply bg-white/[0.06] rounded-xl px-[6px];
    min-height: 30px !important;
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12) !important;
  }

  .el-select__selected-item {
    @apply flex-1 min-w-0 text-center justify-center text-[12px] leading-4 font-semibold text-white/90;
  }

  .el-select__caret {
    @apply text-[#c2ff00];
  }

  // 下拉箭头占位过大时压缩，避免“中文”被截断
  .el-select__suffix {
    @apply ml-[2px];
  }

  &.is-disabled {
    @apply opacity-55;
  }
}

.locale-select-popper {
  // Prefer Element Plus CSS variables for theming.
  // Reason: avoids fighting component default specificity with many !important rules.
  @apply border-0 rounded-[8px] overflow-hidden;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.55) !important;

  // dropdown container/background
  --el-bg-color-overlay: #070707;
  --el-select-dropdown-bg-color: #070707;
  --el-border-color-light: rgba(255, 255, 255, 0.12);

  // interactive tokens
  --el-color-primary: #c2ff00;
  --el-text-color-regular: rgba(255, 255, 255, 0.86);
  --el-fill-color-light: rgba(184, 255, 26, 0.12);

  .el-popper__arrow {
    @apply hidden;
  }

  .el-select-dropdown {
    @apply border-0;
    background: var(--el-select-dropdown-bg-color) !important;
    border: none !important;
  }

  .el-select-dropdown__item {
    @apply rounded-[8px] font-semibold text-[12px] leading-[28px] h-7 px-1;
    color: var(--el-text-color-regular) !important;

    &:hover {
      background: var(--el-fill-color-light) !important;
      color: #C2FF00 !important;
    }

    &.is-selected {
      background: rgba(194, 255, 0, 0.18) !important;
      color: var(--el-color-primary) !important;
    }
  }
}

.logout-confirm {
  width: 740px !important;
  max-width: 740px !important;
  height: 256px !important;
  background: #FFFFFF !important;
  border-radius: 16px !important;
  padding: 40px !important;

  .el-message-box__header {
    padding: 0 !important;
  }

  .el-message-box__title {
    font-size: 24px;
    font-weight: 700;
    color: #000000;
    line-height: 32px;
  }

  .el-message-box__content {
    padding: 12px 0 0 !important;
  }

  .el-message-box__message {
    font-size: 20px;
    font-weight: 300;
    color: #666666;
    line-height: 28px;

    p {
      margin: 0;
    }
  }

  .el-message-box__btns {
    padding: 40px 0 0 !important;

    .el-button {
      width: 144px !important;
      height: 48px !important;
      border-radius: 8px !important;
      font-size: 16px !important;
      font-weight: 500 !important;
      margin: 0 !important;
      color: #000000;

      &:first-child {
        margin-right: 16px !important;
        border: 2px solid #000000;

        &:hover {
          background-color: transparent;
        }
      }
    }

    .el-button--primary {
      border: none !important;
      background: #000000 !important;
      color: #C2FF00 !important;
    }
  }
}

</style>
