<template>
  <div class="w-[240px] shrink-0 flex flex-col bg-[#0A0A0A] px-4 py-[34px]">
    <!-- 创建新项目按钮 -->
    <div
        class="w-full h-[46px] mb-5 bg-[rgba(240,246,221,0.1)] rounded-[10px] flex-center gap-[14px] text-white border border-[rgba(240,246,221,0.5)] cursor-pointer hover:bg-[rgba(240,246,221,0.15)] transition-colors"
        @click="emit('create-new')"
    >
      <svg-icon name="gy-New" size="14" color="white"></svg-icon>
      <span class="text-[14px]">{{ t('creation.history.newProject') }}</span>
    </div>

    <!-- 搜索框 -->
    <div class="h-[46px] rounded-[10px] bg-[rgba(240,246,221,0.5)] flex-center gap-[14px]">
      <svg-icon class="!text-[#F0F6DD]" name="gy-search" size="14" color="#F0F6DD"></svg-icon>
      <input
          v-model.trim="sessionName"
          type="text"
          :placeholder="t('creation.history.searchPlaceholder')"
          class="w-[84px] h-[22px] text-[14px] bg-transparent text-white placeholder-[#F0F6DD] outline-none transition-colors"
      />
    </div>

    <!-- 历史项目标题 -->
    <div class="mt-5 mb-[10px] text-white text-[18px] leading-[26px]">{{ t('creation.history.title') }}</div>

    <!-- 项目列表 -->
    <div ref="historyListRef" class="flex-1 overflow-y-hidden hover:overflow-y-auto custom-scrollbar">
      <!-- 加载状态（仅首次加载显示） -->
      <div v-if="loadingHistory && currentPage === 1 && isEmpty(chatHistory)" class="flex-col-center py-8 text-gray-500">
        <el-icon :size="32" class="animate-spin mb-2">
          <Loading/>
        </el-icon>
        <span class="text-sm">{{ t('common.loading') }}</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="isEmpty(chatHistory) && !loadingHistory" class="flex-col-center py-8 text-gray-500">
        <el-icon :size="48" class="mb-2">
          <FolderOpened/>
        </el-icon>
        <span class="text-sm">{{ isNotEmpty(sessionName) ? t('creation.history.noMatch') : t('creation.history.empty') }}</span>
      </div>

      <!-- 列表 -->
      <template v-else>
        <div
            v-for="item in chatHistory"
            :key="item.id"
            :class="[
              'group mb-[10px] px-2 py-2 rounded-[10px] cursor-pointer transition-all h-[64px] flex items-center gap-[6px]',
              item.id === activeSessionId ? 'bg-[rgba(194,255,0,0.2)] border border-[#C2FF00]' : 'hover:bg-[#1a1a1a] border border-transparent',
            ]"
            :title="item.name"
            @click="emit('select-chat', item)"
        >
          <img :src="item.cover" class="w-[48px] h-[48px] rounded-[4px] object-cover flex-shrink-0"/>
          <div class="flex-1 min-w-0 flex flex-col justify-center">
            <div class="text-white text-[16px] leading-[26px] truncate">
              {{ item.name }}
            </div>
            <div class="text-gray-400 text-[12px] leading-[20px] mt-[2px] whitespace-nowrap">
              {{ item.date }}
            </div>
          </div>
          <el-dropdown
              v-if="canEditCreation"
              trigger="click"
              placement="bottom-end"
              popper-class="creation-history-dropdown"
              :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }"
          >
            <div
                :class="[
                  'w-[32px] h-[32px] rounded-[10px] flex-center shrink-0 transition-all',
                  item.id === activeSessionId ? 'opacity-100 bg-white/10' : 'opacity-0 group-hover:opacity-100 hover:bg-white/10',
                ]"
                @click.stop
            >
              <div class="flex flex-col items-center gap-[3px]">
                <span class="w-[4px] h-[4px] rounded-full bg-white/80"></span>
                <span class="w-[4px] h-[4px] rounded-full bg-white/80"></span>
                <span class="w-[4px] h-[4px] rounded-full bg-white/80"></span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleRenameHistoryChat(item)">
                  <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                    <svg-icon name="gy-edit" size="16" color="#FFFFFF"></svg-icon>
                    <span class="text-[14px] font-medium text-white/90">{{ t('resource.renameTitle') }}</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item @click="handleDeleteHistoryChat(item)">
                  <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                    <svg-icon name="gy-delete" size="16" color="#FF6B6B"></svg-icon>
                    <span class="text-[14px] font-medium text-[#FF6B6B]">{{ t('resource.actions.delete') }}</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <!-- 加载更多提示 -->
        <div v-if="loadingHistory && currentPage > 1" class="flex-col-center py-4 text-gray-500">
          <el-icon :size="24" class="animate-spin mb-2">
            <Loading/>
          </el-icon>
          <span class="text-xs">{{ t('common.loading') }}</span>
        </div>

        <!-- 没有更多数据提示 -->
        <div v-if="!hasMoreHistory && chatHistory.length > 0" class="flex justify-center py-4 text-gray-500 text-xs">
          {{ t('account.points.noMore') }}
        </div>

        <div v-if="hasMoreHistory" ref="historyLoadMoreRef" class="h-1"></div>
      </template>
    </div>
  </div>
