<template>
  <div class="w-full h-full overflow-hidden bg-[#121212] text-white">
    <div class="w-full h-full max-w-[980px] mx-auto px-6 py-8 flex flex-col">
      <div class="flex items-center justify-between mb-6">
        <button
          class="w-10 h-10 rounded-full flex-center hover:opacity-90 cursor-pointer"
          @click="handleBack"
        >
          <svg-icon name="gy-return" size="34" color="#FFFFFF"></svg-icon>
        </button>
        <h1 class="text-[28px] leading-[36px] font-bold text-center">{{ pageTitle }}</h1>
        <div class="w-10"></div>
      </div>

      <div class="text-center text-[13px] text-white/55 mb-6">{{ pageSubtitle }}</div>

      <div class="policy-body flex-1 min-h-0 rounded-[16px] p-6 overflow-y-auto no-scrollbar select-none">
        <p
          v-for="(line, index) in policyLines"
          :key="`policy-line-${index}`"
        >
          {{ line }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import {POLICY_TEXT_MAP} from '@/layout/policy-text';
import {useI18nText} from '@/i18n';
import {useSafeBack} from '@/composables/useSafeBack.js';

const route = useRoute();
const safeBack = useSafeBack();
const {t, locale} = useI18nText();

const policyType = computed(() => {
  return route.params?.type === 'privacy' ? 'privacy' : 'terms';
});

const policyText = computed(() => {
  const currentLocale = String(locale.value || '');
  const localeTexts = POLICY_TEXT_MAP[currentLocale] || POLICY_TEXT_MAP['en-US'];
  return localeTexts?.[policyType.value] || '';
});

const policyLines = computed(() => policyText.value.split('\n'));

const pageTitle = computed(() => {
  return policyType.value === 'privacy'
    ? t('login.policy.privacyTitle')
    : t('login.policy.termsTitle');
});

const pageSubtitle = computed(() => {
  return policyType.value === 'privacy'
    ? t('login.policy.privacySubtitle')
    : t('login.policy.termsSubtitle');
});

const handleBack = () => safeBack();
</script>

<style scoped lang="scss">
.policy-body {
  white-space: pre-wrap;
  line-height: 28px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
}

.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    display: none;
  }
}
</style>
