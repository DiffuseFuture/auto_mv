<template>
  <div class="subscription-mobile-page relative w-full min-h-screen bg-[#232426] overflow-x-hidden">
    <!-- 顶部轻量光效（移动端） -->
    <div class="pointer-events-none absolute -top-28 left-1/2 -translate-x-1/2 w-[520px] h-[520px] rounded-full blur-[90px] opacity-70"
         style="background: radial-gradient(circle, rgba(194,255,0,0.25) 0%, rgba(194,255,0,0.10) 35%, rgba(194,255,0,0) 70%);"
    ></div>

    <div class="relative z-[1]">
      <!-- 第一屏：仅展示套餐 -->
      <section class="min-h-screen px-4 pt-[35px] pb-24">
        <!-- 标题区 -->
        <div class="flex flex-col items-center text-center mb-[25px]">
          <div class="text-[29px] font-bold text-white">
            {{ t('subscription.titlePrefix') }}<span class="text-[#C2FF00]">{{ t('subscription.titleHighlight') }}</span>{{ t('subscription.titleSuffix') }}
          </div>
          <div class="mt-[10px] text-[12px] leading-[18px] text-white">
            <span>{{ t('subscription.needMorePointsPrefix') }}</span>
            <button
              type="button"
              class="text-[#C2FF00] cursor-pointer"
              @click="handleGoPointsPackage"
            >
              {{ t('subscription.buyAddOnPackage') }}
            </button>
          </div>

          <!-- 月付/年付切换 -->
          <div class="mt-[10px] flex-center rounded-full bg-[#ECECEC] p-1">
            <button
              class="w-[67px] h-[24px] rounded-full text-[15px] text-[#5D634A] transition-all duration-150 cursor-pointer"
              :class="{ 'bg-black text-[#C2FF00]': billingCycle === 'monthly' }"
              @click="billingCycle = 'monthly'"
            >{{ t('subscription.monthly') }}</button>
            <button
              class="w-[67px] h-[24px] rounded-full text-[15px] text-[#5D634A] transition-all duration-150 cursor-pointer relative"
              :class="{ 'bg-black text-[#C2FF00]': billingCycle === 'yearly' }"
              @click="billingCycle = 'yearly'"
            >
              {{ t('subscription.yearly') }}
              <img
                :src="hotImg"
                alt="hot"
                class="absolute -top-3 -right-[14px] w-[37px] h-auto rotate-[346°] select-none pointer-events-none"
              />
            </button>
          </div>
        </div>

        <div class="flex items-center justify-center mb-3">
          <div v-if="loading" class="text-[14px] text-white/70">{{ t('subscription.loadingPackages') }}</div>
        </div>

        <div v-if="!loading" class="relative">
          <!-- 左右切换按钮（非必须，提供“类似轮播”的控制） -->
          <button
            class="absolute left-0 top-1/2 -translate-y-1/2 z-20 w-5 h-5 rounded-full bg-[#8E937E] text-white flex-center"
            @click="scrollPlans(-1)"
            aria-label="prev"
          >
            <svg-icon name="gy-enter" size="12" color="#FFFFFF" class="rotate-180"></svg-icon>
          </button>
          <button
            class="absolute right-0 top-1/2 -translate-y-1/2 z-20 w-5 h-5 rounded-full bg-[#8E937E] text-white flex-center"
            @click="scrollPlans(1)"
            aria-label="next"
          >
            <svg-icon name="gy-enter" size="12" color="#FFFFFF"></svg-icon>
          </button>

          <div
            ref="plansScrollerRef"
            class="overflow-x-auto no-scrollbar px-[68px]"
            style="scroll-snap-type: x mandatory;"
            @scroll.passive="onPlansScroll"
          >
            <div class="flex gap-3 w-max py-2">
              <div
                v-for="(plan, idx) in currentPlans"
                :key="plan.tierCode"
                data-plan-card
                class="w-[240px] h-[375px] shrink-0 rounded-[10px] bg-white px-[15px] pt-5 border"
                :class="idx === activePlanIndex ? 'border-[#C2FF00] shadow-[0_0_0_1px_rgba(194,255,0,0.35),0_10px_30px_rgba(0,0,0,0.25)]' : 'border-white/0 shadow-[0_8px_26px_rgba(0,0,0,0.18)]'"
                style="scroll-snap-align: center;"
              >
                <div class="text-[18px] leading-none font-bold text-[#192100] mb-[10px]">{{ plan.tierName }}</div>

                <div class="text-black text-[36px] leading-none mb-[10px] font-bold">
                  ¥ {{ plan.displayPrice }}
                  <span class="text-[14px] leading-[20px]">{{ plan.isFree ? '' : t('subscription.perMonth') }}</span>
                </div>

                <div class="text-[12px] leading-[16px] text-black/80 mb-4">
                  {{ plan.isFree ? t('subscription.freeGift') : t('subscription.monthlyPoints') }}{{ plan.grantedPoints }}{{ plan.isFree ? t('subscription.pointsSuffix') : '' }}
                </div>

                <!-- 按钮三态：当前计划（disabled）/ 不可降级（disabled）/ 升级或订阅（可点） -->
                <button
                  v-if="comparePlan(plan) === 'current'"
                  class="w-full h-[34px] rounded-[10px] mb-4 bg-[#DDE3CA] text-black text-[14px]"
                  disabled
                >{{ t('subscription.currentPlan') }}</button>
                <button
                  v-else-if="comparePlan(plan) === 'downgrade'"
                  class="w-full h-[34px] rounded-[10px] mb-4 bg-[#DDE3CA] text-black text-[14px] cursor-not-allowed opacity-70"
                  disabled
                >{{ t('subscription.subscribe') }}</button>
                <button
                  v-else
                  class="w-full h-[34px] rounded-[10px] mb-4 bg-[linear-gradient(90deg,#C2FF00_0%,#83FF75_100%)] text-black text-[14px] cursor-pointer"
                  @click="handleNotSupported"
                >{{ comparePlan(plan) === 'upgrade' ? t('subscription.upgrade') : t('subscription.subscribe') }}</button>

                <div class="text-[12px] mb-2 text-black font-[600]">{{ t('subscription.featuresTitle') }}</div>
                <div
                  v-for="(feature, fIdx) in plan.features"
                  :key="fIdx"
                  class="text-[12px] leading-[16px] mb-2 text-black"
                >{{ feature }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 第二屏：积分附加包（两列） -->
      <section ref="pointsPackageSectionRef" class="min-h-screen px-4 pt-8 pb-28">
        <div class="text-center">
          <div class="text-[16px] font-bold text-white">
            {{ t('subscription.buyPointsTitlePrefix') }}<span class="text-[#C2FF00]">{{ t('subscription.buyPointsTitleHighlight') }}</span>{{ t('subscription.buyPointsTitleSuffix') }}
          </div>
          <div class="mt-1 text-[12px] text-white/60">{{ t('subscription.buyPointsDesc') }}</div>
        </div>

        <div v-if="pointsPackageLoading" class="mt-4 text-center text-[14px] text-white/70">
          {{ t('subscription.loadingPackages') }}
        </div>
        <div v-else-if="!pointsPackageList.length" class="mt-4 text-center text-[14px] text-white/70">
          {{ t('subscription.emptyPackages') }}
        </div>
        <div v-else class="mt-5 grid grid-cols-2 gap-3">
          <div
            v-for="item in pointsPackageList"
            :key="item.tierCode"
            class="rounded-[16px] bg-white px-4 pt-4 pb-3 shadow-[0_10px_30px_rgba(0,0,0,0.22)]"
          >
            <div class="flex items-center gap-1 text-[12px] font-bold text-[#111]">
              <svg-icon name="gy-integral" size="14"></svg-icon>
              <span>{{ item.tierName }}</span>
            </div>
            <div class="mt-2 text-[30px] leading-[34px] font-bold text-[#111]">¥ {{ formatPrice(item.price) }}</div>
            <button
              class="mt-3 w-full h-9 font-medium rounded-[10px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] text-[#111] text-[14px] border-0 cursor-pointer"
              @click="handleNotSupported"
            >
              {{ t('subscription.buyNow') }}
            </button>
          </div>
        </div>

        <div class="mt-8 text-center text-[12px] leading-[18px] text-white/70">
          {{ t('common.view') }}
          <a href="javascript:void(0)" class="text-[#C2FF00]" @click="handleOpenFaqPage">{{ t('subscription.faqLink') }}</a>
        </div>
      </section>
    </div>

    <MobileTabBar/>

    <MobileLoginSheet v-model="loginVisible"/>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount, nextTick} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {ElMessage} from 'element-plus';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import MobileLoginSheet from '@/components/mobile/MobileLoginSheet.vue';
