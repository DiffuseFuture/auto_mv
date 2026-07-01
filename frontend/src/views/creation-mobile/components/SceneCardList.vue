<template>
  <!-- 分镜卡片列表：用于展示当前消息里的 sceneList -->
  <div v-if="sceneList.length" class="mt-3 mb-4 flex flex-col gap-3">
    <div
      v-for="(scene, index) in sceneList"
      :key="scene.messageChunkId || index"
      class="overflow-hidden rounded-[12px] bg-[#181818]"
    >
      <!-- 卡片顶部：分镜封面、序号标签、编辑入口 -->
      <div class="relative flex flex-col overflow-hidden rounded-[10px]">
        <el-image
          v-if="getActiveSceneVersion(scene)?.coverUrl"
          :src="getActiveSceneVersion(scene)?.coverUrl"
          fit="contain"
          class="aspect-[16/9] w-full cursor-pointer"
          @click="handleCoverClick(scene, index)"
        />

        <!-- 封面缺失时显示占位态，保证卡片布局稳定 -->
        <div
          v-else
          class="flex aspect-[16/9] w-full cursor-pointer items-center justify-center bg-white/6"
          @click="handleCoverClick(scene, index)"
        >
          <svg-icon name="gy-video" size="22" color="#C2FF00"></svg-icon>
        </div>

        <!-- 左上角分镜序号，和 PC 端 sceneLabel 语义保持一致 -->
        <button
          type="button"
          class="absolute left-[8px] top-[8px] rounded-[6px] bg-black/60 px-[8px] py-[3px] text-[10px] font-semibold leading-[14px] text-[#C2FF00] shadow-[0_4px_10px_rgba(0,0,0,0.35)]"
          @click.stop="handleCycleVersion(scene)"
        >
          {{ t('creation.messageArea.sceneLabel', {index: indexFromAssetKey(scene.assetKey)}) }} · {{ getSceneDurationLabel(scene) }}
        </button>

        <!-- 左下角起止时间 -->
        <div class="absolute bottom-[8px] left-[8px] rounded-[6px] bg-black/60 px-[8px] py-[2px] text-[10px] font-medium leading-[14px] text-white">
          {{ formatSceneTimeRange(scene) }}
        </div>

        <!-- 右上角编辑入口，点击后交由父层打开全屏编辑页 -->
        <button
          v-if="canEdit"
          type="button"
          class="absolute right-[8px] top-[8px] z-[2]"
          :title="t('creation.messageArea.editSceneTitle')"
          @click.stop="handleEditClick(scene, index)"
        >
          <svg-icon name="gy-edit" size="14" color="#C2FF00"></svg-icon>
        </button>
      </div>

      <!-- 卡片底部：当前激活版本对应的分镜提示词摘要 -->
      <div class="px-[10px] py-[8px]">
        <div class="line-clamp-3 text-[11px] leading-[16px] text-[#C2C2C2]">
          {{ formatScenePromptDisplay(scene) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {useI18nText} from '@/i18n';
import {replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';

/**
 * 移动端分镜卡片列表：
 * - 展示 sceneList 当前激活版本封面与提示词
 * - 支持封面预览多版本
 * - 抛出编辑事件给父层统一打开分镜编辑页
 */
const props = defineProps({
  /** scene 所属整条消息，编辑时用于定位原消息对象。 */
  message: {type: Object, default: null},
  /** 当前消息中的分镜列表。 */
  sceneList: {type: Array, default: () => []},
  canEdit: {type: Boolean, default: false},
});

/** 通知父组件进入分镜编辑流程。 */
const emit = defineEmits(['edit', 'version-change', 'play']);
const {t} = useI18nText();

/**
 * 从 assetKey 取尾部序号（scene_1 → "1"）用作左上角标签，跟后端身份对齐，比数组下标稳定。
 */
const indexFromAssetKey = (assetKey) => {
  const m = (assetKey || '').match(/(\d+)$/);
  return m ? m[1] : '';
};

/**
 * 获取分镜当前激活版本。
 * SCENE 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} scene
 * @returns {any|null}
 */
const getActiveSceneVersion = (scene) => {
  const versions = scene?.versions || [];
  if (!versions.length) return null;
  return versions.find((item) => item?.version === scene?.activeVersion) || versions[0];
};

/**
 * 格式化分镜提示词展示：当前版本 visualPrompt，@subject_X 替换为 @图片N。
 * SCENE versions[].subjects[].id 后端 string。
 * @param {any} scene
 * @returns {string}
 */
const formatScenePromptDisplay = (scene) => {
  const active = getActiveSceneVersion(scene);
  const visualPrompt = active?.visualPrompt ?? scene?.description ?? '';
  return replaceMentionValuesToLabels(visualPrompt, active?.subjects || [], {
    idFields: ['id'],
  }) || '--';
};

/**
 * 秒数 → mm:ss.ff 时间码。
 * 0 → "0:00.00"，2.64 → "0:02.64"
 * @param {number} sec
 * @returns {string}
 */
const fmtTimecode = (sec) => {
  if (!sec) return '0:00.00';
  const total = Math.floor(sec);
  const min = Math.floor(total / 60);
  const s = total % 60;
  const frac = Math.round((sec - total) * 100);
  return `${min}:${String(s).padStart(2, '0')}.${String(frac).padStart(2, '0')}`;
};

/**
 * 分镜起止时间：如 `0:00.00-0:02.64`
 * @param {any} scene
 * @returns {string}
 */
const formatSceneTimeRange = (scene) => {
  const {startTime, endTime} = scene || {};
  if (startTime != null && endTime != null && endTime >= startTime) {
    return `${fmtTimecode(startTime)}-${fmtTimecode(endTime)}`;
  }
  return '--:--.--:--';
};

/**
 * 读取分镜时长秒数。SCENE startTime/endTime/duration 后端皆 number。
 * @param {any} scene
 * @returns {number}
 */
const getSceneDurationSeconds = (scene) => {
  if (!scene) return 0;
  const {startTime, endTime, duration} = scene;
  if (startTime != null && endTime != null && endTime > startTime) return Math.max(0, endTime - startTime);
  return duration || 0;
};

/**
 * 分镜时长标签（如 `2.64s`）。
 * @param {any} scene
 * @returns {string}
 */
const getSceneDurationLabel = (scene) => {
  return `${getSceneDurationSeconds(scene).toFixed(2)}s`;
};

/**
 * 点击编辑时，把消息、分镜对象和索引透传给父组件。
 * @param {any} scene
 * @param {number} sceneIndex
 * @returns {void}
 */
const handleEditClick = (scene, sceneIndex) => {
  emit('edit', {
    message: props.message,
    scene,
    sceneIndex,
  });
};

/**
 * 点击分镜封面：
 * - 可编辑时直接进入分镜编辑
 * - 只读时打开分镜播放弹层
 * @param {any} scene
 * @param {number} sceneIndex
 * @returns {void}
 */
const handleCoverClick = (scene, sceneIndex) => {
  const payload = {
    message: props.message,
    scene,
    sceneIndex,
  };
  if (props.canEdit) {
    emit('edit', payload);
    return;
  }
  emit('play', payload);
};

const getNextSceneVersion = (scene) => {
  const versions = scene?.versions || [];
  if (!versions.length) return scene?.activeVersion;
  const currentIndex = versions.findIndex((item) => item?.version === scene?.activeVersion);
  const safeIndex = currentIndex >= 0 ? currentIndex : 0;
  return versions[(safeIndex + 1) % versions.length]?.version;
};

const handleCycleVersion = (scene) => {
  if (!props.canEdit) return;
  const nextVersion = getNextSceneVersion(scene);
  // version 后端 number；缺失（undefined）或与当前一致都不触发
  if (nextVersion == null || nextVersion === scene?.activeVersion) return;
  emit('version-change', {
    message: props.message,
    scene,
    version: nextVersion,
  });
};
</script>
