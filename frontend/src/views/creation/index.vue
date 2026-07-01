<template>
  <div class="h-full flex flex-col bg-[#0A0A0A]">
    <CreationTopBar
        v-model:chat-title="chatTitle"
        :session-id="sessionId"
        :can-edit-creation="canEditCreation"
    />

    <!-- 中间主体区域 -->
    <div class="flex-1 flex min-h-0 overflow-hidden">
      <HistorySidebar
          :active-session-id="sessionId"
          :can-edit-creation="canEditCreation"
          @create-new="handleCreateNewChat"
          @select-chat="handleSelectChat"
          @renamed="handleHistoryRenamed"
          @deleted="handleHistoryDeleted"
      />

      <CreationChatPanel
          ref="chatPanelRef"
          :session-id="sessionId"
          :content-width-class="contentWidthClass"
          :loading-messages="loadingMessages"
          :loading="loading"
          :messages="messages"
          :current-ai-message-id="currentAIMessageId"
          :can-edit-creation="canEditCreation"
          :show-feedback="showFeedback"
          :player-state="playerState"
          :audio-player-ref="audioPlayerRef"
          :pause-all-videos="pauseAllVideos"
          v-model:show-music-trimmer="showMusicTrimmer"
          :mv-raw-file="mvRawFile"

          @chat-scroll="handleChatScroll"
          @open-storyboard="handleOpenStoryboard"
          @trimmer-play-start="handleTrimmerPlayStart"
          @trimmer-confirm="handleTrimmerConfirm"
          @send="handleSend"
          @action-required-clarify="handleClarifyActionRequired"
      />

      <!-- 右侧边栏 -->
      <CreationDetailSidebar
          v-show="showDetailPanel"
          v-bind="detailSidebarProps"
          @close="handleCloseDetailPanel"
          @open-scene-edit="handleOpenSceneEdit"
          @remake-video="handleRemakeVideo"
          @prepare-mv="handlePrepareMV"
      />
    </div>

    <!-- 全局隐藏 audio 元素 -->
    <audio ref="audioPlayerRef" @timeupdate="onAudioTimeUpdate" @loadedmetadata="onAudioLoadedMetadata" @ended="onAudioEnded" @error="onAudioError"/>

    <StoryboardSceneEditDialog
        v-model="sceneEditVisible"
        :scene="editingScene"
        :scene-index="editingSceneIndex"
        :can-edit-creation="canEditCreation"
        @save="handleSaveScene"
    />

  </div>
</template>

<script setup>
import {ref, computed, reactive, nextTick, onMounted, onUnmounted} from 'vue';
import {ElMessage} from 'element-plus';
import {useRoute, useRouter, onBeforeRouteLeave} from 'vue-router';
import {useUserStore} from '@/store/user';
import {useModelStore} from '@/store/model';
import {
  getHistoryMessage,
  uploadFile,
  getVideoMeta,
  remakeVideoSubmit,
} from '@/api/creation';
import {
  isNotEmpty,
} from '@/utils/index.js';
import {appendBlock} from '@/views/creation/utils/creationMessageBlocks';
import CreationTopBar from './components/CreationTopBar.vue';
import HistorySidebar from './components/HistorySidebar.vue';
import StoryboardSceneEditDialog from './components/StoryboardSceneEditDialog.vue';
import CreationDetailSidebar from './components/CreationDetailSidebar.vue';
import CreationChatPanel from './components/CreationChatPanel.vue';
import {creationBus} from './creationBus';
import {useCreationTaskControllers} from '@/views/creation/composables/useCreationTaskControllers';
import {useCreationPendingDirectEditTasks} from '@/views/creation/composables/useCreationPendingDirectEditTasks';
import {useCreationSubjectEditStore} from '@/store/creationSubjectEdit';
import {useCreationTypewriter} from '@/views/creation/composables/useCreationTypewriter';
import {useCreationChatSse} from '@/views/creation/composables/useCreationChatSse';
import {extractUserAttachmentsFromChunks} from '@/views/creation/utils/creationUserAttachments';
import {convertMessageChunks} from '@/views/creation/utils/creationMessageChunks';
import {createCreationSseEventApplier} from '@/views/creation/utils/creationSseEventApplier';
import {createAiMessage, getLastAiMessage} from '@/views/creation/utils/creationAiMessage';
import {useI18nText} from '@/i18n';
import {saveUserTracking} from '@/api/tracking.js';

// ─────────────────────────────────────────────
// 基础依赖
// ─────────────────────────────────────────────

const userStore = useUserStore();
const {t} = useI18nText();

// ─────────────────────────────────────────────
// 响应式状态
// ─────────────────────────────────────────────

