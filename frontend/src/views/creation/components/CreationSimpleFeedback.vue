<template>
  <div v-if="state !== 'hidden'" ref="feedbackRootRef" class="mb-4">
    <template v-if="state === 'pending'">
      <div class="flex items-center gap-5 text-[16px]">
        <span class="text-[#9B9B9B]">{{ title }}</span>
        <button
            class="inline-flex items-center gap-1 cursor-pointer transition-colors"
            :class="{
            'text-[#C2FF00]': selectedAttitude === FEEDBACK_ATTITUDE.SATISFIED,
            'text-[#9B9B9B] hover:text-[#C2FF00]': selectedAttitude !== FEEDBACK_ATTITUDE.SATISFIED,
          }"
            :disabled="submitting"
            @click="handleSatisfiedClick"
        >
          <svg-icon name="gy-praise" size="14"></svg-icon>
          <span>{{ t('creation.feedback.satisfied') }}</span>
        </button>
        <button
            class="inline-flex items-center gap-1 cursor-pointer transition-colors"
            :class="{
            'text-[#C2FF00]': selectedAttitude === FEEDBACK_ATTITUDE.NOT_SATISFIED,
            'text-[#9B9B9B] hover:text-[#C2FF00]': selectedAttitude !== FEEDBACK_ATTITUDE.NOT_SATISFIED,
          }"
            :disabled="submitting"
            @click="openNegativeInput"
        >
          <svg-icon name="gy-stepon" size="14"></svg-icon>
          <span>{{ t('creation.feedback.notSatisfied') }}</span>
        </button>
      </div>
      <div v-if="showNegativeInput" class="mt-3 relative">
      <textarea
          :value="content"
          class="feedback-textarea"
          :disabled="submitting"
          :placeholder="t('creation.feedback.placeholder')"
          @input="updateContent"
      ></textarea>
        <button
            class="absolute right-3 bottom-3 h-10 w-10 rounded-[10px] bg-[#C2FF00] text-black text-[14px] font-bold flex-center cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
            :disabled="submitting"
            @click="submitNegativeFeedback"
        >
          {{ t('creation.feedback.send') }}
        </button>
      </div>
    </template>
    <div v-else class="text-[14px] text-[#9B9B9B]">
      <template v-if="state === 'done'">
        <svg-icon name="gy-success" size="14"></svg-icon>
        {{ t('creation.feedback.thanks') }}
      </template>
      <template v-else>{{ t('common.loading') }}</template>
    </div>
  </div>
</template>

<script setup>
import {nextTick, onMounted, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {addFeedback, queryFeedback} from '@/api/creation';
import {useI18nText} from '@/i18n';

const FEEDBACK_ATTITUDE = {
  SATISFIED: 'SATISFIED',
  NOT_SATISFIED: 'NOT_SATISFIED',
};

const props = defineProps({
  title: {type: String, required: true},
  // 后端契约：messageId 为 string，本地占位也已统一为 string
  messageId: {type: String, required: true},
  allowPending: {type: Boolean, default: true},
});

// state: 'loading' | 'pending' | 'done' | 'hidden'
const state = ref('loading');
const submitting = ref(false);
const showNegativeInput = ref(false);
const content = ref('');
const selectedAttitude = ref('');
const feedbackRootRef = ref(null);
const {t} = useI18nText();

/**
 * 将反馈区域滚动到可视区域底部。
 * @returns {void}
 */
const scrollFeedbackIntoView = () => {
  nextTick(() => {
    feedbackRootRef.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'end',
      inline: 'nearest',
    });
  });
};

const handleSatisfiedClick = async () => {
  const messageId = props.messageId;
  if (!messageId || submitting.value) return;
  selectedAttitude.value = FEEDBACK_ATTITUDE.SATISFIED;
  submitting.value = true;
  try {
    await addFeedback({messageId, attitudeType: FEEDBACK_ATTITUDE.SATISFIED, content: ''});
    state.value = 'done';
    showNegativeInput.value = false;
  } catch (error) {
    ElMessage.error(error?.message || t('creation.feedback.submitFailed'));
  } finally {
    submitting.value = false;
  }
};

const openNegativeInput = () => {
  if (submitting.value) return;
  selectedAttitude.value = FEEDBACK_ATTITUDE.NOT_SATISFIED;
  showNegativeInput.value = true;
  content.value = '';
  scrollFeedbackIntoView();
};

const updateContent = (event) => {
  // textarea value 总是 string，无需 String() 包装
  content.value = event.target.value;
};

const submitNegativeFeedback = async () => {
  const messageId = props.messageId;
  if (!messageId || submitting.value) return;
  const draft = content.value.trim();
  if (!draft) {
    ElMessage.warning(t('creation.feedback.emptySuggestion'));
    return;
  }
  submitting.value = true;
  try {
    await addFeedback({messageId, attitudeType: FEEDBACK_ATTITUDE.NOT_SATISFIED, content: draft});
    state.value = 'done';
    showNegativeInput.value = false;
  } catch (error) {
    ElMessage.error(error?.message || t('creation.feedback.submitFailed'));
  } finally {
    submitting.value = false;
  }
};

watch(
  () => props.allowPending,
  (allowPending) => {
    if (allowPending) return;
    if (state.value !== 'pending') return;
    state.value = 'hidden';
    showNegativeInput.value = false;
  },
);

onMounted(async () => {
  const messageId = props.messageId;
  if (!messageId) {
    state.value = 'pending';
    return;
  }
  try {
    const res = await queryFeedback({messageId});
    const attitudeType = res?.attitudeType;
    const hasFeedback = attitudeType === FEEDBACK_ATTITUDE.SATISFIED || attitudeType === FEEDBACK_ATTITUDE.NOT_SATISFIED;
    if (hasFeedback) {
      state.value = 'done';
      return;
    }
    state.value = props.allowPending ? 'pending' : 'hidden';
  } catch {
    state.value = props.allowPending ? 'pending' : 'hidden';
  }
});
</script>

<style scoped lang="scss">
.feedback-textarea {
  width: 100%;
  height: 80px;
  border: 1px solid #9b9b9b;
  border-radius: 12px;
  background: #101010;
  color: #fff;
  font-size: 16px;
  line-height: 22px;
  padding: 10px 12px;
  outline: none;
  resize: none;
}
</style>
