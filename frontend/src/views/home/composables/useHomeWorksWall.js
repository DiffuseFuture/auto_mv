import {ref} from 'vue';
import {getCommonPreview} from '@/api/resource';

/**
 * 首页创作广场瀑布流：分页、加载、封面比例预载与滚动触底加载。
 * @param {Object} options
 * @param {number} [options.pageSize=20]
 * @param {number} [options.scrollBottomOffset=80]
 * @param {(params: {page:number,size:number}) => Promise<any>} [options.fetcher]
 * @param {(ctx: {reset:boolean}) => void} [options.onBeforeLoad]
 * @param {(ctx: {reset:boolean,items:Array,total:number}) => void} [options.onAfterLoad]
 * @param {(ctx: {reset:boolean,success:boolean,error?:any}) => void} [options.onFinally]
 * @param {(error:any) => void} [options.onError]
 * @returns {{
 *   worksList: import('vue').Ref<Array>,
 *   worksLoading: import('vue').Ref<boolean>,
 *   worksHasMore: import('vue').Ref<boolean>,
 *   loadWorksList: (reset?: boolean) => Promise<void>,
 *   handleWorksScroll: (event: Event) => void
 * }}
 */
export const useHomeWorksWall = (options = {}) => {
  const {
    pageSize = 20,
    scrollBottomOffset = 80,
    fetcher = getCommonPreview,
    onBeforeLoad,
    onAfterLoad,
    onFinally,
    onError,
  } = options;

  const worksList = ref([]);
  const worksLoading = ref(false);
  const worksPage = ref(1);
  const worksHasMore = ref(true);

  /**
   * 预加载图片并把比例归一到两档：横向 16:9 / 竖向 9:16。
   * 避免后端封面是 9:21、4:5 之类奇怪比例时，瀑布流卡片高度参差不齐。
   * @param {string} url
   * @returns {Promise<number>}
   */
  const preloadImageDimensions = (url) => {
    return new Promise((resolve) => {
      if (!url) {
        resolve(16 / 9);
        return;
      }
      const img = new Image();
      img.onload = () => {
        const w = img.naturalWidth || img.width;
        const h = img.naturalHeight || img.height;
        if (!w || !h) return resolve(16 / 9);
        // 横向 / 方形按 16:9，竖向按 9:16
        resolve(w >= h ? 16 / 9 : 9 / 16);
      };
      img.onerror = () => resolve(16 / 9);
      img.src = url;
    });
  };

  /**
   * 获取创作广场分页数据。
   * @param {boolean} reset
   * @returns {Promise<void>}
   */
  const loadWorksList = async (reset = false) => {
    if (worksLoading.value) return;
    if (!reset && !worksHasMore.value) return;

    onBeforeLoad?.({reset});
    if (reset) {
      worksPage.value = 1;
      worksHasMore.value = true;
      worksList.value = [];
    }

    worksLoading.value = true;
    let error = null;
    try {
      const res = await fetcher({page: worksPage.value, size: pageSize});
      const items = res?.data || [];
      await Promise.all(
        items.map(async (item) => {
          item._coverRatio = await preloadImageDimensions(item.fileCoverUrl);
        }),
      );
      worksList.value = reset ? items : [...worksList.value, ...items];
      worksHasMore.value = worksList.value.length < (res?.total || 0);
      worksPage.value += 1;
      onAfterLoad?.({reset, items, total: res?.total || 0});
    } catch (err) {
      error = err;
      onError?.(err);
    } finally {
      worksLoading.value = false;
      onFinally?.({reset, success: !error, error});
    }
  };

  /**
   * 滚动接近底部自动加载下一页。
   * @param {Event} event
   */
  const handleWorksScroll = (event) => {
    if (worksLoading.value || !worksHasMore.value) return;
    const {scrollTop, scrollHeight, clientHeight} = event.target;
    if (scrollHeight - scrollTop - clientHeight < scrollBottomOffset) {
      loadWorksList(false);
    }
  };

  return {
    worksList,
    worksLoading,
    worksHasMore,
    loadWorksList,
    handleWorksScroll,
  };
};