const defaultChatTitle = computed(() => t('creation.defaultChatTitle'));
const sessionId = ref('');           // 当前会话 ID
const chatTitle = ref(defaultChatTitle.value);      // 当前会话标题
const loading = ref(false);          // SSE 回复进行中
const loadingMessages = ref(false);  // 历史消息加载中
const messages = ref([]);            // 消息列表
const currentAIMessageId = ref(null); // 当前 SSE 对应的 AI 消息 ID
const showDetailPanel = ref(false);  // 右侧详情栏是否显示
const showMusicTrimmer = ref(false); // MV 音乐剪辑器是否显示
const mvRawFile = ref(null);         // 制作 MV 时的原始音频文件

const modelStore = useModelStore();

let pointsRefreshTimer = null; // 积分刷新定时器，确保同一时刻仅有一个刷新任务

/**
 * 清理积分刷新定时器，避免重复调度。
 */
const clearPointsRefreshTimer = () => {
  if (!pointsRefreshTimer) return;
  clearTimeout(pointsRefreshTimer);
  pointsRefreshTimer = null;
};

/**
 * 在对话结束后延迟刷新一次积分。
 * 使用单定时器覆盖策略，避免 COMPLETE 与 onClose 重复触发导致的并发请求。
 */
const schedulePointsRefresh = () => {
  if (!userStore.isLoggedIn) return;

  clearPointsRefreshTimer();
  pointsRefreshTimer = setTimeout(async () => {
    try {
      await userStore.fetchUserPlan();
    } catch (error) {
      ElMessage.error(error?.message || t('creation.pointsRefreshFail'));
      console.error(error);
    } finally {
      pointsRefreshTimer = null;
    }
  }, 800);
};

/**
 * 统一处理“对话结束”收尾逻辑：
 * - 关闭 loading
 * - 清空当前流式消息标识
 * - 刷新顶部积分
 */
const handleChatFinished = () => {
  flushTypewriter();
  loading.value = false;
  currentAIMessageId.value = null;
  schedulePointsRefresh();
};

// 播放器全局 audio 元素引用
const audioPlayerRef = ref(null);
// 用于在页面级任务恢复时调用 CreationChatPanel.handleEditContextChange，
// 让自动应用的新版本能够进入 editContext。
const chatPanelRef = ref(null);

// 播放器状态
const playerState = reactive({
  currentAudio: null,  // 当前播放的音频对象（含 lyrics、style 字段）
  currentIndex: -1,    // 在 audioList 中的索引
  audioList: [],       // 当前消息的音频列表
  isPlaying: false,
  currentTime: 0,
  duration: 0,
});

/**
 * 停止音频播放器。
 * 用于切换会话、创建新项目、关闭右侧栏、打开分镜编辑等场景。
 */
const stopAudioPlayer = () => {
  const audio = audioPlayerRef.value;
  if (audio && playerState.isPlaying) {
    audio.pause();
    playerState.isPlaying = false;
  }
};

/**
 * 暂停页面内所有视频，避免与音乐同时播放。
 */
const pauseAllVideos = () => {
  const videos = document.querySelectorAll('video');
  videos.forEach((video) => {
    if (!video.paused) video.pause();
  });
};

/**
 * 重置右侧栏相关状态（关闭侧边栏、退出分镜模式、停止播放）。
 * 用于创建新项目、切换会话等需要完全重置的场景。
 */
const resetSidePanelState = () => {
  stopAudioPlayer();
  showMusicTrimmer.value = false;
  showDetailPanel.value = false;
  storyboardMode.value = false;
};

const historySnapshot = ref([]);

// 分镜编辑状态
const storyboardMode = ref(false);       // 右侧栏是否处于分镜编辑模式
const storyboardVideo = ref(null);       // 当前分镜的元数据 { mvId, scenes, fileId, fileUrl }
const storyboardScenes = ref([]);        // 分镜场景列表
const storyboardLoading = ref(false);    // 加载分镜中
const storyboardRemaking = ref(false);   // 重新生成中

// 任务控制器抽离：中止/轮询等逻辑从视图编排中解耦
const {
  taskControllers,
  registerTaskController,
  resetStreamingState,
  pollRemakeTask,
  cleanupTaskControllerIfSame,
} = useCreationTaskControllers({
  creationBus,
  loading,
  loadingMessages,
  currentAIMessageId,
  storyboardRemaking,
});

const {resumeAllPendingDirectEditTasks} = useCreationPendingDirectEditTasks();
const subjectEditStore = useCreationSubjectEditStore();

