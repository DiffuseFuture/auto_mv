import { ref, computed, watch, onUnmounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/user';
import { sendSmsCode } from '@/api/auth';
import { saveUserTracking } from '@/api/tracking.js';
import { useI18nText } from '@/i18n';

// 已处理过的微信扫码 code（模块级），用于跨组件重挂载去重，
// 防止同一个 code 触发两次 loginByWechat 接口调用。
let lastHandledWechatCode = null;

// 微信 OAuth 跳转会把 URL query 整体换成 wechat 的 code/state，原本 URL 上的 ?inviteCode= 会丢，
// 整页刷新后表单状态也清空。所以扫码前把邀请码暂存 localStorage，回跳后再取回作为兜底。
const WECHAT_INVITE_CODE_STORAGE_KEY = 'pendingWechatInviteCode';

/**
 * 登录成功后的 reload：必须先把 body 从 Element Plus 锁定状态彻底解开 + scrollTo(0,0)，
 * 否则 iOS Safari 会把"body 还固定在 top:-Npx"那一刻的状态作为 reload 后的初始 scrollY 恢复，
 * 表现为登录后页面停在 N px 而不是顶部（用户在页面中段触发登录时尤其明显——首页输入框是
 * fixed 底部、上传按钮要求登录，所以用户登录入口可能在任何 scrollY）。
 */
const reloadFromTop = async () => {
  // 1. 让 Element Plus 跑完一轮 v-model 联动的 unlock 逻辑
  await nextTick();
  // 2. 兜底强制移除 body 上的锁定样式：Element Plus 的清理可能跨多个 tick，不能完全依赖
  const body = document.body;
  body.style.position = '';
  body.style.top = '';
  body.style.left = '';
  body.style.right = '';
  body.style.overflow = '';
  body.style.width = '';
  // 3. body 已 unlock，scrollTo 现在能真正写入 documentElement.scrollTop
  window.scrollTo(0, 0);
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
  // 4. 再等一帧，确保浏览器把 scrollY=0 这个状态实际提交了再触发 reload
  await new Promise((resolve) => requestAnimationFrame(resolve));
  window.location.reload();
};

/**
 * 登录业务逻辑（PC / Mobile 共用）
 * @param {Object} [options]
 * @param {import('vue').Ref<boolean>} [options.visible] 外部控制的显示状态
 * @param {import('vue').Ref<string>} [options.inviteCode] 外部邀请码
 * @param {boolean} [options.requireAgreement=true] 是否需要勾选协议
 * @param {boolean} [options.enableWechatLogin=false] 是否启用微信扫码登录逻辑
 */
export function useLogin(options = {}) {
  const {
    visible: externalVisible,
    inviteCode: externalInviteCode,
    requireAgreement = true,
    enableWechatLogin = false,
  } = options;

  const userStore = useUserStore();
  const route = useRoute();
  const router = useRouter();
  const { t } = useI18nText();

  const loginVisible = externalVisible || ref(false);
  const loginFormRef = ref(null);
  const activeTab = ref('phone');
  const countdown = ref(0);
  const sendingCode = ref(false);
  const loginLoading = ref(false);
  const defaultAgreed = !requireAgreement;
  const loginForm = ref({
    phone: '',
    code: '',
    inviteCode: externalInviteCode?.value || '',
    agreed: defaultAgreed,
  });

  let countdownTimer = null;

  const _t = (key) => {
    try {
      return t(key);
    } catch {
      return key;
    }
  };

  const canSendCode = computed(() => {
    return loginForm.value.phone && /^1[3-9]\d{9}$/.test(loginForm.value.phone) && countdown.value === 0 && !sendingCode.value;
  });

  const canLogin = computed(() => {
    const agreed = requireAgreement ? loginForm.value.agreed : true;
    return loginForm.value.phone && loginForm.value.code && agreed && !loginLoading.value;
  });

  const loginRules = computed(() => ({
    phone: [
      { required: true, message: t('login.form.phoneRequired'), trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: t('login.form.phoneInvalid'), trigger: 'blur' },
    ],
    code: [
      { required: true, message: t('login.form.codeRequired'), trigger: 'blur' },
      { pattern: /^\d{4}$/, message: t('login.form.codeInvalid'), trigger: 'blur' },
    ],
    agreed: [
      {
        validator: (_, value, callback) => {
          if (!requireAgreement) {
            callback();
            return;
          }
          callback(value ? undefined : new Error(t('login.form.policyRequired')));
        },
        trigger: 'change',
      },
    ],
  }));

  const clearWechatQuery = () => {
    if (!route.query.code || !route.query.state) return;
    const query = { ...route.query };
    delete query.code;
    delete query.state;
    router.replace({ query });
  };

  const resetCountdown = () => {
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    countdown.value = 0;
  };

  const resetLoginForm = () => {
    loginForm.value = {
      phone: '',
      code: '',
      inviteCode: externalInviteCode?.value || '',
      agreed: defaultAgreed,
    };
    loginFormRef.value?.clearValidate?.();
    resetCountdown();
    activeTab.value = 'phone';
  };

  const validatePhone = () => {
    if (!loginForm.value.phone) {
      ElMessage.warning(_t('login.form.phoneRequired'));
      return false;
    }
    if (!/^1[3-9]\d{9}$/.test(loginForm.value.phone)) {
      ElMessage.warning(_t('login.form.phoneInvalid'));
      return false;
    }
    return true;
  };

  const openLogin = () => {
    loginVisible.value = true;
  };

  const closeLogin = () => {
    loginVisible.value = false;
    if (enableWechatLogin) clearWechatQuery();
    resetLoginForm();
  };

  const openPolicyDialog = (type) => {
    const normalizedType = type === 'privacy' ? 'privacy' : 'terms';
    const lang = typeof route.params?.lang === 'string' ? route.params.lang : 'zh';
    loginVisible.value = false;
    router.push({
      name: 'policy',
      params: { lang, type: normalizedType },
    });
  };

  const handleSendCode = async () => {
    if (!canSendCode.value && !validatePhone()) return;

    try {
      sendingCode.value = true;
      await sendSmsCode({ mobile: loginForm.value.phone });
      ElMessage.success(t('login.codeSent'));
      countdown.value = 60;
      countdownTimer = setInterval(() => {
        if (--countdown.value <= 0) {
          clearInterval(countdownTimer);
          countdownTimer = null;
        }
      }, 1000);
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.phone?.[0]?.message || error?.message || t('login.sendCodeFail'));
    } finally {
      sendingCode.value = false;
    }
  };

  const runValidatedLogin = async () => {
    try {
      loginLoading.value = true;
      await userStore.login({
        mobile: loginForm.value.phone,
        code: loginForm.value.code,
        inviteCode: loginForm.value.inviteCode,
      });
      await saveUserTracking({
        target: 'LOGIN',
      });
      // 手机登录成功后清掉微信扫码兜底用的 localStorage，避免下次跨账号脏数据
      localStorage.removeItem(WECHAT_INVITE_CODE_STORAGE_KEY);
      closeLogin();
      await reloadFromTop();
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || t('login.loginFail'));
    } finally {
      loginLoading.value = false;
    }
  };

  const handleLogin = async () => {
    if (loginFormRef.value?.validate) {
      try {
        await loginFormRef.value.validate();
      } catch {
        return;
      }
      await runValidatedLogin();
      return;
    }

    if (!validatePhone()) return;

    if (!loginForm.value.code) {
      ElMessage.warning(_t('login.form.codeRequired'));
      return;
    }

    if (requireAgreement && !loginForm.value.agreed) {
      ElMessage.warning(_t('login.form.policyRequired'));
      return;
    }

    await runValidatedLogin();
  };

  const initWechatQRCode = () => {
    if (!enableWechatLogin) return;
    nextTick(() => {
      const container = document.getElementById('wechat-qrcode');
      if (!container || !window.WxLogin) return;
      container.innerHTML = '';
      const lang = typeof route.params?.lang === 'string' ? route.params.lang : 'zh';
      new window.WxLogin({
        self_redirect: false,
        id: 'wechat-qrcode',
        appid: '？？？',
        scope: 'snsapi_login',
        redirect_uri: encodeURIComponent(`${window.location.origin}/${lang}/mv`),
        state: Math.random().toString(36).substring(2),
        style: 'black',
        href: '',
      });
    });
  };

  if (externalInviteCode) {
    watch(externalInviteCode, (val) => {
      if (!loginVisible.value) return;
      loginForm.value.inviteCode = val || '';
    });
  }

  if (enableWechatLogin) {
    watch(
      () => route.query.code,
      async (code) => {
        if (!code) return;
        // 同一个 code 只处理一次，避免 immediate + 路由变化 / 组件重挂载导致接口被调两次
        if (code === lastHandledWechatCode) return;
        lastHandledWechatCode = code;
        const state = route.query.state;
        // OAuth 回跳后 form / URL 上的邀请码都没了，从扫码前暂存的 localStorage 取回
        const stashedInviteCode = localStorage.getItem(WECHAT_INVITE_CODE_STORAGE_KEY) || '';
        localStorage.removeItem(WECHAT_INVITE_CODE_STORAGE_KEY);
        try {
          await userStore.loginByWechat({
            code,
            state,
            inviteCode: stashedInviteCode || loginForm.value.inviteCode || externalInviteCode?.value || '',
          });
          await saveUserTracking({
            target: 'LOGIN',
          });
          closeLogin();
          await reloadFromTop();
        } catch (error) {
          console.error(error);
          ElMessage.error(error?.message);
        } finally {
          clearWechatQuery();
        }
      },
      { immediate: true },
    );

    // 启用扫码登录的入口（如 PC 弹窗）打开时即初始化微信 QR；不再依赖 activeTab tab 切换
    watch(loginVisible, (visible) => {
      if (!visible) return;
      loginForm.value.inviteCode = externalInviteCode?.value || '';
      // 弹窗一打开就把当前邀请码写入 localStorage，覆盖"自动填入但用户没改"的常见路径
      if (loginForm.value.inviteCode) {
        localStorage.setItem(WECHAT_INVITE_CODE_STORAGE_KEY, loginForm.value.inviteCode);
      } else {
        localStorage.removeItem(WECHAT_INVITE_CODE_STORAGE_KEY);
      }
      initWechatQRCode();
    });

    // 用户在弹窗里手动改邀请码时同步到 localStorage，确保扫码时取到的是最新值
    watch(() => loginForm.value.inviteCode, (val) => {
      if (!loginVisible.value) return;
      if (val) localStorage.setItem(WECHAT_INVITE_CODE_STORAGE_KEY, val);
      else localStorage.removeItem(WECHAT_INVITE_CODE_STORAGE_KEY);
    });
  }

  onUnmounted(() => {
    resetCountdown();
  });

  return {
    activeTab,
    loginVisible,
    loginFormRef,
    loginForm,
    countdown,
    sendingCode,
    loginLoading,
    canSendCode,
    canLogin,
    loginRules,
    validatePhone,
    handleSendCode,
    handleLogin,
    openLogin,
    closeLogin,
    resetLoginForm,
    openPolicyDialog,
  };
}
