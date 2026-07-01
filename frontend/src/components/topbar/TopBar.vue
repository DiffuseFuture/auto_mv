<!--
  顶部账户操作区（首页 / 创作 / 资源共用）：功能按钮（指南 / FAQ / 微信 / 邀请）+ 积分胶囊 + 头像账户菜单。
  三者始终成组展示，故合并为一个组件。登录判断逐项保留：邀请按钮、积分胶囊、头像各自按登录态切换。
  mode 控制积分胶囊 hover 内容：'menu'（home / resource）下拉购买/明细；'session'（creation）展示当前会话流水。
-->
<template>
  <div class="flex items-center gap-3">
    <!-- ===== 功能按钮组（仅首页展示，创作 / 资源页隐藏）===== -->
    <div v-if="isHomeRoute" class="flex items-center gap-2">
      <!-- 使用指南：所有用户可见 -->
      <button class="topbar-tool-btn" :title="t('layout.menu.guide')" @click="handleGoGuide">
        <svg-icon name="gy-help" size="20" color="#C2FF00"></svg-icon>
      </button>

      <!-- 常见问题：所有用户可见 -->
      <button class="topbar-tool-btn" :title="t('layout.menu.faq')" @click="handleGoFaq">
        <svg-icon name="gy-questions" size="20" color="#C2FF00"></svg-icon>
      </button>

      <!-- 微信群 / 客服：所有用户可见，点击弹二维码 -->
      <el-popover v-model:visible="wechatQrPopoverVisible" placement="bottom-end" trigger="click" :width="390" :show-arrow="false" popper-class="wechat-hover-popover">
        <template #reference>
          <button class="topbar-tool-btn" :title="t('layout.wechat.joinGroup')">
            <svg-icon name="gy-WeChat" size="20" color="#C2FF00"></svg-icon>
          </button>
        </template>

        <div class="flex gap-[10px]">
          <div>
            <div class="w-[180px] h-[180px] bg-white rounded-[20px] mb-[10px] overflow-hidden">
              <img :src="wechatGroupQr" alt="community-qr" class="w-full h-full object-contain">
            </div>
            <div class="text-center text-[14px] leading-[20px] text-[#C2FF00]">{{ t('layout.wechat.joinGroup') }}</div>
            <div class="text-center text-[12px] leading-[18px] text-white/55 mt-1">{{ t('layout.wechat.joinGroupDesc') }}</div>
          </div>

          <div>
            <div class="w-[180px] h-[180px] bg-white rounded-[20px] mb-[10px] overflow-hidden">
              <img :src="wechatServiceQr" alt="support-qr" class="w-full h-full object-contain">
            </div>
            <div class="text-center text-[14px] leading-[20px] text-[#C2FF00]">{{ t('layout.wechat.contactSupport') }}</div>
            <div class="text-center text-[12px] leading-[18px] text-white/55 mt-1">{{ t('layout.wechat.contactSupportDesc') }}</div>
          </div>
        </div>
      </el-popover>

      <!-- 邀请好友：仅登录态可见 -->
      <button v-if="isLogin" class="topbar-tool-btn" :title="t('layout.account.inviteFriend')" @click="handleOpenInviteDialog">
        <svg-icon name="gy-share-gift" size="20" color="#C2FF00"></svg-icon>
      </button>
    </div>

    <!-- ===== 积分胶囊（登录态：胶囊 + hover；未登录态：定价按钮）===== -->
    <el-popover
        v-if="isLogin"
        trigger="hover"
        placement="bottom-end"
        :width="mode === 'session' ? 550 : 220"
        :show-arrow="false"
        :popper-class="mode === 'session' ? 'session-points-popover' : 'points-menu-popover'"
        @before-enter="handlePointsPopoverEnter"
    >
      <template #reference>
        <div
            class="inline-flex items-center gap-1.5 h-9 px-[14px] rounded-full border border-[rgba(194,255,0,0.55)] bg-[rgba(194,255,0,0.12)] text-[#C2FF00] text-[14px] font-semibold leading-none cursor-pointer transition hover:bg-[rgba(194,255,0,0.22)] hover:shadow-[0_0_12px_rgba(194,255,0,0.45)]"
            :title="String(userStore.pointsBalance)"
            @click="handleCapsuleClick"
        >
          <svg-icon name="gy-integral" size="20"></svg-icon>
          <span>{{ formattedPoints }}</span>
        </div>
      </template>

      <!-- menu 模式：购买附加积分 / 积分使用详情 -->
      <div v-if="mode === 'menu'" class="flex flex-col gap-0.5">
        <button class="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-[10px] border-0 bg-transparent text-white text-[14px] leading-none cursor-pointer transition-colors hover:bg-white/10 hover:text-[#C2FF00]" @click="handleBuyAddOnPoints">
          <svg-icon name="gy-integral" size="18"></svg-icon>
          <span>{{ t('layout.account.buyAddOnPoints') }}</span>
        </button>
        <button class="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-[10px] border-0 bg-transparent text-white text-[14px] leading-none cursor-pointer transition-colors hover:bg-white/10 hover:text-[#C2FF00]" @click="handlePointsDetail">
          <svg-icon name="gy-Script" size="18"></svg-icon>
          <span>{{ t('layout.account.pointsDetail') }}</span>
        </button>
      </div>

      <!-- session 模式：当前会话积分流水 -->
      <template v-else-if="mode === 'session'">
        <div class="flex justify-between items-center mb-2">
          <span class="text-[16px] font-bold text-[#000000]">{{ t('account.points.detail') }}</span>
          <span v-if="sessionPointsUpdateTime" class="text-[13px] text-[#A7A7A6]">{{ t('creation.topBar.updatedAt') }}{{ sessionPointsUpdateTime }}</span>
        </div>
        <div class="border-t border-[#A7A7A6]/30 mb-2"></div>

        <div class="max-h-[200px] overflow-y-auto no-scrollbar">
          <div v-if="sessionPointsLoading" class="py-4 text-center text-[#A7A7A6] text-[14px]">{{ t('common.loading') }}</div>
          <div v-else-if="!sessionPointsList.length" class="py-4 text-center text-[#A7A7A6] text-[14px]">{{ t('account.points.empty') }}</div>
          <div v-else v-for="item in sessionPointsList" :key="item.id" class="flex justify-between items-center py-[6px] text-[14px] text-[#141414]">
            <span class="w-[53%] truncate">{{ item.description }}</span>
            <span class="w-[35%] text-center">{{ formatDate(item.createTime, 'YYYY/MM/DD HH:mm:ss') }}</span>
            <span class="w-[12%] text-right">{{ item.amount }}</span>
          </div>
        </div>

        <div class="border-t border-[#A7A7A6]/30 pt-2">
          <span class="text-[12px] text-[#A7A7A6]">{{ t('creation.topBar.pointsTip') }}</span>
        </div>
      </template>
    </el-popover>

    <button
        v-else
        class="inline-flex items-center gap-1.5 h-9 px-4 rounded-full border border-current text-[#C2FF00] text-[14px] leading-none cursor-pointer transition hover:shadow-[0_0_12px_rgba(194,255,0,0.55)]"
        @click="handlePricing"
    >
      <svg-icon name="gy-integral" size="16"></svg-icon>
      <span>{{ t('common.pricing') }}</span>
    </button>

    <!-- ===== 头像账户菜单（登录态：头像 + hover；未登录态：登录 / 注册）===== -->
    <el-popover
        v-if="isLogin"
        placement="bottom-end"
        trigger="hover"
        :width="260"
        :show-arrow="false"
        popper-class="user-info-popover"
    >
      <template #reference>
        <el-avatar class="cursor-pointer" :src="userStore.userInfo?.avatarImg || defaultAvatar" :size="40"></el-avatar>
      </template>
      <div class="px-2 py-[14px]">
        <div class="flex items-center mb-[10px]">
          <el-avatar :src="userStore.userInfo?.avatarImg || defaultAvatar" :size="60"></el-avatar>
          <div class="ml-[10px]">
            <div class="text-[16px] leading-5 font-normal mb-0.5 text-white">
              {{ userStore.userInfo?.nickName || t('layout.account.userNameFallback') }}
            </div>
            <div class="text-[14px] leading-5 font-normal text-white">{{ userStore.userInfo?.mobile || '' }}</div>
          </div>
        </div>

        <div class="bg-[#F0F6DD] rounded-[10px] px-[6px] py-[10px] mb-[10px]">
          <div class="flex-between">
            <span class="text-[14px] leading-5 text-[#000000]">{{ userStore.tierName }}</span>
            <button class="w-[50px] h-5 rounded-[6px] text-[14px] cursor-pointer bg-[#000000] text-[#C2FF00]" @click="handleUpgrade">
              {{ t('common.upgrade') }}
            </button>
          </div>
          <div class="border-t border-dashed border-[#A7A7A6] mt-[6px] mb-1"></div>
          <div class="flex-between text-[#000000]">
            <span class="text-[14px] leading-[22px]">{{ t('layout.account.vPoints') }}</span>
            <span class="text-[14px] leading-[22px]">{{ userStore.pointsBalance }}</span>
          </div>
        </div>

        <div class="flex-between mb-[6px] text-white text-[14px] leading-[22px] cursor-pointer" @click="handleManage">
          <span>{{ t('layout.account.manage') }}</span>
          <svg-icon name="gy-enter" size="12"></svg-icon>
        </div>

        <div class="text-white text-[14px] leading-[22px]">
          <span @click="logout" class="cursor-pointer">{{ t('layout.account.logout') }}</span>
        </div>
      </div>
    </el-popover>

    <button
        v-else
        class="inline-flex items-center gap-1.5 h-9 px-4 rounded-full bg-[#C2FF00] text-black text-[14px] font-semibold leading-none cursor-pointer transition hover:brightness-105 hover:shadow-[0_0_12px_rgba(194,255,0,0.55)]"
        @click="openLogin"
    >
      <svg-icon name="gy-character" size="16"></svg-icon>
      <span>{{ t('layout.account.loginNow') }}</span>
    </button>

    <!-- 共享 dialogs：composable 用 module 级 ref，多个实例共用同一份显隐状态 -->
    <invite-dialog v-model="inviteDialogVisible"></invite-dialog>
    <LoginDialog v-model="loginVisible" v-model:invite-code="referralCode" />
    <AccountDialog v-model="accountVisible" :default-tab="accountDialogTab" />
  </div>
