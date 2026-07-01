/**
 * 顶部账户区共享逻辑。
 *
 * 设计动机：顶栏 TopBar 内的「积分使用详情」与「管理账号」打开的是同一个 AccountDialog，
 * 靠 props/events 桥接太啰嗦，用一个 module 级 ref 在所有调用方间天然共享。
 *
 * 约定：AccountDialog / LoginDialog 在 TopBar 内统一渲染。页面（如 home）若也需要触发
 * 登录弹窗，直接取这里的 loginVisible 即可，与 TopBar 共用同一份显隐状态。
 */
import {ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessageBox} from 'element-plus';
import {useUserStore} from '@/store/user';
import {translate as t} from '@/i18n';
import {saveUserTracking} from '@/api/tracking';

// === 跨实例共享的 dialog 状态（module-level）===
// 同一时刻只有一个页面活跃，多个 TopBar 实例共用一份显隐状态是合理的。
const loginVisible = ref(false);
const accountVisible = ref(false);
const accountDialogTab = ref('profile');
// 邀请码：home 页通过 onMounted 直接写入（来自 URL ?inviteCode=xxx），LoginDialog 双向读取
const referralCode = ref('');

/**
 * 顶部账户区共享逻辑。
 * @returns {{
 *   loginVisible: import('vue').Ref<boolean>,
 *   accountVisible: import('vue').Ref<boolean>,
 *   accountDialogTab: import('vue').Ref<string>,
 *   referralCode: import('vue').Ref<string>,
 *   openLogin: () => void,
 *   openAccount: (tab?: string) => void,
 *   goSubscribe: (trackingTarget?: string) => void,
 *   logout: () => Promise<void>,
 * }}
 */
export function useTopBarAccount() {
  const router = useRouter();
  const route = useRoute();
  const userStore = useUserStore();

  /** 取当前路由语言前缀，缺省 zh（兜底用于路由跳转 params.lang） */
  const resolveLang = () => (typeof route.params?.lang === 'string' ? route.params.lang : 'zh');

  /** 打开登录弹窗 */
  const openLogin = () => {
    loginVisible.value = true;
  };

  /**
   * 打开账户管理弹窗并定位到指定 tab。
   * @param {string} [tab='profile'] 'profile' | 'vpoint' 等 AccountDialog 支持的 tab
   */
  const openAccount = (tab = 'profile') => {
    accountDialogTab.value = tab;
    accountVisible.value = true;
  };

  /**
   * 升级 / 跳订阅页。
   * @param {string} [trackingTarget] 传入则上报 saveUserTracking（home 页传 'HOME_CLICK_UPGRADE'）
   */
  const goSubscribe = (trackingTarget) => {
    if (trackingTarget) {
      saveUserTracking({target: trackingTarget}).catch((error) => {
        console.error('点击升级埋点上报失败:', error);
      });
    }
    router.push({name: 'subscribe', params: {lang: resolveLang()}});
  };

  /** 退出登录：二次确认 → store.logoutUser() */
  const logout = async () => {
    try {
      await ElMessageBox.confirm(t('layout.logoutConfirm.message'), t('layout.logoutConfirm.title'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        showClose: false,
        closeOnClickModal: false,
        customClass: 'logout-confirm',
      });
      await userStore.logoutUser();
    } catch {
      // 用户取消
    }
  };

  return {
    loginVisible,
    accountVisible,
    accountDialogTab,
    referralCode,
    openLogin,
    openAccount,
    goSubscribe,
    logout,
  };
}
