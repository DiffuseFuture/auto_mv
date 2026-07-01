<template>
  <div class="h-[80px] shrink-0 flex-between px-4">
    <div class="flex items-center gap-4 text-[#C2FF00]">
      <div class="text-[22px] font-semibold max-w-2xl text-nowrap overflow-hidden text-ellipsis cursor-default" :title="props.chatTitle">
        {{ props.chatTitle }}
      </div>
      <svg-icon v-if="props.sessionId && props.canEditCreation" name="gy-edit" size="20" class="cursor-pointer" @click="handleEditTitle"></svg-icon>
    </div>

    <!-- 右侧账户区：积分胶囊（session 模式：hover 弹当前会话积分流水）+ 头像气泡 / 登录按钮 -->
    <TopBar mode="session" :session-id="props.sessionId" />
  </div>
</template>

<script setup>
import {ElMessage, ElMessageBox} from 'element-plus';
import TopBar from '@/components/topbar/TopBar.vue';
import {renameChat} from '@/api/creation';
import {creationBus} from '../creationBus';
import {useI18nText} from '@/i18n';

const props = defineProps({
  chatTitle: {type: String, default: ''},
  sessionId: {type: String, default: ''},
  canEditCreation: {type: Boolean, default: false},
});

const emit = defineEmits(['update:chatTitle']);
const {t} = useI18nText();

/**
 * 弹出重命名并同步 `chatTitle` + bus 事件。
 * @returns {Promise<void>}
 */
const handleEditTitle = async () => {
  if (!props.sessionId || !props.canEditCreation) return;
  try {
    const {value} = await ElMessageBox.prompt('', t('resource.renameTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputValue: props.chatTitle || '',
      inputPlaceholder: t('resource.renamePlaceholder'),
      inputValidator: (inputValue) => inputValue.trim() ? true : t('creation.topBar.titleRequired'),
      showClose: false,
      closeOnClickModal: false,
      customClass: 'rename-confirm',
    });

    const newName = value.trim();
    if (newName === (props.chatTitle || '').trim()) return;

    await renameChat({sessionId: props.sessionId, name: newName});
    emit('update:chatTitle', newName);
    creationBus.emit('session:renamed', {id: props.sessionId, name: newName});
    ElMessage.success(t('resource.renameSuccess'));
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    ElMessage.error(error?.message || t('creation.topBar.renameFailed'));
  }
};
</script>

