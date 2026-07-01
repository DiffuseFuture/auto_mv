<!-- 登录弹窗 -->
<template>
  <el-dialog
      v-model="dialogVisible"
      width="904px"
      :show-close="false"
      class="login-dialog"
      modal-class="login-dialog-overlay"
  >
    <div class="absolute top-11 right-11 cursor-pointer z-10" @click="handleClose">
      <svg-icon name="gy-closure" :size="28"></svg-icon>
    </div>

    <div class="flex-center h-[572px]">
      <!-- 左侧：扫码登录 -->
      <div class="w-[400px] h-full border-r border-[#D8D8D8]">
        <div id="wechat-qrcode" class="wechat-qrcode-container w-full h-full flex items-center justify-center overflow-visible"></div>
      </div>

      <!-- 右侧：手机号登录 -->
      <div class="flex-1 flex flex-col px-10">
        <h3 class="text-[24px] font-medium mb-10 mt-[30px] text-center">{{ t('login.title') }}</h3>

        <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="login-form">
          <el-form-item prop="phone">
            <el-input
                class="custom-input"
                v-model="loginForm.phone"
                :placeholder="t('login.phonePlaceholder')"
                size="large"
                maxlength="11"
            />
          </el-form-item>

          <el-form-item prop="code">
            <el-input
                class="custom-input"
                v-model="loginForm.code"
                :placeholder="t('login.codePlaceholder')"
                size="large"
                maxlength="6"
            >
              <template #suffix>
                <el-button
                    link
                    :disabled="countdown > 0 || !loginForm.phone"
                    :loading="sendingCode"
                    class="custom-btn"
                    @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : t('login.sendCode') }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="inviteCode">
            <el-input
                class="custom-input"
                v-model="loginForm.inviteCode"
                :placeholder="t('login.invitePlaceholder')"
                size="large"
                maxlength="32"
            />
          </el-form-item>

          <el-form-item prop="agreed">
            <el-checkbox v-model="loginForm.agreed" class="login-checkbox">
              <div class="text-[14px] text-[#292929] flex">
                {{ t('login.agreedPrefix') }}
                <div class="cursor-pointer text-[#A0D200]" @click.stop="openPolicyDialog('terms')">{{
                    t('login.terms')
                  }}
                </div>
                {{ t('login.and') }}
                <div class="cursor-pointer text-[#A0D200]" @click.stop="openPolicyDialog('privacy')">
                  {{ t('login.privacy') }}
                </div>
              </div>
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <button
                class="w-full h-[60px] bg-[#292929] text-[#C0FF00] text-[20px] font-medium rounded-[10px] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="!canLogin || loginLoading"
                @click="handleLogin"
            >
              {{ loginLoading ? t('login.loggingIn') : t('login.loginNow') }}
            </button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </el-dialog>

</template>

<script setup>
import {useLogin} from '@/composables/useLogin.js';
import {useI18nText} from '@/i18n';

const {t} = useI18nText();
const dialogVisible = defineModel({type: Boolean, default: false});
const inviteCode = defineModel('inviteCode', {type: String, default: ''});
const {
  loginFormRef,
  loginForm,
  countdown,
  sendingCode,
  loginLoading,
  canLogin,
  loginRules,
  handleSendCode,
  handleLogin,
  closeLogin: handleClose,
  openPolicyDialog,
} = useLogin({
  visible: dialogVisible,
  inviteCode,
  requireAgreement: true,
  enableWechatLogin: true,
});
</script>

<style lang="scss">
// 登录弹窗专属遮罩：90% 黑 + 4px 模糊。
// 通过 el-dialog 的 modal-class="login-dialog-overlay" 精准命中遮罩 DOM，
// 不会污染其它弹窗（账户管理、邀请规则等）的默认遮罩。
.login-dialog-overlay {
  background-color: rgba(0, 0, 0, 0.9) !important;
  backdrop-filter: blur(4px) !important;
}

.login-dialog {
  border-radius: 20px !important;
  padding: 0 !important;

  .el-dialog__header {
    display: none !important;
  }

  .el-dialog__body {
    padding: 0;
    color: #292929 !important;
  }
}

// 表单项间距 + 复选框圆形样式（Element Plus 覆盖必须非 scoped）
.login-form {
  .el-form-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  // 协议复选框：圆形外观
  .login-checkbox {
    .el-checkbox__inner {
      border-radius: 50%;
      width: 18px;
      height: 18px;
    }

    .el-checkbox__input.is-checked .el-checkbox__inner {
      background-color: #C2FF00;
      border-color: #C2FF00;
    }

    .el-checkbox__input.is-checked + .el-checkbox__label {
      color: #292929;
    }
  }
}

</style>

<style scoped lang="scss">
.wechat-qrcode-container {
  :deep(.impowerBox) {
    margin: 0 auto !important;
  }

  :deep(iframe) {
    display: block;
    margin: 0 auto;
    border: none;
  }
}

.custom-input {
  height: 60px;
  border: 1px solid #292929;
  border-radius: 10px;
  overflow: hidden;

  :deep(.el-input__wrapper) {
    box-shadow: none !important;
    border: none !important;
    padding: 18px 12px !important;
    background-color: transparent !important;
  }

  :deep(.el-input__inner) {
    font-size: 20px !important;
    color: #7A7A7A !important;
    line-height: 24px !important;
    height: auto !important;
    padding: 0 !important;

    &::placeholder {
      color: #7A7A7A !important;
    }
  }

  :deep(.el-input__prefix),
  :deep(.el-input__suffix) {
    background-color: transparent !important;
  }
}

.custom-btn {
  font-size: 20px;
  line-height: 24px;
  color: #525252;
  font-weight: 400;
}
</style>
