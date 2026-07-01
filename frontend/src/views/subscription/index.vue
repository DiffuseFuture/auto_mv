<template>
  <div ref="pageScrollRef" class="subscription-scroll-container relative w-full h-full overflow-y-auto overflow-x-hidden bg-[#232426]">
    <!-- 顶部光效 -->
    <svg class="subscription-hero-glow-svg subscription-hero-glow-svg--left absolute left-5 top-[147px]" width="480" height="480" viewBox="0 0 480 480" aria-hidden="true">
      <defs>
        <radialGradient id="subscriptionGlowLeftGradient" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#BEFA00" stop-opacity="0.38"/>
          <stop offset="24%" stop-color="#BEFA00" stop-opacity="0.30"/>
          <stop offset="52%" stop-color="#BEFA00" stop-opacity="0.18"/>
          <stop offset="76%" stop-color="#BEFA00" stop-opacity="0.08"/>
          <stop offset="100%" stop-color="#BEFA00" stop-opacity="0"/>
        </radialGradient>
        <filter id="subscriptionGlowLeftSoft" x="-360%" y="-360%" width="820%" height="820%" color-interpolation-filters="sRGB">
          <feGaussianBlur stdDeviation="122"/>
        </filter>
      </defs>
      <ellipse cx="240" cy="240" rx="252" ry="252" fill="url(#subscriptionGlowLeftGradient)" filter="url(#subscriptionGlowLeftSoft)"/>
      <ellipse cx="206" cy="262" rx="214" ry="206" fill="#BEFA00" fill-opacity="0.17" filter="url(#subscriptionGlowLeftSoft)"/>
    </svg>

    <svg class="subscription-hero-glow-svg subscription-hero-glow-svg--right absolute right-12 top-[83px]" width="480" height="480" viewBox="0 0 480 480" aria-hidden="true">
      <defs>
        <radialGradient id="subscriptionGlowRightGradient" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#95FD88" stop-opacity="0.32"/>
          <stop offset="24%" stop-color="#95FD88" stop-opacity="0.25"/>
          <stop offset="52%" stop-color="#95FD88" stop-opacity="0.15"/>
          <stop offset="76%" stop-color="#95FD88" stop-opacity="0.065"/>
          <stop offset="100%" stop-color="#95FD88" stop-opacity="0"/>
        </radialGradient>
        <filter id="subscriptionGlowRightSoft" x="-360%" y="-360%" width="820%" height="820%" color-interpolation-filters="sRGB">
          <feGaussianBlur stdDeviation="122"/>
        </filter>
      </defs>
      <ellipse cx="240" cy="240" rx="252" ry="252" fill="url(#subscriptionGlowRightGradient)" filter="url(#subscriptionGlowRightSoft)"/>
      <ellipse cx="278" cy="206" rx="220" ry="204" fill="#95FD88" fill-opacity="0.13" filter="url(#subscriptionGlowRightSoft)"/>
    </svg>

    <div class="relative z-[1] min-h-screen w-full max-w-[1280px] mx-auto px-4 py-6 flex flex-col justify-around items-center gap-6">
      <!-- 标题区 -->
      <div class="flex-col-center w-full">
        <div class="flex-between w-full">
          <!-- 返回按钮 -->
          <button
              class="w-10 h-10 rounded-full flex-center hover:opacity-90 cursor-pointer"
              @click="handleBack"
          >
            <svg-icon name="gy-return" size="34" color="#FFFFFF"></svg-icon>
          </button>
          <h1 class="text-[42px] leading-[52px] mb-0.5 font-bold text-white text-center">
            {{ t('subscription.titlePrefix') }}<span class="text-[#C2FF00]">{{ t('subscription.titleHighlight') }}</span>{{ t('subscription.titleSuffix') }}
          </h1>
          <div class="w-[34px]"></div>
        </div>

        <div class="text-[18px] leading-[26px] mb-5 text-white">{{ t('subscription.yearlySaveTip') }}</div>

        <div class="w-[190px] flex-center rounded-[48px] bg-[#ECECEC] p-[6px]">
          <button
              class="w-[88px] h-[28px] rounded-[48px] text-[20px] text-[#5D634A] transition-all duration-150 cursor-pointer"
              :class="{ 'bg-black text-[#C2FF00]': billingCycle === 'monthly' }"
              @click="billingCycle = 'monthly'"
          >{{ t('subscription.monthly') }}
          </button>
          <button
              class="w-[88px] h-[28px] rounded-[48px] text-[20px] text-[#5D634A] transition-all duration-150 cursor-pointer"
              :class="{ 'bg-black text-[#C2FF00]': billingCycle === 'yearly' }"
              @click="billingCycle = 'yearly'"
          >{{ t('subscription.yearly') }}
          </button>
        </div>
      </div>

      <!-- 卡片区 -->
      <div class="w-full flex flex-wrap justify-center gap-4">
        <div
            v-for="plan in currentPlans"
            :key="plan.tierCode"
            class="w-full max-w-[296px] rounded-[20px] bg-[#FFFFFF] px-[20px] pt-[30px] pb-5 flex flex-col"
        >
          <div class="text-[24px] mb-5 font-bold text-black">{{ plan.tierName }}</div>

          <div class="text-black text-[52px] leading-[56px] mb-[24px] font-bold">¥ {{ plan.displayPrice }}
            <span class="text-[16px] leading-[24px]">{{ plan.isFree ? '' : t('subscription.perMonth') }}</span></div>

          <div class="text-[18px] text-[#000000] mb-[30px]">
            {{plan.isFree ? t('subscription.freeGift') : t('subscription.monthlyPoints') }}{{ plan.grantedPoints }}{{ plan.isFree ? t('subscription.pointsSuffix') : '' }}
          </div>

          <!-- 按钮三态：当前计划（disabled）/ 不可降级（disabled）/ 升级或订阅（可点） -->
          <button
              v-if="comparePlan(plan) === 'current'"
              class="w-full h-[36px] shrink-0 rounded-[10px] mb-8 bg-[#DDE3CA] text-black text-[18px]"
              disabled
          >{{ t('subscription.currentPlan') }}
          </button>
          <button
              v-else-if="comparePlan(plan) === 'downgrade'"
              class="w-full h-[36px] shrink-0 rounded-[10px] mb-8 bg-[#DDE3CA] text-black text-[18px] cursor-not-allowed opacity-70"
              disabled
          >{{ t('subscription.subscribe') }}
          </button>
          <button
              v-else
              class="w-full h-[36px] shrink-0 rounded-[10px] mb-8 bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[18px] cursor-pointer"
              @click="handleSubscribe(plan)"
          >{{ comparePlan(plan) === 'upgrade' ? t('subscription.upgrade') : t('subscription.subscribe') }}
          </button>

          <div class="text-[14px] mb-[10px] text-black font-[500]">{{ t('subscription.featuresTitle') }}</div>
          <div
              v-for="(feature, index) in plan.features"
              :key="index"
              class="text-[14px] mb-[10px] text-black"
          >{{ feature }}
          </div>
        </div>
      </div>

      <!-- 底部文案 -->
      <div class="text-center">
        <div class="cursor-pointer text-[18px] leading-[28px] text-[#C2FF00] hover:underline" @click="handleGoPointsPackage">
          <svg-icon name="gy-integral" size="16"></svg-icon>
          {{ t('subscription.needMorePoints') }}
        </div>
        <div class="text-[18px] leading-[28px] text-white">
          {{ t('common.view') }}<a href="javascript:void(0)" class="text-[#C2FF00] hover:underline" @click="handleOpenFaqPage">{{ t('subscription.faqLink') }}</a>
        </div>
      </div>
    </div>

    <section
      ref="pointsPackageSectionRef"
      class="relative z-[1] min-h-screen w-full max-w-[1280px] mx-auto px-4 py-12 flex flex-col items-center justify-center"
    >
      <h2 class="text-[48px] mb-3 font-bold text-white text-center">
        {{ t('subscription.buyPointsTitlePrefix') }}<span class="text-[#C2FF00]">{{ t('subscription.buyPointsTitleHighlight') }}</span>{{ t('subscription.buyPointsTitleSuffix') }}
      </h2>
      <p class="text-[18px] text-white/70 text-center">
        {{ t('subscription.buyPointsDesc') }}
      </p>

      <div v-if="pointsPackageLoading" class="mt-6 text-[18px] text-white/80">{{ t('subscription.loadingPackages') }}</div>
      <div v-else-if="!pointsPackageList.length" class="mt-6 text-[18px] text-white/80">{{ t('subscription.emptyPackages') }}</div>
      <div v-else class="mt-8 w-full max-w-[1280px] flex flex-nowrap justify-center gap-6">
        <div
          v-for="item in pointsPackageList"
          :key="item.tierCode"
          class="w-[296px] h-[302px] shrink-0 pt-[50px] px-5 pb-[38px] rounded-[20px] bg-[#f5f5f5] text-[#111] flex flex-col justify-between"
        >
          <div class="text-[24px] font-bold"><svg-icon name="gy-integral" size="24"></svg-icon> {{ item.tierName }}</div>
          <div class="text-[64px] font-bold">¥ {{ formatPrice(item.price) }}</div>
          <button
            class="h-12 font-medium rounded-[10px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] text-[#111] text-[20px] border-0 cursor-pointer"
            @click="handleBuyPointsPackage(item)"
          >
            {{ t('subscription.buyNow') }}
          </button>
        </div>
      </div>
    </section>

    <!-- 支付弹窗 -->
    <payment-dialog
        v-model="showPayment"
        :tier-code="selectedPlan?.tierCode"
        :plan-name="selectedPlan?.tierName"
        :price="selectedPlan?.originalPrice"
        @success="handlePaymentSuccess"
    ></payment-dialog>

    <!-- 未登录态点击购买按钮弹出的登录弹窗（与 layout 中同款组件） -->
    <login-dialog v-model="loginVisible"></login-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {useSafeBack} from '@/composables/useSafeBack.js';
