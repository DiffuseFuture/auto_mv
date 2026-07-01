<template>
  <div class="resource-mobile-page relative w-full h-screen flex flex-col bg-[#141414] text-white overflow-x-hidden">
    <!-- Top bar：标题在左，搜索框在右（与 PC 端结构对齐，复用同一 i18n key 与 searchKeyword 状态） -->
    <div class="px-4 pt-6 pb-3 bg-[#141414]/95 backdrop-blur flex-between">
      <div class="shrink-0 text-[18px] font-medium">{{ t('resource.title') }}</div>
      <div class="relative w-40">
        <input
            v-model="searchKeyword"
            type="text"
            :placeholder="t('creation.history.searchPlaceholder')"
            class="w-full h-9 bg-white/10 rounded-[10px] pl-9 pr-3 text-[14px] text-white placeholder-white/30 outline-none border border-transparent focus:border-[#C2FF00]/50 transition-colors"
        />
        <svg-icon name="gy-search" size="16" color="rgba(255,255,255,0.4)" class="absolute left-3 top-1/2 -translate-y-1/2"></svg-icon>
      </div>
    </div>

    <!-- List -->
    <div ref="scrollRef" class="flex-1 overflow-y-auto px-[15px] pb-28 no-scrollbar" @scroll.passive="handleScroll">
      <div v-if="loading && list.length === 0" class="flex-center py-14 text-white/60">
        <span class="text-[14px]">{{ t('common.loading') }}</span>
      </div>

      <div v-else-if="!loading && list.length === 0" class="flex-col-center py-16 text-white/50">
        <svg-icon name="gy-empty" size="56" color="rgba(255,255,255,0.18)"></svg-icon>
        <div class="mt-3 text-[14px]">{{ t('resource.empty') }}</div>
      </div>

      <!-- 历史项目列表 -->
      <div v-else class="flex flex-col gap-4">
        <div
            v-for="item in list"
            :key="item.id"
            class="rounded-[16px] overflow-hidden border border-[#C2FF00]/50 bg-black cursor-pointer"
            @click="handleGoToChat(router, item.id)"
        >
          <div class="relative h-[160px] bg-black">
            <img :src="item.cover" class="w-full h-full object-cover" :alt="t('resource.title')"/>
          </div>

          <!-- Bottom bar：项目名称 + 日期堆叠在左，操作菜单在右 -->
          <div class="px-3 py-[10px] flex items-center justify-between gap-3 bg-[#0A0A0A]">
            <div class="min-w-0 flex-1">
              <div class="truncate text-[15px] font-medium leading-[20px] text-white">
                {{ item.name }}
              </div>
              <div class="mt-[2px] text-[12px] leading-[16px] text-white/55">
                {{ item.date }}
              </div>
            </div>

            <el-dropdown
                trigger="click"
                placement="bottom-end"
                popper-class="resource-more-dropdown-mobile"
                :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }"
            >
              <button @click.stop aria-label="more">
                <div class="flex items-center gap-1.5">
                  <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                  <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                  <span class="w-[5px] h-[5px] rounded-full bg-[#C2FF00]"></span>
                </div>
              </button>
              <template #dropdown>
                <el-dropdown-menu>

                  <el-dropdown-item @click.stop="handleRename(item)">
                    <div class="flex items-center gap-2 py-1.5 px-1.5 w-full">
                      <svg-icon name="gy-edit" size="16" color="#FFFFFF"></svg-icon>
                      <span class="text-[14px] font-medium text-white/90">{{ t('resource.renameTitle') }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item @click.stop="handleDelete(item)">
                    <div class="flex items-center gap-2 py-1.5 px-1.5 w-full">
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

      <div v-if="loading && list.length > 0" class="flex-center py-6 text-white/60">
        <span class="text-[14px]">{{ t('resource.loadingMore') }}</span>
      </div>
    </div>

    <MobileTabBar/>
  </div>
</template>

<script setup>
import {ref, watch, onBeforeUnmount, onMounted} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import {useI18nText} from '@/i18n';
import {getChatList, renameChat, deleteChat} from '@/api/creation';
import {formatDate} from '@/utils';

import coverImg from '@/assets/common/coverImg.png';

const router = useRouter();
const route = useRoute();
const {t} = useI18nText();

const scrollRef = ref(null);

const getLangPrefix = () => (typeof route.params?.lang === 'string' ? route.params.lang : 'zh');

// ─────────────────────────────────────────────
// 历史项目列表状态（仅移动端 resource 使用）
// ─────────────────────────────────────────────
const list = ref([]);
const loading = ref(false);

let page = 1;
let total = 0;
let searchTimer = null;
const searchKeyword = ref('');

const resetPaging = () => {
  page = 1;
  total = 0;
  list.value = [];
};

const loadList = async (reset = false) => {
  if (loading.value) return;
  if (!reset && list.value.length >= total) return;
  if (reset) resetPaging();

  loading.value = true;
  try {
    const res = await getChatList({
      page,
      size: 15,
      sessionName: searchKeyword.value.trim(),
    });

    const incoming = (res.data || []).map(item => ({
      id: item.sessionId,
      name: item.sessionName || t('resource.projectUntitled'),
      date: formatDate(item.updateTime, 'YYYY/MM/DD HH:mm'),
      cover: item.sessionCover || coverImg,
    }));

    list.value = reset ? incoming : [...list.value, ...incoming];
    total = res.total || 0;
    page += 1;
  } catch (error) {
    console.error(error);
    if (Number(error?.code) === 4003) {
      ElMessage.warning(t('resource.loginFirst'));
    } else {
      ElMessage.error(error?.message || t('resource.loadFail'));
    }
  } finally {
    loading.value = false;
  }
};

const handleScrollLoadMore = (scrollTop, scrollHeight, clientHeight, threshold = 80) => {
  if (scrollHeight - scrollTop - clientHeight < threshold) loadList(false);
};

const handleGoToChat = (routerInstance, sessionId) => {
  const lang = getLangPrefix();
  const target = list.value.find((item) => item.id === sessionId);
  const sessionName = String(target?.name || '').trim();
  routerInstance.push({
    name: 'creation',
    params: {lang},
    query: {sessionId, sessionName},
  });
};

const handleRename = async (item) => {
  if (!item?.id) return;

  try {
    const {value} = await ElMessageBox.prompt('', t('resource.renameTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputValue: item.name || '',
      inputPlaceholder: t('resource.renamePlaceholder'),
      inputPattern: /.+/,
      inputErrorMessage: t('resource.renameRequired'),
      showClose: false,
      closeOnClickModal: false,
      customClass: 'rename-confirm',
    });

    const newName = value.trim();
    if (!newName) return;

    await renameChat({sessionId: item.id, name: newName});
    item.name = newName;
    ElMessage.success(t('resource.renameSuccess'));
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    ElMessage.error(error?.message || t('creation.topBar.renameFailed'));
  }
};

const handleDelete = async (item) => {
  if (!item?.id) return;

  try {
    await ElMessageBox.confirm(
        t('creation.history.deleteConfirmMessage', {name: item.name || t('resource.projectUntitled')}),
        t('creation.history.deleteConfirmTitle'),
        {
          confirmButtonText: t('creation.history.deleteConfirmButton'),
          cancelButtonText: t('common.cancel'),
          showClose: false,
          closeOnClickModal: false,
          customClass: 'resource-delete-confirm-mobile',
        },
    );

    await deleteChat({sessionId: item.id});
    list.value = list.value.filter(i => i.id !== item.id);
    total = Math.max(total - 1, 0);
    ElMessage.success(t('resource.deleteSuccess'));
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
  }
};

watch(searchKeyword, () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadList(true), 300);
});

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer);
});

