<template>
  <div class="creation-mobile-message-list">
    <!-- 历史消息加载态 -->
    <div v-if="loadingMessages" class="flex-col-center h-full text-white/45 py-8">
      <el-icon :size="32" class="animate-spin mb-3">
        <Loading/>
      </el-icon>
      <p class="text-[14px]">历史记录加载中...</p>
    </div>

    <!-- 空状态：当前没有消息，且不处于流式生成中 -->
    <div v-else-if="isEmpty(messages) && !loading" class="flex-col-center h-full text-white/45 py-8">
      <el-icon :size="40" class="mb-3">
        <ChatDotRound/>
      </el-icon>
      <p class="text-[14px]">开始你的创作吧</p>
    </div>

    <template v-else>
      <!-- 消息列表：这里已提前把每条消息转换为适合渲染的结构 -->
      <div v-for="(item, msgIdx) in renderMessages" :key="item.msg.messageId || msgIdx" class="mb-3">
        <!-- 用户消息：文本气泡 + 上传附件（图片/音频） -->
        <div v-if="item.msg.senderType === 'USER'" class="flex justify-end">
          <div class="flex max-w-full flex-col items-end gap-2">
            <div
                v-if="item.msg.content"
                class="w-fit max-w-full break-words whitespace-pre-wrap rounded-[14px] rounded-tr-none bg-[#C2FF00] p-[10px] text-[15px] leading-[21px] text-[#111]"
            >
              {{ item.msg.content }}
            </div>

            <!-- 用户消息附件：展示风格与 PC 对齐（音频方块 + 图片方块） -->
            <div v-if="item.msg.attachments && (item.msg.attachments.audio || item.msg.attachments.images?.length)" class="flex items-center gap-2">
              <button
                  v-if="item.msg.attachments.audio"
                  type="button"
                  class="h-12 w-12 rounded-[12px] bg-white/10 flex-center"
                  :class="{'user-audio-pulse': isUserAudioPlaying(item.msg)}"
                  aria-label="play-user-audio"
                  @click="emit('user-audio-play', item.msg)"
              >
                <svg-icon name="gy-audiofiles" size="20" color="#C2FF00"></svg-icon>
              </button>
              <div
                  v-for="(img, idx) in item.msg.attachments.images"
                  :key="img.fileId || idx"
                  class="h-12 w-12 overflow-hidden rounded-[12px]"
              >
                <el-image
                    :src="img.fileUrl || img.previewUrl"
                    :preview-src-list="item.msg.attachments.images.map((image) => image.fileUrl || image.previewUrl)"
                    :initial-index="idx"
                    :hide-on-click-modal="true"
                    preview-teleported
                    fit="cover"
                    class="h-full w-full"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- AI 消息：统一走注册表渲染链路 -->
        <div v-else class="max-w-full">
          <template v-for="(block, blockIdx) in item.renderBlocks" :key="`${item.msg.messageId || msgIdx}-${block.type}-${blockIdx}`">
            <component
                :is="resolveBlockRenderer(block.type)"
                v-bind="getBlockProps(block, item.msg, blockIdx, item.renderBlocks.length)"
                :player-state="playerState"
                @open="(payload) => emit('audio-open', payload)"
                @edit="(payload) => block.type === 'SCENE_LIST' ? emit('scene-edit', payload) : emit('subject-edit', payload)"
                @version-change="(payload) => block.type === 'SCENE_LIST' ? emit('scene-version-change', payload) : emit('subject-version-change', payload)"
                @play="(payload) => emit('scene-play', payload)"
                @scene-script-change="(payload) => emit('scene-script-change', {message: item.msg, ...payload})"
                @action-required-submit="(payload) => emit('action-required-submit', payload)"
            />

            <!-- SUBJECT 引导：放在 SUBJECT_LIST 消息块下方（参照 PC 文案/结构） -->
            <div
                v-if="
                block.type === 'SUBJECT_LIST' &&
                isFirstRenderedBlockType(item.renderBlocks, 'SUBJECT_LIST', blockIdx) &&
                shouldShowSubjectGuide(item.msg, msgIdx)
              "
                class="mb-3 mt-1"
            >
              <div class="flex flex-col items-start gap-3">
                <div class="text-[12px] leading-[17px]" :class="{'opacity-60': isSubjectContinueLocked()}">
                  {{t('creation.messageArea.mobileNextStep')}}
                </div>

                <button
                    type="button"
                    class="w-[100px] h-[30px] rounded-[10px] bg-[#C2FF00] text-[12px] text-[#192100] disabled:opacity-60"
                    :disabled="isSubjectContinueLocked()"
                    @click="handleSubjectContinue(item.msg)"
                >
                  <span>{{ t('creation.messageArea.mobileConfirmAndContinue') }}</span>
                </button>
              </div>
            </div>

            <!-- SCENE_SCRIPT 引导：放在 SCENE_SCRIPT 消息块下方（参照 PC 文案/结构） -->
            <div
                v-if="
                block.type === 'SCENE_SCRIPT' &&
                isFirstRenderedBlockType(item.renderBlocks, 'SCENE_SCRIPT', blockIdx) &&
                shouldShowSceneScriptGuide(item.msg, msgIdx)
              "
                class="mb-3 mt-1"
            >
              <div class="flex flex-col items-start gap-2">
                <div class="text-[12px] leading-[17px]" :class="{'opacity-60': isSceneScriptContinueLocked()}">
                  {{t('creation.messageArea.mobileNextStep')}}
                </div>

                <button
                    type="button"
                    class="w-[100px] h-[30px] rounded-[10px] bg-[#C2FF00] text-[12px] text-[#192100] disabled:opacity-60"
                    :disabled="isSceneScriptContinueLocked()"
                    @click="handleSceneScriptContinue(item.msg)"
                >
                  <span>{{ t('creation.messageArea.mobileConfirmAndContinue') }}</span>
                </button>
              </div>
            </div>

            <!-- SCENE 引导：放在 SCENE_LIST 消息块下方 -->
            <div
                v-if="
                block.type === 'SCENE_LIST' &&
                isFirstRenderedBlockType(item.renderBlocks, 'SCENE_LIST', blockIdx) &&
                shouldShowSceneGuide(item.msg, msgIdx)
              "
                class="mb-3 mt-1"
            >
              <div class="flex flex-col gap-2.5">
                <!-- 状态提示行 -->
                <div class="flex items-center gap-2" :class="{ 'opacity-60': isSceneContinueLocked() }">
                  <span class="w-2 h-2 rounded-full bg-[#C2FF00] shrink-0"/>
                  <span class="text-[12px] font-semibold">
                    <span class="text-[#C2FF00]">{{ t('creation.messageArea.finalStepPrefix') }}</span>
                    <span class="text-white/85">{{ t('creation.messageArea.exportMvVideo') }}</span>
                  </span>
                </div>

                <!-- 两个开关 -->
                <div class="grid grid-cols-2 gap-2">
                  <!-- 字幕开关 -->
                  <div class="rounded-[10px] bg-[#1a1a1a] border border-white/10 px-3 py-2.5 flex items-center justify-between gap-2">
                    <span class="text-white text-[13px] font-semibold leading-[18px]">{{ t('creation.messageArea.autoSubtitleTitle') }}</span>
                    <el-switch class="scene-guide-switch" v-model="subtitle" :disabled="isSceneContinueLocked()" size="small" inline-prompt style="--el-switch-on-color: #C2FF00;" />
                  </div>

                  <!-- 口型同步开关 -->
                  <div class="rounded-[10px] bg-[#1a1a1a] border border-white/10 px-3 py-2.5 flex items-center justify-between gap-2">
                    <span class="text-white text-[13px] font-semibold leading-[18px]">{{ t('creation.messageArea.lipSyncTitle') }}</span>
                    <el-switch class="scene-guide-switch" v-model="lipSync" :disabled="isSceneContinueLocked()" size="small" inline-prompt style="--el-switch-on-color: #C2FF00;" />
                  </div>
                </div>

                <!-- 合成按钮 -->
                <button
                    type="button"
                    class="w-full h-[44px] rounded-[12px] bg-[#C2FF00] text-[14px] font-semibold text-black active:opacity-80 transition-opacity disabled:opacity-50"
                    :disabled="isSceneContinueLocked()"
                    @click="handleSceneContinue(item.msg)"
                >
                  <span>{{ t('creation.messageArea.finalComposeButton') }}</span>
                </button>
              </div>
            </div>
          </template>

          <!-- 反馈（与 PC 端一致：仅在音频/视频存在时显示） -->
          <CreationSimpleFeedback
              v-if="showFeedback && Array.isArray(item.msg.audioList) && item.msg.audioList.length"
              :title="t('creation.feedback.musicTitle')"
              :message-id="item.msg.messageId"
              :allow-pending="isLastMessage(msgIdx)"
          />
          <CreationSimpleFeedback
              v-if="showFeedback && Array.isArray(item.msg.videoList) && item.msg.videoList.length"
              :title="t('creation.feedback.videoTitle')"
              :message-id="item.msg.messageId"
              :allow-pending="isLastMessage(msgIdx)"
          />

          <!--
            「正在生成中」独立气泡：挂在当前流式 AI 消息的最末端，只要本轮还没 COMPLETE 就一直显示。
            从 TextBlock 抽出来的原因：SCENE_SCRIPT 等非 TEXT 块成为消息末块时，原来的 dots 会消失。
          -->
          <div
              v-if="isMessageStreaming(item.msg)"
              class="mb-3 w-fit min-h-[41px] flex items-center rounded-[10px] rounded-tl-none bg-[#5D634A]/50 px-[10px] py-[10px]"
          >
            <div class="typing-dots flex items-center gap-[8px]">
              <span class="typing-dot w-[8px] h-[8px] rounded-full bg-[#c2ff00]"></span>
              <span class="typing-dot w-[8px] h-[8px] rounded-full bg-[#c2ff00]"></span>
              <span class="typing-dot w-[8px] h-[8px] rounded-full bg-[#c2ff00]"></span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