</template>

<script setup>
import {ref, computed, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {useUserStore} from '@/store/user';
import {useI18nText} from '@/i18n';
import {useTopBarAccount} from '@/composables/useTopBarAccount.js';
import {getSessionPoints} from '@/api/creation';
import {saveUserTracking} from '@/api/tracking.js';
import {formatDate, isMobileClient} from '@/utils/index.js';
import InviteDialog from '@/components/InviteDialog.vue';
import LoginDialog from '@/layout/login-dialog.vue';
import AccountDialog from '@/layout/account-dialog.vue';
import wechatGroupQr from '@/assets/common/wechat-group.jpeg';
import wechatServiceQr from '@/assets/common/kefu.jpeg';
import defaultAvatar from '@/assets/common/avatar.jpg';

const props = defineProps({
  /** 积分胶囊 hover 内容模式：'menu'（home / resource）/ 'session'（creation 当前会话流水）。 */
  mode: {
    type: String,
    default: 'menu',
    validator: (v) => ['menu', 'session'].includes(v),
  },
  /** session 模式必填：当前会话 ID，用来拉取该会话的积分流水。 */
  sessionId: {type: String, default: ''},
  /** 升级 / 定价相关埋点 target。home 传 'HOME_CLICK_UPGRADE'，其它页留空不上报。 */
  upgradeTracking: {type: String, default: ''},
});

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const {t, locale, getLocalePrefix} = useI18nText();
const {
  loginVisible,
  accountVisible,
  accountDialogTab,
  referralCode,
  openLogin,
  openAccount,
  goSubscribe,
  logout,
} = useTopBarAccount();

/** 是否已登录：决定邀请按钮、积分胶囊、头像各自的展示分支。 */
const isLogin = computed(() => userStore.isLoggedIn);
/** 仅首页展示「指南 / FAQ / 微信 / 邀请」功能按钮组。 */
const isHomeRoute = computed(() => route.name === 'home');
/** 当前语言前缀，用于带语言跳转指南 / FAQ。 */
const lang = computed(() => (getLocalePrefix(locale.value) === 'zh' ? 'zh' : 'en'));
/** 积分余额带千位分隔展示。 */
const formattedPoints = computed(() => (userStore.pointsBalance ?? 0).toLocaleString());

const wechatQrPopoverVisible = ref(false);
const inviteDialogVisible = ref(false);

// 微信二维码气泡打开时，仅首页且非移动端上报「查看二维码」埋点
watch(wechatQrPopoverVisible, (open) => {
  if (!open || isMobileClient() || route.name !== 'home') return;
  saveUserTracking({target: 'HOME_VIEW_QR_CODE'}).catch((error) => console.error('查看二维码埋点上报失败:', error));
});

/** 跳转使用指南页。 */
const handleGoGuide = () => router.push({name: 'guide', params: {lang: lang.value}});

/** 跳转常见问题页。 */
const handleGoFaq = () => router.push({name: 'faq', params: {lang: lang.value}});

/** 打开邀请好友弹窗（顺手在首页上报埋点）。 */
const handleOpenInviteDialog = () => {
  inviteDialogVisible.value = true;
  if (!isMobileClient() && route.name === 'home') {
    saveUserTracking({target: 'HOME_VIEW_INVITE_RULES'}).catch((error) => console.error('查看邀请规则埋点上报失败:', error));
  }
};

// ==== 积分胶囊 ====
const sessionPointsList = ref([]);
const sessionPointsLoading = ref(false);
const sessionPointsUpdateTime = ref('');

/** popover 打开前：session 模式拉当前会话积分明细，menu 模式无副作用。 */
const handlePointsPopoverEnter = () => {
  if (props.mode !== 'session') return;
  fetchSessionPoints();
};

/** 拉取当前会话积分流水与更新时间。 */
const fetchSessionPoints = async () => {
  if (!props.sessionId) return;
  sessionPointsLoading.value = true;
  try {
    const data = await getSessionPoints({sessionId: props.sessionId});
    sessionPointsList.value = data || [];
    sessionPointsUpdateTime.value = sessionPointsList.value.length
        ? formatDate(sessionPointsList.value[0].createTime, 'YYYY/MM/DD HH:mm:ss')
        : '';
  } catch (error) {
    console.error(error);
    sessionPointsList.value = [];
    sessionPointsUpdateTime.value = '';
  } finally {
    sessionPointsLoading.value = false;
  }
};

/** 胶囊点击：session 模式跳订阅页；menu 模式不响应（hover 已给入口）。 */
const handleCapsuleClick = () => {
  if (props.mode !== 'session') return;
  goSubscribe(props.upgradeTracking);
};

/** menu 模式「购买附加积分」：跳订阅页（保留原行为：不上报埋点）。 */
const handleBuyAddOnPoints = () => goSubscribe();

/** menu 模式「积分使用详情」：打开账户弹窗的「V 点」tab。 */
const handlePointsDetail = () => openAccount('vpoint');

/** 未登录态「定价」按钮：跳订阅页（按 upgradeTracking 上报埋点）。 */
const handlePricing = () => goSubscribe(props.upgradeTracking);

// ==== 头像账户菜单 ====
/** 信息卡内「升级」按钮：跳订阅 + 按需上报埋点。 */
const handleUpgrade = () => goSubscribe(props.upgradeTracking);

/** 信息卡内「管理账号」行：打开账户管理弹窗到「个人资料」tab。 */
const handleManage = () => openAccount('profile');
</script>

<style lang="scss" scoped>
.topbar-tool-btn {
  @apply w-9 h-9 flex-center rounded-[10px] cursor-pointer transition-colors;
  @apply bg-[rgb(194_255_0_/_0.1)];

  &:hover {
    @apply bg-[rgba(90,100,60,0.75)];
  }
}
</style>

<!-- popper 容器样式：el-popover teleport 到 body，必须用非 scoped 块靠 popper-class 命中 -->
<style lang="scss">
.wechat-hover-popover {
  @apply h-auto border-0 rounded-[20px] bg-[#383E25] p-[10px] !important;
}

.points-menu-popover {
  @apply border-0 rounded-[10px] bg-[#81876E] p-2 shadow-[0_0_10px_0_rgba(0,0,0,0.3)] !important;
}

.session-points-popover {
  @apply border-0 rounded-[20px] bg-[#F0F6DD] px-3 pt-5 pb-[18px] shadow-[0_4px_20px_rgba(0,0,0,0.3)] !important;
}

.user-info-popover {
  @apply border-0 rounded-[10px] bg-[#81876E] p-0 shadow-[0_0_10px_0_rgba(0,0,0,0.3)] !important;
}
</style>
