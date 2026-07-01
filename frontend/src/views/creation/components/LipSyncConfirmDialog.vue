<template>
  <el-dialog
      :model-value="visible"
      :width="width"
      align-center
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="lip-sync-confirm-dialog"
      modal-class="lip-sync-confirm-overlay"
      @update:model-value="(val) => { if (!val) handleCancel(); }"
  >
    <div :class="mobile ? 'rounded-[12px] p-4' : 'rounded-[16px] p-6'" class="bg-[#0d0d0d] border border-white/10 relative">
      <!-- 标题区 -->
      <div :class="mobile ? 'gap-2.5 mb-4' : 'gap-3 mb-5'" class="flex items-center">
        <div :class="mobile ? 'w-8 h-8 rounded-[8px] text-[16px]' : 'w-10 h-10 rounded-[10px] text-[18px]'" class="bg-[#C2FF00]/15 border border-[#C2FF00]/40 flex-center text-[#C2FF00] shrink-0">✦</div>
        <div :class="mobile ? 'text-[16px]' : 'text-[18px]'" class="text-white font-semibold flex-1">{{ t('creation.messageArea.lipSyncDialogTitle') }}</div>
        <button class="text-white/60 hover:text-white text-[20px] cursor-pointer shrink-0" @click="handleCancel" aria-label="close">×</button>
      </div>

      <!-- 分镜列表卡片 -->
      <div :class="mobile ? 'rounded-[10px] p-3 mb-3' : 'rounded-[12px] p-4 mb-4'" class="bg-[#161616] border border-white/10">
        <div :class="mobile ? 'mb-2.5' : 'mb-3'" class="flex items-center justify-between">
          <span :class="mobile ? 'text-[13px]' : 'text-[14px]'" class="text-white/85 font-semibold">{{ t('creation.messageArea.lipSyncDialogScenesTitle') }}</span>
          <span :class="mobile ? 'text-[12px]' : 'text-[13px]'" class="text-white/60">
            {{ t('creation.messageArea.lipSyncDialogTotalDuration', { seconds: lipSyncPointsLoading ? '...' : (lipSyncInfo?.totalDuration ?? 0) }) }}
          </span>
        </div>
        <div v-if="sortedScenes.length" :class="mobile ? 'gap-1.5' : 'gap-2'" class="flex flex-wrap">
          <span
              v-for="scene in sortedScenes"
              :key="scene.sceneId"
              :class="mobile ? 'px-2.5 py-1 rounded-[6px] text-[12px]' : 'px-3 py-1.5 rounded-[8px] text-[13px]'"
              class="bg-white/[0.06] border border-white/10 text-white/85"
          >{{ t('creation.messageArea.lipSyncDialogSceneItem', {index: sceneNum(scene.sceneId), seconds: scene.duration}) }}</span>
        </div>
        <div v-else-if="!lipSyncPointsLoading" :class="mobile ? 'text-[12px]' : 'text-[13px]'" class="text-white/40 py-2">{{ t('creation.messageArea.lipSyncDialogEmpty') }}</div>
      </div>

      <!-- 积分消耗卡片 -->
      <div :class="mobile ? 'rounded-[10px] px-3 py-2.5 mb-3' : 'rounded-[12px] px-4 py-3 mb-4'" class="border border-[#C2FF00]/30 flex items-center justify-between"
           style="background: linear-gradient(90deg, rgba(184,255,26,0.10) 0%, rgba(184,255,26,0.02) 100%);">
        <span :class="mobile ? 'text-[13px]' : 'text-[14px]'" class="text-white">{{ t('creation.messageArea.lipSyncDialogPointsLabel') }}</span>
        <span :class="mobile ? 'text-[13px]' : 'text-[14px]'" class="text-white/85">
          <span :class="mobile ? 'text-[20px]' : 'text-[22px]'" class="text-[#C2FF00] font-bold">{{ lipSyncPointsLoading ? '…' : (lipSyncInfo?.totalPoints ?? '?') }}</span>
          <span class="ml-1">{{ t('creation.messageArea.lipSyncDialogPointsSuffix') }}</span>
        </span>
      </div>

      <!-- 说明 -->
      <div :class="mobile ? 'text-[11px] leading-[16px] mb-4' : 'text-[12px] leading-[18px] mb-5'" class="text-white/50">{{ t('creation.messageArea.lipSyncDialogDesc') }}</div>

      <!-- 按钮 -->
      <div :class="mobile ? 'gap-2.5' : 'gap-3'" class="flex items-center justify-end">
        <button :class="mobile ? 'px-4 py-2.5 rounded-[8px] text-[13px] active:bg-white/15' : 'px-5 py-2.5 rounded-[10px] text-[14px] hover:bg-white/15'" class="bg-white/10 text-white cursor-pointer" @click="handleCancel">{{ t('creation.messageArea.lipSyncDialogCancel') }}</button>
        <button :class="mobile ? 'px-4 py-2.5 rounded-[8px] text-[13px] active:opacity-80' : 'px-5 py-2.5 rounded-[10px] text-[14px] hover:opacity-90'" class="bg-[#C2FF00] text-black font-semibold transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed" :disabled="lipSyncPointsLoading || !sortedScenes.length" @click="handleConfirm">{{ t('creation.messageArea.lipSyncDialogConfirm') }}</button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { getLipSyncPoints } from '@/api/creation';
