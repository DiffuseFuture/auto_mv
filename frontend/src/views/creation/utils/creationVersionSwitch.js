import {switchVersion} from '@/api/creation';
import {isNotEmpty} from '@/utils/index.js';

/**
 * 版本号展示文案：后端 version 从 0 开始（number 契约），前端显示从 V1 开始。
 * @param {number} version
 * @returns {string}
 */
export const formatCreationVersionLabel = (version) => `V${version + 1}`;

/**
 * 统一处理 SUBJECT / SCENE 的版本切换。
 * - 本地更新 activeVersion
 * - 调用 switchVersion 持久化
 * - 上抛 edit-context-change 参与下一轮发送上下文
 *
 * @param {{
 *  version: number,
 *  type: 'SUBJECT'|'SCENE',
 *  target: any,
 *  message: any,
 *  emitEditContextChange?: (payload: Record<string, any>) => void,
 * }} params
 * @returns {void}
 */
export const applyCreationVersionChange = ({
  version,
  type,
  target,
  message,
  emitEditContextChange,
}) => {
  if (!target || !message) return;

  const previousVersion = target.activeVersion;
  const nextVersion = version;
  if (nextVersion == null) return;
  if (previousVersion === nextVersion) return;

  target.activeVersion = nextVersion;

  if (isNotEmpty(target?.messageChunkId)) {
    void switchVersion({chunkId: target.messageChunkId, activeVersion: nextVersion}).catch(() => {});
  }

  emitEditContextChange?.({
    type,
    messageId: message?.messageId,
    itemKey: target.messageChunkId,
    previousVersion,
    nextVersion,
  });
};
