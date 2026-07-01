import { ref } from 'vue';
import { ElLoading, ElMessage } from 'element-plus';
import { uploadFile } from '@/api/creation';
import { normalizeAudioFile, trimAudio } from '@/utils/audio';

const MIN_UPLOAD_AUDIO_SECONDS = 10;
const MAX_UPLOAD_AUDIO_SECONDS = 6 * 60;
const GET_DURATION_TIMEOUT = 8000;

/**
 * 通过 HTMLAudioElement 读取音频时长
 */
const getDurationByAudioTag = (file) => {
  return new Promise((resolve, reject) => {
    const audio = new Audio();
    const url = URL.createObjectURL(file);
    let timeoutId;
    let isResolved = false;

    const cleanup = () => {
      clearTimeout(timeoutId);
      URL.revokeObjectURL(url);
      audio.src = '';
      audio.removeEventListener('loadedmetadata', onLoadedMetadata);
      audio.removeEventListener('canplay', onCanPlay);
      audio.removeEventListener('error', onError);
    };

    const resolveOnce = (value) => {
      if (isResolved) return;
      isResolved = true;
      cleanup();
      resolve(value);
    };

    const rejectOnce = (error) => {
      if (isResolved) return;
      isResolved = true;
      cleanup();
      reject(error);
    };

    const onLoadedMetadata = () => {
      console.log('[AudioTag] loadedmetadata, duration:', audio.duration);
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        resolveOnce(audio.duration);
        return;
      }
      // Safari/部分 VBR MP3 可能先给 Infinity，seek 到极大值后拿真实时长
      if (audio.duration === Infinity) {
        try {
          audio.currentTime = Number.MAX_SAFE_INTEGER;
        } catch {
          rejectOnce(new Error('无法读取音频文件'));
        }
        return;
      }
    };

    const onCanPlay = () => {
      console.log('[AudioTag] canplay, duration:', audio.duration);
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        resolveOnce(audio.duration);
      }
    };

    const onError = (e) => {
      console.error('[AudioTag] error event:', e);
      rejectOnce(new Error('无法读取音频文件'));
    };

    audio.addEventListener('loadedmetadata', onLoadedMetadata);
    audio.addEventListener('canplay', onCanPlay);
    audio.addEventListener('error', onError);

    timeoutId = setTimeout(() => {
      console.warn('[AudioTag] timeout, duration:', audio.duration);
      // 超时后如果拿到了时长，也接受
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        resolveOnce(audio.duration);
      } else {
        rejectOnce(new Error('读取音频超时'));
      }
    }, GET_DURATION_TIMEOUT);

    audio.src = url;
  });
};

/**
 * 通过 WebAudio 解码读取时长（兜底方案）。
 * @param {File} file
 * @returns {Promise<number>}
 */
const getDurationByWebAudio = async (file) => {
  console.log('[WebAudio] 开始解码音频文件...');
  const arrayBuffer = await file.arrayBuffer();
  const AudioCtx = window.AudioContext || window.webkitAudioContext;
  if (!AudioCtx) throw new Error('当前浏览器不支持 AudioContext');

  const ctx = new AudioCtx();
  try {
    console.log('[WebAudio] 音频数据大小:', arrayBuffer.byteLength, 'bytes');
    const decoded = await ctx.decodeAudioData(arrayBuffer.slice(0));
    const duration = Number(decoded.duration);
    console.log('[WebAudio] 解码成功，时长:', duration, '秒');
    if (Number.isFinite(duration) && duration > 0) return duration;
    throw new Error('解码后的时长无效');
  } catch (decodeError) {
    console.error('[WebAudio] 解码失败:', decodeError);
    throw new Error('无法解码音频文件');
  } finally {
    try {
      if (ctx.state !== 'closed') await ctx.close();
    } catch (_) {
    }
  }
};

/**
 * 获取音频文件时长（优先 audio 标签，失败后 WebAudio 兜底）
 */
const getAudioDuration = async (file) => {
  try {
    return await getDurationByAudioTag(file);
  } catch {
    try {
      return await getDurationByWebAudio(file);
    } catch {
      throw new Error('无法读取音频文件，请检查文件是否损坏');
    }
  }
};

/**
 * 音频裁剪上传 Composable
 */