import { useI18nText } from '@/i18n';

const { t } = useI18nText();

const props = defineProps({
  /** 弹窗显示/隐藏 */
  visible: { type: Boolean, default: false },
  /** 消息列表，用于查找最新 SCENE 卡片的 messageChunkId */
  messages: { type: Array, default: () => [] },
  /** 弹窗宽度，桌面端 560px，移动端 92% */
  width: { type: String, default: '560px' },
  /** 是否移动端模式，影响字号/间距/圆角 */
  mobile: { type: Boolean, default: false },
});

const emit = defineEmits([
  /** 用户点击确认应用 */
  'confirm',
  /** 用户点击取消/关闭 */
  'cancel',
]);

/** @type {import('vue').Ref<{totalPoints: number, totalDuration: number, scenes: Array<{sceneId: string, duration: number, points: number}>}|null>} */
const lipSyncInfo = ref(null);
const lipSyncPointsLoading = ref(false);
let lipSyncQueryToken = 0;

/** 从 sceneId 尾部提取数字，与 indexFromAssetKey 同模式 */
const sceneNum = (sceneId) => {
  const m = (sceneId || '').match(/(\d+)$/);
  return m ? parseInt(m[1], 10) : 0;
};

/** 按 sceneId 数字顺序排列的分镜列表 */
const sortedScenes = computed(() => {
  const scenes = lipSyncInfo.value?.scenes;
  if (!scenes?.length) return [];
  return [...scenes].sort((a, b) => sceneNum(a.sceneId) - sceneNum(b.sceneId));
});

/** 从消息列表反向找最近一条 SCENE 卡片的 messageChunkId */
const getLatestSceneChunkId = () => {
  const arr = props.messages || [];
  for (let i = arr.length - 1; i >= 0; i--) {
    const list = arr[i]?.sceneList;
    if (list?.length) return list[list.length - 1]?.messageChunkId ?? null;
  }
  return null;
};

/** 调用 get-lipsync-points 接口查询口型同步积分与分镜明细，竞态安全 */
const fetchLipSyncPoints = async () => {
  lipSyncInfo.value = null;
  const messageChunkId = getLatestSceneChunkId();
  if (!messageChunkId) return;
  const token = ++lipSyncQueryToken;
  lipSyncPointsLoading.value = true;
  try {
    const data = await getLipSyncPoints({ messageChunkId });
    if (token !== lipSyncQueryToken) return;
    lipSyncInfo.value = data;
  } catch (error) {
    console.error(error);
    if (token === lipSyncQueryToken) lipSyncInfo.value = null;
  } finally {
    if (token === lipSyncQueryToken) lipSyncPointsLoading.value = false;
  }
};

/** 弹窗打开时自动查询积分 */
watch(() => props.visible, (open) => {
  if (open) fetchLipSyncPoints();
});

const handleConfirm = () => {
  emit('confirm');
};

const handleCancel = () => {
  emit('cancel');
};
</script>

<style lang="scss">
.lip-sync-confirm-dialog.el-dialog {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
}

.lip-sync-confirm-dialog .el-dialog__header {
  display: none;
}

.lip-sync-confirm-dialog .el-dialog__body {
  padding: 0 !important;
}

.lip-sync-confirm-overlay {
  background-color: rgba(0, 0, 0, 0.9) !important;
  backdrop-filter: blur(4px) !important;
}
</style>
