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
        <h1 class="text-[28px] leading-[36px] font-bold text-center">{{ t('subscription.faqTitle') }}</h1>
        <div class="w-10"></div>
      </div>

      <div class="text-center text-[13px] text-white/55 mb-6">{{ t('subscription.faqSubtitle') }}</div>

      <div class="faq-content flex-1 min-h-0 rounded-[16px] p-6 bg-[linear-gradient(180deg,rgba(26,26,26,0.98)_0%,rgba(18,18,18,0.98)_100%)] border border-white/10">
        <div class="faq-text">
          <template v-for="(line, index) in faqLines" :key="`faq-line-${index}`">
            <p
              :class="{
                'faq-section-title': /^\d+\./.test(line),
                'faq-subsection-title': /^\\s+- /.test(line) || /^- /.test(line),
                'faq-divider': /^---$/.test(line.trim()),
              }"
            >
              {{ line }}
            </p>

            <div v-if="line.includes(tableInsertKeyword)" class="price-table">
              <table>
                <thead>
                  <tr>
                    <th v-for="(header, idx) in tableHeaders" :key="`faq-th-${idx}`">{{ header }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, rowIdx) in tableRows" :key="`faq-row-${rowIdx}`">
                    <td v-for="(col, colIdx) in row" :key="`faq-col-${rowIdx}-${colIdx}`">{{ col }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useI18nText} from '@/i18n';
import {useSafeBack} from '@/composables/useSafeBack.js';
import {SUBSCRIPTION_FAQ_MAP} from './faq-text';

const safeBack = useSafeBack();
const {t, locale} = useI18nText();

const faqMeta = computed(() => {
  const currentLocale = String(locale.value || '');
  return SUBSCRIPTION_FAQ_MAP[currentLocale] || SUBSCRIPTION_FAQ_MAP['en-US'];
});

const faqLines = computed(() => String(faqMeta.value.text || '').split('\n'));
const tableInsertKeyword = computed(() => String(faqMeta.value.chargeSectionTitleKeyword || ''));
const tableHeaders = computed(() => (Array.isArray(faqMeta.value.tableHeaders) ? faqMeta.value.tableHeaders : []));
const tableRows = computed(() => (Array.isArray(faqMeta.value.tableRows) ? faqMeta.value.tableRows : []));

const handleBack = () => safeBack();
</script>

<style scoped lang="scss">
.faq-content {
  overflow-y: auto;
}

.faq-text {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 28px;
  color: rgba(255, 255, 255, 0.82);
}

.faq-section-title {
  margin: 18px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #c2ff00;
}

.faq-subsection-title {
  margin: 12px 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.faq-divider {
  margin: 12px 0;
  opacity: 0.4;
}

.price-table {
  margin: 16px 0;
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  table {
    width: 100%;
    min-width: 560px;
    border-collapse: collapse;
    background: transparent;
  }

  th, td {
    padding: 12px 16px;
    text-align: left;
    border: 1px solid rgba(255, 255, 255, 0.15);
    white-space: nowrap;
    line-height: 22px;
  }

  th {
    background: rgba(255, 255, 255, 0.08);
    color: #ffffff;
    font-weight: 600;
    font-size: 14px;
  }

  td {
    color: rgba(255, 255, 255, 0.82);
    font-size: 14px;
  }

  tr:nth-child(even) {
    background: rgba(255, 255, 255, 0.03);
  }
}

@media (max-width: 768px) {
  .price-table {
    margin: 12px 0;

    table {
      min-width: 520px;
    }

    th, td {
      padding: 10px 12px;
      font-size: 13px;
    }
  }
}

.faq-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.faq-content::-webkit-scrollbar-track {
  background-color: rgba(255, 255, 255, 0.05);
}

.faq-content::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.faq-content::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.3);
}
</style>