</template>

<script setup>
import {ref, nextTick, watch, onMounted, onUnmounted} from 'vue';
import {Loading, FolderOpened} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {getChatList, renameChat, deleteChat} from '@/api/creation';
import {formatDate} from '@/utils';
import {isEmpty, isNotEmpty} from '@/utils/index.js';
import coverImg from '@/assets/common/coverImg.png';
import {creationBus} from '../creationBus';
import {useI18nText} from '@/i18n';

const props = defineProps({
  activeSessionId: {
    type: String,
    default: '',
  },
  canEditCreation: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['create-new', 'select-chat', 'renamed', 'deleted']);
const {t} = useI18nText();

const HISTORY_PAGE_SIZE = 15;
const chatHistory = ref([]);
const sessionName = ref('');
const currentPage = ref(1);
const total = ref(0);
const loadingHistory = ref(false);
const hasMoreHistory = ref(true);

const historyListRef = ref(null);
const historyLoadMoreRef = ref(null);
let historyListObserver = null;
let chatListRequestSeq = 0;
let historySearchDebounceTimer = null;

/**
 * creationBus `session:renamed`：同步本地列表项标题。
 * @param {{id: string, name: string}} param0
 * @returns {void}
 */
const handleSessionRenamed = ({id, name}) => {
  syncChatName(id, name);
};

/**
 * 首次进入创作页：拉取列表并广播 `history:initialized`。
 * @returns {Promise<void>}
 */
const handleHistoryInitialize = async () => {
  try {
    await initialize();
  } finally {
    creationBus.emit('history:initialized');
  }
};
/**
 * 空白会话首条消息发送成功后由 bus 触发，整表刷新。
 * @returns {Promise<void>}
 */
const handleHistoryRefresh = async () => {
  await initialize();
};

/**
 * 将当前历史列表快照广播给其它订阅者（如顶栏积分等）。
 * @returns {void}
 */
const broadcastHistorySnapshot = () => {
  creationBus.emit('history:snapshot', chatHistory.value.map(item => ({...item})));
};

/**
 * 请求分页会话列表；`append` 为 true 时追加；`overrideLoading` 用于初始化强制请求。
 * @param {boolean} [append=false]
 * @param {{overrideLoading?: boolean}} [options]
 * @returns {Promise<void>}
 */
const loadChatList = async (append = false, {overrideLoading = false} = {}) => {
  if (loadingHistory.value && !overrideLoading) return;

  const requestSeq = ++chatListRequestSeq;
  loadingHistory.value = true;
  try {
    const response = await getChatList({
      page: currentPage.value,
      size: HISTORY_PAGE_SIZE,
      sessionName: sessionName.value,
    });

    if (requestSeq !== chatListRequestSeq) return;
    const list = response.data || [];

    const newItems = list.map(item => ({
      id: item.sessionId,
      name: item.sessionName || t('creation.chatFallbackTitle'),
      date: formatDate(item.updateTime, 'YYYY/MM/DD'),
      cover: item.sessionCover || coverImg,
    }));

    chatHistory.value = append ? [...chatHistory.value, ...newItems] : newItems;
    total.value = response.total || 0;
    hasMoreHistory.value = chatHistory.value.length < total.value;
    broadcastHistorySnapshot();
    await nextTick();
  } catch (error) {
    if (requestSeq !== chatListRequestSeq) return;
    if (error.code === 4003) return;
    console.error(error);
    ElMessage.error(error.message);
  } finally {
    if (requestSeq === chatListRequestSeq) loadingHistory.value = false;
  }
};

/**
 * 无限滚动：加载下一页并追加。
 * @returns {Promise<void>}
 */
const loadNextHistoryPage = async () => {
  if (loadingHistory.value || !hasMoreHistory.value) return;
  currentPage.value += 1;
  await loadChatList(true);
};

/**
 * 绑定/重建 IntersectionObserver，触底自动 `loadNextHistoryPage`。
 * @returns {Promise<void>}
 */
const syncHistoryListObserver = async () => {
  await nextTick();

  if (historyListObserver) {
    historyListObserver.disconnect();
  }

  if (!historyListRef.value || !historyLoadMoreRef.value || !hasMoreHistory.value) return;

  historyListObserver = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          loadNextHistoryPage().catch((error) => {
            console.error(error);
          });
        }
      },
      {
        root: historyListRef.value,
        rootMargin: '0px 0px 80px 0px',
      },
  );

  historyListObserver.observe(historyLoadMoreRef.value);
};