import {ChatDotRound, Loading} from '@element-plus/icons-vue';
import {useI18nText} from '@/i18n';
import {isEmpty} from '@/utils/index.js';
import 'github-markdown-css/github-markdown-dark.css';
import ActionRequiredBlock from '@/views/creation-mobile/components/ActionRequiredBlock.vue';
import TextBlock from '@/views/creation-mobile/components/TextBlock.vue';
import LyricsBlock from '@/views/creation-mobile/components/LyricsBlock.vue';
import SceneScriptBlock from '@/views/creation-mobile/components/SceneScriptBlock.vue';
import AudioCardList from '@/views/creation-mobile/components/AudioCardList.vue';
import SubjectCardList from '@/views/creation-mobile/components/SubjectCardList.vue';
import SceneCardList from '@/views/creation-mobile/components/SceneCardList.vue';
import VideoCardList from '@/views/creation-mobile/components/VideoCardList.vue';
import CreationSimpleFeedback from '@/views/creation/components/CreationSimpleFeedback.vue';

/**
 * 移动端创作消息列表：
 * - 负责切换加载态 / 空态 / 消息列表
 * - 区分用户消息与 AI 消息的展示结构
 * - 为 AI 消息分发不同 block 渲染器，并挂载音频/主体/分镜/视频卡片
 */