const sceneEditVisible = ref(false);     // 分镜编辑弹窗
const editingScene = ref(null);          // 当前编辑的场景
const editingSceneIndex = ref(-1);       // 当前编辑分镜索引
const TEMP_AUDIO_FILE_ID_PREFIX = 'temp_audio_';


// 创作者身份（由首页预览/分享数据接口新增 userId 提供）
const creatorUserId = ref('');

// ─────────────────────────────────────────────
// 计算属性
// ─────────────────────────────────────────────

/**
 * 当前登录用户 ID。
 * 兼容不同登录来源可能返回的字段命名。
 */
const currentUserId = computed(() => {
  return userStore.userInfo?.id;
});

/** 当前是否处于分享会话上下文（URL 携带 creatorUserId） */
const hasShareCreatorContext = computed(() => isNotEmpty(creatorUserId.value));

/** 当前登录用户是否为该会话创作者 */
const isCreator = computed(() =>
    hasShareCreatorContext.value
    && isNotEmpty(currentUserId.value)
    && String(currentUserId.value) === String(creatorUserId.value),
);

/**
 * 创作页面是否允许编辑/生成。
 * 规则：
 * - 普通创作上下文：允许编辑；
 * - 分享上下文：未登录或非创作者均不可编辑，只有创作者本人可编辑。
 */
const canEditCreation = computed(() => {
  return !hasShareCreatorContext.value || (userStore.isLoggedIn && isCreator.value);
});

/**
 * 反馈组件显示规则：
 * - 分享上下文且未登录/非作者：不显示反馈；
 * - 其它场景：显示反馈。
 */
const showFeedback = computed(() => {
  if (!hasShareCreatorContext.value) return true;
  return userStore.isLoggedIn && isCreator.value;
});

const contentWidthClass = computed(() => showDetailPanel.value ? 'w-[600px]' : 'w-[800px]');
const detailSidebarProps = computed(() => ({
  storyboardMode: storyboardMode.value,
  storyboardLoading: storyboardLoading.value,
  storyboardScenes: storyboardScenes.value,
  storyboardRemaking: storyboardRemaking.value,
  canEditCreation: canEditCreation.value,
  playerState,
  audioPlayerRef: audioPlayerRef.value,
  pauseAllVideos,
  chatTitle: chatTitle.value,
}));

const AUTO_SCROLL_THRESHOLD = 80;
const shouldAutoScrollToBottom = ref(true);
const chatCenterElRef = ref(null);
const composerElRef = ref(null);

/** @returns {HTMLElement|null} */
const getChatCenterEl = () => chatCenterElRef.value;
/** @returns {HTMLElement|null} */
const getComposerEl = () => composerElRef.value;

/**
 * CreationChatPanel 挂载后回传中间区与 composer DOM，供滚动与滚轮代理使用。
 * @param {{chatCenterEl?: HTMLElement|null, composerEl?: HTMLElement|null}} param0
 * @returns {void}
 */
const handleChatElements = ({chatCenterEl = null, composerEl = null} = {}) => {
  chatCenterElRef.value = chatCenterEl;
  composerElRef.value = composerEl;
};

/** 打开右侧栏并切到音乐详情（退出分镜模式）。 */
const handleDetailOpenAudio = () => {
  storyboardMode.value = false;
  showDetailPanel.value = true;
};

/** 关闭右侧栏并退出分镜模式。 */
const handleDetailClose = () => {
  showDetailPanel.value = false;
  storyboardMode.value = false;
};

/**
 * HistorySidebar 广播的列表快照，供 `findHistoryItemById` 等使用。
 * @param {any[]} list
 * @returns {void}
 */
const handleHistorySnapshot = (list) => {
  historySnapshot.value = Array.isArray(list) ? list : [];
};

/**
 * @param {string} id
 * @returns {any|undefined}
 */
const findHistoryItemById = (id) => historySnapshot.value.find(item => item.id === id);

/**
 * 等待左侧历史组件完成首次加载（或超时），避免依赖未就绪的列表数据。
 * @param {number} [timeout=8000]
 * @returns {Promise<void>}
 */
const waitForHistoryInitialized = (timeout = 8000) => new Promise((resolve) => {
  let resolved = false;
  const timer = setTimeout(() => {
    if (resolved) return;
    resolved = true;
    creationBus.off('history:initialized', onInitialized);
    resolve();
  }, timeout);
  const onInitialized = () => {
    if (resolved) return;
    resolved = true;
    clearTimeout(timer);
    creationBus.off('history:initialized', onInitialized);
    resolve();
  };
  creationBus.on('history:initialized', onInitialized);
  creationBus.emit('history:initialize');
});


/**
 * 将消息区域滚动到底部（等待 DOM 更新后执行）
 */