/**
 * 重置分页状态并加载第一页，随后同步观察器。
 * @returns {Promise<void>}
 */
const initialize = async () => {
  currentPage.value = 1;
  total.value = 0;
  hasMoreHistory.value = true;
  chatHistory.value = [];
  await loadChatList(false, {overrideLoading: true});
  await syncHistoryListObserver();
};

/**
 * @param {string} id
 * @returns {any|undefined}
 */
const findChatById = (id) => chatHistory.value.find(item => item.id === id);

/**
 * 更新内存中的会话名并广播快照。
 * @param {string} id
 * @param {string} name
 * @returns {void}
 */
const syncChatName = (id, name) => {
  const historyItem = findChatById(id);
  if (historyItem) {
    historyItem.name = name;
    broadcastHistorySnapshot();
  }
};

/**
 * 弹出重命名框并调用接口；成功后 emit `renamed`。
 * @param {{id: string, name: string}} param0
 * @returns {Promise<void>}
 */
const promptRenameChat = async ({id, name}) => {
  try {
    const {value} = await ElMessageBox.prompt('', t('resource.renameTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputValue: name || '',
      inputPlaceholder: t('resource.renamePlaceholder'),
      inputValidator: (inputValue) => inputValue.trim() ? true : t('creation.topBar.titleRequired'),
      showClose: false,
      closeOnClickModal: false,
      customClass: 'rename-confirm',
    });

    const newName = value.trim();
    if (newName === (name || '').trim()) return;

    await renameChat({sessionId: id, name: newName});
    const historyItem = findChatById(id);
    if (historyItem) historyItem.name = newName;
    broadcastHistorySnapshot();
    emit('renamed', {id, name: newName});
    ElMessage.success(t('resource.renameSuccess'));
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    ElMessage.error(error?.message || t('creation.topBar.renameFailed'));
  }
};

/**
 * 列表项菜单：重命名（受 canEditCreation 限制）。
 * @param {any} item
 * @returns {Promise<void>}
 */
const handleRenameHistoryChat = async (item) => {
  if (!props.canEditCreation) return;
  await promptRenameChat(item);
};

/**
 * 列表项菜单：删除会话及关联资源。
 * @param {any} item
 * @returns {Promise<void>}
 */
const handleDeleteHistoryChat = async (item) => {
  if (!props.canEditCreation) return;

  try {
    await ElMessageBox.confirm(
        t('creation.history.deleteConfirmMessage', {name: item.name || t('resource.projectUntitled')}),
        t('creation.history.deleteConfirmTitle'),
        {
          confirmButtonText: t('creation.history.deleteConfirmButton'),
          cancelButtonText: t('common.cancel'),
          showClose: false,
          closeOnClickModal: false,
          customClass: 'delete-confirm',
        },
    );

    await deleteChat({sessionId: item.id});

    const nextHistory = chatHistory.value.filter(historyItem => historyItem.id !== item.id);
    if (nextHistory.length !== chatHistory.value.length) {
      chatHistory.value = nextHistory;
      total.value = Math.max(total.value - 1, 0);
      hasMoreHistory.value = chatHistory.value.length < total.value;
      broadcastHistorySnapshot();
      syncHistoryListObserver().catch((error) => {
        console.error(error);
      });
    }

    emit('deleted', item);
    ElMessage.success(t('resource.deleteSuccess'));
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    ElMessage.error(error?.message || t('creation.history.deleteFailed'));
  }
};

watch(sessionName, (val, oldVal) => {
  const nextVal = String(val || '').trim();
  const prevVal = String(oldVal || '').trim();
  if (nextVal === prevVal) return;

  if (historySearchDebounceTimer) clearTimeout(historySearchDebounceTimer);
  historySearchDebounceTimer = setTimeout(async () => {
    currentPage.value = 1;
    total.value = 0;
    hasMoreHistory.value = true;
    chatHistory.value = [];

    await loadChatList(false, {overrideLoading: true});
  }, 300);
});

watch([hasMoreHistory, sessionName], () => {
  syncHistoryListObserver().catch((error) => {
    console.error(error);
  });
});

