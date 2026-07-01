<!--
  移动端通用登录全屏覆盖层：
  - 由父组件用 v-model 控制显示/隐藏
  - 内部接管 useLogin 全部状态（表单 / 倒计时 / 提交）
  - 登录成功后 useLogin 内部会 reload 页面，URL 保留 → 自动停在触发登录的页面
-->
<template>
  <div v-if="modelValue" class="fixed inset-0 z-[200] overflow-hidden bg-black">
    <div class="relative z-[1] flex h-full w-full flex-col p-[15px]">
      <div class="flex-between pl-[5px] pt-[13px] mb-[65px]">
        <div class="flex-center">
          <el-image :src="logo2Img" class="w-[25px] aspect-square mr-[7px]"></el-image>
          <div class="text-white text-[18px] font-extrabold">Oh<span class="text-[#C2FF00]">Yes</span>AI</div>
        </div>
        <button type="button" @click="closeLogin">
          <svg-icon name="gy-closure" size="18" color="#FFFFFF"></svg-icon>
        </button>
      </div>

      <div class="flex-1 flex flex-col">
        <div class="text-center text-[25px] leading-[35px] font-semibold text-white mb-[72px]">{{ t('login.welcomeLogin') }}</div>

        <!-- 手机号 + 验证码登录（移动端只保留手机号登录） -->
        <div class="flex flex-col">
          <div class="h-10 rounded-[5px] mb-[15px] bg-[rgba(216,216,216,0.1)] px-[10px] text-[14px] text-white flex items-center">
            <span class="text-[#9E9E9E]">+86</span>
            <el-divider direction="vertical" class="!h-[14px] !mx-[10px] !border-l-[#D8D8D8]"/>
            <input
                v-model="loginForm.phone"
                type="tel"
                inputmode="tel"
                maxlength="11"
                autocapitalize="off"
                autocomplete="tel"
                class="flex-1 bg-transparent text-white outline-none placeholder:text-[#9E9E9E]"
                :placeholder="t('login.phonePlaceholder')"
            />
          </div>

          <div class="flex gap-[6px] h-10 mb-[45px]">
            <input
                v-model="loginForm.code"
                type="tel"
                inputmode="numeric"
                maxlength="6"
                autocapitalize="off"
                class="flex-1 min-w-0 rounded-[5px] bg-[rgba(216,216,216,0.1)] px-[10px] text-[14px] text-white outline-none placeholder:text-[#9E9E9E]"
                :placeholder="t('login.codePlaceholder')"
            />
            <button
                type="button"
                :disabled="!canSendCode || sendingCode"
                class="w-[89px] shrink-0 rounded-[5px] bg-[rgba(216,216,216,0.2)] text-[14px] disabled:opacity-50"
                :class="canSendCode ? 'text-[#C2FF00]' : 'text-[#9E9E9E]'"
                @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : t('login.sendCode') }}
            </button>
          </div>

          <button
              type="button"
              class="h-10 rounded-[5px] text-[15px] text-black disabled:opacity-50 bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)]"
              :disabled="!canLogin"
              @click="handleLogin"
          >
            {{ loginLoading ? t('login.loggingIn') : t('login.loginNow') }}
          </button>
        </div>

        <!-- 服务条款 / 隐私政策（mt-auto 推到底部，键盘弹起时跟随父容器自然收缩） -->
        <div class="mt-auto pt-[20px] pb-[29px] w-full flex-center text-[12px] leading-[17px] text-[#9E9E9E]">
          {{ t('login.agreedMobile') }}
          <span class="text-[#C2FF00]" @click="openPolicyDialog('terms')">{{ t('login.terms') }}</span>
          {{ t('login.and') }}
          <span class="text-[#C2FF00]" @click="openPolicyDialog('privacy')">{{ t('login.privacy') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useI18nText} from '@/i18n';
import {useLogin} from '@/composables/useLogin';
import logo2Img from '@/assets/common/logo2.png';

const props = defineProps({
  /** 显示/隐藏：与父组件 v-model 双向绑定。 */
  modelValue: {type: Boolean, default: false},
});
const emit = defineEmits(['update:modelValue']);

const {t} = useI18nText();

// 桥接 v-model 到 useLogin 的 externalVisible（useLogin 内部 .value = true/false 会回写 emit）
const visibleRef = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const {
  loginForm,
  countdown,
  sendingCode,
  loginLoading,
  canSendCode,
  canLogin,
  handleSendCode,
  handleLogin,
  closeLogin,
  openPolicyDialog,
} = useLogin({
  visible: visibleRef,
  requireAgreement: false,
  enableWechatLogin: false,
});
</script>
