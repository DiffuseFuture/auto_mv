import {marked} from 'marked';
import hljs from 'highlight.js';
import createDOMPurify from 'dompurify';

let isMarkedConfigured = false; // 标记是否已完成 marked 初始化（避免组件重复 setOptions）

// 允许的 HTML 标签集合（最小化允许范围以降低 v-html 风险）
const ALLOWED_TAGS = new Set([
  'a',
  'blockquote',
  'br',
  'code',
  'del',
  'div',
  'em',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'hr',
  'img',
  'ins',
  'kbd',
  'li',
  'ol',
  'p',
  'pre',
  's',
  'span',
  'strong',
  'table',
  'tbody',
  'td',
  'tfoot',
  'th',
  'thead',
  'tr',
  'ul',
]);

const DOMPURIFY_FORBID_TAGS = [
  'script',
  'style',
  'iframe',
  'object',
  'embed',
  'link',
  'meta',
  'base',
];

const DOMPURIFY_ALLOWED_ATTR = [
  'href',
  'src',
  'alt',
  'title',
  'target',
  'rel',
  'class',
  'id',
  'colspan',
  'rowspan',
];

const DOMPURIFY_FORBID_ATTR = [
  'style',
  'srcset',
];

let domPurifyInstance = null;
let domPurifyHookConfigured = false;

const getDomPurify = () => {
  if (domPurifyInstance) return domPurifyInstance;
  if (typeof window === 'undefined') return null;

  domPurifyInstance = createDOMPurify(window);

  // 仅配置一次：补齐 target="_blank" 的 opener 防护 rel
  if (!domPurifyHookConfigured) {
    domPurifyInstance.addHook('afterSanitizeAttributes', (node) => {
      if (!node || node.nodeType !== 1) return; // 只处理 Element
      if (String(node.tagName || '').toLowerCase() !== 'a') return;

      // 修正 marked 自动链接时把末尾括号/标点拼进 href 的问题
      const href = node.getAttribute('href');
      if (href) {
        const trimmed = trimTrailingHrefPunctuation(href);
        const safe = getSafeUrl(trimmed);
        if (!safe) node.removeAttribute('href');
        else node.setAttribute('href', safe);
      }

      const target = node.getAttribute('target');
      if (target !== '_blank') return;

      const rel = node.getAttribute('rel') || '';
      const relLower = rel.toLowerCase();
      const needsNoopener = !relLower.includes('noopener');
      const needsNoreferrer = !relLower.includes('noreferrer');
      if (!needsNoopener && !needsNoreferrer) return;

      node.setAttribute(
        'rel',
        `${rel} ${needsNoopener ? 'noopener' : ''} ${needsNoreferrer ? 'noreferrer' : ''}`.trim(),
      );
    });

    domPurifyHookConfigured = true;
  }

  return domPurifyInstance;
};

const MAX_MARKDOWN_CACHE_SIZE = 200;
const markdownHtmlCache = new Map(); // key: markdown 原文, value: sanitized html

// 表格单元格视觉宽度判定：CJK 字符按 2 个 latin 计；阈值 24 ≈ 12 个汉字 / 24 个英文字符
// 范围：　-鿿 CJK 标点+基本汉字、豈-﫿 CJK 兼容、＀-￯ 全/半角
const TABLE_CELL_CJK_REGEX = /[　-鿿豈-﫿＀-￯]/;
const TABLE_CELL_LONG_THRESHOLD = 24;

/**
 * 给已 sanitized 的 HTML 里所有 <td>/<th> 按内容视觉宽度打 cell-short / cell-long 类。
 * 配合 CSS：
 *   td.cell-short { width: max-content; white-space: nowrap }  → 精确占自己 max-content 宽度
 *   td.cell-long  { white-space: pre-wrap }                    → 抢剩余宽度、长内容自然换行
 * 这样短列绝不被挤断行、长列吃掉剩余空间，表格自然撑满父容器。
 * @param {string} html 已 sanitized 的 HTML
 * @returns {string} 已打类的 HTML（无表格或环境不支持 DOMParser 时原样返回）
 */
