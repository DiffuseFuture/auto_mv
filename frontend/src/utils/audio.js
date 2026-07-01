import { FFmpeg } from '@ffmpeg/ffmpeg';
import { fetchFile } from '@ffmpeg/util';

let ffmpeg = null;
const AUDIO_SIGNATURE_READ_BYTES = 64;
const UNSUPPORTED_AUDIO_FORMAT_ERROR = '无法识别音频真实格式，仅支持 MP3/WAV/AAC/M4A/FLAC/AIFF/OGG，请检查文件是否被改后缀或文件已损坏';

const AUDIO_FORMATS = {
  mp3: {
    extension: 'mp3',
    mimeType: 'audio/mpeg',
    acceptedMimeTypes: ['audio/mpeg', 'audio/mp3'],
  },
  wav: {
    extension: 'wav',
    mimeType: 'audio/wav',
    acceptedMimeTypes: ['audio/wav', 'audio/x-wav', 'audio/wave', 'audio/vnd.wave'],
  },
  aac: {
    extension: 'aac',
    mimeType: 'audio/aac',
    acceptedMimeTypes: ['audio/aac', 'audio/x-aac'],
  },
  m4a: {
    extension: 'm4a',
    mimeType: 'audio/mp4',
    acceptedMimeTypes: ['audio/mp4', 'audio/x-m4a', 'audio/m4a'],
  },
  flac: {
    extension: 'flac',
    mimeType: 'audio/flac',
    acceptedMimeTypes: ['audio/flac', 'audio/x-flac'],
  },
  aiff: {
    extension: 'aiff',
    mimeType: 'audio/aiff',
    acceptedMimeTypes: ['audio/aiff', 'audio/x-aiff'],
  },
  ogg: {
    extension: 'ogg',
    mimeType: 'audio/ogg',
    acceptedMimeTypes: ['audio/ogg', 'audio/vorbis', 'application/ogg'],
  },
};

const getFormatMetaByKind = (kind) => {
  const format = AUDIO_FORMATS[kind];
  if (!format) return null;

  return {
    extension: format.extension,
    mimeType: format.mimeType,
  };
};

const replaceFileExtension = (fileName = '', extension) => {
  const safeName = fileName || `audio_${Date.now()}`;
  const baseName = safeName.replace(/\.[^/.]+$/, '');
  return `${baseName}.${extension}`;
};

const matchesAsciiText = (bytes, offset, text) => {
  if (offset + text.length > bytes.length) return false;
  return text.split('').every((char, index) => bytes[offset + index] === char.charCodeAt(0));
};

const isValidMp3FrameHeader = (bytes, offset) => {
  if (offset + 3 >= bytes.length) return false;
  if (bytes[offset] !== 0xff || (bytes[offset + 1] & 0xe0) !== 0xe0) return false;

  const versionBits = (bytes[offset + 1] >> 3) & 0x03;
  const layerBits = (bytes[offset + 1] >> 1) & 0x03;
  const bitrateBits = (bytes[offset + 2] >> 4) & 0x0f;
  const sampleRateBits = (bytes[offset + 2] >> 2) & 0x03;

  if (versionBits === 0x01) return false;
  if (layerBits === 0x00) return false;
  if (bitrateBits === 0x0f) return false;
  if (sampleRateBits === 0x03) return false;

  return true;
};

const detectAudioFormatFromSignature = (bytes) => {
  // WAV: RIFF header
  if (bytes.length >= 12 && matchesAsciiText(bytes, 0, 'RIFF') && matchesAsciiText(bytes, 8, 'WAVE')) {
    return 'wav';
  }

  // MP3: ID3 tag
  if (bytes.length >= 3 && matchesAsciiText(bytes, 0, 'ID3')) {
    return 'mp3';
  }

  // AAC: ADTS header (0xFFF sync word) - 必须在 MP3 帧头检测之前
  if (bytes.length >= 7 && bytes[0] === 0xFF && (bytes[1] & 0xF6) === 0xF0) {
    return 'aac';
  }

  // MP3: frame header scan - 在 AAC 检测之后，避免误判
  for (let index = 0; index <= bytes.length - 4; index += 1) {
    if (isValidMp3FrameHeader(bytes, index)) {
      return 'mp3';
    }
  }

  // FLAC: fLaT marker
  if (bytes.length >= 4 && matchesAsciiText(bytes, 0, 'fLaC')) {
    return 'flac';
  }

  // AIFF: FORM + AIFF
  if (bytes.length >= 12 && matchesAsciiText(bytes, 0, 'FORM') && matchesAsciiText(bytes, 8, 'AIFF')) {
    return 'aiff';
  }

  // OGG: OggS marker
  if (bytes.length >= 4 && matchesAsciiText(bytes, 0, 'OggS')) {
    return 'ogg';
  }

  // M4A/AAC: ftyp box (MP4 container)
  if (bytes.length >= 8) {
    const ftypIndex = 4;
    if (matchesAsciiText(bytes, ftypIndex, 'ftyp') || 
        matchesAsciiText(bytes, ftypIndex, 'M4A ') ||
        matchesAsciiText(bytes, ftypIndex, 'mp42') ||
        matchesAsciiText(bytes, ftypIndex, 'isom')) {
      return 'm4a';
    }
  }

  return null;
};

