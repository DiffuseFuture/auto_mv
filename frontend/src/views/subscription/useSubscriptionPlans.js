import {computed, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {getSubscriptionPlan, getPointsPackage} from '@/api/creation';

/**
 * Shared business logic for subscription plans/points packages.
 * Used by both desktop and mobile subscription pages.
 */
export function useSubscriptionPlans({t, userStore}) {
  const billingCycle = ref('monthly');
  const planData = ref({free: [], yearly: [], monthly: []});
  const loading = ref(false);
  const pointsPackageLoading = ref(false);
  const pointsPackageList = ref([]);

  const formatPrice = (price) => Math.floor(Number(price) || 0);

  const buildPlanFeatures = (plan) => {
    const description = plan.description || {};
    const features = [];
    features.push(t('subscription.planFeatures.songCount', {count: description.songQuantity}));
    features.push(t('subscription.planFeatures.mvCount', {count: description.narrativeMv}));
    features.push(
      description.commercialUsable
        ? t('subscription.planFeatures.commercialUseAllowed')
        : t('subscription.planFeatures.commercialUseNotAllowed'),
    );
    features.push(
      description.watermarkRemoved
        ? t('subscription.planFeatures.watermarkRemoved')
        : t('subscription.planFeatures.watermarkNotRemoved'),
    );
    return features;
  };

  const normalizePlan = (plan, isFree = false, isYearly = false) => {
    const monthlyPrice = isYearly ? Math.floor(Number(plan.price) / 12) : formatPrice(plan.price);
    return {
      ...plan,
      displayPrice: monthlyPrice,
      originalPrice: formatPrice(plan.price),
      features: buildPlanFeatures(plan),
      isFree,
      isYearly,
    };
  };

  const freePlans = computed(() => (planData.value.free || []).map((plan) => normalizePlan(plan, true)));
  const paidPlans = computed(() => {
    const isYearly = billingCycle.value === 'yearly';
    return (planData.value[billingCycle.value] || []).map((plan) => normalizePlan(plan, false, isYearly));
  });
  const currentPlans = computed(() => [...freePlans.value, ...paidPlans.value]);

  const isCurrentPlan = (plan) => {
    const userTierCode = userStore.userPlan?.tierCode;
    if (!userTierCode) return false;
    return userTierCode === plan.tierCode;
  };

  /**
   * 计划周期分层。年付 > 月付 > 免费——周期不同就直接按层级比，不看价格。
   * 设计意图：年付用户切到月付视为降级（长期合同切短期）；免费用户切到任何付费档都是升级。
   */
  const CYCLE_LEVEL = {free: 0, monthly: 1, yearly: 2};

  /** plan 的周期分层（normalizePlan 后的 plan 有 isFree / isYearly 标记）。 */
  const getPlanCycle = (plan) => {
    if (plan?.isFree) return 'free';
    return plan?.isYearly ? 'yearly' : 'monthly';
  };

  /** 用户当前 plan 的周期分层，从 planData 全集里按 tierCode 反查归属。 */
  const userCycle = computed(() => {
    const userTierCode = userStore.userPlan?.tierCode;
    if (!userTierCode) return 'none';
    if ((planData.value.free || []).some((p) => p.tierCode === userTierCode)) return 'free';
    if ((planData.value.yearly || []).some((p) => p.tierCode === userTierCode)) return 'yearly';
    if ((planData.value.monthly || []).some((p) => p.tierCode === userTierCode)) return 'monthly';
    return 'none';
  });

  /** 用户当前订阅的月度等价值；同周期内按这个值比较 tier 高低。 */
  const userPlanMonthlyValue = computed(() => {
    const userTierCode = userStore.userPlan?.tierCode;
    if (!userTierCode) return 0;
    const isYearly = userCycle.value === 'yearly';
    const all = [
      ...(planData.value.free || []),
      ...(planData.value.monthly || []),
      ...(planData.value.yearly || []),
    ];
    const matched = all.find((p) => p.tierCode === userTierCode);
    if (!matched) return 0;
    const raw = Number(matched.price) || 0;
    return isYearly ? raw / 12 : raw;
  });

  /** 同周期内 plan 的月度等价值。 */
  const planMonthlyValue = (plan) => {
    if (!plan) return 0;
    const raw = Number(plan.price) || 0;
    return plan.isYearly ? raw / 12 : raw;
  };

  /**
   * 计划相对当前用户订阅的位置：
   * - 'current'   当前计划本身
   * - 'upgrade'   升级（更高周期 或 同周期更高 tier）
   * - 'downgrade' 降级（更低周期 或 同周期更低 tier）—— 按钮禁用
   * - 'subscribe' 用户未登录 / 无订阅记录，按"订阅"语义
   *
   * 周期优先于价格：年付 > 月付 > 免费——年付用户看月付永远是降级（不允许切短期合同），
   * 即便月付 PRO 的月均价高于用户当前年付 BASIC 的月均价。
   */
  const comparePlan = (plan) => {
    if (isCurrentPlan(plan)) return 'current';
    const userTierCode = userStore.userPlan?.tierCode;
    if (!userTierCode) return 'subscribe';
    const planCycle = getPlanCycle(plan);
    const cur = CYCLE_LEVEL[userCycle.value];
    const tgt = CYCLE_LEVEL[planCycle];
    if (tgt !== cur) return tgt > cur ? 'upgrade' : 'downgrade';
    // 同周期，按月度等价值比 tier
    const userValue = userPlanMonthlyValue.value;
    const planValue = planMonthlyValue(plan);
    if (planValue > userValue) return 'upgrade';
    if (planValue < userValue) return 'downgrade';
    return 'current';
  };

  const fetchPlans = async () => {
    loading.value = true;
    try {
      planData.value = await getSubscriptionPlan();
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || t('subscription.planFetchFail'));
    } finally {
      loading.value = false;
    }
  };

  const fetchPointsPackages = async () => {
    pointsPackageLoading.value = true;
    try {
      const data = await getPointsPackage();
      pointsPackageList.value = Array.isArray(data) ? data : [];
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || t('subscription.packageFetchFail'));
    } finally {
      pointsPackageLoading.value = false;
    }
  };

  return {
    billingCycle,
    loading,
    pointsPackageLoading,
    pointsPackageList,
    currentPlans,
    formatPrice,
    isCurrentPlan,
    comparePlan,
    fetchPlans,
    fetchPointsPackages,
  };
}
