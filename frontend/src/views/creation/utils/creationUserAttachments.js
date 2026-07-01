/**
 * 从后端历史消息 chunks 的 TEXT data 中提取用户上传附件（audios/imgs）。
 *
 * 用于历史回显：当用户消息携带上传音频/图片时，把后端字段映射成前端展示用的结构。
 *
 * 后端契约（见 数据结构.txt 用户消息带音频附件 / 带图片素材示例）：
 * - chunk.content.data 在 type==='TEXT' 时形如 { text:string, imgs:null|array, audios:null|array }
 * - audios[i]: { title, style, lyrics, audioFileId, audioUrl, coverFileId, coverUrl }
 *   其中 title / style / coverFileId / coverUrl 后端可能返回 null
 * - imgs[i]: { imgFileId, imgUrl }
 *
 * 业务约定：只取 audios 的第一个音频作为回显音频。
 */

/**
 * 从 chunks 中提取附件。
 * @param {Array<any>} chunks - 后端 messageChunks
 * @returns {{audio: (null|{fileId: string, fileName: string, fileUrl: string}), images: Array<{fileId: string, fileUrl: string, previewUrl: string}>}}
 */
export const extractUserAttachmentsFromChunks = (chunks) => {
  const attachments = {audio: null, images: []};

  // chunks 后端契约为 array；|| [] 兜底防 null/undefined 边界
  for (const chunk of (chunks || [])) {
    if (chunk?.content?.type !== 'TEXT') continue;

    const data = chunk.content.data || {};
    // audios / imgs 后端契约为 null | array，这里 || [] 处理 null
    const audios = data.audios || [];
    const imgs = data.imgs || [];

    if (!attachments.audio && audios.length > 0) {
      const rawAudio = audios[0];
      attachments.audio = {
        fileId: rawAudio.audioFileId,
        // title 后端可能为 null，回退到占位文案
        fileName: rawAudio.title || '用户上传音频',
        fileUrl: rawAudio.audioUrl,
      };
    }

    if (imgs.length > 0) {
      const mappedImages = imgs.map((img) => ({
        fileId: img.imgFileId,
        fileUrl: img.imgUrl,
        previewUrl: img.imgUrl,
      }));
      attachments.images.push(...mappedImages);
    }
  }

  return attachments;
};
