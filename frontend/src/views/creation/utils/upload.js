/**
 * creation 页面专属上传能力：
 * - 文件类型白名单
 * - 上传 accept 常量
 * - 图片压缩
 * - 粘贴上传分发
 *
 * 注意：该模块仅服务 creation 视图，避免污染全局 utils。
 */

const IMAGE_UPLOAD_EXTENSIONS = ['.jpg', '.jpeg', '.png']; // 图片扩展名白名单
const AUDIO_UPLOAD_EXTENSIONS = ['.mp3', '.wav', '.aac', '.m4a', '.flac', '.aiff', '.ogg']; // 音频扩展名白名单
const IMAGE_UPLOAD_MIME_TYPES = ['image/jpeg', 'image/png']; // 图片 MIME 白名单
const AUDIO_UPLOAD_MIME_TYPES = [
  // MP3
  'audio/mpeg', 'audio/mp3',
  // WAV
  'audio/wav', 'audio/x-wav', 'audio/wave', 'audio/vnd.wave',
  // AAC
  'audio/aac', 'audio/x-aac',
  // M4A
  'audio/mp4', 'audio/x-m4a', 'audio/m4a',
  // FLAC
  'audio/flac', 'audio/x-flac',
  // AIFF
  'audio/aiff', 'audio/x-aiff',
  // OGG
  'audio/ogg', 'audio/vorbis', 'application/ogg',
];

/**
 * 判断文件名是否命中支持的扩展名。
 * @param {string} [fileName=''] 文件名
 * @param {string[]} [extensions=[]] 扩展名集合（含点）
 * @returns {boolean}
 */
const hasSupportedExtension = (fileName = '', extensions = []) => {
  const normalizedName = fileName.toLowerCase(); // 统一小写，避免大小写差异
  return extensions.some(extension => normalizedName.endsWith(extension));
};

export const IMAGE_UPLOAD_ACCEPT = '.jpg,.jpeg,.png,image/jpeg,image/png'; // 图片 accept
export const AUDIO_UPLOAD_ACCEPT = '.mp3,.wav,.aac,.m4a,.flac,.aiff,.ogg,audio/mpeg,audio/mp3,audio/wav,audio/x-wav,audio/wave,audio/vnd.wave,audio/aac,audio/x-aac,audio/mp4,audio/x-m4a,audio/m4a,audio/flac,audio/x-flac,audio/aiff,audio/x-aiff,audio/ogg,audio/vorbis,application/ogg'; // 音频 accept
export const CREATION_UPLOAD_ACCEPT = `${AUDIO_UPLOAD_ACCEPT},${IMAGE_UPLOAD_ACCEPT}`; // creation 综合 accept

/**
 * 判断文件是否为支持上传的图片。
 * @param {File | null | undefined} file 上传文件
 * @returns {boolean}
 */
export const isSupportedImageUpload = (file) => {
  if (!file) return false;
  const fileType = (file.type || '').toLowerCase(); // 统一处理 MIME 大小写
  return IMAGE_UPLOAD_MIME_TYPES.includes(fileType) || hasSupportedExtension(file.name, IMAGE_UPLOAD_EXTENSIONS);
};

/**
 * 判断文件是否为支持上传的音频。
 * @param {File | null | undefined} file 上传文件
 * @returns {boolean}
 */
export const isSupportedAudioUpload = (file) => {
  if (!file) return false;
  const fileType = (file.type || '').toLowerCase(); // 统一处理 MIME 大小写
  return AUDIO_UPLOAD_MIME_TYPES.includes(fileType) || hasSupportedExtension(file.name, AUDIO_UPLOAD_EXTENSIONS);
};

/**
 * 上传前压缩图片。
 * 默认策略：最长边限制 1600px，JPEG 质量 0.8；若压缩后更大则回退原图。
 * @param {File} file 原始图片文件
 * @param {{ maxWidth?: number, maxHeight?: number, quality?: number, minSizeKB?: number }} [options] 压缩参数
 * @returns {Promise<File>}
 */
export const compressImageBeforeUpload = async (file, options = {}) => {
  if (!file || typeof window === 'undefined' || typeof document === 'undefined') return file;

  const {
    maxWidth = 1600,
    maxHeight = 1600,
    quality = 0.8,
    minSizeKB = 300,
  } = options;

  const fileType = (file.type || '').toLowerCase();
  if (!fileType.startsWith('image/')) return file;

  const shouldCompress = file.size > minSizeKB * 1024 || fileType === 'image/png';
  if (!shouldCompress) return file;

  const objectUrl = URL.createObjectURL(file);

  try {
    const image = await new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = reject;
      img.src = objectUrl;
    });

    const scale = Math.min(1, maxWidth / image.width, maxHeight / image.height);
    const targetWidth = Math.max(1, Math.round(image.width * scale));
    const targetHeight = Math.max(1, Math.round(image.height * scale));

    if (scale === 1 && file.size <= minSizeKB * 1024 && fileType !== 'image/png') {
      return file;
    }

    const canvas = document.createElement('canvas');
    canvas.width = targetWidth;
    canvas.height = targetHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) return file;

    ctx.drawImage(image, 0, 0, targetWidth, targetHeight);

    const outputType = fileType === 'image/png' ? 'image/png' : 'image/jpeg';
    const blob = await new Promise(resolve => canvas.toBlob(resolve, outputType, quality));
    if (!blob || blob.size >= file.size) return file;

    return new File([blob], file.name, {
      type: outputType,
      lastModified: Date.now(),
    });
  } catch {
    return file;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
};

/**
 * 对一批文件按类型分流处理（图片截量、音频取首个）。
 * @param {File[]} files
 * @param {{ maxImages: number, currentImagesCount: number, onImageFile: Function, onAudioFile: Function, onOverLimit?: Function, onMultipleAudio?: Function }} options
 * @returns {Promise<boolean>}
 */
export const processUploadFiles = async (files, options) => {
  const { maxImages, currentImagesCount, onImageFile, onAudioFile, onOverLimit, onMultipleAudio } = options || {};
  if (!files.length) return false;

  const imageFiles = files.filter(isSupportedImageUpload);
  const audioFiles = files.filter(isSupportedAudioUpload);

  const remainingSlots = Math.max(0, (maxImages || 0) - (currentImagesCount || 0));
  const imageQueue = remainingSlots > 0 ? imageFiles.slice(0, remainingSlots) : [];

  if (imageFiles.length > remainingSlots) onOverLimit?.(maxImages);
  for (const file of imageQueue) await onImageFile?.(file);

  if (audioFiles.length > 1) onMultipleAudio?.();
  if (audioFiles[0]) await onAudioFile?.(audioFiles[0]);

  return true;
};

/**
 * 从拖拽事件中提取文件并分流处理。
 * @param {DragEvent} event
 * @param {Parameters<typeof processUploadFiles>[1]} options
 */
export const handleDropUploadFiles = (event, options) =>
  processUploadFiles(Array.from(event.dataTransfer?.files || []), options);

/**
 * 从粘贴事件中提取文件并分流处理。
 * @param {ClipboardEvent} event
 * @param {Parameters<typeof processUploadFiles>[1]} options
 */
export const handlePasteUploadFiles = async (event, options) => {
  const files = Array.from(event.clipboardData?.items || [])
    .filter(item => item.kind === 'file')
    .map(item => item.getAsFile())
    .filter(Boolean);
  if (!files.length) return false;
  event.preventDefault();
  return processUploadFiles(files, options);
};
