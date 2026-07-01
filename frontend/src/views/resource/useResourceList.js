import {ref, watch, onBeforeUnmount} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {findMv, findMusic, deleteProject, renameProject} from '@/api/resource';
import {saveUserTracking} from '@/api/tracking';
import {shareByProjectId} from '@/utils/share.js';

/**
 * Resource page shared business logic (desktop & mobile).
 *
 * Responsibilities:
 * - MV/music list fetching with pagination
 * - tab switching
 * - share/download/rename/delete actions
 * - music preview playback state (via a single <audio> element)
 *
 * UI responsibilities (dialogs layout, cards, preview video, etc.) stay in view components.
 */
export function useResourceList({
  t,
  getLangPrefix,
  pageSize = 20,
  debounceMs = 300,
  renamePromptClass = 'rename-confirm',
  deleteConfirmClass = 'resource-delete-confirm',
} = {}) {
  if (typeof t !== 'function') throw new Error('useResourceList: missing t()');
  if (typeof getLangPrefix !== 'function') throw new Error('useResourceList: missing getLangPrefix()');

  const searchKeyword = ref('');
  const activeTab = ref('mv'); // 'mv' | 'music'
  const list = ref([]);
  const loading = ref(false);

  // pagination state
  let page = 1;
  let total = 0;
  let searchTimer = null;

  const resetPaging = () => {
    page = 1;
    total = 0;
    list.value = [];
  };

  /** Load current tab list. If reset=true, start from page 1. */
  const loadList = async (reset = false) => {
    if (loading.value) return;
    if (!reset && list.value.length >= total) return;

    if (reset) resetPaging();

    loading.value = true;
    try {
      const api = activeTab.value === 'mv' ? findMv : findMusic;
      const res = await api({
        page,
        size: pageSize,
        projectName: searchKeyword.value.trim(),
      });
      list.value = reset ? res.data : [...list.value, ...res.data];
      total = res.total;
      page++;
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

  const handleTabChange = (tab) => {
    if (activeTab.value === tab) return;
    activeTab.value = tab;
    stopAudio();
    loadList(true);
  };

  // search debounce (desktop uses it; mobile can ignore by not binding input)
  watch(searchKeyword, () => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => loadList(true), debounceMs);
  });

  /** Infinite scroll trigger. Works with any scroll container that calls it. */
  const handleScrollLoadMore = (scrollTop, scrollHeight, clientHeight, threshold = 60) => {
    if (scrollHeight - scrollTop - clientHeight < threshold) loadList(false);
  };

  const handleGoToChat = (router, sessionId) => {
    const lang = getLangPrefix();
    router.push({name: 'creation', params: {lang}, query: {sessionId}});
  };

  const handleShare = async (projectId) => {
    const {mode, error} = await shareByProjectId({
      projectId,
      langPrefix: getLangPrefix(),
      trackingTarget: activeTab.value === 'mv' ? 'PROJECT_MV_SHARE' : 'PROJECT_MUSIC_SHARE',
    });
    if (mode === 'native' || mode === 'cancelled') return;
    if (mode === 'clipboard') ElMessage.success(t('resource.shareSuccess'));
    else ElMessage.error(error?.message || t('resource.shareFailed'));
  };

  const handleDownload = (item) => {
    const url = String(item?.fileUrl || '');
    if (!url) return;

    const downloadTarget = activeTab.value === 'mv' ? 'PROJECT_MV_DOWNLOAD' : 'PROJECT_MUSIC_DOWNLOAD';
    saveUserTracking({target: downloadTarget}).catch((error) => {
      console.error('资产页下载埋点上报失败:', error);
    });

    const link = document.createElement('a');
    link.href = url;
    link.download = String(item?.projectName || 'download');
    link.target = '_blank';
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleRename = async (item) => {
    try {
      const {value} = await ElMessageBox.prompt('', t('resource.renameTitle'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValue: item.projectName || '',
        inputPlaceholder: t('resource.renamePlaceholder'),
        inputPattern: /.+/,
        inputErrorMessage: t('resource.renameRequired'),
        showClose: false,
        closeOnClickModal: false,
        customClass: renamePromptClass,
      });
      await renameProject({projectId: item.projectId, projectName: value});
      item.projectName = value;
      ElMessage.success(t('resource.renameSuccess'));
    } catch {
      // user cancelled
    }
  };

  const handleDelete = async (item) => {
    try {
      await ElMessageBox.confirm(
        t('resource.deleteConfirmMessage', {
          type: activeTab.value === 'mv' ? t('resource.mvType') : t('resource.musicType'),
        }),
        t('resource.deleteConfirmTitle'),
        {
          confirmButtonText: t('resource.deleteConfirmBtn'),
          cancelButtonText: t('common.cancel'),
          showClose: false,
          closeOnClickModal: false,
          customClass: deleteConfirmClass,
        },
      );
      await deleteProject({projectId: item.projectId});
      list.value = list.value.filter((i) => i.projectId !== item.projectId);
      ElMessage.success(t('resource.deleteSuccess'));
    } catch {
      // user cancelled
    }
  };

  // ---- music preview (single <audio>) ----
  const audioRef = ref(null);
  const playingId = ref('');
  const isPlaying = ref(false);

  const stopAudio = () => {
    const audio = audioRef.value;
    if (audio) audio.pause();
    isPlaying.value = false;
    playingId.value = '';
  };

  const handleTogglePlay = (item) => {
    const audio = audioRef.value;
    if (!audio || !item?.fileUrl) return;

    if (playingId.value === item.projectId) {
      if (isPlaying.value) {
        audio.pause();
        isPlaying.value = false;
      } else {
        audio.play();
        isPlaying.value = true;
      }
      return;
    }

    playingId.value = item.projectId;
    audio.src = item.fileUrl;
    audio.play();
    isPlaying.value = true;
  };

  const onAudioEnded = () => {
    isPlaying.value = false;
  };

  const onAudioError = () => {
    isPlaying.value = false;
    playingId.value = '';
  };

  onBeforeUnmount(() => {
    if (searchTimer) clearTimeout(searchTimer);
    stopAudio();
  });

  return {
    // state
    searchKeyword,
    activeTab,
    list,
    loading,

    // list actions
    loadList,
    handleTabChange,
    handleScrollLoadMore,

    // item actions
    handleGoToChat,
    handleShare,
    handleDownload,
    handleRename,
    handleDelete,

    // music preview
    audioRef,
    playingId,
    isPlaying,
    handleTogglePlay,
    onAudioEnded,
    onAudioError,
    stopAudio,
  };
}