const annotateTableCells = (html) => {
  if (typeof window === 'undefined' || typeof DOMParser === 'undefined') return html;
  if (!html || (!html.includes('<td') && !html.includes('<th'))) return html;
  try {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const cells = doc.body.querySelectorAll('td, th');
    if (!cells.length) return html;
    for (const cell of cells) {
      const text = cell.textContent || '';
      let weight = 0;
      for (const ch of text) {
        weight += TABLE_CELL_CJK_REGEX.test(ch) ? 2 : 1;
      }
      cell.classList.add(weight > TABLE_CELL_LONG_THRESHOLD ? 'cell-long' : 'cell-short');
    }
    return doc.body.innerHTML;
  } catch {
    return html;
  }
};

/**
 * 确保 marked 的全局配置只执行一次。
 * 说明：该配置是为了统一代码块高亮行为，并避免在组件中重复调用 setOptions。
 */
const ensureMarkedConfigured = () => {
  if (isMarkedConfigured) return;

  marked.setOptions({
    highlight: (code, lang) => {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, {language: lang}).value;
      }
      return hljs.highlightAuto(code).value;
    },
    breaks: true, // 保留换行渲染行为（更符合常见 Markdown 预期）
    gfm: true, // 启用 GFM 扩展
  });

  isMarkedConfigured = true;
};

/**
 * 判断链接/资源 URL 是否安全（用于 href/src 等）。
 * @param {string | null | undefined} value 原始 URL
 * @returns {string} 通过校验的 URL；不通过则返回空字符串
 */
const getSafeUrl = (value) => {
  const raw = String(value ?? '').trim();
  if (!raw) return '';

  const lower = raw.toLowerCase();

  // 明确拦截危险协议
  if (lower.startsWith('javascript:')) return '';

  // 允许常见可点击协议
  if (lower.startsWith('http://') || lower.startsWith('https://')) return raw;
  if (lower.startsWith('mailto:') || lower.startsWith('tel:')) return raw;

  // 相对链接与锚点（通常由后端或 markdown 输入生成）
  if (lower.startsWith('/') || lower.startsWith('#')) return raw;

  // 默认拒绝其它协议（例如 data: text/html / blob: / file: 等）
  return '';
};

/**
 * marked 在自动识别 URL 时，可能把“句末标点/括号”也当作 URL 的一部分。
 * 例如：原文 `www.32r.com）` 会被解析成 `href="http://www.32r.com%EF%BC%89"`。
 * 这里裁剪 href 末尾的常见中文/英文标点后缀，确保链接可用。
 * @param {string | null | undefined} href 原始 href
 * @returns {string} 清理后的 href
 */
const trimTrailingHrefPunctuation = (href) => {
  let v = String(href ?? '').trim();
  if (!v) return v;

  // 先处理“已 URL 编码”的尾部标点
  const encodedTails = [
    '%EF%BC%89', // ）
    '%29', // )
    '%EF%BC%8C', // ，
    '%E3%80%82', // 。
    '%5D', // ]
    '%7D', // }
    '%3E', // >
  ];

  let changed = true;
  while (changed) {
    changed = false;
    const lower = v.toLowerCase();
    for (const tail of encodedTails) {
      if (lower.endsWith(tail)) {
        v = v.slice(0, v.length - tail.length);
        changed = true;
        break;
      }
    }
  }

  // 再处理“未编码”的尾部标点
  v = v.replace(/[)\]}>）。，]+$/u, '');

  return v;
};

/**
 * 图片 src 更严格一些：只允许 http(s) 或安全的 data:image。
 * @param {string | null | undefined} value 原始 src
 * @returns {string} 通过校验的 src；不通过则返回空字符串
 */
const getSafeImgSrc = (value) => {
  const raw = String(value ?? '').trim();
  if (!raw) return '';

  const lower = raw.toLowerCase();

  if (lower.startsWith('http://') || lower.startsWith('https://')) return raw;
  if (lower.startsWith('/') || lower.startsWith('#')) return raw;

  // 允许 data:image/* 的常见位图类型；禁止 svg（避免复杂脚本/事件风险）
  if (lower.startsWith('data:image/')) {
    const mime = lower.slice('data:image/'.length).split(';')[0];
    const allowed = [
      'image/png',
      'image/jpeg',
      'image/jpg',
      'image/gif',
      'image/webp',
    ];
    if (allowed.includes(mime)) return raw;
  }

  return '';
};