onUnmounted(() => {
  creationBus.off('history:initialize', handleHistoryInitialize);
  creationBus.off('history:refresh', handleHistoryRefresh);
  creationBus.off('session:renamed', handleSessionRenamed);
  historyListObserver?.disconnect();
  historyListObserver = null;
  if (historySearchDebounceTimer) {
    clearTimeout(historySearchDebounceTimer);
    historySearchDebounceTimer = null;
  }
});

onMounted(() => {
  creationBus.on('history:initialize', handleHistoryInitialize);
  creationBus.on('history:refresh', handleHistoryRefresh);
  creationBus.on('session:renamed', handleSessionRenamed);
});

</script>

<style scoped lang="scss">
.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background-color: rgba(255, 255, 255, 0.05);
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 3px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.3);
    }
  }
}
</style>

<style lang="scss">
// 历史项目更多操作菜单
.creation-history-dropdown {
  background: #181818 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 20px !important;
  padding: 8px 0 !important;
  min-width: 148px !important;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.38) !important;

  .el-dropdown-menu {
    background: transparent !important;
    border: none !important;
    padding: 0 !important;
    box-shadow: none !important;
  }

  .el-dropdown-menu__item {
    padding: 0 12px !important;
    margin: 0 !important;
    height: 48px !important;
    line-height: 48px !important;
    color: #FFFFFF !important;
    background: transparent !important;

    &:hover,
    &:focus {
      background: rgba(255, 255, 255, 0.06) !important;
    }
  }

  .el-popper__arrow {
    display: none !important;
  }
}

// 重命名弹窗：沿用原有浅色风格
.rename-confirm {
  width: 500px !important;
  max-width: 500px !important;
  background: #FFFFFF !important;
  border-radius: 16px !important;
  padding: 40px !important;

  .el-message-box__header {
    padding: 0 !important;
  }

  .el-message-box__title {
    font-size: 24px;
    font-weight: 700;
    color: #000000;
    line-height: 32px;
  }

  .el-message-box__content {
    padding: 24px 0 0 !important;
  }

  .el-message-box__message {
    display: none;
  }

  .el-message-box__input {
    padding: 0 !important;

    .el-input__wrapper {
      height: 48px;
      border-radius: 8px;
      box-shadow: 0 0 0 1px #DCDFE6;
      padding: 0 16px;

      &:hover,
      &.is-focus {
        box-shadow: 0 0 0 1px #909399;
      }
    }

    .el-input__inner {
      font-size: 16px;
      color: #000000;
      height: 48px;
      line-height: 48px;

      &::placeholder {
        color: #999999;
      }
    }
  }

  .el-message-box__errormsg {
    padding-top: 4px;
  }

  .el-message-box__btns {
    padding: 32px 0 0 !important;
    display: flex;
    justify-content: flex-end;
    gap: 16px;

    .el-button {
      width: 144px !important;
      height: 48px !important;
      border-radius: 8px !important;
      font-size: 16px !important;
      font-weight: 500 !important;
      margin: 0 !important;
      color: #000000;

      &:first-child {
        border: 2px solid #000000;
        background: #FFFFFF !important;

        &:hover {
          background: #F5F5F5 !important;
        }
      }
    }

    .el-button--primary {
      border: none !important;
      background: #000000 !important;
      color: #C2FF00 !important;

      &:hover {
        background: #333333 !important;
      }
    }
  }
}

// 删除确认弹窗：沿用原有浅色风格
.delete-confirm {
  width: 740px !important;
  max-width: 740px !important;
  background: #FFFFFF !important;
  border-radius: 16px !important;
  padding: 40px !important;

  .el-message-box__header {
    padding: 0 !important;
  }

  .el-message-box__title {
    font-size: 24px;
    font-weight: 700;
    color: #000000;
    line-height: 32px;
  }

  .el-message-box__content {
    padding: 12px 0 0 !important;
  }

  .el-message-box__message {
    font-size: 20px;
    font-weight: 300;
    color: #666666;
    line-height: 28px;

    p {
      margin: 0;
    }
  }

  .el-message-box__btns {
    padding: 40px 0 0 !important;

    .el-button {
      width: 144px !important;
      height: 48px !important;
      border-radius: 8px !important;
      font-size: 16px !important;
      font-weight: 500 !important;
      margin: 0 !important;
      color: #000000;

      &:first-child {
        margin-right: 16px !important;
        border: 2px solid #000000;

        &:hover {
          background-color: transparent;
        }
      }
    }

    .el-button--primary {
      border: none !important;
      background: #000000 !important;
      color: #C2FF00 !important;
    }
  }
}
</style>
