<template>
  <div class="h-full flex flex-col bg-[#141414] px-10 pt-10 text-white overflow-hidden">
    <!-- 头部区域 -->
    <div class="flex justify-between items-center mb-8 shrink-0">
      <div class="text-[32px] font-bold text-[#C2FF00]">{{ t('resource.title') }}</div>

      <!-- 搜索框 + 右上角账户区（跟首页完全一致） -->
      <div class="flex items-center gap-4">
        <div class="relative w-[360px]">
          <input
              v-model="searchKeyword"
              type="text"
              :placeholder="t('creation.history.searchPlaceholder')"
              class="w-full h-11 bg-white/10 rounded-xl px-4 pl-11 text-white placeholder-white/30 outline-none border border-transparent focus:border-[#C2FF00]/50 transition-all"
          />
          <svg-icon name="gy-search" size="20" color="rgba(255,255,255,0.3)" class="absolute left-4 top-1/2 -translate-y-1/2"></svg-icon>
        </div>

        <TopBar mode="menu" />
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="flex gap-[50px] mb-8 border-b border-white/5 shrink-0">
      <div
          v-for="tab in tabs"
          :key="tab.value"
          class="pb-4 text-[20px] cursor-pointer transition-all relative"
          :class="activeTab === tab.value ? 'text-white font-bold' : 'text-white/40 font-medium'"
          @click="handleTabChange(tab.value)"
      >
        {{ tab.label }}
        <div v-if="activeTab === tab.value" class="absolute bottom-0 left-0 right-0 h-1 bg-[#C2FF00] rounded-full"></div>
      </div>
    </div>

    <!-- 项目列表滚动区域 -->
    <div ref="scrollRef" class="flex-1 overflow-y-auto no-scrollbar pb-10" @scroll="handleScroll">

      <!-- 加载中骨架 -->
      <div v-if="loading && list.length === 0" class="flex-center py-20 text-white/30">
        <el-icon :size="28" class="animate-spin mr-3">
          <Loading/>
        </el-icon>
        <span>{{ t('common.loading') }}</span>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!loading && list.length === 0" class="flex-col-center py-20 text-white/30">
        <svg-icon name="gy-empty" size="64" color="rgba(255,255,255,0.15)"></svg-icon>
        <span class="mt-4 text-[16px]">{{ t('resource.empty') }}</span>
      </div>

      <!-- MV Tab - 网格视图 -->
      <div v-else-if="activeTab === 'mv'" class="flex flex-wrap justify-between gap-y-6">
        <div
            v-for="item in list"
            :key="item.messageId"
            class="group flex flex-col bg-[#000000] rounded-[24px] overflow-hidden border border-white/5 hover:border-[#C2FF00]/30 transition-all duration-300 w-[300px] h-[300px] shrink-0 cursor-pointer"
            @click="handleOpenMvPreview(item)"
        >
          <!-- 封面图 -->
          <div class="h-[140px] w-full relative overflow-hidden bg-black flex-center">
            <img v-if="item.fileCoverUrl" :src="item.fileCoverUrl" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"/>
            <div v-else class="w-full h-full bg-white/5 flex-center">
              <svg-icon name="gy-MV" size="40" color="rgba(255,255,255,0.2)"></svg-icon>
            </div>
            <div class="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity"></div>
          </div>

          <!-- 内容信息 -->
          <div class="p-4 flex flex-col flex-1 min-h-0">
            <div class="text-[18px] font-bold mb-2 truncate">{{ item.projectName || t('resource.projectUntitled') }}</div>
            <div class="text-[12px] text-white/40 truncate">
              {{ formatDate(item.createTime, 'YYYY/MM/DD HH:mm') }}
            </div>

            <!-- 底部按钮栏 -->
            <div class="flex items-center gap-2 mt-auto" @click.stop>
              <button
                  class="flex-1 h-9 bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] hover:opacity-90 rounded-full flex-center gap-2 text-black font-bold text-[13px] transition-all active:scale-[0.98] cursor-pointer"
                  @click.stop="handleGoToChat(router, item.sessionId)"
              >
                <svg-icon name="gy-process" size="16" color="black"></svg-icon>
                <span>{{ t('resource.process') }}</span>
              </button>

              <el-dropdown trigger="click" popper-class="resource-more-dropdown" placement="bottom-start" :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }">
                <div class="w-9 h-9 bg-[#2F3A14] rounded-[8px] flex-center cursor-pointer hover:bg-[#37421a] transition-colors shrink-0" @click.stop>
                  <div class="flex items-center gap-1">
                    <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                    <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                    <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                  </div>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleDownload(item)">
                      <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                        <svg-icon name="gy-download" size="16" color="#C2FF00"></svg-icon>
                        <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.download') }}</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleShare(item.projectId)">
                      <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                        <svg-icon name="gy-share" size="16" color="#C2FF00"></svg-icon>
                        <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.share') }}</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleRename(item)">
                      <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                        <svg-icon name="gy-Rename" size="16" color="#C2FF00"></svg-icon>
                        <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.rename') }}</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleDelete(item)">
                      <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                        <svg-icon name="gy-delete" size="16" color="#ff4d4f"></svg-icon>
                        <span class="text-[14px] font-medium text-[#ff4d4f]">{{ t('resource.actions.delete') }}</span>
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
        <!-- 占位元素用于最后一行左对齐 -->
        <div v-for="i in 4" :key="'place-'+i" class="w-[300px] h-0"></div>
      </div>

      <!-- 音乐 Tab - 列表视图 -->
      <div v-else class="flex flex-col gap-[10px]">
        <div
            v-for="item in list"
            :key="item.messageId"
            class="flex items-center bg-[#000000] hover:bg-[#2F3A14] rounded-[20px] p-4 transition-all duration-300 border border-white/5 group"
        >
          <!-- 左侧封面 -->
          <div
              class="w-[70px] h-[70px] rounded-[12px] overflow-hidden relative shrink-0 bg-black flex-center cursor-pointer"
              @click="handleTogglePlay(item)"
          >
            <img v-if="item.fileCoverUrl" :src="item.fileCoverUrl" class="w-full h-full object-cover"/>
            <div v-else class="w-full h-full bg-white/5 flex-center">
              <svg-icon name="gy-audio" size="36" color="rgba(255,255,255,0.2)"></svg-icon>
            </div>
            <div class="absolute inset-0 bg-black/20 flex-center">
              <svg-icon
                  :name="playingId === item.projectId && isPlaying ? 'gy-pause' : 'gy-play'"
                  size="32"
                  color="white"
                  class="opacity-80"
              ></svg-icon>
            </div>
          </div>

          <!-- 中间信息 -->
          <div class="ml-6 flex-1 flex flex-col justify-center min-w-0">
            <div class="text-[20px] font-bold mb-2 truncate">{{ item.projectName || t('resource.projectUntitled') }}</div>
            <div class="text-[16px] text-[#C2FF00]/80 font-medium">
              {{ formatDate(item.createTime, 'YYYY/MM/DD HH:mm') }}
            </div>
          </div>

          <!-- 右侧按钮 -->
          <div class="flex items-center gap-4 ml-4">
            <button
                class="w-[180px] h-[40px] text-[14px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] hover:opacity-90 rounded-[14px] flex-center gap-2 text-black font-bold transition-all active:scale-[0.98] cursor-pointer"
                @click="handleGoToChat(router, item.sessionId)"
            >
              <svg-icon name="gy-process" size="20" color="black"></svg-icon>
              <span>{{ t('resource.process') }}</span>
            </button>

            <el-dropdown trigger="click" popper-class="resource-more-dropdown" placement="bottom-end" :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }">
              <div class="w-[40px] h-[40px] bg-white/10 rounded-[14px] flex-center cursor-pointer hover:bg-white/20 transition-colors shrink-0">
                <div class="flex items-center gap-1.5">
                  <span class="w-[6px] h-[6px] rounded-full bg-[#C2FF00]"></span>
                  <span class="w-[6px] h-[6px] rounded-full bg-[#C2FF00]"></span>
                  <span class="w-[6px] h-[6px] rounded-full bg-[#C2FF00]"></span>
                </div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleDownload(item)">
                    <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                      <svg-icon name="gy-download" size="16" color="#C2FF00"></svg-icon>
                      <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.download') }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleShare(item.projectId)">
                    <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                      <svg-icon name="gy-share" size="16" color="#C2FF00"></svg-icon>
                      <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.share') }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleRename(item)">
                    <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                      <svg-icon name="gy-Rename" size="16" color="#C2FF00"></svg-icon>
                      <span class="text-[14px] font-medium text-white/90">{{ t('resource.actions.rename') }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleDelete(item)">
                    <div class="flex items-center gap-3 py-1.5 px-1.5 w-full">
                      <svg-icon name="gy-delete" size="16" color="#ff4d4f"></svg-icon>
                      <span class="text-[14px] font-medium text-[#ff4d4f]">{{ t('resource.actions.delete') }}</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 底部加载更多提示 -->
      <div v-if="loading && list.length > 0" class="flex-center py-6 text-white/30">
        <el-icon :size="20" class="animate-spin mr-2">
          <Loading/>
        </el-icon>
        <span class="text-[14px]">{{ t('resource.loadingMore') }}</span>
      </div>
    </div>

    <!-- MV 播放弹窗 -->
    <el-dialog
        v-model="mvPreviewVisible"
        width="1200px"
        :show-close="false"
        align-center
        destroy-on-close
        headerless
        class="resource-mv-preview-dialog"
    >
      <div class="relative rounded-[20px] overflow-hidden bg-black/95 border border-white/10 shadow-2xl">
        <div class="absolute top-4 right-4 z-10 w-10 h-10 rounded-full bg-black/55 flex-center cursor-pointer hover:bg-black/75 transition-colors" @click="mvPreviewVisible = false">
          <svg-icon name="gy-closure" size="24" color="white"></svg-icon>
        </div>
        <video
            v-if="currentMvItem?.fileUrl"
            :src="currentMvItem.fileUrl"
            :poster="currentMvItem.fileCoverUrl"
            controls
            autoplay
            class="w-full aspect-video object-contain"
        ></video>
      </div>
    </el-dialog>

    <!-- 隐藏的音频播放器 -->
    <audio ref="audioRef" @ended="onAudioEnded" @error="onAudioError"></audio>

  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {Loading} from '@element-plus/icons-vue';
import TopBar from '@/components/topbar/TopBar.vue';
import {useUserStore} from '@/store/user';
import {formatDate} from '@/utils/index.js';
import {useI18nText} from '@/i18n';
import {useResourceList} from './useResourceList';

const router = useRouter();
const userStore = useUserStore();
const {t} = useI18nText();

const scrollRef = ref(null);
const pageScrollRef = ref(null);

const mvPreviewVisible = ref(false);
const currentMvItem = ref(null);
const getLangPrefix = () => {
  const lang = router.currentRoute.value?.params?.lang;
  return typeof lang === 'string' ? lang : 'zh';
};

const {
  searchKeyword,
  activeTab,
  list,
  loading,
  loadList,
  handleTabChange,
  handleScrollLoadMore,
  handleGoToChat,
  handleShare,
  handleDownload,
  handleRename,
  handleDelete,
  audioRef,
  playingId,
  isPlaying,
  handleTogglePlay,
  onAudioEnded,
  onAudioError,
  stopAudio,
} = useResourceList({
  t,
  getLangPrefix,
  renamePromptClass: 'rename-confirm',
  deleteConfirmClass: 'resource-delete-confirm',
});

const tabs = [
  {label: t('resource.tabs.mv'), value: 'mv'},
  {label: t('resource.tabs.music'), value: 'music'},
];

const handleScroll = (e) => {
  const {scrollTop, scrollHeight, clientHeight} = e.target;
  handleScrollLoadMore(scrollTop, scrollHeight, clientHeight, 60);
};

const handleOpenMvPreview = (item) => {
  if (activeTab.value !== 'mv') return;
  currentMvItem.value = item;
  mvPreviewVisible.value = true;
};

// 右上角账户区相关 state / 跳转逻辑均封进 TopBar，本页不需要再处理

onMounted(() => {
  loadList(true);
  // 直接进入资源页时主动拉一次套餐，跟首页、创作页同样的口径
  if (userStore.isLoggedIn) {
    userStore.fetchUserPlan();
  }
});
</script>

<style lang="scss">
@use './resource-dialogs.scss' as *;

.resource-more-dropdown {
  background: #333921 !important;
  border: none !important;
  border-radius: 10px !important;
  padding: 0 !important;
  width: 104px !important;
  min-width: 104px !important;

  .el-dropdown-menu {
    background: #333921 !important;
    border: none !important;
    padding: 10px 0 !important;
    box-shadow: none !important;
  }

  .el-dropdown-menu__item {
    padding: 0 !important;
    margin: 0 !important;
    height: 28px !important;
    line-height: 28px !important;
    background: transparent !important;

    &:hover,
    &:focus {
      background: rgba(255, 255, 255, 0.06) !important;
    }

    a {
      color: inherit;
      text-decoration: none;
    }
  }

  .el-popper__arrow {
    display: none !important;
  }
}

.resource-mv-preview-dialog {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  --el-dialog-bg-color: transparent;
  --el-dialog-box-shadow: none;

  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    padding: 0 !important;
    background: transparent !important;
  }
}

.el-overlay:has(.resource-mv-preview-dialog) {
  background-color: transparent !important;
}

.resource-delete-confirm {
  width: 740px !important;
  max-width: 740px !important;
  height: 256px !important;
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

.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

// Rename dialog styles moved to ./resource-dialogs.scss
</style>