const props = defineProps({
  /** 历史消息是否仍在加载。 */
  loadingMessages: {type: Boolean, default: false},
  /** 当前是否处于流式生成阶段。 */
  loading: {type: Boolean, default: false},
  /** 原始消息列表。 */
  messages: {type: Array, default: () => []},
  /** 当前正在流式返回的 AI 消息 ID。后端契约 messageId 为 string，本地占位也已统一为 string；未发起请求时为 null。 */
  currentAiMessageId: {type: String, default: null},
  /** 音频播放器共享状态，用于同步音乐卡片播放态。 */
  playerState: {type: Object, required: true},
  /** 当前正在播放“用户上传音频”的消息 ID（messageId 为 string，未指向时为 null）。 */
  currentUserAudioMessageId: {type: String, default: null},
  /** 当前是否允许编辑创作链路。 */
  canEditCreation: {type: Boolean, default: false},
  // 后端 messageId 为 string；未指向时为 null
  subjectContinueMsgId: {type: String, default: null},
  subjectContinueDisabled: {type: Boolean, default: false},
  // 后端 messageId 为 string；未指向时为 null
  sceneScriptContinueMsgId: {type: String, default: null},
  sceneScriptContinueDisabled: {type: Boolean, default: false},
  // 后端 messageId 为 string；未指向时为 null
  sceneContinueMsgId: {type: String, default: null},
  sceneContinueDisabled: {type: Boolean, default: false},
  showFeedback: {type: Boolean, default: true},
});