/**
 * 使用 DOMParser 进行更可靠的结构化清理。
 * @param {string} html marked 输出的 HTML 字符串
 * @returns {string} 清理后的 HTML 字符串
 */
const sanitizeHtmlWithDomParser = (html) => {
  const parser = new DOMParser(); // DOM 解析器（仅浏览器环境有意义）
  const doc = parser.parseFromString(html, 'text/html'); // 用 doc.body 承载内容
  const container = doc.body; // 容器根节点

  // 移除高风险标签（不在允许集内且可能包含脚本）
  container.querySelectorAll('script,style,iframe,object,embed,link,meta').forEach((el) => el.remove());

  const allElements = Array.from(container.querySelectorAll('*')); // 遍历所有元素

  for (const el of allElements) {
    const tag = el.tagName.toLowerCase(); // 当前元素标签名

    // 对不在允许集中的标签：用文本替换（尽量保留内容，丢弃未知结构/属性）
    if (!ALLOWED_TAGS.has(tag)) {
      const text = el.textContent ?? '';
      el.replaceWith(doc.createTextNode(text));
      continue;
    }

    // 清理属性：事件处理器 / style / 不安全的 href/src
    const attrs = Array.from(el.attributes);
    for (const attr of attrs) {
      const name = attr.name.toLowerCase(); // 属性名
      const value = attr.value; // 属性值

      // 移除事件属性
      if (name.startsWith('on')) {
        el.removeAttribute(attr.name);
        continue;
      }

      // 移除 style 属性（避免 CSS 注入/复杂兼容风险）
      if (name === 'style') {
        el.removeAttribute(attr.name);
        continue;
      }

      // 针对链接与图片资源做协议校验
      if (tag === 'a' && name === 'href') {
        const trimmed = trimTrailingHrefPunctuation(value);
        const safe = getSafeUrl(trimmed);
        if (!safe) el.removeAttribute('href');
        else el.setAttribute('href', safe);
      }

      if (tag === 'img' && name === 'src') {
        const safe = getSafeImgSrc(value);
        if (!safe) el.removeAttribute('src');
        else el.setAttribute('src', safe);
      }

      // 阻止 srcset（可能包含额外 URL 变体）
      if (tag === 'img' && name === 'srcset') {
        el.removeAttribute('srcset');
      }
    }

    // a[target=_blank] 追加 rel，防止 window.opener 风险
    if (tag === 'a') {
      const target = el.getAttribute('target');
      if (target === '_blank') {
        const rel = el.getAttribute('rel') || '';
        const hasNoopener = rel.toLowerCase().includes('noopener');
        const hasNoreferrer = rel.toLowerCase().includes('noreferrer');
        if (!hasNoopener || !hasNoreferrer) {
          el.setAttribute('rel', `${rel} noopener noreferrer`.trim());
        }
      }
    }
  }

  return container.innerHTML;
};

/**
 * 无法使用 DOMParser 时的降级清理（正则/替换方式，尽量拦截脚本与事件属性）。
 * @param {string} html marked 输出的 HTML 字符串
 * @returns {string} 清理后的 HTML 字符串
 */
const sanitizeHtmlFallback = (html) => {
  return String(html ?? '')
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
    .replace(/<iframe[\s\S]*?>[\s\S]*?<\/iframe>/gi, '')
    .replace(/<object[\s\S]*?>[\s\S]*?<\/object>/gi, '')
    .replace(/<embed[\s\S]*?>[\s\S]*?<\/embed>/gi, '')
    .replace(/<link[\s\S]*?>/gi, '')
    .replace(/<meta[\s\S]*?>/gi, '')
    .replace(/<base[\s\S]*?>/gi, '')
    .replace(/\son\w+\s*=\s*"[^"]*"/gi, '')
    .replace(/\son\w+\s*=\s*'[^']*'/gi, '')
    .replace(/\son\w+\s*=\s*[^\s>]+/gi, '')
    .replace(/\sstyle\s*=\s*"[^"]*"/gi, '')
    .replace(/\sstyle\s*=\s*'[^']*'/gi, '');
};