const isNearChatBottom = () => {
  const container = getChatCenterEl();
  if (!container) return true;
  return container.scrollHeight - container.scrollTop - container.clientHeight <= AUTO_SCROLL_THRESHOLD;
};

const handleChatScroll = () => {
  shouldAutoScrollToBottom.value = isNearChatBottom();
};

/**
 * 滚动消息列表到底部；`force` 时忽略用户是否曾上滑离开底部。
 * @param {boolean} [force=false]
 * @returns {void}
 */
const scrollToBottom = (force = false) => {
  nextTick(() => {
    const container = getChatCenterEl();
    if (!container) return;
    if (!force && !shouldAutoScrollToBottom.value) return;
    container.scrollTop = container.scrollHeight;
    shouldAutoScrollToBottom.value = true;
  });
};

/**
 * 中央聊天区域滚轮代理：仅当鼠标位于"中间聊天区域"内时，
 * 允许滚轮同时驱动消息列表（即使不在消息列表容器上）。
 * 这样不会影响左侧历史列表和右侧边栏自身滚动。
 * @param {WheelEvent} event
 */
const handleGlobalWheelForMessages = (event) => {
  const container = getChatCenterEl();
  if (!container) return;

  const target = event.target;
  const inCenterArea = target instanceof Node && container.contains(target);
  if (!inCenterArea) return;

  // 输入区域（含输入框及其容器）保持原生滚动行为，避免滚轮被消息区劫持
  if (
      target instanceof Element &&
      (
          target.closest('textarea, input, .el-textarea, .el-textarea__inner, .el-input, .el-input__inner, [contenteditable="true"]') ||
          (getComposerEl() && getComposerEl().contains(target))
      )
  ) {
    return;
  }

  // 这些浮层或弹窗内保持自身滚动行为，避免冲突
  if (
      target instanceof Element &&
      target.closest('.el-popper, .el-select-dropdown, .el-message-box, .el-dialog')
  ) {
    return;
  }

  const maxScrollTop = container.scrollHeight - container.clientHeight;
  if (maxScrollTop <= 0) return;

  const prev = container.scrollTop;
  const next = Math.min(maxScrollTop, Math.max(0, prev + event.deltaY));
  if (next !== prev) {
    container.scrollTop = next;
    event.preventDefault();
  }
};

// appendBlock/toArray 从 utils 抽离

// ─────────────────────────────────────────────
// 打字机效果
// ─────────────────────────────────────────────
const {
  setTypewriterBlock,
  enqueueTypewriterText,
  flushTypewriter,
  resetTypewriter,
} = useCreationTypewriter({scrollToBottom});

/**
 * SSE ERROR 事件：写入占位文案、提示并结束 loading。
 * @param {any} msg
 * @param {any} [payload={}]
 * @returns {void}
 */
const handleSSEErrorEvent = (msg, payload = {}) => {
  flushTypewriter();

  const errorText = payload.text || t('creation.requestFailRetry');
  if (!msg.content) {
    appendBlock(msg, 'TEXT', errorText);
    msg.content = errorText;
  }

  ElMessage.error(errorText);
  handleChatFinished();
};

/**
 * 将一条 SSE 事件应用到指定的 AI 消息对象上。
 * 所有 SSE 流（新发送、断线续传）都复用此函数，避免重复逻辑。
 * @param {Object} msg - 要更新的 AI 消息对象
 * @param {{type:string, data:any}} event - SSE 事件
 */
const applySSEEvent = createCreationSseEventApplier({
  appendBlock,
  flushTypewriter,
  enqueueTypewriterText,
  setTypewriterBlock,
  creationBus,
  scrollToBottom,
  handleChatFinished,
  handleSSEErrorEvent,
  messages,
  onAssetGenerated: schedulePointsRefresh,
});

/**
 * 关闭右侧详情侧边栏，并退出分镜编辑模式，同时停止播放。
 */
const handleCloseDetailPanel = () => {
  stopAudioPlayer();
  showDetailPanel.value = false;
  storyboardMode.value = false;
};

// ─────────────────────────────────────────────
// 历史会话列表
// ─────────────────────────────────────────────

/**
 * 点击"创建新项目"：先打点（CREATE_NEW_PROJECT），再重置当前对话状态进入空白对话。
 */
const handleCreateNewChat = () => {
  saveUserTracking({target: 'CREATE_NEW_PROJECT'}).catch((error) => {
    console.error('创作页创建新项目埋点上报失败:', error);
  });
  resetCurrentChat();
  ElMessage.success(t('creation.newChatCreated'));
};