/** 向父组件抛出音频打开、主体编辑、分镜编辑等事件，由页面容器统一处理。 */
const emit = defineEmits([
  'audio-open',
  'subject-edit',
  'scene-edit',
  'scene-play',
  'subject-version-change',
  'scene-version-change',
  'scene-script-change',
  'action-required-submit',
  'user-audio-play',
  'subject-continue',
  'scene-script-continue',
  'scene-continue',
  'scene-lipsync-confirm',
]);

const {t} = useI18nText();

/**
 * 过滤掉无效 block，保证渲染器只处理结构正确的消息块。
 * @param {any} msg
 * @returns {Array<any>}
 */
const normalizeBlocks = (msg) => {
  const blocks = Array.isArray(msg?.blocks) ? msg.blocks : [];
  return blocks.filter((block) => block && typeof block.type === 'string');
};

/**
 * 构建渲染块列表：
 * - 先渲染普通 blocks（TEXT/LYRICS/IMAGE 等）
 * - SUBJECT_LIST/SCENE_LIST/SCENE_SCRIPT 这些确认类型的 block 始终显示在末尾
 * - 最后追加 AUDIO_LIST/VIDEO_LIST
 * @param {any} msg
 * @returns {Array<any>}
 */
const buildRenderBlocks = (msg) => {
  const blocks = normalizeBlocks(msg);
  
  // 分离普通 blocks 和确认类型 blocks
  const confirmTypes = ['SUBJECT_LIST', 'SCENE_LIST', 'SCENE_SCRIPT'];
  const confirmBlocks = blocks.filter((b) => confirmTypes.includes(b?.type));
  const normalBlocks = blocks.filter((b) => !confirmTypes.includes(b?.type));
  
  // 普通 blocks 在前，确认类型在最后
  const orderedBlocks = [...normalBlocks, ...confirmBlocks];
  
  if (Array.isArray(msg?.audioList) && msg.audioList.length > 0) orderedBlocks.push({
    type: 'AUDIO_LIST',
    audioList: msg.audioList,
  });
  if (Array.isArray(msg?.subjectList) && msg.subjectList.length > 0) orderedBlocks.push({
    type: 'SUBJECT_LIST',
    subjectList: msg.subjectList,
  });
  if (msg?.sceneList?.length > 0) orderedBlocks.push({
    type: 'SCENE_LIST',
    sceneList: msg.sceneList,
  });
  if (Array.isArray(msg?.videoList) && msg.videoList.length > 0) orderedBlocks.push({
    type: 'VIDEO_LIST',
    videoList: msg.videoList,
  });
  return orderedBlocks;
};

/**
 * 为不同 block 组装最小必需 props，避免引入多余壳组件。
 * @param {any} block
 * @param {any} msg
 * @param {number} blockIdx
 * @param {number} blocksLength
 * @returns {Record<string, any>}
 */
