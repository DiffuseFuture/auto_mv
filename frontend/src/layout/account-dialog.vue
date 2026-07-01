<template>
  <el-dialog
      v-model="visible"
      width="976px"
      :show-close="false"
      align-center
      destroy-on-close
      headerless
      class="account-management-dialog"
  >
    <div class="bg-white rounded-[32px] overflow-hidden p-10 relative h-[560px] flex flex-col">
      <!-- 顶部标题和关闭按钮 -->
      <div class="flex justify-between items-center mb-[32px]">
        <div class="flex items-center gap-10">
          <span
              class="text-[24px] leading-8 font-medium cursor-pointer transition-colors"
              :class="activeTab === 'profile' ? 'text-[#141414]' : 'text-[#A7A7A6]'"
              @click="activeTab = 'profile'"
          >{{ t('account.tabs.profile') }}</span>
          <span
              class="text-[24px] leading-8 font-medium cursor-pointer transition-colors"
              :class="activeTab === 'vpoint' ? 'text-[#141414]' : 'text-[#A7A7A6]'"
              @click="activeTab = 'vpoint'"
          >{{ t('account.tabs.vPoints') }}</span>
        </div>
        <div class="cursor-pointer hover:opacity-80 transition-opacity" @click="visible = false">
          <svg-icon name="gy-closure" size="28" color="#333333"></svg-icon>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="flex-1">
        <!-- 个人资料 Tab -->
        <template v-if="activeTab === 'profile'">
          <!-- 头像和昵称部分 -->
          <div class="flex items-start gap-8 mb-6">
            <button type="button" class="relative group cursor-pointer border-0 bg-transparent p-0" @click="editProfileFileInputRef?.click()">
              <el-avatar
                  :size="120"
                  :src="userStore.avatar || defaultAvatar"
                  class="border-4 border-[#F0F6DD]"
              />
              <div class="absolute inset-0 bg-black/30 rounded-full flex-center opacity-0 group-hover:opacity-100 transition-opacity">
                <el-icon :size="24" color="white">
                  <Camera/>
                </el-icon>
              </div>
              <input
                  ref="editProfileFileInputRef"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="handleEditProfileAvatarChange"
              />
            </button>

            <div class="flex-1 pt-2">
              <div class="text-[22px] leading-8 font-medium text-[#292929] mb-2">{{
                  t('account.profile.nickname')
                }}
              </div>
              <div class="relative w-full max-w-[480px]">
                <input
                    v-model="editProfileNickName"
                    type="text"
                    class="w-full h-[44px] bg-[#F0F6DD] rounded-[16px] px-[14px] text-[20px] text-[#000000] outline-none border border-transparent focus:border-[#C2FF00] transition-all"
                    @keyup.enter="handleEditProfileSave"
                />
                <div
                    v-if="editProfileNickName"
                    class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer text-[#A7A7A6] hover:text-[#333333]"
                    @click="editProfileNickName = ''"
                >
                  <svg-icon name="gy-closure" size="20"></svg-icon>
                </div>
              </div>
            </div>
          </div>

          <div class="h-[1px] bg-[#E5E5E5] w-full mb-6"></div>

          <!-- 删除账户部分 -->
          <div class="text-[22px] leading-8 font-medium text-[#0A0B0E] mb-1">{{
              t('account.profile.deleteAccount')
            }}
          </div>
          <div class="flex justify-between items-center">
            <span class="text-[20px] leading-[28px] text-[#A7A7A6]">{{ t('account.profile.deleteDesc') }}</span>
            <span class="text-[20px] leading-[28px] text-[#FF4D4F] cursor-pointer hover:opacity-80 transition-opacity" @click="handleDeleteAccount">{{
                t('account.profile.deleteAction')
              }}</span>
          </div>

          <div class="h-[1px] bg-[#E5E5E5] w-full my-6"></div>

          <!-- API Key 部分 -->
          <div class="text-[22px] leading-8 font-medium text-[#0A0B0E] mb-1">{{ t('account.apiKey.title') }}</div>
          <div class="flex justify-between items-center gap-4">
            <div v-if="userStore.apiKey" class="flex-1 min-w-0 flex items-center gap-2">
              <span class="text-[20px] leading-[28px] text-[#A7A7A6] truncate font-mono">{{ apiKeyMasked }}</span>
              <svg-icon
                  name="gy-copy"
                  size="18"
                  class="shrink-0 cursor-pointer text-[#A7A7A6] hover:text-[#141414] transition-colors"
                  @click="handleCopyApiKey"
              ></svg-icon>
            </div>
            <span v-else class="flex-1 text-[20px] leading-[28px] text-[#A7A7A6]">{{ t('account.apiKey.desc') }}</span>
            <span
                class="shrink-0 text-[20px] leading-[28px] cursor-pointer hover:opacity-80 transition-opacity"
                :class="editProfileSaving ? 'text-[#A7A7A6] pointer-events-none' : 'text-[#52c41a]'"
                @click="handleCreateApiKey"
            >{{ userStore.apiKey ? t('account.apiKey.regenerate') : t('account.apiKey.generate') }}</span>
          </div>
        </template>

        <!-- V点 Tab -->
        <template v-else>
          <!-- 余额卡片 -->
          <div class="bg-[#F0F6DD] rounded-[20px] mb-[18px]">
            <div class="flex justify-between items-center mb-[18px] border-b border-[#A7A7A6]/20 pb-3 pl-[26px] pr-[20px] pt-8">
              <span class="text-[22px] text-[#000000]">{{ userStore.tierName }}</span>
              <button class="w-24 h-9 cursor-pointer bg-[#292929] text-[#C2FF00] rounded-[10px] text-[20px] font-medium hover:opacity-90 transition-opacity" @click="handleUpgrade">
                {{ t('account.points.upgrade') }}
              </button>
            </div>
            <div class="flex justify-between items-center pl-[26px] pr-[20px] pb-[18px]">
              <span class="text-[22px] text-[#000000]">{{ t('account.points.value') }}</span>
              <span class="text-[20px] text-[#000000] font-medium">{{ userStore.pointsBalance }}</span>
            </div>
          </div>

          <!-- 详情列表 -->
          <div class="rounded-[20px] overflow-hidden bg-[#F0F6DD]">
            <!-- 表头 -->
            <div class="bg-[#292929] h-12 px-[26px] flex justify-between items-center text-[#C2FF00] text-[22px] font-medium">
              <span class="w-[60%]">{{ t('account.points.detail') }}</span>
              <span class="w-[28%] text-center">{{ t('account.points.time') }}</span>
              <span class="w-[12%] text-right">{{ t('account.points.value') }}</span>
            </div>

            <!-- 表体 -->
            <div class="px-8 py-1 max-h-[200px] overflow-y-auto no-scrollbar" @scroll="handlePointsLogScroll">
              <div v-if="pointsLogLoading && !pointsLogList.length" class="py-8 text-center text-[#A7A7A6] text-[20px]">
                {{ t('account.points.loading') }}
              </div>
              <div v-else-if="!pointsLogList.length" class="py-8 text-center text-[#A7A7A6] text-[20px]">
                {{ t('account.points.empty') }}
              </div>
              <template v-else>
                <div v-for="item in pointsLogList" :key="item.id" class="border-b border-[#A7A7A6]/10 last:border-none">
                  <div class="h-[50px] flex justify-between items-center -mx-2 rounded-lg">
                    <span class="w-[60%] text-[18px] text-[#141414] truncate leading-[50px]" :title="item.description">{{ item.description }}</span>
                    <span class="w-[28%] text-center text-[18px] text-[#141414]">
                      {{ formatDate(item.createTime, 'YYYY/MM/DD HH:mm:ss') }}
                    </span>
                    <span class="w-[12%] text-right text-[18px]" :class="item.amount > 0 ? 'text-[#52c41a]' : 'text-[#141414]'">{{
                        item.amount > 0 ? '+' : ''
                      }}{{ item.amount }}</span>
                  </div>
                </div>
                <div v-if="pointsLogLoading" class="py-3 text-center text-[#A7A7A6] text-[20px]">
                  {{ t('account.points.loading') }}
                </div>
                <div v-else-if="!hasMoreLog" class="py-3 text-center text-[#A7A7A6] text-[20px]">
                  {{ t('account.points.noMore') }}
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {Camera} from '@element-plus/icons-vue';
import {useUserStore} from '@/store/user.js';
import {formatDate} from '@/utils/index.js';
import {useI18nText} from '@/i18n';
import {usePointsLog} from '@/views/home/composables/usePointsLog.js';
import {useAccountProfileDialog} from '@/views/home/composables/useAccountProfileDialog.js';

const props = defineProps({
  modelValue: Boolean,
  /** 弹窗打开时默认展示的 tab：'profile'（个人资料）或 'vpoint'（V 点交易日志）。 */
  defaultTab: {
    type: String,
    default: 'profile',
  },
});
const emit = defineEmits(['update:modelValue']);

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const {t} = useI18nText();
const visible = ref(false);
const activeTab = ref('profile'); // profile | vpoint
const {
  editProfileSaving,
  editProfileNickName,
  apiKeyMasked,
  syncEditProfileState,
  handleEditProfileAvatarChange,
  handleEditProfileSave,
  handleDeleteAccount,
  handleCreateApiKey,
  handleCopyApiKey,
} = useAccountProfileDialog();
const editProfileFileInputRef = ref(null);

/** 跳转到订阅页面 */
const handleUpgrade = () => {
  visible.value = false;
  const lang = typeof route.params?.lang === 'string' ? route.params.lang : 'zh';
  router.push({name: 'subscribe', params: {lang}});
};

// V点交易日志
const {
  pointsLogList,
  pointsLogLoading,
  hasMoreLog, // hasMorePointsLog
  resetAndFetchPointsLog,
  handlePointsLogScroll,
} = usePointsLog();

// 重置分页状态并加载第一页（别名兼容旧代码）
const resetAndFetchLog = resetAndFetchPointsLog;

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    activeTab.value = props.defaultTab === 'vpoint' ? 'vpoint' : 'profile';
    syncEditProfileState();
    userStore.fetchUserPlan();
    if (activeTab.value === 'vpoint') resetAndFetchLog();
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
});

watch(activeTab, (val) => {
  if (val === 'vpoint') resetAndFetchLog();
});

</script>

<style lang="scss">
.account-management-dialog {
  height: 480px !important;
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  --el-dialog-bg-color: transparent;
  --el-dialog-box-shadow: none;
  --el-dialog-border-color: transparent;

  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    padding: 0 !important;
    background: transparent !important;
  }
}

.no-scrollbar {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari and Opera */
  }
}

// 账户确认弹窗（与资源页 .resource-delete-confirm 风格一致）
.account-confirm {
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

  // 危险操作变体：确认按钮红色
  &.account-confirm--danger .el-message-box__btns .el-button--primary {
    background: #FF4D4F !important;
    color: #FFFFFF !important;
  }
}
</style>