/**
 * 侧栏重命名后若当前会话命中则同步标题。
 * @param {{id: string, name: string}} param0
 * @returns {void}
 */
const handleHistoryRenamed = ({id, name}) => {
  if (sessionId.value === id) chatTitle.value = name;
};

/**
 * 删除历史项：若删的是当前会话则重置聊天区。
 * @param {any} item
 * @returns {void}
 */
const handleHistoryDeleted = (item) => {
  if (sessionId.value === item.id) {
    resetCurrentChat();
  }
};

/**
 * 点击历史列表中的某条会话：切换到该会话并加载其历史消息。
 * 若该会话的 SSE 未完成，则自动续连。
 * @param {{id:string, name:string}} item - 历史会话项
 * @param {boolean} [preserveCreatorContext=false] - 是否保留外部传入的 creatorUserId
 */
const handleSelectChat = async (item, preserveCreatorContext = false) => {
  // 点击历史项目时明确进入"当前会话上下文"，清空外部传入的 creatorUserId
  if (!preserveCreatorContext) {
    creatorUserId.value = '';
  }

  // 切换会话前中止所有流并重置发送态，避免旧会话阻塞/污染新会话
  resetTypewriter();
  resetStreamingState();

  // 关闭右侧栏并重置相关状态（含停止播放）
  resetSidePanelState();

  sessionId.value = item.id;
  chatTitle.value = item.name;
  creationBus.emit('chat:draft:clear');
  const nextQuery = preserveCreatorContext && isNotEmpty(creatorUserId.value)
      ? {sessionId: item.id, creatorUserId: creatorUserId.value}
      : {sessionId: item.id};
  const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
  router.replace({name: 'creation', params: {lang}, query: nextQuery});
  loadingMessages.value = true;
  messages.value = [];

  try {
    const data = await getHistoryMessage(item.id);

    // 已有会话锁定其分辨率，所有模型选择框据此过滤可选模型
    modelStore.setConversationResolution(data.resolution);

    messages.value = (data.chatMessages || []).map(msg => {
      const {content, audioList, videoList, subjectList, sceneList, blocks} = convertMessageChunks(msg.messageChunks);
      const mapped = {
        messageId: msg.messageId,
        senderType: msg.senderType,
        content,
        audioList,
        videoList,
        subjectList,
        sceneList,
        blocks,
        sessionId: item.id,
      };

      if (msg.senderType === 'USER') {
        const attachments = extractUserAttachmentsFromChunks(msg.messageChunks);
        if (attachments.audio || attachments.images.length > 0) {
          mapped.attachments = attachments;
        }
      }

      return mapped;
    });

    scrollToBottom(true);

    // 历史消息加载完后，统一扫描 SUBJECT / SCENE 是否有进行中的生成新版本任务，
    // 命中则后台续上轮询。版本变更记录由 store 在轮询成功时直接处理，无需回调。
    resumeAllPendingDirectEditTasks(item.id, messages.value);

    // 历史消息回显：如果最后已出现 SUBJECT，则初始化一次 SUBJECT 等待确认状态
    const lastSubjectMsg = [...messages.value].reverse().find(m => Array.isArray(m.subjectList) && m.subjectList.length > 0);
    if (lastSubjectMsg) {
      creationBus.emit('subject:received', {msgId: lastSubjectMsg.messageId});
    } else {
      creationBus.emit('subject:reset');
    }
    // blocks 由 createAiMessage 初始化为数组
    const lastSceneScriptMsg = [...messages.value].reverse().find(
        (m) => m.blocks?.some((b) => b?.type === 'SCENE_SCRIPT'),
    );
    if (lastSceneScriptMsg) {
      creationBus.emit('scene-script:received', {msgId: lastSceneScriptMsg.messageId});
    } else {
      creationBus.emit('scene-script:reset');
    }
    // sceneList 由 createAiMessage 初始化为数组；这里只判 length 即可
    const lastSceneMsg = [...messages.value].reverse().find((m) => m.sceneList?.length > 0);
    if (lastSceneMsg) {
      creationBus.emit('scene:received', {msgId: lastSceneMsg.messageId});
    } else {
      creationBus.emit('scene:reset');
    }

    // 服务端标记该会话尚未结束，恢复 SSE 续连
    if (data.resumeChat && data.lastMessageId) {
      handleResumeChat(data.sessionId, data.lastMessageId);
    }
  } catch (error) {
    ElMessage.error(error?.message || t('creation.historyLoadFail'));
    messages.value = [];
  } finally {
    loadingMessages.value = false;
  }
};

// ─────────────────────────────────────────────
// 消息发送与 SSE（已拆分到 composable）
// ─────────────────────────────────────────────