const getBlockProps = (block, msg, blockIdx, blocksLength) => {
  if (block?.type === 'AUDIO_LIST') {
    return {audioList: block.audioList || [], playerState: props.playerState};
  }
  if (block?.type === 'SUBJECT_LIST') {
    return {message: msg, subjectList: block.subjectList || [], canEdit: props.canEditCreation};
  }
  if (block?.type === 'SCENE_LIST') {
    return {message: msg, sceneList: block.sceneList || [], canEdit: props.canEditCreation};
  }
  if (block?.type === 'SCENE_SCRIPT') {
    return {block, canEdit: props.canEditCreation};
  }
  if (block?.type === 'ACTION_REQUIRED') {
    return {block, canEdit: props.canEditCreation, loading: props.loading};
  }
  if (block?.type === 'VIDEO_LIST') {
    return {videoList: block.videoList || []};
  }
  return {
    block,
    message: msg,
    loading: props.loading,
    currentAiMessageId: props.currentAiMessageId,
    blockIndex: blockIdx,
    blocksLength,
  };
};

const renderMessages = computed(() => (
    Array.isArray(props.messages)
        ? props.messages.map((msg) => ({
          msg,
          renderBlocks: buildRenderBlocks(msg),
        }))
        : []
));

const getMessagesCount = () => (Array.isArray(props.messages) ? props.messages.length : 0);
const isLastMessage = (msgIdx) => msgIdx === getMessagesCount() - 1;

/**
 * 引导按钮“点击即隐藏”的本地去重状态（对齐 PC dismissedGuideMap）。
 * 结构：{ subject: { [messageId]: true }, sceneScript: {...}, scene: {...} }
 */
const dismissedGuideMap = ref({
  subject: {},
  sceneScript: {},
  scene: {},
});

/** 最终合成阶段：字幕与口型同步开关 */
const subtitle = ref(false);
const lipSync = ref(false);

const getGuideKey = (msg) => String(msg?.messageId ?? '');

const isGuideDismissed = (type, msg) => {
  const key = getGuideKey(msg);
  if (!key) return false;
  return Boolean(dismissedGuideMap.value[type]?.[key]);
};

const dismissGuide = (type, msg) => {
  const key = getGuideKey(msg);
  if (!key) return;
  if (!dismissedGuideMap.value[type]) dismissedGuideMap.value[type] = {};
  dismissedGuideMap.value[type][key] = true;
};

const clearGuideDismissedByType = (type) => {
  dismissedGuideMap.value[type] = {};
};

// 当继续目标消息变化时清理旧隐藏态，保证新消息仍能显示引导按钮
watch(
    () => props.subjectContinueMsgId,
    () => clearGuideDismissedByType('subject'),
);
watch(
    () => props.sceneScriptContinueMsgId,
    () => clearGuideDismissedByType('sceneScript'),
);
watch(
    () => props.sceneContinueMsgId,
    () => clearGuideDismissedByType('scene'),
);

const getSceneScriptScenes = (msg) => {
  const blocks = Array.isArray(msg?.blocks) ? msg.blocks : [];
  const block = blocks.find((b) => b?.type === 'SCENE_SCRIPT') || null;
  return Array.isArray(block?.scenes) ? block.scenes : [];
};

/**
 * 对齐 PC 的 shouldShowGuide：
 * 1) 有编辑权限；2) 当前不在 loading 里；3) 命中 continueMsgId；
 * 4) 当前是最后一条消息；5) sceneScript 需存在 SCENE_SCRIPT 块；
 * 6) 未被本地 dismiss。
 */
const shouldShowGuide = ({type, msg, msgIdx, continueMsgId, extraCheck = null}) => {
  if (!props.canEditCreation) return false;
  if (props.loading) return false;
  if (String(continueMsgId ?? '') !== String(msg?.messageId ?? '')) return false;
  if (!isLastMessage(msgIdx)) return false;
  if (typeof extraCheck === 'function' && !extraCheck(msg)) return false;
  if (isGuideDismissed(type, msg)) return false;
  return true;
};

const shouldShowSubjectGuide = (msg, msgIdx) => shouldShowGuide({
  type: 'subject',
  msg,
  msgIdx,
  continueMsgId: props.subjectContinueMsgId,
  extraCheck: (targetMsg) => Array.isArray(targetMsg?.subjectList) && targetMsg.subjectList.length > 0,
});

