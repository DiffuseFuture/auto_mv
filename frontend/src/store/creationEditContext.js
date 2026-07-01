import {defineStore} from 'pinia';
import {reactive, watch} from 'vue';

const STORAGE_KEY = 'creation_edit_context_v1';

const createEmptyEditContextState = () => ({
  subjectInitial: {}, subjectCurrent: {},
  sceneInitial: {}, sceneCurrent: {},
  sceneScriptInitial: {}, sceneScriptCurrent: {},
});

// sessionKey 后端契约 string；空时落到 __draft__ 桶以便首发未拿到 sessionId 也能暂存
const normalizeSessionKey = (sessionKey) => sessionKey || '__draft__';

export const useCreationEditContextStore = defineStore('creationEditContext', () => {
  const stateBySession = reactive({});

  // 启动时恢复
  if (typeof window !== 'undefined') {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      Object.keys(parsed || {}).forEach((key) => {
        const item = parsed[key];
        if (item && typeof item === 'object') {
          // Object.keys 返回的 key 已经是 string，无需再包装
          stateBySession[key] = {...createEmptyEditContextState(), ...item};
        }
      });
    } catch {/* ignore */}
  }

  // 任何变更落盘
  watch(stateBySession, () => {
    if (typeof window === 'undefined') return;
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(stateBySession)); } catch {/* ignore */}
  }, {deep: true});

  const getState = (sessionKey) => {
    const key = normalizeSessionKey(sessionKey);
    if (!stateBySession[key]) stateBySession[key] = createEmptyEditContextState();
    return stateBySession[key];
  };

  const resetState = (sessionKey) => {
    const key = normalizeSessionKey(sessionKey);
    stateBySession[key] = createEmptyEditContextState();
    return stateBySession[key];
  };

  /**
   * 记录 SUBJECT / SCENE 版本变化。
   * - 首次记录把 previousVersion 写入 initial 作基线
   * - nextVersion 回到 initial 视为没改动，删除 current
   * - 无 sessionKey 直接 return（避免污染 __draft__）
   *
   * 字段契约：messageId/itemKey(=chunkId) 后端 string；activeVersion 后端 number。
   */
  const recordVersionChange = (sessionKey, type, payload = {}) => {
    if (!sessionKey) return;
    const {messageId, itemKey} = payload;
    if (!messageId || !itemKey) return;

    const state = getState(sessionKey);
    const initialKey = type === 'SUBJECT' ? 'subjectInitial' : 'sceneInitial';
    const currentKey = type === 'SUBJECT' ? 'subjectCurrent' : 'sceneCurrent';
    const key = `${messageId}::${itemKey}`;

    if (state[initialKey][key] === undefined) state[initialKey][key] = payload.previousVersion;
    if (payload.nextVersion === state[initialKey][key]) {
      delete state[currentKey][key];
      return;
    }
    state[currentKey][key] = {messageId, itemKey, activeVersion: payload.nextVersion};
  };

  return {stateBySession, getState, resetState, recordVersionChange};
});