/**
 * 转义不成对的单波浪号，避免被 marked 当成 strikethrough。
 *
 * 标准 GFM 的删除线是 `~~xxx~~`，但 marked 17 等较新版本 GFM 实现里对 `~xxx~` 单波浪号也宽容
 * 识别成 del——后端文本里普通用作分隔符的单 `~` 会被误渲染成删除线（整段被划掉）。
 *
 * 用占位符护住 `~~`、把剩下的孤立 `~` 替成 `\~`（marked 会渲染成字面 `~`），还原 `~~`。
 *
 * @param {string} text
 * @returns {string}
 */
const escapeStrayTilde = (text) => {
  if (!text || !text.includes('~')) return text;
  const PLACEHOLDER = '\x00TILDE_PAIR\x00';
  return text
    .replace(/~~/g, PLACEHOLDER)
    .replace(/~/g, '\\~')
    .replace(new RegExp(PLACEHOLDER, 'g'), '~~');
};

/**
 * 把 Creation Markdown 渲染为”已清理”的 HTML（可直接用于 v-html）。
 * @param {string} content Markdown 文本
 * @returns {string} 安全 HTML 字符串
 */
export const renderCreationMarkdown = (content) => {
  const markdown = typeof content === 'string' ? content : '';
  if (!markdown) return '';

  const cached = markdownHtmlCache.get(markdown);
  if (cached != null) return cached;

  ensureMarkedConfigured();
  const rawHtml = marked.parse(escapeStrayTilde(markdown));

  // 首选 DOMPurify：更符合社区最佳实践（成熟 XSS 过滤器）
  const purify = getDomPurify();
  if (purify) {
    try {
      const sanitized = purify.sanitize(rawHtml, {
        ALLOWED_TAGS: Array.from(ALLOWED_TAGS),
        ALLOWED_ATTR: DOMPURIFY_ALLOWED_ATTR,
        FORBID_TAGS: DOMPURIFY_FORBID_TAGS,
        FORBID_ATTR: DOMPURIFY_FORBID_ATTR,
      });

      const annotated = annotateTableCells(sanitized);
      if (markdownHtmlCache.size >= MAX_MARKDOWN_CACHE_SIZE) {
        const firstKey = markdownHtmlCache.keys().next().value;
        if (firstKey != null) markdownHtmlCache.delete(firstKey);
      }
      markdownHtmlCache.set(markdown, annotated);
      return annotated;
    } catch (error) {
      // DOMPurify 失败时尽可能降级，避免页面空白
      console.warn('DOMPurify sanitize failed, fallback to DOMParser sanitizer:', error);
    }
  }

  // 次选：DOMParser allowlist 清理（仅浏览器环境可用）
  try {
    if (typeof window !== 'undefined' && typeof DOMParser !== 'undefined') {
      const sanitized = sanitizeHtmlWithDomParser(rawHtml);
      const annotated = annotateTableCells(sanitized);
      if (markdownHtmlCache.size >= MAX_MARKDOWN_CACHE_SIZE) {
        const firstKey = markdownHtmlCache.keys().next().value;
        if (firstKey != null) markdownHtmlCache.delete(firstKey);
      }
      markdownHtmlCache.set(markdown, annotated);
      return annotated;
    }
  } catch (error) {
    // 最终：正则降级，保证渲染可用性
    console.warn('Markdown sanitize failed, fallback to regex sanitizer:', error);
  }

  const sanitized = sanitizeHtmlFallback(rawHtml);
  const annotated = annotateTableCells(sanitized);
  if (markdownHtmlCache.size >= MAX_MARKDOWN_CACHE_SIZE) {
    const firstKey = markdownHtmlCache.keys().next().value;
    if (firstKey != null) markdownHtmlCache.delete(firstKey);
  }
  markdownHtmlCache.set(markdown, annotated);
  return annotated;
};