const shouldShowSceneScriptGuide = (msg, msgIdx) => shouldShowGuide({
  type: 'sceneScript',
  msg,
  msgIdx,
  continueMsgId: props.sceneScriptContinueMsgId,
  extraCheck: (targetMsg) => getSceneScriptScenes(targetMsg).length > 0,
});

const shouldShowSceneGuide = (msg, msgIdx) => shouldShowGuide({
  type: 'scene',
  msg,
  msgIdx,
  continueMsgId: props.sceneContinueMsgId,
  extraCheck: (targetMsg) => targetMsg?.sceneList?.length > 0,
});

const isFirstRenderedBlockType = (blocks, type, idx) => {
  if (!Array.isArray(blocks)) return false;
  return blocks.findIndex((b) => b?.type === type) === idx;
};

/**
 * 对齐 PC 的 isGuideContinueLocked：
 * - 只要不能编辑，或对应阶段禁用，则禁止点击。
 */
const isGuideContinueLocked = (type) => {
  const disabledByType = {
    subject: props.subjectContinueDisabled,
    sceneScript: props.sceneScriptContinueDisabled,
    scene: props.sceneContinueDisabled,
  };
  return !props.canEditCreation || Boolean(disabledByType[type]);
};

const isSubjectContinueLocked = () => isGuideContinueLocked('subject');
const isSceneScriptContinueLocked = () => isGuideContinueLocked('sceneScript');
const isSceneContinueLocked = () => isGuideContinueLocked('scene');

const handleSubjectContinue = (msg) => {
  if (isSubjectContinueLocked()) return;
  dismissGuide('subject', msg);
  emit('subject-continue', msg?.messageId);
};

const handleSceneScriptContinue = (msg) => {
  if (isSceneScriptContinueLocked()) return;
  dismissGuide('sceneScript', msg);
  emit('scene-script-continue', msg?.messageId);
};

const handleSceneContinue = (msg) => {
  if (isSceneContinueLocked()) return;
  if (lipSync.value) {
    const extras = { subtitle: subtitle.value, lipSync: lipSync.value };
    emit('scene-lipsync-confirm', msg, extras);
    return;
  }
  const extras = { subtitle: subtitle.value, lipSync: lipSync.value };
  dismissGuide('scene', msg);
  emit('scene-continue', msg?.messageId, extras);
  subtitle.value = false;
  lipSync.value = false;
};

defineExpose({
  dismissSceneGuide(msg) { dismissGuide('scene', msg); },
  resetLipSync() { lipSync.value = false; },
});

/** 组件内本地映射：不再依赖外部 renderers 文件。 */
const localBlockRenderers = {
  TEXT: TextBlock,
  LYRICS: LyricsBlock,
  SCENE_SCRIPT: SceneScriptBlock,
  ACTION_REQUIRED: ActionRequiredBlock,
  AUDIO_LIST: AudioCardList,
  SUBJECT_LIST: SubjectCardList,
  SCENE_LIST: SceneCardList,
  VIDEO_LIST: VideoCardList,
};

/**
 * 根据块类型选择对应的移动端渲染组件。
 * 未命中的类型统一回退到文本块组件。
 * @param {string} type
 * @returns {any}
 */
const resolveBlockRenderer = (type) => {
  return localBlockRenderers[type] || localBlockRenderers.TEXT;
};

/**
 * 判断当前用户附件音频是否正在播放（用于脉冲动画态）。
 * @param {any} msg
 * @returns {boolean}
 */
const isUserAudioPlaying = (msg) => {
  return props.currentUserAudioMessageId === msg?.messageId;
};

/**
 * 当前消息是否仍在流式输出中（用于驱动底部「正在生成中」气泡）。
 * - props.loading 为 true 即整轮 SSE 未 COMPLETE
 * - currentAiMessageId 命中本条消息即为当前 AI 回复
 * 只要二者都满足就一直亮，避免依赖具体末尾 block 类型。
 */
const isMessageStreaming = (msg) => {
  if (!props.loading) return false;
  return String(msg?.messageId ?? '') === String(props.currentAiMessageId ?? '');
};
</script>