export function useAudioTrimUpload() {
  const showTrimmer = ref(false);
  const rawFile = ref(null);
  const audioDuration = ref(0);
  const audioFormatMeta = ref(null);
  const isPreparingAudio = ref(false);
  const isUploadingTrimmedAudio = ref(false);

  /**
   * 根据裁剪后的 Blob 生成上传用的 MP3 文件。
   * trimAudio 返回的 blob 已经是 MP3 格式，这里统一输出为.mp3
   */
  const createTrimmedUploadFile = (blob, timeRange, sourceFile = rawFile.value) => {
    if (!sourceFile) {
      throw new Error('缺少音频源文件');
    }

    const base = sourceFile.name.replace(/\.[^/.]+$/, '');
    const fileName = `${base}_trimmed_${timeRange}.mp3`; // 始终使用 .mp3 后缀

    return new File([blob], fileName, {
      type: 'audio/mpeg', // 始终使用 MP3 的 MIME 类型
    });
  };

  /**
   * 打开文件并校验时长，然后转换为 MP3 格式
   */
  const openWithFile = async (file) => {
    showTrimmer.value = true;
    isPreparingAudio.value = true;
    rawFile.value = null;
    audioDuration.value = 0;

    try {
      const { file: normalizedFile, formatMeta } = await normalizeAudioFile(file);

      const isAlreadyMP3 = formatMeta.extension === 'mp3';
      let duration;

      // 非 MP3 音频：先转码再读取时长（解决 AIFF/FLAC 等格式无法直接读取时长问题）
      if (!isAlreadyMP3) {
        try {
          // 先用超大时长让 FFmpeg 处理完整文件
          const tempMp3Blob = await trimAudio(normalizedFile, 0, 9999);
          const baseName = normalizedFile.name.replace(/\.[^/.]+$/, '');
          const tempMp3File = new File([tempMp3Blob], `${baseName}.mp3`, {
            type: 'audio/mpeg',
          });
          
          console.log('[音频上传] 尝试从转码后的 MP3 读取时长...');
          // 从转码后的 MP3 读取真实时长
          duration = await getAudioDuration(tempMp3File);
          console.log('[音频上传] 从 MP3 读取的时长:', duration);

          // 如果读取的时长仍然无效，使用 FFmpeg 强制读取
          if (!Number.isFinite(duration) || duration <= 0) {
            console.warn('[音频上传] MP3 读取时长失败，使用 FFmpeg 强制读取...');
            // 创建一个 5 分钟的短音频来测试真实时长
            const shortMp3Blob = await trimAudio(normalizedFile, 0, 300);
            const shortMp3File = new File([shortMp3Blob], `${baseName}_short.mp3`, {
              type: 'audio/mpeg',
            });
            duration = await getAudioDuration(shortMp3File);
            console.log('[音频上传] FFmpeg 强制读取的时长:', duration);
          }

          // 校验时长
          if (!Number.isFinite(duration) || duration < MIN_UPLOAD_AUDIO_SECONDS) {
            console.error('[音频上传] 时长校验失败，实际时长:', duration);
            ElMessage.error(`音频时长需大于等于${MIN_UPLOAD_AUDIO_SECONDS}秒`);
            showTrimmer.value = false;
            return false;
          }
          if (duration > MAX_UPLOAD_AUDIO_SECONDS) {
            ElMessage.error('音频时长不能超过6分钟');
            showTrimmer.value = false;
            return false;
          }

          // 重新转码正确时长的版本
          const finalMp3Blob = await trimAudio(normalizedFile, 0, duration);

          rawFile.value = new File([finalMp3Blob], `${baseName}.mp3`, {
            type: 'audio/mpeg',
          });
        } catch (convertError) {
          console.error('[音频上传] 转码失败:', convertError);
          throw new Error('音频格式转换失败，请检查文件是否损坏');
        }
      } else {
        // MP3 音频：直接读取时长并使用
        console.log('[音频上传] MP3 文件，直接读取时长...');
        duration = await getAudioDuration(normalizedFile);
        console.log('[音频上传] MP3 读取的时长:', duration);

        // 校验时长
        if (!Number.isFinite(duration) || duration < MIN_UPLOAD_AUDIO_SECONDS) {
          console.error('[音频上传] MP3 时长校验失败，实际时长:', duration);
          ElMessage.error(`音频时长需大于等于${MIN_UPLOAD_AUDIO_SECONDS}秒`);
          showTrimmer.value = false;
          return false;
        }
        if (duration > MAX_UPLOAD_AUDIO_SECONDS) {
          ElMessage.error('音频时长不能超过6分钟');
          showTrimmer.value = false;
          return false;
        }

        rawFile.value = normalizedFile;
      }

      audioFormatMeta.value = { extension: 'mp3', mimeType: 'audio/mpeg' };
      audioDuration.value = duration;
      return true;
    } catch (error) {
      console.error('[音频上传] 处理失败:', error);
      ElMessage.error(error?.message || '无法读取音频文件');
      showTrimmer.value = false;
      return false;
    } finally {
      isPreparingAudio.value = false;
    }
  };

  /**
   * 上传裁剪后的音频
   */
  const uploadTrimmedBlob = async ({ blob, timeRange }, options = {}) => {
    const { silent = false, showSuccess = false, sourceFile = rawFile.value } = options;
    isUploadingTrimmedAudio.value = true;
    const loading = silent
      ? null
      : ElLoading.service({
        lock: true,
        text: '正在上传...',
        background: 'rgba(0, 0, 0, 0.7)',
      });

    try {
      const file = createTrimmedUploadFile(blob, timeRange, sourceFile);
      const formData = new FormData();
      formData.append('file', file);
      const data = await uploadFile(formData);

      if (showSuccess) ElMessage.success('上传成功');
      return { fileId: data.fileId, fileName: file.name, fileUrl: data.fileUrl };
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || '上传失败');
      throw error;
    } finally {
      loading?.close();
      isUploadingTrimmedAudio.value = false;
    }
  };

  /**
   * 清理上传裁剪器状态
   */
  const clearUploadState = () => {
    showTrimmer.value = false;
    rawFile.value = null;
    audioDuration.value = 0;
    audioFormatMeta.value = null;
    isPreparingAudio.value = false;
  };

  return { showTrimmer, rawFile, audioDuration, isPreparingAudio, isUploadingTrimmedAudio, openWithFile, uploadTrimmedBlob, clearUploadState };
}