import {useUserStore} from '@/store/user.js';
import {useI18nText} from '@/i18n';
import {useSubscriptionPlans} from './useSubscriptionPlans';
import hotImg from '@/assets/common/hot.png';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const {t} = useI18nText();

/**
 * 订阅页（移动端）业务逻辑与 PC 端共享：
 * - 套餐列表/积分包接口
 * - 套餐字段归一化（displayPrice/features）
 * - 当前套餐判断
 *
 * 这样能保证两端展示/规则完全一致，避免日后一端改动另一端遗漏。
 */
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

/**
 * 套餐横滑“轮播感”：
 * - 使用 scroll-snap + 指示点
 * - 通过计算“视口中心点最接近的卡片”得到 activePlanIndex
 * - scroll 事件节流到 requestAnimationFrame，避免频繁计算引发卡顿
 */
const plansScrollerRef = ref(null);
const activePlanIndex = ref(0);
let plansScrollRaf = 0;

/** 第二屏积分包区域，用于“购买附加包”点击后平滑滚动定位。 */
const pointsPackageSectionRef = ref(null);

/** 登录全屏覆盖层显隐：未登录用户在本页内完成登录，登录成功后保留在订阅页（useLogin 内部 reload 不改 URL）。 */
const loginVisible = ref(false);

