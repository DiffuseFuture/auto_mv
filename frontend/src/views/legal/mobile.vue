<template>
  <div class="h-full bg-[#0f0f10] text-white overflow-hidden flex flex-col">
    <div class="flex items-center gap-3 bg-[#0f0f10]/95 px-4 py-3 shrink-0">
      <button type="button" class="flex items-center justify-center rounded-full bg-white/8" @click="handleBack">
        <svg-icon name="gy-return" size="16" color="#FFFFFF"></svg-icon>
      </button>
      <div class="min-w-0 flex-1 text-center">
        <div class="truncate text-[16px] font-semibold leading-6">{{ pageTitle }}</div>
      </div>
      <div class="w-9"></div>
    </div>

    <div class="flex-1 min-h-0 px-4 pb-4">
      <div class="h-full overflow-y-auto no-scrollbar select-none">
        <p
          v-for="(line, index) in policyLines"
          :key="`policy-line-${index}`"
          class="whitespace-pre-wrap text-[14px] leading-[28px]"
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

const policyType = computed(() => (route.params?.type === 'privacy' ? 'privacy' : 'terms'));

const policyText = computed(() => {
  const currentLocale = String(locale.value || '');
  const localeTexts = POLICY_TEXT_MAP[currentLocale] || POLICY_TEXT_MAP['en-US'];
  return localeTexts?.[policyType.value] || '';
});

const policyLines = computed(() => policyText.value.split('\n'));

const pageTitle = computed(() => policyType.value === 'privacy' ? t('login.policy.privacyTitle') : t('login.policy.termsTitle'));

const handleBack = () => safeBack();
</script>

<style scoped>
.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