<style lang="scss">
.creation-mobile-message-list {
  .mobile-markdown-body {
    background: transparent !important;
    color: inherit !important;
    font-family: inherit !important;
    font-size: 14px !important;
    line-height: 1.45 !important;

    // 覆盖 github-markdown-css 的默认间距，移动端气泡里需要更紧凑
    & > *:first-child { margin-top: 0 !important; }
    & > *:last-child { margin-bottom: 0 !important; }

    p,
    blockquote,
    ul,
    ol,
    dl,
    pre,
    details {
      margin-top: 0 !important;
      margin-bottom: 4px !important;
    }

    h1, h2, h3, h4, h5, h6 {
      margin-top: 6px !important;
      margin-bottom: 2px !important;
      padding-bottom: 0 !important;
      border-bottom: 0 !important;
      line-height: 1.25 !important;
    }

    ul, ol {
      padding-left: 1.3em !important;
    }

    li {
      margin-top: 0 !important;
      line-height: 1.45 !important;
    }

    li + li {
      margin-top: 1px !important;
    }

    li > p,
    li > ul,
    li > ol {
      margin-top: 1px !important;
      margin-bottom: 0 !important;
    }

    blockquote {
      border-left: 0 !important;
      padding-bottom: 8px !important;
      border-bottom: 2px solid rgba(255, 255, 255, 0.1) !important;
    }

    hr {
      height: 0 !important;
      background: transparent !important;
      margin: 0!important;
    }

    pre {
      background: rgba(0, 0, 0, 0.35) !important;
      border-radius: 8px;
      padding: 6px 8px !important;
      font-size: 12px !important;
      line-height: 1.45 !important;
    }

    // 表格：仅收紧间距与行高，不动 github-markdown-css 自带的边框/隔行底色等视觉
    table {
      margin-top: 0 !important;
      margin-bottom: 4px !important;
      font-size: 12px !important;
      line-height: 1.3 !important;
      width: auto !important;

      th, td {
        padding: 3px 6px !important;
        line-height: 1.3 !important;
      }

      // 短内容：精确占自己 max-content 宽度、绝不换行
      // 长内容：保最小列宽，避免被多个短列挤到极窄逐字换行硬拉行高；
      //        超出父宽时整表横向滚动而非垂直撑高。
      // 类由 markdown 解析阶段 annotateTableCells 按内容视觉宽度打上（th 永远视作短）
      th,
      td.cell-short {
        width: max-content !important;
        white-space: nowrap !important;
      }

      td.cell-long {
        min-width: 12em !important;
        white-space: pre-wrap !important;
      }
    }
  }

  .typing-dot {
    opacity: 0.28;
    animation: dot-flash 1.4s infinite linear;
  }

  .typing-dots .typing-dot:nth-child(2) {
    animation-delay: 0.2s;
  }

  .typing-dots .typing-dot:nth-child(3) {
    animation-delay: 0.4s;
  }

  .user-audio-pulse {
    position: relative;
    animation: userAudioPulse 1.2s ease-in-out infinite;
    box-shadow: 0 0 0 0 rgba(194, 255, 0, 0.55);
  }

  .user-audio-pulse::after {
    content: '';
    position: absolute;
    inset: 6px;
    border-radius: 10px;
    border: 1px solid rgba(194, 255, 0, 0.65);
    opacity: 0.8;
    animation: userAudioRing 1.2s ease-out infinite;
  }
}

@keyframes dot-flash {
  0%,
  80%,
  100% {
    opacity: 0.28;
    transform: scale(0.86);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes userAudioPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

@keyframes userAudioRing {
  0% {
    transform: scale(0.9);
    opacity: 0.8;
  }
  70% {
    transform: scale(1.1);
    opacity: 0.15;
  }
  100% {
    transform: scale(1.18);
    opacity: 0;
  }
}

/* SCENE 引导区域开关：旋钮改为黑色 */
.scene-guide-switch .el-switch__action {
  background-color: #000 !important;
}
</style>

