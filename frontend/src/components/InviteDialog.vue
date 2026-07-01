<template>
  <el-dialog
    v-model="visibleModel"
    width="min(560px, calc(100vw - 16px))"
    :show-close="false"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    class="invite-dialog"
    append-to-body
  >
    <!-- 右上角关闭按钮：保留独立交互，避免影响弹窗主体布局。 -->
    <button
      class="absolute right-[18px] top-[18px] z-[2] flex h-7 w-7 items-center justify-center rounded-full border-0 bg-transparent text-white/75 cursor-pointer transition-colors hover:bg-white/10 hover:text-white max-sm:right-3 max-sm:top-3"
      aria-label="close-invite-dialog"
      @click="visibleModel = false"
    >
      <el-icon :size="22">
        <Close/>
      </el-icon>
    </button>

    <!-- 弹窗主体：图标、标题、规则说明、邀请链接和复制按钮。 -->
    <div class="box-border w-full px-7 pb-[26px] pt-[30px] max-sm:px-4 max-sm:py-6">
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[rgba(95,154,38,0.35)] max-sm:h-[52px] max-sm:w-[52px] max-sm:rounded-[14px]">
        <svg-icon name="gy-share-gift" size="34" color="#C2FF00"></svg-icon>
      </div>

      <div class="mt-[18px] text-center text-[24px] font-bold leading-[32px] text-white max-sm:mt-3 max-sm:text-[20px] max-sm:leading-7">
        {{ t('layout.invite.title') }}
      </div>

      <div class="mt-5 rounded-2xl border border-white/5 bg-white/8 p-5 max-sm:mt-4 max-sm:rounded-[14px] max-sm:p-4">
        <div class="text-[20px] font-bold leading-[28px] text-[#C2FF00] max-sm:text-[16px] max-sm:leading-6">
          {{ t('layout.invite.rulesTitle') }}
        </div>
        <div class="mt-3 whitespace-pre-line text-[16px] leading-[24px] text-white/85 max-sm:mt-2 max-sm:text-[13px] max-sm:leading-5">
          {{ t('layout.invite.rulesDesc') }}
        </div>
      </div>

      <div class="mt-2 text-[16px] leading-[34px] text-white/72 max-sm:text-[13px] max-sm:leading-7">
        {{ t('layout.invite.linkLabel') }}
      </div>
      <div class="flex items-center gap-[10px] max-sm:gap-2">
        <div
          class="flex h-12 min-w-0 flex-1 items-center overflow-hidden rounded-xl bg-black/45 px-[14px] text-base leading-6 text-white/92 whitespace-nowrap text-ellipsis max-sm:h-10 max-sm:rounded-[10px] max-sm:px-3 max-sm:text-[12px] max-sm:leading-[18px]"
          :title="inviteLinkDisplay"
        >
          {{ inviteLinkDisplay }}
        </div>
        <button
          class="inline-flex h-12 min-w-[98px] items-center justify-center gap-1.5 rounded-xl border-0 bg-[#C2FF00] px-4 text-xl font-semibold text-black transition-opacity duration-200 ease-in-out hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60 max-sm:h-10 max-sm:min-w-[74px] max-sm:rounded-[10px] max-sm:px-[10px] max-sm:text-[14px]"
          :disabled="!inviteLink"
          @click="handleCopyInviteLink"
        >
          <svg-icon name="gy-copy" size="16" color="#000"></svg-icon>
          <span>{{ t('common.copy') }}</span>
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
import {Close} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import {getInviteCode} from '@/api/share';
import {useI18nText} from '@/i18n';

// 控制弹窗显隐，父组件通过 v-model 绑定。
const visibleModel = defineModel({type: Boolean, default: false});
const {t, locale, getLocalePrefix} = useI18nText();

// 邀请码加载状态，避免弹窗重复打开时重复请求。
const inviteCodeLoading = ref(false);
// 接口返回的邀请码。
const inviteCode = ref('');

// 根据当前语言决定邀请链接的路径前缀。
const lang = computed(() => {
  const prefix = getLocalePrefix(locale.value);
  return prefix === 'zh' ? 'zh' : 'en';
});

// 不同环境使用不同域名，确保本地和测试环境都能正确跳转。
const inviteBaseDomain = computed(() => {
  const host = (typeof window !== 'undefined' ? window.location.host : '').toLowerCase();
  if (host.includes('dev.ohyesai.com') || host.includes('localhost') || host.includes('127.0.0.1')) return 'http://127.0.0.1:5173';
  return 'https://ohyesai.com';
});

// 拼出最终邀请链接；如果邀请码还没拿到，就返回空字符串。
const inviteLink = computed(() => (inviteCode.value ? `${inviteBaseDomain.value}/${lang.value}/mv?inviteCode=${inviteCode.value}` : ''));
// 链接未生成时显示“生成中”文案。
const inviteLinkDisplay = computed(() => inviteLink.value || t('layout.invite.linkGenerating'));

// 弹窗打开后，按需拉取邀请码，确保只请求一次。
watch(visibleModel, async (visible) => {
  if (!visible) return;
  if (inviteCode.value || inviteCodeLoading.value) return;
  inviteCodeLoading.value = true;
  try {
    inviteCode.value = await getInviteCode();
    if (!inviteCode.value) ElMessage.warning(t('layout.invite.codeEmpty'));
  } catch (error) {
    ElMessage.error(error?.message || t('layout.invite.codeFailed'));
  } finally {
    inviteCodeLoading.value = false;
  }
});

// 点击复制按钮，将邀请链接复制到剪贴板。
const handleCopyInviteLink = async () => {
  if (!inviteLink.value) {
    ElMessage.warning(t('layout.invite.copyingUnavailable'));
    return;
  }
  try {
    await navigator.clipboard.writeText(inviteLink.value);
    ElMessage.success(t('layout.invite.copySuccess'));
  } catch {
    ElMessage.error(t('layout.invite.copyFail'));
  }
};
</script>

<style lang="scss">
.invite-dialog {
  border-radius: 24px !important;
  overflow: hidden;
  position: relative;
  padding: 0 !important;
  background: #1A1F15;
  border: 1px solid rgba(194, 255, 0, 0.2);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.55);

  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    padding: 0 !important;
    overflow: hidden;
  }

  @media (max-width: 767px) {
    width: calc(100vw - 16px) !important;
    max-width: calc(100vw - 16px) !important;
    border-radius: 18px !important;
  }
}
</style>
