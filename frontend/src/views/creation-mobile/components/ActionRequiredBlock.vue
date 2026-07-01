<template>
  <div v-if="block" class="mb-4">
    <div class="rounded-[18px] px-[4px] py-[6px] text-white">
      <div class="mb-[18px] text-center">
        <div class="text-[18px] leading-[28px] font-semibold text-white/88">
          <span>{{ titleParts.before }}</span>
          <span class="text-[#C2FF00]">{{ actionQuery }}</span>
          <span>{{ titleParts.after }}</span>
        </div>
        <div class="mt-[6px] text-[14px] leading-[22px] text-white/70 whitespace-pre-wrap">
          {{ t('creation.messageArea.actionRequired.fixedSubtitle') }}
        </div>
      </div>

      <!-- 移动端：含义选项按“上下堆叠”全宽排列（参照设计图） -->
      <div class="flex flex-col gap-[10px]">
        <button
          v-for="(option, optionIdx) in actionOptions"
          :key="String(option?.id ?? optionIdx)"
          type="button"
          class="action-required-option-card w-full rounded-[16px] border bg-[#1f2024] px-[14px] py-[14px] transition-colors text-left"
          :disabled="locked || !canEdit || loading"
          :class="[
            locked
              ? 'opacity-75 cursor-not-allowed'
              : 'hover:border-[#c2ff00]/70 hover:bg-[#23251a] cursor-pointer',
            isOptionSelected(option) ? 'border-[#c2ff00] bg-[#23251a]' : 'border-white/14',
          ]"
          @click.stop="handleOptionClick(option)"
        >
          <div class="flex h-full flex-col">
            <div class="text-[16px] leading-[24px] font-semibold text-white mb-[6px]">
              含义{{ optionIdx + 1 }}
            </div>
            <div class="text-[14px] leading-[22px] font-semibold text-white/92 mb-[6px]">
              {{ option?.label || '--' }}
            </div>
            <div v-if="option?.source" class="text-[12px] leading-[20px] text-white/45 mb-[8px]">
              {{ option.source }}
            </div>
            <div class="flex-1 text-[13px] leading-[20px] text-white/78 whitespace-pre-wrap">
              {{ option?.snippet || '' }}
            </div>
            <div class="mt-[12px] flex justify-end">
              <span
                class="inline-flex h-[30px] min-w-[68px] items-center justify-center rounded-[10px] px-[12px] text-[12px] font-semibold transition-colors"
                :class="isOptionSelected(option) ? 'bg-[#c2ff00] text-[#0d0d0d]' : 'bg-white/10 text-white/72'"
              >
                {{ isOptionSelected(option) ? '已选择' : '选这个' }}
              </span>
            </div>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useI18nText} from '@/i18n';

const props = defineProps({
  block: {type: Object, required: true},
  canEdit: {type: Boolean, default: false},
  loading: {type: Boolean, default: false},
});

const emit = defineEmits(['action-required-submit']);

const {t} = useI18nText();

/**
 * ACTION_REQUIRED data 字段契约：
 * - actionId / kind / query / selectedOptionId：string
 * - options：array
 * - options[i].id / label / source / snippet / query：string
 */
const actionData = computed(() => props.block?.data || {});

const locked = computed(() => !!actionData.value?.selectedOptionId);

const actionOptions = computed(() => (actionData.value?.options || []).slice(0, 2));

const actionQuery = computed(() => {
  const direct = (actionData.value?.query || '').trim();
  if (direct) return direct;
  return (actionData.value?.options?.[0]?.query || '').trim();
});

const titleParts = computed(() => {
  const title = t('creation.messageArea.actionRequired.detectedAmbiguityTitle', {query: '__QUERY__'});
  const [before, after] = title.split('__QUERY__');
  return {before: before || '', after: after || ''};
});

// options[i].id 与 selectedOptionId 后端契约皆 string，直接 ===
const isOptionSelected = (option) => {
  const selectedOptionId = actionData.value?.selectedOptionId;
  return !!selectedOptionId && !!option?.id && selectedOptionId === option.id;
};

const handleOptionClick = (option) => {
  if (!props.canEdit || props.loading) return;
  if (!option?.id) return;
  if (locked.value) return;
  if (isOptionSelected(option)) return;

  // Mark local selected state so placeholder/guide can update immediately.
  if (props.block?.data) {
    props.block.data.selectedOptionId = option.id;
  }

  emit('action-required-submit', {
    kind: actionData.value?.kind,
    actionId: actionData.value?.actionId,
    optionId: option.id,
    optionLabel: option.label,
    query: option.query,
  });
};
</script>