const readFileHeader = async (file, byteLength = AUDIO_SIGNATURE_READ_BYTES) => {
  const chunk = file.slice(0, byteLength);
  const buffer = await chunk.arrayBuffer();
  return new Uint8Array(buffer);
};

/**
 * 通过文件头识别真实音频格式，不信任扩展名和 MIME
 */
async function detectAudioFormatMeta(file) {
  const headerBytes = await readFileHeader(file);
  const detectedKind = detectAudioFormatFromSignature(headerBytes);
  return detectedKind ? getFormatMetaByKind(detectedKind) : null;
}

/**
 * 严格校验真实音频格式，识别不到时直接拒绝
 */
export async function assertSupportedAudioFormat(file) {
  try {
    const formatMeta = await detectAudioFormatMeta(file);
    if (formatMeta) return formatMeta;
  } catch {}

  throw new Error(UNSUPPORTED_AUDIO_FORMAT_ERROR);
}

/**
 * 尝试通过 FFmpeg 检测音频格式（兜底方案）
 */
const tryDetectFormatByFFmpeg = async (file) => {
  let instance;
  try {
    instance = await loadFFmpeg();
  } catch {
    return null;
  }

  const taskId = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const inputName = `probe_${taskId}`;
  
  try {
    const fileData = await fetchFile(file);
    await instance.writeFile(inputName, fileData);
    
    // 使用 ffprobe 探测格式
    await instance.exec(['-i', inputName, '-f', 'null', '-']);
    
    // 如果能成功执行，说明 FFmpeg 能识别该格式
    // 默认返回 mp3 作为安全格式
    return getFormatMetaByKind('mp3');
  } catch {
    return null;
  } finally {
    await deleteFileIfExists(instance, inputName);
  }
};

/**
 * 规范化音频文件名和 MIME，确保后续处理与真实格式一致
 */
export async function normalizeAudioFile(file) {
  let formatMeta;
  
  // 首先尝试通过文件头检测
  try {
    formatMeta = await assertSupportedAudioFormat(file);
  } catch {
    // 文件头检测失败，尝试使用 FFmpeg 兜底检测
    formatMeta = await tryDetectFormatByFFmpeg(file);
    
    // 如果 FFmpeg 也无法检测，直接拒绝
    if (!formatMeta) {
      throw new Error(UNSUPPORTED_AUDIO_FORMAT_ERROR);
    }
  }
  
  const normalizedName = replaceFileExtension(file?.name, formatMeta.extension);
  const currentType = (file?.type || '').toLowerCase();
  const allowedMimeTypes = AUDIO_FORMATS[formatMeta.extension]?.acceptedMimeTypes || [];
  const hasCorrectExtension = (file?.name || '').toLowerCase().endsWith(`.${formatMeta.extension}`);
  const hasCompatibleMimeType = !currentType || allowedMimeTypes.includes(currentType);
  const corrected = !hasCorrectExtension || !hasCompatibleMimeType;

  if (!corrected || !(file instanceof File)) {
    return { file, formatMeta, corrected };
  }

  const normalizedFile = new File([file], normalizedName, {
    type: formatMeta.mimeType,
    lastModified: file.lastModified,
  });

  return {
    file: normalizedFile,
    formatMeta,
    corrected: true,
  };
}

// FFmpeg 虚拟文件系统中删除失败不影响主流程，静默清理
const deleteFileIfExists = async (instance, fileName) => {
  try {
    await instance.deleteFile(fileName);
  } catch {}
};

/**
 * 加载 ffmpeg 核心（完全本地资源，不走 CDN）
 */
async function loadFFmpeg() {
  if (ffmpeg) return ffmpeg;

  ffmpeg = new FFmpeg();

  try {
    const baseURL = `${window.location.origin}/ffmpeg`;
    await ffmpeg.load({
      coreURL: `${baseURL}/ffmpeg-core.js`,
      wasmURL: `${baseURL}/ffmpeg-core.wasm`,
    });
    return ffmpeg;
  } catch (error) {
    ffmpeg = null;
    throw error;
  }
}

/**
 * 裁剪音频文件并转换为 MP3 格式
 */
export async function trimAudio(file, start, duration) {
  const { extension } = await assertSupportedAudioFormat(file);
  const taskId = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const inputName = `input_${taskId}.${extension}`;
  const outputName = `output_${taskId}.mp3`;

  let instance;
  try {
    instance = await loadFFmpeg();
  } catch (err) {
    throw new Error('音频处理核心加载失败，请检查网络连接');
  }

  let data;
  try {
    const fileData = await fetchFile(file);
    await instance.writeFile(inputName, fileData);

    // 转换为 MP3 格式并使用 VBR 质量 2(约 190kbps)
    await instance.exec([
      '-ss', start.toFixed(3),
      '-t', duration.toFixed(3),
      '-i', inputName,
      '-c:a', 'libmp3lame',
      '-q:a', '2',
      '-ar', '44100',
      '-b:a', '192k',
      outputName
    ]);
    data = await instance.readFile(outputName);
  } finally {
    await Promise.allSettled([
      deleteFileIfExists(instance, inputName),
      deleteFileIfExists(instance, outputName),
    ]);
  }

  return new Blob([data], { type: 'audio/mpeg' });
}
