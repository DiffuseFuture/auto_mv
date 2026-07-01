import {computed, inject, reactive} from 'vue';

const I18N_SYMBOL = Symbol('app-i18n');
const LOCALE_STORAGE_KEY = 'ohyesai-locale';

const LOCALE_PREFIX = {
  'en-US': 'en',
  'zh-CN': 'zh',
};

const PREFIX_LOCALE = {
  en: 'en-US',
  zh: 'zh-CN',
};

const SUPPORTED_LOCALES = ['en-US', 'zh-CN'];

const loadedMessages = reactive({});

const loadLocaleMessages = async (locale) => {
  if (!SUPPORTED_LOCALES.includes(locale)) return;
  if (loadedMessages[locale]) return;

  const mod = locale === 'zh-CN'
    ? await import('./messages/zh-CN.js')
    : await import('./messages/en-US.js');

  loadedMessages[locale] = mod.default || {};
};

const getInitialLocale = () => {
  if (typeof localStorage !== 'undefined') {
    const cached = localStorage.getItem(LOCALE_STORAGE_KEY);
    if (cached && SUPPORTED_LOCALES.includes(cached)) return cached;
  }
  const navLang = typeof navigator !== 'undefined' ? navigator.language : '';
  if (String(navLang).toLowerCase().startsWith('zh')) return 'zh-CN';
  return 'en-US';
};

const state = reactive({
  locale: getInitialLocale(),
});

export const getCurrentLocale = () => state.locale;

export const ensureLocaleLoaded = async (locale) => {
  await loadLocaleMessages(locale);
};

/**
 * 统一的语言切换入口：先确保语言包就绪，再切换 locale。
 * @param {string} locale
 * @returns {Promise<void>}
 */
export const changeLocale = async (locale) => {
  await ensureLocaleLoaded(locale);
  setLocale(locale);
};

export const getLocalePrefix = (locale) => {
  return LOCALE_PREFIX[locale] || 'en';
};

export const getLocaleByPrefix = (prefix) => {
  const normalized = String(prefix || '').toLowerCase();
  return PREFIX_LOCALE[normalized] || getInitialLocale();
};

/**
 * 将普通 path（如 /mv）转换为带语言前缀的 path（如 /en/mv）。
 * @param {string} path
 * @param {string} [locale]
 * @returns {string}
 */
export const toLocalizedPath = (path, locale = state.locale) => {
  const raw = String(path || '').trim();
  const normalized = raw.startsWith('/') ? raw : `/${raw}`;
  return `/${getLocalePrefix(locale)}${normalized}`;
};

/**
 * 根据 key 从当前语言包读取文案。
 * @param {string} key 多级 key，使用点号分隔。
 * @returns {string|Array<string>} 命中的文案或 key 本身。
 */
const getMessageByKey = (key) => {
  const localeMessages = loadedMessages[state.locale] || loadedMessages['en-US'] || {};
  const value = key.split('.').reduce((acc, segment) => (acc && acc[segment] !== undefined ? acc[segment] : undefined), localeMessages);
  return value ?? key;
};

/**
 * 基础翻译函数，支持 {token} 模板替换。
 * @param {string} key 语言 key。
 * @param {Record<string, string|number>} [params] 替换参数。
 * @returns {string|Array<string>} 翻译结果。
 */
const t = (key, params = {}) => {
  const raw = getMessageByKey(key);
  if (Array.isArray(raw)) return raw;
  if (typeof raw !== 'string') return key;
  return raw.replace(/\{(\w+)\}/g, (_, token) => String(params[token] ?? `{${token}}`));
};

export const translate = (key, params = {}) => t(key, params);

export const setLocale = (locale) => {
  if (!SUPPORTED_LOCALES.includes(locale)) return;
  state.locale = locale;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  }
  if (typeof document !== 'undefined' && document.documentElement) {
    document.documentElement.lang = locale;
  }
};

const i18nApi = {
  locale: computed(() => state.locale),
  t,
  setLocale,
  changeLocale,
  availableLocales: computed(() => SUPPORTED_LOCALES),
  getLocalePrefix,
  getLocaleByPrefix,
  toLocalizedPath,
  ensureLocaleLoaded,
};

export const setupI18n = (app) => {
  app.provide(I18N_SYMBOL, i18nApi);
  app.config.globalProperties.$t = t;
};

export const useI18nText = () => {
  return inject(I18N_SYMBOL, i18nApi);
};