/** 积分包购买与订阅移动端均暂不开放，登录态走 toast 引导去 PC 端，未登录态先在本页弹登录。 */
const handleNotSupported = () => {
  if (!userStore.isLoggedIn) {
    loginVisible.value = true;
    return;
  }
  ElMessage.warning(t('subscription.mobileNotSupported'));
};

const handleOpenFaqPage = () => {
  const lang = typeof route.params?.lang === 'string' ? route.params.lang : 'zh';
  router.push({name: 'subscribeFaq', params: {lang}});
};

const handleGoPointsPackage = () => {
  const section = pointsPackageSectionRef.value;
  if (!section) return;
  section.scrollIntoView({behavior: 'smooth', block: 'start'});
};

/** 横向滑动：根据当前滚动位置更新“激活卡片”指示点。 */
const updateActivePlanIndex = () => {
  const el = plansScrollerRef.value;
  if (!el) return;
  const scrollLeft = el.scrollLeft;
  const viewportCenter = scrollLeft + el.clientWidth / 2;

  const items = el.querySelectorAll('[data-plan-card]');
  if (!items?.length) return;

  let bestIdx = 0;
  let bestDist = Infinity;
  items.forEach((node, idx) => {
    const n = node;
    const center = n.offsetLeft + n.offsetWidth / 2;
    const dist = Math.abs(center - viewportCenter);
    if (dist < bestDist) {
      bestDist = dist;
      bestIdx = idx;
    }
  });
  activePlanIndex.value = bestIdx;
};

const onPlansScroll = () => {
  if (plansScrollRaf) cancelAnimationFrame(plansScrollRaf);
  plansScrollRaf = requestAnimationFrame(() => {
    plansScrollRaf = 0;
    updateActivePlanIndex();
  });
};

const scrollPlans = async (delta) => {
  await nextTick();
  const el = plansScrollerRef.value;
  if (!el) return;
  const cards = el.querySelectorAll('[data-plan-card]');
  if (!cards?.length) return;

  const nextIdx = Math.max(0, Math.min(cards.length - 1, activePlanIndex.value + delta));
  const target = cards[nextIdx];
  const targetLeft = target.offsetLeft - (el.clientWidth - target.offsetWidth) / 2;
  el.scrollTo({left: Math.max(0, targetLeft), behavior: 'smooth'});
};

onMounted(async () => {
  await fetchPlans();
  // 积分附加包在第二屏展示，但提前拉取数据以减少用户下滑后的等待时间
  fetchPointsPackages();
  if (userStore.isLoggedIn) userStore.fetchUserPlan();
  await nextTick();
  updateActivePlanIndex();
});

onBeforeUnmount(() => {
  if (plansScrollRaf) {
    cancelAnimationFrame(plansScrollRaf);
    plansScrollRaf = 0;
  }
});
</script>

<style scoped lang="scss">
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.no-scrollbar::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}
</style>
