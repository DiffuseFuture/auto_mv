/**
 * 分享相关的浏览器环境检测、剪贴板兜底、以及全局统一的分享行为入口。
 *
 * 项目内所有"分享按钮"必须用这里的 shareUrl / shareByProjectId，避免分散各处导致每改一处都漏。
 * 调用矩阵：
 *  - 资源页"分享" / 创作页移动端音乐播放器 / 创作页移动端视频卡：用 shareByProjectId
 *  - 分享页"分享当前链接"：用 shareUrl(location.href)
 */
import {shareLink} from '@/api/share';
import {saveUserTracking} from '@/api/tracking';

/**
 * 判断当前是否为"真实 iPhone"。
 *
 * - Chromium 系（Chrome/Edge/Android Chrome）：navigator.userAgentData 必定存在，返回的
 *   是真实操作系统（"Windows"/"macOS"/"Linux"/"Android"），DevTools 设备模拟也不会改这个值。
 *   只要 userAgentData 存在就直接判 false——挡得住 PC + 任何模拟。
 * - Safari 系（不支持 userAgentData）：fallback 到 UA + 老 navigator.platform 双重检测。
 *   真实 iPhone Safari 上 platform === "iPhone"。
 *
 * @returns {boolean}
 */
export const isRealIOS = () => {
  if (navigator.userAgentData) return false;
  const ua = navigator.userAgent || '';
  const platform = navigator.platform || '';
  return /iPhone|iPod/i.test(ua) && /iPhone|iPod/i.test(platform);
};

/**
 * 复制文本到剪贴板。
 *
 * 优先用同步的 document.execCommand('copy')，原因：
 * - navigator.clipboard.writeText 在多种场景下不可用：HTTP 上下文拒绝、鸿蒙浏览器抛
 *   "write permission denied"、await 之后 user activation 已过期；
 * - execCommand 虽然规范上 deprecated，但所有浏览器都还支持，对 user activation 要求宽松，最稳定。
 *
 * @param {string} text
 * @returns {Promise<boolean>}
 */
export const copyTextToClipboard = async (text) => {
  try {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.cssText = 'position:fixed;opacity:0;left:-9999px;top:0';
    ta.setAttribute('readonly', '');
    document.body.appendChild(ta);
    ta.select();
    ta.setSelectionRange(0, text.length); // iOS Safari 需要
    const ok = document.execCommand('copy');
    document.body.removeChild(ta);
    if (ok) return true;
  } catch {}
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {}
  }
  return false;
};

/**
 * 分享一个 URL。iOS 走系统分享面板，其他设备复制链接。
 * 不耦合 ElMessage / i18n——调用方根据返回的 mode 自行 toast。
 *
 * @param {string} url
 * @returns {Promise<'native'|'clipboard'|'cancelled'|'failed'>}
 *   - 'native'    iOS 系统分享面板已弹出（系统自带反馈，调用方不必 toast）
 *   - 'clipboard' 复制到剪贴板成功（调用方应 toast "已复制"）
 *   - 'cancelled' iOS 用户取消系统分享（调用方应静默退出）
 *   - 'failed'    既无法系统分享也无法复制（调用方应 toast "分享失败"）
 */
export const shareUrl = async (url) => {
  if (isRealIOS() && navigator.share) {
    try {
      await navigator.share({url});
      return 'native';
    } catch (error) {
      if (error?.name === 'AbortError') return 'cancelled';
      // 系统分享失败兜底走复制
    }
  }
  const ok = await copyTextToClipboard(url);
  return ok ? 'clipboard' : 'failed';
};

/**
 * 通过 projectId 分享：调 shareLink 接口拿 shareId → 拼分享 URL → shareUrl 分发。
 * 自动处理埋点（不阻塞主流程），不耦合 ElMessage / i18n——调用方根据返回的 mode 自行 toast。
 *
 * @param {Object} options
 * @param {string} options.projectId 必填
 * @param {string} options.langPrefix 当前语言前缀（"zh"/"en"），用于拼 share URL
 * @param {string} [options.trackingTarget] 埋点 target；不传则不上报
 * @returns {Promise<{mode: 'native'|'clipboard'|'cancelled'|'failed', url?: string, error?: any}>}
 */
export const shareByProjectId = async ({projectId, langPrefix, trackingTarget}) => {
  if (trackingTarget) {
    saveUserTracking({target: trackingTarget}).catch((error) => {
      console.error('分享埋点上报失败:', error);
    });
  }
  try {
    const shareId = await shareLink({projectId});
    const url = `${location.origin}/${langPrefix}/share?shareId=${shareId}`;
    const mode = await shareUrl(url);
    return {mode, url};
  } catch (error) {
    console.error(error);
    return {mode: 'failed', error};
  }
};