const handleScroll = (e) => {
  const target = e?.target;
  if (!target) return;
  const {scrollTop, scrollHeight, clientHeight} = target;
  handleScrollLoadMore(scrollTop, scrollHeight, clientHeight, 80);
};

onMounted(() => loadList(true));
</script>

<style lang="scss">
@use './resource-dialogs.scss' as *;

.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.resource-more-dropdown-mobile {
  background: #333921 !important;
  border: none !important;
  border-radius: 10px !important;
  padding: 0 !important;
  width: 140px !important;
  min-width: 140px !important;

  .el-dropdown-menu {
    background: #333921 !important;
    border: none !important;
    padding: 10px 0 !important;
    box-shadow: none !important;
  }

  .el-dropdown-menu__item {
    padding: 0 !important;
    margin: 0 !important;
    height: 32px !important;
    line-height: 32px !important;
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

.resource-mv-preview-dialog-mobile {
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

.resource-delete-confirm-mobile {
  width: 92vw !important;
  max-width: 92vw !important;
  background: #ffffff !important;
  border-radius: 16px !important;
  padding: 22px !important;

  .el-message-box__header {
    padding: 0 !important;
  }

  .el-message-box__title {
    font-size: 18px;
    font-weight: 700;
    color: #000000;
    line-height: 26px;
  }

  .el-message-box__content {
    padding: 10px 0 0 !important;
  }

  .el-message-box__message {
    font-size: 14px;
    color: #666666;
    line-height: 20px;
    margin: 0;
  }

  .el-message-box__btns {
    padding: 18px 0 0 !important;
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .el-button {
      height: 38px !important;
      border-radius: 10px !important;
      font-size: 14px !important;
      margin: 0 !important;
    }

    .el-button--primary {
      border: none !important;
      background: #000000 !important;
      color: #c2ff00 !important;
    }
  }
}
</style>
