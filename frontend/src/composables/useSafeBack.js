import {useRouter, useRoute} from 'vue-router';

/**
 * 安全返回上一页。
 *
 * router.back() 等价于 window.history.go(-1)，完全依赖浏览器历史栈。
 * 用户在创作 / 订阅 / 法律等独立页直接刷新或新标签页打开时，SPA 内部导航
 * 历史归零；浏览器历史栈的上一条要么是外站、要么为空，表现为"点了没反应"
 * （微信 / UC 等内置浏览器尤其明显），或者跳出本站。
 *
 * Vue Router 4 在 window.history.state.back 里维护本次会话内的上一条站内 URL，
 * 为空则说明站内没有上一页，需要兜底跳转。
 *
 * @returns {(fallbackPath?: string) => void} 调用时若站内有上一页则 router.back()，
 *   否则 router.replace 到 fallbackPath（默认 /:lang/mv 首页）。
 *   用 replace 而非 push，避免用户连按"返回"在历史栈叠加多条首页。
 */
export function useSafeBack() {
  const router = useRouter();
  const route = useRoute();

  return (fallbackPath) => {
    const back = window.history.state?.back;
    if (back) {
      try {
        const backUrl = new URL(back, window.location.origin);
        // 同源且不是当前页才真的回退；上一页就是当前页时（刷新后某些场景）走兜底，避免原地不动
        if (backUrl.origin === window.location.origin && backUrl.pathname !== route.path) {
          router.back();
          return;
        }
      } catch (error) {
        console.error('解析上一页路由失败:', error);
      }
    }
    const lang = route.params?.lang || 'zh';
    router.replace(fallbackPath || `/${lang}/mv`);
  };
}
