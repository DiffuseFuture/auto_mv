import {defineStore} from 'pinia';
import {ref, computed} from 'vue';
import {loginByPhone, getUserInfo, logout} from '@/api/auth';
import {getUserPlan} from '@/api/creation';
import request from '@/utils/request';
import {ElMessage} from 'element-plus';
import {translate as t} from '@/i18n';
import {reportBaiduConvert, BAIDU_CONVERT_TYPE} from '@/utils/baiduTrack';

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref(localStorage.getItem('token') || '');
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'));
    // 计算属性 - 是否已登录
    const isLoggedIn = computed(() => !!token.value);
    // 计算属性 - 用户名
    const userName = computed(() => userInfo.value?.nickName || t('userStore.notLoggedIn'));
    // 计算属性 - 用户头像
    const avatar = computed(() => userInfo.value?.avatarImg || '');
    // 计算属性 - API Key
    const apiKey = computed(() => userInfo.value?.apiKey || '');

    // 用户订阅计划（含积分余额）
    const userPlan = ref(null);
    const pointsBalance = computed(() => userPlan.value?.pointsBalance ?? 0);
    const tierName = computed(() => userPlan.value?.tierName || t('userStore.freePlan'));

    /**
     * 设置 token
     * @param {string} newToken - 新的 token
     */
    const setToken = (newToken) => {
        token.value = newToken;
        localStorage.setItem('token', newToken);
    };

    /**
     * 设置用户信息
     * @param {object} info - 用户信息对象
     */
    const setUserInfo = (info) => {
        userInfo.value = info;
        localStorage.setItem('userInfo', JSON.stringify(info));
    };

    /**
     * 手机号登录
     * @param {object} loginData
     * @returns {Promise}
     */
    const login = async (loginData) => {
        try {
            // 调用登录接口
            const data = await loginByPhone(loginData);

            // 保存 token 和用户信息
            setToken(data.token);
            setUserInfo(data);
            // 百度推广转化上报（登录）
            reportBaiduConvert(BAIDU_CONVERT_TYPE.FORM_SUCCESS);
            ElMessage.success(t('userStore.loginSuccess'));
            return data;
        } catch (error) {
            console.error('登录失败:', error);
            throw error;
        }
    };

    /**
     * 微信登录
     * @param {object} data - { code, state }
     */
    const loginByWechat = async ({ code, state, inviteCode = '' }) => {
        try {
            const data = await request({
                url: '/ohyesai-next/user/wx-web-signin',
                method: 'post',
                data: { code, state, inviteCode }
            });
            // 响应实例中 token 和用户信息都在 data 根级
            setToken(data.token);
            setUserInfo(data);
            // 百度推广转化上报（登录）
            reportBaiduConvert(BAIDU_CONVERT_TYPE.FORM_SUCCESS);
            ElMessage.success(t('userStore.loginSuccess'));
            return data;
        } catch (error) {
            console.error('微信登录失败:', error);
            throw error;
        }
    };

    /**
     * 获取用户信息
     * @returns {Promise}
     */
    const fetchUserInfo = async () => {
        try {
            const data = await getUserInfo();
            setUserInfo(data);
            return data;
        } catch (error) {
            console.error('获取用户信息失败:', error);
            throw error;
        }
    };

    /** 获取用户订阅计划（含积分余额） */
    const fetchUserPlan = async () => {
        try {
            userPlan.value = await getUserPlan();
        } catch (error) {
            console.error(error);
        }
    };

    /**
     * 退出登录
     * @returns {Promise}
     */
    const logoutUser = async () => {
        try {
            await logout();
        } catch (error) {
            console.error('退出登录失败:', error);
        } finally {
            // 无论接口成功与否,都清除本地数据
            clearUserData();
            ElMessage.success(t('userStore.logoutSuccess'));
        }
    };

    /**
     * 清空当前地址栏中的所有路由参数（query/hash）。
     * 退出登录后统一回到纯路径地址，避免参数污染后续访问。
     */
    const clearRouteParams = () => {
        const purePath = window.location.pathname;
        window.history.replaceState({}, document.title, purePath);
    };

    /**
     * 清除用户数据 (用于 token 过期 / 主动退出登录)
     *
     * 退出后若用户已不在首页（例如从 /creation、/resource 点退出登录），跳到首页再加载；
     * 已经在首页则原地 reload。用 location.replace 避免在历史栈里留下登录态下的旧页面，
     * 防止用户按"后退"回到刚被清状态的页面看到一堆未登录的报错/空状态。
     */
    const clearUserData = () => {
        token.value = '';
        userInfo.value = null;
        userPlan.value = null;
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        clearRouteParams();

        const path = window.location.pathname;
        const isHome = /^\/(zh|en)\/mv\/?$/.test(path);
        if (isHome) {
            window.location.reload();
            return;
        }
        const lang = path.match(/^\/(zh|en)\//)?.[1] || 'zh';
        window.location.replace(`/${lang}/mv`);
    };

    // 监听 token 过期事件
    if (typeof window !== 'undefined') {
        window.addEventListener('token-expired', () => {
            clearUserData();
        });
    }

    return {
        // 状态
        token,
        userInfo,
        userPlan,
        // 计算属性
        isLoggedIn,
        userName,
        avatar,
        apiKey,
        pointsBalance,
        tierName,
        // 方法
        setToken,
        setUserInfo,
        login,
        loginByWechat,
        fetchUserInfo,
        fetchUserPlan,
        logoutUser,
        clearUserData,
    };
});