import {useUserStore} from '@/store/user.js';
import PaymentDialog from './PaymentDialog.vue';
import LoginDialog from '@/layout/login-dialog.vue';
import {saveUserTracking} from '@/api/tracking.js';
import {isMobileClient} from '@/utils/index.js';
import {useI18nText} from '@/i18n';
import {useSubscriptionPlans} from './useSubscriptionPlans';

const router = useRouter();
const route = useRoute();
const safeBack = useSafeBack();
const userStore = useUserStore();
const {t} = useI18nText();

const pointsPackageSectionRef = ref(null);
const pageScrollRef = ref(null);
let scrollAnimationFrameId = 0;

const {
  billingCycle,
  loading,
  pointsPackageLoading,
  pointsPackageList,
  currentPlans,
  formatPrice,
  comparePlan,
  fetchPlans,
  fetchPointsPackages,
} = useSubscriptionPlans({t, userStore});

const showPayment = ref(false);
const selectedPlan = ref(null);
const loginVisible = ref(false);

/** 返回上一页：站内无上一页时兜底跳首页 */
const handleBack = () => safeBack();

/** 打开订阅 FAQ 独立页面。 */
const handleOpenFaqPage = () => {
  const lang = typeof route.params?.lang === 'string' ? route.params.lang : 'zh';
  router.push({name: 'subscribeFaq', params: {lang}});
};