// ─────────────────────────────────────────────
// 对话标题编辑
// ─────────────────────────────────────────────

/**
 * 清空当前会话状态、消息列表与 URL query，并广播重置各阶段「继续」流程。
 * @returns {void}
 */
const resetCurrentChat = () => {
  resetTypewriter();
  resetStreamingState();
  resetSidePanelState();
  creatorUserId.value = '';
  sessionId.value = '';
  chatTitle.value = defaultChatTitle.value;
  messages.value = [];
  // 新会话解除分辨率锁定，模型选项跟随用户当前所选分辨率
  modelStore.setConversationResolution(null);
  creationBus.emit('subject:reset');
  creationBus.emit('scene-script:reset');
  creationBus.emit('scene:reset');
  creationBus.emit('chat:draft:clear');
  const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
  router.replace({name: 'creation', params: {lang}});
};

// ─────────────────────────────────────────────
// 播放器
// ─────────────────────────────────────────────

/** audio 元素 timeupdate 事件：同步当前播放时间到 playerState */
const onAudioTimeUpdate = () => {
  const audio = audioPlayerRef.value;
  if (!audio) return;
  playerState.currentTime = audio.currentTime;
};

/** audio 元素 loadedmetadata 事件：获取总时长 */
const onAudioLoadedMetadata = () => {
  const audio = audioPlayerRef.value;
  if (!audio) return;
  playerState.duration = audio.duration;
};

/** audio 元素播放结束：重置播放状态 */
const onAudioEnded = () => {
  playerState.isPlaying = false;
  playerState.currentTime = 0;
};

/** audio 元素播放出错：重置播放状态 */
const onAudioError = () => {
  playerState.isPlaying = false;
};

/**
 * 裁剪器开始播放时：立即暂停右侧播放栏音频，确保页面同一时刻仅一个音频播放。
 */
const handleTrimmerPlayStart = () => {
  stopAudioPlayer();
  pauseAllVideos();
};

// ─────────────────────────────────────────────
// 制作 MV
// ─────────────────────────────────────────────

/**
 * 点击"制作 MV"：仅校验当前音频源后打开裁剪器。
 * 统一由 MusicTrimmer 处理本地/远程音频读取，避免入口预取导致的 blob 失效噪音。
 */
const handlePrepareMV = async () => {
  if (!canEditCreation.value) return;
  const currentAudio = playerState.currentAudio;
  const currentAudioUrl = String(currentAudio?.audioUrl || '');
  if (!currentAudio || !currentAudioUrl) {
    ElMessage.warning(t('creation.selectAudioFirst'));
    return;
  }

  // Reason: blob URL 跨会话/刷新后可能失效，先做一次可用性探测，避免打开裁剪器后持续报错刷屏。
  if (currentAudioUrl.startsWith('blob:')) {
    try {
      const response = await fetch(currentAudioUrl);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
    } catch {
      ElMessage.warning(t('creation.audioSourceExpired'));
      return;
    }
  }

  if (Number(playerState.duration) > 0 && Number(playerState.duration) < 10) {
    ElMessage.warning(t('creation.audioTooShort'));
    return;
  }

  mvRawFile.value = null;
  showMusicTrimmer.value = true;
};

/**
 * 裁剪器确认回调：上传裁剪后的音频，然后仅回填到底部输入框，不自动发送消息。
 * @param {{blob:Blob, timeRange:string}} param
 */
const handleTrimmerConfirm = async ({blob, timeRange, durationSeconds}) => {
  showMusicTrimmer.value = false;
  const tempFileId = `${TEMP_AUDIO_FILE_ID_PREFIX}${Date.now()}`;
  const fileName = `mv_trimmed_${timeRange}.mp3`;
  // 制作 MV 流程：把当前播放音频的歌词随草稿一起带到 composer，发送时作为 audioLyrics 发往后端
  // 数据源：AUDIO chunk 的 lyrics 字段（见数据结构示例.txt），通过 playerState.currentAudio 取
  const audioLyrics = playerState.currentAudio?.lyrics || '';

  // 先显示占位音频卡片，上传完成后再回填真实 fileId。
  creationBus.emit('chat:draft:seed', {
    audio: {fileId: tempFileId, fileName, fileUrl: '', durationSeconds, audioLyrics},
  });

  try {
    const formData = new FormData();
    formData.append('file', new File([blob], fileName, {type: 'audio/mpeg'}));
    const data = await uploadFile(formData);

    creationBus.emit('chat:draft:seed', {
      audio: {fileId: data.fileId, fileName, fileUrl: data.fileUrl || '', durationSeconds, audioLyrics},
    });
  } catch {
    creationBus.emit('chat:draft:seed', {audio: {}});
    ElMessage.error(t('creation.makeMvFail'));
  }
};

