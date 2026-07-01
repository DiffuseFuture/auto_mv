<template>
  <!-- 主体卡片列表：展示 AI 返回的 subjectList -->
  <div v-if="subjectList.length" class="mt-3 mb-[10px] grid grid-cols-2 gap-3">
    <div
      v-for="(subject, index) in subjectList"
      :key="subject.messageChunkId || index"
      class="overflow-hidden rounded-[10px]"
      @click="handleEditClick(subject, index)"
    >
      <!-- 卡片顶部：主体预览图 + 序号标签 + 编辑按钮 + 类型标签 -->
      <div class="relative flex flex-col">
        <el-image
          v-if="getActiveSubjectVersion(subject)?.imgUrl"
          :src="getActiveSubjectVersion(subject)?.imgUrl"
          fit="cover"
          class="aspect-square w-full"
        />

        <!-- 没有可用图片时的占位态，避免卡片高度塌陷 -->
        <div v-else class="flex aspect-square w-full items-center justify-center bg-white/6">
          <svg-icon name="gy-image" size="24" color="#C2FF00"></svg-icon>
        </div>

        <!-- 左上角参考图序号，和 PC 端保持一致的语义表达 -->
        <button
          type="button"
          class="absolute left-[8px] top-[8px] rounded-[6px] bg-black/60 px-[8px] py-[3px] text-[10px] font-semibold leading-[14px] text-[#C2FF00] shadow-[0_4px_10px_rgba(0,0,0,0.35)]"
          @click.stop="handleCycleVersion(subject)"
        >
          {{ t('creation.messageArea.referenceLabel', {index: indexFromAssetKey(subject.assetKey)}) }}
        </button>

        <!-- 右上角编辑按钮：点击后由父层打开移动端全屏编辑伪页面 -->
        <button
          type="button"
          class="absolute right-[8px] top-[8px] z-[2]"
          :title="t('creation.messageArea.editReferenceTitle')"
          @click.stop="handleEditClick(subject, index)"
        >
          <svg-icon name="gy-edit" size="14" color="#C2FF00"></svg-icon>
        </button>

        <!-- 左下角主体类型标签，用于快速识别角色/物体/环境 -->
        <div class="absolute left-[8px] bottom-[8px] rounded-[5px] p-1 bg-black/60 text-[10px] font-medium text-[#C2FF00]">
          {{ getSubjectTypeLabel(subject.type) }}
        </div>
      </div>

      <!-- 卡片底部：当前激活版本对应的提示词摘要 -->
      <div class="px-[10px] py-[6px] bg-[#181818]">
        <div class="line-clamp-3 text-[10px] leading-[13px] text-[#C2C2C2]">
          {{ formatSubjectPromptDisplay(subject) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {useI18nText} from '@/i18n';
import {replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';

/**
 * 移动端主体卡片列表：
 * - 负责渲染 subjectList 中的主体预览图
 * - 始终读取 activeVersion 对应的图片与提示词
 * - 将提示词中的引用占位替换为可读标签后再展示
 */
const props = defineProps({
  /** subject 所属的整条消息，供上层继续编辑时定位原消息。 */
  message: {type: Object, default: null},
  /** 当前消息中的主体列表。 */
  subjectList: {type: Array, default: () => []},
  canEdit: {type: Boolean, default: false},
});

/** 通知父组件打开主体编辑页。 */
const emit = defineEmits(['edit', 'version-change']);

const {t} = useI18nText();

/** 主体类型到文案标签的映射。 */
const SUBJECT_TYPE_LABEL = {
  character: t('creation.intention.character'),
  object: t('creation.messageArea.subjectTypeObject'),
  environment: t('creation.intention.environment'),
};

/**
 * 从 assetKey 取尾部序号（subject_1 → "1"）用作左上角标签，跟后端身份对齐，比数组下标稳定。
 */
const indexFromAssetKey = (assetKey) => {
  const m = (assetKey || '').match(/(\d+)$/);
  return m ? m[1] : '';
};

/**
 * 获取主体当前激活版本。
 * SUBJECT 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} subject
 * @returns {any|null}
 */
const getActiveSubjectVersion = (subject) => {
  const versions = subject?.versions || [];
  if (!versions.length) return null;
  return versions.find((version) => version?.version === subject?.activeVersion) || versions[0];
};

/**
 * 根据主体类型返回展示标签，未识别时回退到"未分类"。
 * SUBJECT.type 后端契约 string；toLowerCase 仅做大小写收敛。
 * @param {string} type
 * @returns {string}
 */
const getSubjectTypeLabel = (type) => {
  const key = (type || '').toLowerCase();
  return SUBJECT_TYPE_LABEL[key] || t('creation.messageArea.subjectTypeUncategorized');
};

/**
 * 点击编辑时，把消息、主体及索引一起交给父组件，便于后续进入移动端全屏编辑页。
 * @param {any} subject
 * @param {number} subjectIndex
 * @returns {void}
 */
const handleEditClick = (subject, subjectIndex) => {
  emit('edit', {
    message: props.message,
    subject,
    subjectIndex,
  });
};

const getNextSubjectVersion = (subject) => {
  const versions = subject?.versions || [];
  if (!versions.length) return subject?.activeVersion;
  const currentIndex = versions.findIndex((item) => item?.version === subject?.activeVersion);
  const safeIndex = currentIndex >= 0 ? currentIndex : 0;
  return versions[(safeIndex + 1) % versions.length]?.version;
};

const handleCycleVersion = (subject) => {
  if (!props.canEdit) return;
  const nextVersion = getNextSubjectVersion(subject);
  // version 后端 number；缺失（undefined）或与当前一致都不触发
  if (nextVersion == null || nextVersion === subject?.activeVersion) return;
  emit('version-change', {
    message: props.message,
    subject,
    version: nextVersion,
  });
};

/**
 * 格式化主体提示词展示文案：
 * - 优先读取激活版本的 prompt
 * - 再回退到顶层 description
 * - 最后把 @ 引用占位替换为引用资源标签
 * @param {any} subject
 * @returns {string}
 */
const formatSubjectPromptDisplay = (subject) => {
  const activeVersion = getActiveSubjectVersion(subject);
  const prompt = activeVersion?.prompt ?? subject?.description ?? '';
  // versions[].refImages 后端契约 array
  return replaceMentionValuesToLabels(prompt, activeVersion?.refImages || [], {
    idFields: ['fileId'],
  }) || '暂无描述';
};
</script>
