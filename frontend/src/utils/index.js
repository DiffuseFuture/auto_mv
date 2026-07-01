/**
 * 判断值是否为空
 * @param {*} value - 要判断的值
 * @returns {boolean} 为空返回 true，否则返回 false
 */
export const isEmpty = (value) => {
  if (value === null || value === undefined) {
    return true
  }
  if (typeof value === 'string' && value.trim() === '') {
    return true
  }
  if (Array.isArray(value) && value.length === 0) {
    return true
  }
  if (typeof value === 'object' && Object.keys(value).length === 0) {
    return true
  }
  return false
}

/**
 * 判断值是否不为空
 * @param {*} value - 要判断的值
 * @returns {boolean} 不为空返回 true，否则返回 false
 */
export const isNotEmpty = (value) => {
  return !isEmpty(value)
}

/**
 * 移动端浏览器判定：优先识别触控移动设备特征，不依赖页面宽度。
 * 兼顾平板、折叠屏等大屏移动设备，同时避免把桌面端触控设备误判为移动端。
 * @returns {boolean}
 */
export const isMobileClient = () => {
  if (typeof window === 'undefined') return false

  const ua = window.navigator.userAgent || ''
  const hasMobileUa = /Android|iPhone|iPod|iPad|webOS|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua)
  const hasTouchPoints = window.navigator.maxTouchPoints > 0
  const canHover = window.matchMedia('(hover: hover)').matches
  const finePointer = window.matchMedia('(pointer: fine)').matches

  return hasMobileUa || (hasTouchPoints && !canHover && !finePointer)
}

/**
 * 时间格式化
 * @param {Date|number|string} date - 日期对象或时间戳
 * @param {string} format - 格式字符串，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的时间字符串
 */
export const formatDate = (date, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!date) return ''

  const d = new Date(date)
  if (isNaN(d.getTime())) return ''

  const pad = (num) => String(num).padStart(2, '0')

  const year = d.getFullYear()
  const month = pad(d.getMonth() + 1)
  const day = pad(d.getDate())
  const hours = pad(d.getHours())
  const minutes = pad(d.getMinutes())
  const seconds = pad(d.getSeconds())
  const milliseconds = String(d.getMilliseconds()).padStart(3, '0')

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
    .replace('SSS', milliseconds)
}

/**
 * 常见的时间格式化方法
 */
export const dateUtils = {
  // 格式化为日期 YYYY-MM-DD
  formatDay: (date) => formatDate(date, 'YYYY-MM-DD'),
  
  // 格式化为时间 HH:mm:ss
  formatTime: (date) => formatDate(date, 'HH:mm:ss'),
  
  // 格式化为完整日期时间 YYYY-MM-DD HH:mm:ss
  formatDateTime: (date) => formatDate(date, 'YYYY-MM-DD HH:mm:ss'),
  
  // 获取当前时间戳
  getNow: () => Date.now(),
  
  // 判断是否为今天
  isToday: (date) => {
    const today = new Date()
    const d = new Date(date)
    return d.toDateString() === today.toDateString()
  },
  
  // 获取距离现在的时间描述 (如：刚刚、1分钟前、1小时前等)
  getRelativeTime: (date) => {
    const d = new Date(date)
    const now = new Date()
    const diff = now - d
    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (seconds < 60) {
      return '刚刚'
    } else if (minutes < 60) {
      return `${minutes}分钟前`
    } else if (hours < 24) {
      return `${hours}小时前`
    } else if (days < 30) {
      return `${days}天前`
    } else {
      return formatDate(date, 'YYYY-MM-DD')
    }
  }
}

/**
 * 格式化音频/视频时长（秒 → mm:ss）
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时长字符串 (如：01:23)
 */
export const formatDuration = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/**
 * 向后兼容导出：历史页面仍可能从 `@/utils/index.js` 读取上传能力。
 * 实际实现已迁移到 creation 内部模块。
 */
export {
  IMAGE_UPLOAD_ACCEPT,
  AUDIO_UPLOAD_ACCEPT,
  CREATION_UPLOAD_ACCEPT,
  isSupportedImageUpload,
  isSupportedAudioUpload,
  compressImageBeforeUpload,
  handlePasteUploadFiles,
  handleDropUploadFiles,
  processUploadFiles,
} from '@/views/creation/utils/upload.js'