// ─────────────────────────────────────────────
// 分镜编辑
// ─────────────────────────────────────────────

/**
 * 点击视频卡片右上角按钮：打开分镜编辑侧边栏，加载该视频的元信息
 * getVideoMeta 返回 { mvId, scenes, fileId, fileUrl }，将整个响应存入 storyboardVideo
 */
const handleOpenStoryboard = async (video) => {
  if (!canEditCreation.value) return;
  // 打开分镜编辑时停止当前播放的音频
  stopAudioPlayer();
  pauseAllVideos();
  try {
    storyboardLoading.value = true;
    storyboardScenes.value = [];
    storyboardMode.value = true;
    showDetailPanel.value = true;
    const data = await getVideoMeta(video.videoFileId);
    // 保存完整元数据（含 mvId），场景带 reMake=false 初始状态
    storyboardVideo.value = data;
    storyboardScenes.value = data.scenes.map(s => ({...s, reMake: false}));
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    storyboardLoading.value = false;
  }
};

/**
 * 点击某个分镜：打开编辑弹窗，初始化提示词和参考图，并获取预消耗积分
 */
const handleOpenSceneEdit = (scene, idx) => {
  if (!canEditCreation.value) return;
  editingScene.value = scene;
  editingSceneIndex.value = idx;
  sceneEditVisible.value = true;
};

/**
 * 弹窗保存：将编辑后的提示词、参考图与模型写回场景对象（本地），并标记该分镜已修改
 */
const handleSaveScene = ({sceneId, visualPrompt, subject, model}) => {
  if (!canEditCreation.value) return;
  const targetScene = storyboardScenes.value.find(scene => scene.sceneId === sceneId);
  if (!targetScene) return;
  targetScene.visualPrompt = visualPrompt;
  targetScene.subject = [...subject];
  targetScene.model = model ? String(model) : 'VIDUQ2';
  targetScene.reMake = true;
};

/**
 * 重新生成分镜（异步）：提交任务后按固定间隔查询，直到成功/失败/取消
 */
const handleRemakeVideo = async () => {
  if (!canEditCreation.value) return;
  if (!storyboardScenes.value.length) {
    ElMessage.warning(t('creation.noStoryboardData'));
    return;
  }

  const hasEditedScene = storyboardScenes.value.some(scene => scene.reMake);
  if (!hasEditedScene) {
    ElMessage.warning(t('creation.editSceneFirst'));
    return;
  }

  storyboardRemaking.value = true;
  const controller = registerTaskController('remake');

  try {
    const taskId = await remakeVideoSubmit({
      mvId: storyboardVideo.value?.mvId || '',
      scenes: storyboardScenes.value.map(scene => ({
        reMake: scene.reMake,
        sceneId: scene.sceneId,
        model: scene?.model ? String(scene.model) : 'VIDUQ2',
        visualPrompt: scene.visualPrompt,
        subject: (scene.subject || []).map(s => ({
          subjectId: s.subjectId,
          fileId: s.fileId,
          fileUrl: s.fileUrl,
        })),
      })),
    });

    if (!taskId) {
      throw new Error(t('creation.taskSubmitFail'));
    }

    const finalVideoItem = await pollRemakeTask(taskId, controller);

    // 直接把最终视频放入消息列表（复用 AI 消息卡片渲染）
    // AI 消息由 createAiMessage 初始化 videoList 为 array；老历史结构兜空
    const lastAiMsg = getLastAiMessage(messages.value);
    if (lastAiMsg) {
      lastAiMsg.videoList ||= [];
      lastAiMsg.videoList.push(finalVideoItem);
    } else {
      // messageId 后端契约为 string，本地占位也用 string 保持单一类型
      messages.value.push(createAiMessage(String(Date.now()), {videoList: [finalVideoItem], blocks: []}));
    }

    // 完成后自动触发分镜编辑，拉取新视频元信息并刷新分镜
    await handleOpenStoryboard(finalVideoItem);

    // 场景已提交并完成处理，清空改动标记
    storyboardScenes.value.forEach(scene => {
      if (scene.reMake) scene.reMake = false;
    });

    ElMessage.success(t('creation.remakeSuccess'));
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }
    ElMessage.error(error?.message || t('creation.remakeFail'));
  } finally {
    cleanupTaskControllerIfSame('remake', controller);
    storyboardRemaking.value = false;
  }
};

const route = useRoute();
const router = useRouter();

