import axios from 'axios';
import {ElMessage} from 'element-plus';
import {translate as t} from '@/i18n';

// 开发环境用 vite 代理，生产环境由部署侧反向代理。
const request = axios.create({
    baseURL: '',
    timeout: 30000,
    headers: {'Content-Type': 'application/json;charset=UTF-8'},
});

request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) config.headers.token = token;
        return config;
    },
    (error) => Promise.reject(error),
);

// token 失效全局只处理一次：避免并发请求同时返回 4003 时弹多条 toast / 多次跳转。
// clearUserData() 触发 location.reload/replace 会重置模块状态，下次进来自然回到 false。
let tokenExpiredHandled = false;

/**
 * 响应拦截器：只做错误标准化，不弹 toast。调用方自行 try/catch 决定如何提示。
 * - 业务错误（res.code !== 0）：reject 整个 res 对象（含 code/message/data）
 *   - 例外：code === 4003 且本地有 token，说明缓存 token 已失效，全局弹一次提示 + 走退出登录清理
 * - 网络/HTTP 错误：把人话文案塞到 error.message，方便调用方写 `error?.message || fallback`
 */
request.interceptors.response.use(
    (response) => {
        const res = response.data;
        if (res.code !== 0) {
            if (res.code === 4003 && localStorage.getItem('token') && !tokenExpiredHandled) {
                tokenExpiredHandled = true;
                ElMessage.error(t('userStore.tokenExpired'));
                window.dispatchEvent(new Event('token-expired'));
            }
            return Promise.reject(res);
        }
        return res.data;
    },
    (error) => {
        console.error('响应错误:', error);
        const rawMessage = String(error?.message || '');
        if (rawMessage.includes('timeout')) {
            error.message = '请求超时，请稍后重试';
        } else if (rawMessage.includes('Network Error')) {
            error.message = '网络错误，请检查网络连接';
        } else if (error.response?.data?.message) {
            error.message = error.response.data.message;
        }
        // 其它情况保留 axios 原 message（如 "Request failed with status code 500"）
        return Promise.reject(error);
    },
);

export default request;
