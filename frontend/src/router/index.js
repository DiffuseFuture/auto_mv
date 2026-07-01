import {createRouter, createWebHistory} from 'vue-router';
import {useSeo} from '@/composables/useSeo.js';
import {changeLocale, getCurrentLocale, getLocaleByPrefix, getLocalePrefix} from '@/i18n';
import {isMobileClient} from '@/utils/index.js';
import {captureBdVid} from '@/utils/baiduTrack.js';

const useMobile = isMobileClient();
const resolvePage = (desktopLoader, mobileLoader) => (useMobile ? mobileLoader : desktopLoader);
const resolveLayout = useMobile ? () => import('@/layout/mobile.vue') : () => import('@/layout/index.vue');

const getDefaultLangPrefix = () => getLocalePrefix(getCurrentLocale());

const withLangSeoUrl = (lang, url) => `/${lang}${url}`;

const routes = [
    // 根路径：按当前语言重定向
    {
        path: '/',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/mv`, query: to.query}),
    },

    // 语言前缀主壳
    {
        path: '/:lang(en|zh)',
        component: resolveLayout,
        children: [
            {path: '', redirect: (to) => `/${to.params.lang}/mv`},
            // 首页
            {
                path: 'mv',
                name: 'home',
                component: resolvePage(() => import('@/views/home/index.vue'), () => import('@/views/home/mobile.vue')),
                meta: {
                    showMobileTabBar: true,
                    seo: {
                        title: 'AI 音乐可视化创作平台',
                        description: 'OhYesAI 是一个 AI 驱动的音乐可视化创作平台，上传音乐即可一键生成 MV 视频。',
                        keywords: 'AI 音乐可视化, AI MV 制作, 音乐视频生成, AI 创作平台, OhYesAI',
                        url: '/mv',
                    },
                },
            },
            // 创作
            {
                path: 'creation',
                name: 'creation',
                component: resolvePage(() => import('@/views/creation/index.vue'), () => import('@/views/creation-mobile/mobile.vue')),
                meta: {
                    showMobileTabBar: false,
                    seo: {
                        title: 'AI 创作工作台',
                        description: '在 OhYesAI 通过智能对话创作音乐与 MV 视频，支持分镜编辑、参考图上传与多种模型。',
                        keywords: 'AI 创作, MV 制作, 音乐生成, 分镜编辑, 智能创作',
                        url: '/creation',
                    },
                },
            },
            // 资源
            {
                path: 'resource',
                name: 'resource',
                component: resolvePage(() => import('@/views/resource/index.vue'), () => import('@/views/resource/mobile.vue')),
                meta: {
                    showMobileTabBar: true,
                    seo: {
                        title: '我的项目',
                        description: '管理你在 OhYesAI 创作的全部 MV 视频与音乐作品，支持查看、搜索与整理你的资产。',
                        keywords: '我的项目, 创作管理, MV 视频, 音乐作品',
                        url: '/resource',
                    },
                },
            },
            {
                path: 'policy/:type(terms|privacy)',
                name: 'policy',
                component: resolvePage(() => import('@/views/legal/policy.vue'), () => import('@/views/legal/mobile.vue')),
                meta: {
                    showMobileTabBar: false,
                },
            },
        ],
    },

    // 使用指南（独立页面，不含应用外壳）
    {
        path: '/:lang(en|zh)/guide',
        name: 'guide',
        component: () => import('@/views/guide/index.vue'),
        meta: {
            seo: {
                title: '使用指南',
                description: 'OhYesAI 使用指南：模型选择、素材准备、五步掌握全流程，零门槛一键生成高质量 MV。',
                keywords: 'OhYesAI 使用指南, AI MV 教程, 模型选择, 分镜脚本, 一键生成',
                url: '/guide',
            },
        },
    },

    // 常见问题（独立页面，不含应用外壳）
    {
        path: '/:lang(en|zh)/faq',
        name: 'faq',
        component: () => import('@/views/faq/index.vue'),
        meta: {
            seo: {
                title: '常见问题',
                description: 'OhYesAI 常见问题解答，账号登录、视频生成、积分使用等问题一站解决。',
                keywords: 'OhYesAI 常见问题, 账号, 登录, 视频生成, 积分, 帮助',
                url: '/faq',
            },
        },
    },

    // 订阅页（独立页面，不含应用外壳）
    {
        path: '/:lang(en|zh)/subscribe',
        name: 'subscribe',
        component: resolvePage(() => import('@/views/subscription/index.vue'), () => import('@/views/subscription/mobile.vue')),
        meta: {
            seo: {
                title: '订阅方案',
                description: '选择适合你的 OhYesAI 订阅方案，解锁更多 AI 创作功能与积分，支持月付与年付。',
                keywords: 'OhYesAI 订阅, 套餐, 积分, AI 创作会员',
                url: '/subscribe',
            },
        },
    },
    {
        path: '/:lang(en|zh)/subscribe-faq',
        name: 'subscribeFaq',
        component: () => import('@/views/subscription/faq.vue'),
        meta: {
            seo: {
                title: '订阅常见问题',
                description: 'OhYesAI 订阅与积分相关常见问题：积分获取与扣除规则、套餐切换、退款政策、联系方式等一站解答。',
                keywords: 'OhYesAI 订阅常见问题, 积分规则, 套餐切换, 退款政策, 充值, 帮助',
                url: '/subscribe-faq',
            },
        },
    },
    // 分享查看页（独立页面，不含应用外壳）
    {
        path: '/:lang(en|zh)/share',
        name: 'share',
        component: resolvePage(() => import('@/views/share/index.vue'), () => import('@/views/share/mobile.vue')),
        meta: {
            seo: {
                title: '分享作品',
                description: '观看 OhYesAI 用户创作的精彩 MV 视频与音乐作品。',
                keywords: 'OhYesAI 分享, MV 分享, 音乐分享, AI 创作分享',
                url: '/share',
            },
        },
    },

    // 旧路径兼容：无语言前缀时，按当前语言重定向
    {
        path: '/mv',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/mv`, query: to.query}),
    },
    {
        path: '/creation',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/creation`, query: to.query}),
    },
    {
        path: '/resource',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/resource`, query: to.query}),
    },
    {
        path: '/subscribe',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/subscribe`, query: to.query}),
    },
    {
        path: '/share',
        redirect: (to) => ({path: `/${getDefaultLangPrefix()}/share`, query: to.query}),
    },

    // 兜底：未知路径统一回到当前语言首页（避免用户手输 /enxxx 导致空白页）
    {
        path: '/:pathMatch(.*)*',
        redirect: () => `/${getDefaultLangPrefix()}/mv`,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) return savedPosition;
        return {top: 0};
    },
});

// 同步 URL 语言前缀到 i18n locale；同时兜底抓取 bd_vid（防止根路径重定向丢 query）
router.beforeEach(async (to) => {
    // 用 to 而非 window.location：beforeEach 时浏览器 URL 还未切换到目标
    if (to.query?.bd_vid) {
        const search = '?' + new URLSearchParams(to.query).toString();
        captureBdVid({search, fullUrl: window.location.origin + to.fullPath});
    }
    const lang = to.params?.lang;
    if (typeof lang !== 'string') return true;
    const targetLocale = getLocaleByPrefix(lang);
    if (targetLocale !== getCurrentLocale()) await changeLocale(targetLocale);
    return true;
});

// Global navigation guard: update SEO meta on each route change
router.afterEach((to) => {
    if (to.name === 'share') return;
    const seo = to.meta?.seo;
    if (seo) {
        const lang = typeof to.params?.lang === 'string' ? to.params.lang : getDefaultLangPrefix();
        useSeo({...seo, url: withLangSeoUrl(lang, seo.url)});
    }
});

export default router;