const {handleSend, handleResumeChat, handleClarifyActionRequired} = useCreationChatSse({
  userStore,
  router,
  creationBus,
  sessionId,
  chatTitle,
  messages,
  currentAIMessageId,
  loading,
  applySSEEvent,
  handleChatFinished,
  registerTaskController,
  taskControllers,
  scrollToBottom,
  createAiMessage,
  appendBlock,
});

// ─────────────────────────────────────────────
// 页面初始化
// ─────────────────────────────────────────────

onMounted(async () => {
  saveUserTracking({
    target: 'ENTER_CREATE_CHAT',
  }).catch((error) => {
    console.error('进入创作对话页埋点上报失败:', error);
  });

  // 创作页顶部要展示积分余额，直接落地此页时（或退出登录后刷新留在此页）userPlan
  // 还是 null，会显示 0。跟首页 / 移动端创作页一致主动拉一次。
  if (userStore.isLoggedIn) {
    userStore.fetchUserPlan();
  }

  window.addEventListener('wheel', handleGlobalWheelForMessages, {passive: false});

  creationBus.on('chat:elements', handleChatElements);
  creationBus.emit('chat:elements:request');
  creationBus.on('detail:open-audio', handleDetailOpenAudio);
  creationBus.on('detail:close', handleDetailClose);
  creationBus.on('history:snapshot', handleHistorySnapshot);

  await waitForHistoryInitialized();

  const {
    sessionId: targetSessionId,
    prompt,
    audioFileId,
    fileName,
    audioUrl,
    images: imagesJson,
    creatorUserId: queryCreatorUserId,
  } = route.query;

  const hasCreatorContext = isNotEmpty(queryCreatorUserId);
  if (hasCreatorContext) {
    creatorUserId.value = String(queryCreatorUserId);
  }

  // prompt 参数消费后立即清除，防止刷新重复发送；保留 sessionId / creatorUserId 刷新上下文
  if (prompt) {
    const nextQuery = {};
    if (isNotEmpty(targetSessionId)) nextQuery.sessionId = targetSessionId;
    if (isNotEmpty(queryCreatorUserId)) nextQuery.creatorUserId = queryCreatorUserId;
    const lang = typeof router.currentRoute.value?.params?.lang === 'string' ? router.currentRoute.value.params.lang : 'zh';
    router.replace({name: 'creation', params: {lang}, query: nextQuery});
  }

  // 来自资源页「创作过程」跳转或刷新恢复：携带 sessionId，自动打开对应会话
  if (targetSessionId) {
    const target = findHistoryItemById(targetSessionId) || {id: targetSessionId, name: t('creation.chatFallbackTitle')};

    // 刷新恢复：若 URL 带 creatorUserId，保留只读上下文并直接加载会话
    if (hasCreatorContext) {
      sessionId.value = targetSessionId;
      chatTitle.value = target.name || t('creation.chatFallbackTitle');
      await handleSelectChat(target, true);
      return;
    }

    // 非外部只读上下文，按普通历史会话处理（会清空 creatorUserId）
    await handleSelectChat(target);
    return;
  }

  // 来自首页「一键创作」跳转：携带 prompt/model/audioFileId/images，自动填充并发送
  if (prompt) {
    let draftImages = [];
    if (imagesJson) {
      try {
        draftImages = JSON.parse(imagesJson) || [];
      } catch { /* ignore parse errors */
      }
    }
    creationBus.emit('chat:draft:seed', {
      prompt,
      audio: audioFileId
          ? {fileId: audioFileId, fileName: fileName || t('creation.uploadedAudioFallback'), fileUrl: audioUrl || ''}
          : null,
      images: draftImages,
    });
    creationBus.emit('chat:submit');
  }
});

onBeforeRouteLeave(() => {
  creationBus.emit('chat:draft:clear');
  resetTypewriter();
  resetStreamingState();
});

onUnmounted(() => {
  creationBus.off('chat:elements', handleChatElements);
  creationBus.off('detail:open-audio', handleDetailOpenAudio);
  creationBus.off('detail:close', handleDetailClose);
  creationBus.off('history:snapshot', handleHistorySnapshot);
  creationBus.emit('chat:draft:clear');
  resetTypewriter();
  clearPointsRefreshTimer();
  window.removeEventListener('wheel', handleGlobalWheelForMessages);
  resetStreamingState();
  // 停止 SUBJECT/SCENE 直连编辑任务的后台轮询，避免离开页面后继续烧网络；
  // 任务数据保留，下次回到页面 resumeAllPendingDirectEditTasks 会续上并补记录版本变更
  subjectEditStore.stopAllPolling();
  // 离开创作页解除会话分辨率锁定，避免泄漏到首页等其它页面的模型选项
  modelStore.setConversationResolution(null);
});

</script>
