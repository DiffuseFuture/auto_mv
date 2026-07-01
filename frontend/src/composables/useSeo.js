/**
 * SEO Composable - Dynamic meta tag management for Vue SPA
 *
 * Provides functions to update document title, meta tags, Open Graph tags,
 * Twitter Card tags, canonical URL, and JSON-LD structured data dynamically
 * as users navigate between routes.
 */

const SITE_NAME = 'OhYesAI';
const DEFAULT_TITLE = 'OhYesAI - AI音乐可视化创作平台 | Visualize Your Sound';
const DEFAULT_DESCRIPTION = 'OhYesAI是一个AI驱动的音乐可视化创作平台，让每一个声音都能找到属于它的画面。上传音乐，一键生成MV视频，支持AI作曲、分镜编辑和智能视频创作。';
const BASE_URL = 'https://www.ohyesai.com';
const DEFAULT_IMAGE = `${BASE_URL}/logo.png`;

/**
 * Set or update a <meta> tag by name or property attribute.
 * @param {string} attr - 'name' or 'property'
 * @param {string} key - The attribute value (e.g., 'description', 'og:title')
 * @param {string} content - The content value
 */
function setMeta(attr, key, content) {
  let el = document.querySelector(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(attr, key);
    document.head.appendChild(el);
  }
  el.setAttribute('content', content);
}

/**
 * Set or update the canonical link element.
 * @param {string} url - The canonical URL
 */
function setCanonical(url) {
  let el = document.querySelector('link[rel="canonical"]');
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', 'canonical');
    document.head.appendChild(el);
  }
  el.setAttribute('href', url);
}

/**
 * Set or update <link rel="alternate" hreflang="..."> for multi-language pages.
 * 帮助搜索引擎识别同内容的不同语言版本，避免被判重复内容。
 * @param {Record<string, string>} map e.g. { 'zh-CN': '/zh/guide', 'en-US': '/en/guide', 'x-default': '/zh/guide' }
 */
function setAlternateLinks(map) {
  // 先清掉旧的 alternate
  document.querySelectorAll('link[rel="alternate"][hreflang]').forEach((el) => el.remove());
  for (const [lang, path] of Object.entries(map)) {
    const el = document.createElement('link');
    el.setAttribute('rel', 'alternate');
    el.setAttribute('hreflang', lang);
    el.setAttribute('href', path.startsWith('http') ? path : `${BASE_URL}${path}`);
    document.head.appendChild(el);
  }
}

/**
 * Set or update JSON-LD structured data.
 * @param {string} id - Unique identifier for the script tag
 * @param {object} data - The structured data object
 */
function setJsonLd(id, data) {
  const scriptId = `jsonld-${id}`;
  let el = document.getElementById(scriptId);
  if (!el) {
    el = document.createElement('script');
    el.id = scriptId;
    el.type = 'application/ld+json';
    document.head.appendChild(el);
  }
  el.textContent = JSON.stringify(data);
}

/**
 * Remove a JSON-LD script by id.
 * @param {string} id - Unique identifier for the script tag
 */
function removeJsonLd(id) {
  const el = document.getElementById(`jsonld-${id}`);
  if (el) el.remove();
}

/**
 * Update all SEO-related meta tags for the current page.
 * @param {object} options
 * @param {string} [options.title] - Page title (will be appended with site name)
 * @param {string} [options.description] - Page description
 * @param {string} [options.keywords] - Comma-separated keywords
 * @param {string} [options.url] - Canonical URL path (e.g., '/mv')
 * @param {string} [options.image] - OG image URL
 * @param {string} [options.type] - OG type (default: 'website')
 * @param {object} [options.jsonLd] - JSON-LD structured data with { id, data }
 */
export function useSeo(options = {}) {
  const title = options.title
    ? `${options.title} - ${SITE_NAME}`
    : DEFAULT_TITLE;
  const description = options.description || DEFAULT_DESCRIPTION;
  const url = options.url ? `${BASE_URL}${options.url}` : BASE_URL;
  const image = options.image || DEFAULT_IMAGE;
  const type = options.type || 'website';

  // Document title
  document.title = title;

  // Standard meta tags
  setMeta('name', 'title', title);
  setMeta('name', 'description', description);
  if (options.keywords) {
    setMeta('name', 'keywords', options.keywords);
  }

  // Open Graph
  setMeta('property', 'og:type', type);
  setMeta('property', 'og:url', url);
  setMeta('property', 'og:title', title);
  setMeta('property', 'og:description', description);
  setMeta('property', 'og:image', image);

  // Twitter Card
  setMeta('name', 'twitter:url', url);
  setMeta('name', 'twitter:title', title);
  setMeta('name', 'twitter:description', description);
  setMeta('name', 'twitter:image', image);

  // Canonical URL
  setCanonical(url);

  // hreflang 替代语言版本（多语言页面用）
  if (options.alternates) {
    setAlternateLinks(options.alternates);
  }

  // JSON-LD
  if (options.jsonLd) {
    setJsonLd(options.jsonLd.id, options.jsonLd.data);
  }
}

/**
 * Set SEO meta for the share page with dynamic content.
 * @param {object} shareInfo - The share data object
 */
export function useShareSeo(shareInfo) {
  if (!shareInfo) return;

  const langPrefix = (() => {
    if (typeof window === 'undefined') return '';
    const p = String(window.location.pathname || '');
    if (p.startsWith('/en/')) return '/en';
    if (p.startsWith('/zh/')) return '/zh';
    return '';
  })();

  const isVideo = shareInfo.type === 'VIDEO';
  const title = shareInfo.projectName
    ? `${shareInfo.projectName} - OhYesAI创作分享`
    : 'OhYesAI创作分享';
  const description = shareInfo.prompt
    ? shareInfo.prompt.substring(0, 160)
    : `在OhYesAI上查看这个${isVideo ? 'MV视频' : '音乐'}创作`;

  useSeo({
    title,
    description,
    url: `${langPrefix}/share?shareId=${shareInfo.shareId || ''}`,
    image: shareInfo.coverUrl || DEFAULT_IMAGE,
    type: isVideo ? 'video.other' : 'music.song',
    jsonLd: {
      id: 'share-content',
      data: isVideo
        ? {
            '@context': 'https://schema.org',
            '@type': 'VideoObject',
            name: shareInfo.projectName || 'OhYesAI MV',
            description: description,
            thumbnailUrl: shareInfo.coverUrl || '',
            contentUrl: shareInfo.fileUrl || '',
            uploadDate: new Date().toISOString(),
            publisher: {
              '@type': 'Organization',
              name: 'OhYesAI',
              url: BASE_URL,
            },
          }
        : {
            '@context': 'https://schema.org',
            '@type': 'MusicRecording',
            name: shareInfo.projectName || 'OhYesAI Music',
            description: description,
            url: `${BASE_URL}${langPrefix}/share?shareId=${shareInfo.shareId || ''}`,
          },
    },
  });
}

export { removeJsonLd, SITE_NAME, DEFAULT_TITLE, DEFAULT_DESCRIPTION, BASE_URL };
