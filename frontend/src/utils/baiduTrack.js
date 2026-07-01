/**
 * 百度推广服务端转化追踪（CAPI 方案）工具。
 *
 * 使用流程：
 * 1. 用户从百度搜索广告点击进入落地页，URL 上会带 `bd_vid` 查询参数；
 *    在应用启动时调用 `captureBdVid()`，将完整落地 URL 持久化到 localStorage。
 * 2. 用户后续触发关键转化行为（登录 / 购买成功）时，
 *    调用 `reportBaiduConvert(newType)` 把落地 URL + 转化类型上报后端，
 *    由后端再调用百度的转化上报接口（CAPI）。
 *
 * 注意：所有错误都会被 try/catch 静默吞掉，绝不能影响业务主流程。
 */
import {uploadCovertData} from '@/api/tracking';

/** localStorage 中保存带 bd_vid 的落地 URL 的 key */
const STORAGE_KEY = 'baidu_logid_url';

/**
 * 百度转化类型枚举
 * - SERVICE_SUCCESS：扫码支付成功
 * - FORM_SUCCESS：登录
 */
export const BAIDU_CONVERT_TYPE = {
    SERVICE_SUCCESS: 'SERVICE_SUCCESS',
    FORM_SUCCESS: 'FORM_SUCCESS',
};

/**
 * 若 URL 含 `bd_vid` 参数，把完整 URL 写入 localStorage 持久化。
 * 由于支付/登录可能发生在落地后数小时甚至跨会话，必须 localStorage 持久化。
 * 多次点击百度广告时以最近一次落地为准（直接覆盖旧值）。
 *
 * 调用时机（双保险）：
 * - 首次：`main.js` 模块顶层（同步、最早），抓住裸落地 URL；
 * - 兜底：`router.beforeEach`，每次 SPA 导航再扫一次（用 `to` 传入，因为
 *   beforeEach 时 `window.location` 还没切到目标 URL）。
 *
 * @param {{ search?: string, fullUrl?: string }} [override]
 *   可选覆盖入参：`search` 是查询串（可带或不带前导 `?`），`fullUrl` 是要落盘的完整 URL。
 *   不传时默认读 `window.location`。
 */
export const captureBdVid = (override) => {
    try {
        const search = override?.search ?? window.location.search ?? '';
        const fullUrl = override?.fullUrl ?? window.location.href;
        const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
        if (!params.has('bd_vid')) return;
        localStorage.setItem(STORAGE_KEY, fullUrl);
    } catch (error) {
        console.error('[baiduTrack] captureBdVid failed:', error);
    }
};

/**
 * 上报百度推广转化事件。
 * 若 localStorage 中没有 `baidu_logid_url`（即用户不是从百度广告进来的），直接 return。
 * 所有异常静默处理，不影响业务主流程。
 * @param {'SERVICE_SUCCESS'|'FORM_SUCCESS'} newType - 转化类型
 */
export const reportBaiduConvert = async (newType) => {
    try {
        const logidUrl = localStorage.getItem(STORAGE_KEY);
        if (!logidUrl) return;
        await uploadCovertData({logidUrl, newType});
    } catch (error) {
        console.error('[baiduTrack] reportBaiduConvert failed:', error);
    }
};
