import {computed, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {useUserStore} from '@/store/user.js';
import {updateUserInfo, updateAvatar, createApiKey} from '@/api/auth';
import {compressImageBeforeUpload} from '@/utils/index.js';
import defaultAvatar from '@/assets/common/avatar.jpg';
import {useI18nText} from '@/i18n';

/**save
 * 提供个人资料弹窗的通用业务逻辑，供 PC 和移动端复用。
 * @returns {{
 *   editProfileDialogVisible: import('vue').Ref<boolean>,
 *   editProfileSaving: import('vue').Ref<boolean>,
 *   editProfileUploading: import('vue').Ref<boolean>,
 *   editProfileNickName: import('vue').Ref<string>,
 *   editProfileAvatarUrl: import('vue').Ref<string>,
 *   apiKeyMasked: import('vue').ComputedRef<string>,
 *   openEditProfileDialog: () => void,
 *   syncEditProfileState: () => void,
 *   handleEditProfileAvatarChange: (event: Event) => Promise<void>,
 *   handleEditProfileSave: () => Promise<void>,
 *   handleDeleteAccount: () => void,
 *   handleCreateApiKey: () => Promise<void>,
 *   handleCopyApiKey: () => Promise<void>,
 *   handleEditProfileDialogClosed: () => void,
 * }}
 */
export function useAccountProfileDialog() {
  const {t} = useI18nText();
  const userStore = useUserStore();

  const editProfileDialogVisible = ref(false);
  const editProfileSaving = ref(false);
  const editProfileUploading = ref(false);
  const editProfileNickName = ref('');
  const editProfileAvatarUrl = ref('');

  const apiKeyMasked = computed(() => {
    const key = userStore.apiKey;
    if (!key || key.length <= 8) return key;
    return `${key.slice(0, 4)}****${key.slice(-4)}`;
  });

  const syncEditProfileState = () => {
    editProfileNickName.value = userStore.userName || '';
    editProfileAvatarUrl.value = userStore.avatar || defaultAvatar;
  };

  const openEditProfileDialog = () => {
    syncEditProfileState();
    editProfileDialogVisible.value = true;
  };

  const handleEditProfileAvatarChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!file.type?.startsWith('image/')) {
      ElMessage.warning(t('account.profile.avatarInvalid'));
      return;
    }

    editProfileUploading.value = true;
    try {
      const compressedFile = await compressImageBeforeUpload(file, {
        maxWidth: 1024,
        maxHeight: 1024,
        quality: 0.82,
        minSizeKB: 100,
      });
      const formData = new FormData();
      formData.append('avatarImg', compressedFile, compressedFile?.name || 'avatar.jpg');
      await updateAvatar(formData);
      await userStore.fetchUserInfo();
      syncEditProfileState();
      ElMessage.success(t('account.profile.avatarUpdated'));
    } catch (error) {
      console.error('上传头像失败:', error);
      ElMessage.error(error?.message || t('account.profile.avatarUploadFail'));
    } finally {
      editProfileUploading.value = false;
    }
  };

  const handleEditProfileSave = async () => {
    const nextNickName = editProfileNickName.value.trim();
    if (!nextNickName) {
      ElMessage.warning(t('account.profile.nicknameEmpty'));
      return;
    }

    editProfileSaving.value = true;
    try {
      await updateUserInfo({
        userId: userStore.userInfo?.userId || '',
        nickName: nextNickName,
      });
      await userStore.fetchUserInfo();
      syncEditProfileState();
      ElMessage.success(t('account.profile.nicknameUpdated'));
    } catch (error) {
      console.error('修改昵称失败:', error);
      ElMessage.error(error?.message || t('account.profile.nicknameUpdateFail'));
    } finally {
      editProfileSaving.value = false;
    }
  };

  const handleDeleteAccount = () => {
    ElMessageBox.confirm(
      t('account.profile.deleteConfirmMessage'),
      t('account.profile.deleteConfirmTitle'),
      {
        confirmButtonText: t('account.profile.deleteConfirmBtn'),
        cancelButtonText: t('common.cancel'),
        showClose: false,
        closeOnClickModal: false,
        customClass: 'account-confirm account-confirm--danger',
      },
    ).then(() => {
      ElMessage.error(t('account.profile.deleteNotAvailable'));
    }).catch(() => {});
  };

  const handleCreateApiKey = async () => {
    if (editProfileSaving.value) return;
    if (userStore.apiKey) {
      try {
        await ElMessageBox.confirm(
          t('account.apiKey.regenerateMessage'),
          t('account.apiKey.regenerateTitle'),
          {
            confirmButtonText: t('account.apiKey.regenerateConfirm'),
            cancelButtonText: t('common.cancel'),
            showClose: false,
            closeOnClickModal: false,
            customClass: 'account-confirm',
          },
        );
      } catch {
        return;
      }
    }

    editProfileSaving.value = true;
    try {
      await createApiKey();
      await userStore.fetchUserInfo();
      syncEditProfileState();
      ElMessage.success(t('account.apiKey.created'));
    } catch (error) {
      console.error('创建 API Key 失败:', error);
      ElMessage.error(error?.message || t('account.apiKey.createFail'));
    } finally {
      editProfileSaving.value = false;
    }
  };

  const handleCopyApiKey = async () => {
    try {
      await navigator.clipboard.writeText(userStore.apiKey);
      ElMessage.success(t('account.apiKey.copied'));
    } catch {
      ElMessage.error(t('account.apiKey.copyFail'));
    }
  };

  const handleEditProfileDialogClosed = () => {
    editProfileNickName.value = '';
    editProfileAvatarUrl.value = '';
  };

  return {
    editProfileDialogVisible,
    editProfileSaving,
    editProfileUploading,
    editProfileNickName,
    editProfileAvatarUrl,
    apiKeyMasked,
    openEditProfileDialog,
    syncEditProfileState,
    handleEditProfileAvatarChange,
    handleEditProfileSave,
    handleDeleteAccount,
    handleCreateApiKey,
    handleCopyApiKey,
    handleEditProfileDialogClosed,
  };
}
