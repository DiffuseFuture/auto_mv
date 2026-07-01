// 允许匹配后端真实资源标识（如 mv/202604/xxx.png）与 subject_1 这类 token
const MENTION_TOKEN_REGEXP = /@([A-Za-z0-9_./-]+)/g;
const MENTION_LABEL_REGEXP = /@图片(\d+)/g;

const getTokenCandidates = (subject, idFields = ['subjectId']) => {
  if (!subject || typeof subject !== 'object') return [];
  return idFields
    .map((field) => subject[field])
    .filter((value) => value !== undefined && value !== null && String(value) !== '')
    .map((value) => String(value));
};

export const replaceMentionValuesToLabels = (text, subjects = [], options = {}) => {
  if (!text) return '';
  const {
    idFields = ['subjectId'],
    fallbackPattern = /^subject[_-](\d+)$/i,
  } = options;
  const list = Array.isArray(subjects) ? subjects : [];

  return String(text).replace(MENTION_TOKEN_REGEXP, (match, token) => {
    const index = list.findIndex((subject) => getTokenCandidates(subject, idFields).includes(String(token)));
    if (index >= 0) return `@图片${index + 1}`;

    if (!(fallbackPattern instanceof RegExp)) return match;
    const fallback = String(token).match(fallbackPattern);
    if (!fallback) return match;
    const num = Number(fallback[1]);
    return Number.isFinite(num) && num > 0 ? `@图片${num}` : match;
  });
};

export const replaceMentionLabelsToValues = (text, subjects = [], options = {}) => {
  if (!text) return '';
  const {
    valueField = 'subjectId',
  } = options;
  const list = Array.isArray(subjects) ? subjects : [];

  return String(text).replace(MENTION_LABEL_REGEXP, (match, num) => {
    const index = Number(num) - 1;
    const token = list[index]?.[valueField];
    if (!token) return match;
    return `@${token}`;
  });
};