/** 点击订阅：先打点（不论登录态都算一次有效点击意图），再判登录决定下一步 */
const handleSubscribe = (plan) => {
  if (!isMobileClient()) {
    saveUserTracking({
      target: 'SUBSCRIPTION_CLICK_SUBSCRIBE',
    }).catch((error) => {
      console.error('订阅页点击订阅埋点上报失败:', error);
    });
  }
  if (!userStore.isLoggedIn) {
    loginVisible.value = true;
    return;
  }
  selectedPlan.value = plan;
  showPayment.value = true;
};

/** 计算缓动值（easeInOutCubic） */
const easeInOutCubic = (t) => {
  return t < 0.5
    ? 4 * t * t * t
    : 1 - Math.pow(-2 * t + 2, 3) / 2;
};

/** 获取可滚动容器，优先使用页面容器 ref。 */
const getScrollContainer = (targetEl) => {
  if (pageScrollRef.value) return pageScrollRef.value;

  let current = targetEl?.parentElement;
  while (current) {
    if (current.scrollHeight > current.clientHeight) {
      return current;
    }
    current = current.parentElement;
  }
  return document.scrollingElement || document.documentElement;
};

/** 执行平滑滚动动画（不依赖原生 smooth 支持） */
const animateScrollTo = (containerEl, targetTop, duration = 480) => {
  if (!containerEl) return;
  if (scrollAnimationFrameId) {
    cancelAnimationFrame(scrollAnimationFrameId);
    scrollAnimationFrameId = 0;
  }

  const startTop = containerEl.scrollTop;
  const distance = targetTop - startTop;
  if (Math.abs(distance) < 1) return;

  const startTime = performance.now();
  const run = (now) => {
    const elapsed = now - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased = easeInOutCubic(progress);
    containerEl.scrollTop = startTop + distance * eased;
    if (progress < 1) {
      scrollAnimationFrameId = requestAnimationFrame(run);
    } else {
      scrollAnimationFrameId = 0;
    }
  };

  scrollAnimationFrameId = requestAnimationFrame(run);
};

/** 定位到积分包区域。 */
const handleGoPointsPackage = () => {
  const sectionEl = pointsPackageSectionRef.value;
  if (!sectionEl) return;
  const containerEl = getScrollContainer(sectionEl);

  const sectionRect = sectionEl.getBoundingClientRect();
  const containerRect = containerEl.getBoundingClientRect
    ? containerEl.getBoundingClientRect()
    : {top: 0};
  const targetTop = containerEl.scrollTop + (sectionRect.top - containerRect.top);
  animateScrollTo(containerEl, targetTop > 0 ? targetTop : 0);
};

/** 点击购买积分包：未登录先弹登录框，登录态复用支付弹窗。 */
const handleBuyPointsPackage = (item) => {
  if (!userStore.isLoggedIn) {
    loginVisible.value = true;
    return;
  }
  selectedPlan.value = {
    ...item,
    originalPrice: formatPrice(item.price),
  };
  showPayment.value = true;
};

/** 支付成功后刷新页面数据 */
const handlePaymentSuccess = () => {
  fetchPlans();
  userStore.fetchUserPlan();
};

onMounted(() => {
  fetchPlans();
  fetchPointsPackages();
  if (userStore.isLoggedIn) {
    userStore.fetchUserPlan();
  }
});

onBeforeUnmount(() => {
  if (scrollAnimationFrameId) {
    cancelAnimationFrame(scrollAnimationFrameId);
    scrollAnimationFrameId = 0;
  }
});
</script>

<style lang="scss">
.subscription-scroll-container {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.subscription-scroll-container::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.subscription-hero-glow-svg {
  pointer-events: none;
  overflow: visible;
  transform: scale(1.78);
  transform-origin: center;
  opacity: 1;
}

.subscription-hero-glow-svg--left {
  transform-origin: 30% 58%;
}

.subscription-hero-glow-svg--right {
  transform-origin: 68% 42%;
}

</style>
