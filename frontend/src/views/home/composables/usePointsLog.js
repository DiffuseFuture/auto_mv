import { ref } from 'vue';
import { getPointsLog } from '@/api/creation';
import { ElMessage } from 'element-plus';
import { useI18nText } from '@/i18n';

/**
 * 积分交易日志Composable
 * @param {Object} options 配置项
 * @param {number} [options.pageSize=10] 每页数量
 * @param {number} [options.scrollBottomOffset=30] 滚动到底部的偏移量
 * @returns {Object} 积分日志相关状态和方法
 */
export function usePointsLog({ pageSize = 10, scrollBottomOffset = 30 } = {}) {
  const { t } = useI18nText();

  const pointsLogList = ref([]); // 积分交易日志列表数据
  const pointsLogLoading = ref(false); // 积分交易日志加载状态
  const logPage = ref(1); // 积分交易日志当前页码
  const hasMorePointsLog = ref(true); // 是否还有更多积分交易日志

  /**
   * 获取积分交易日志列表（分页追加）。
   */
  const fetchPointsLog = async () => {
    if (pointsLogLoading.value || !hasMorePointsLog.value) return;
    pointsLogLoading.value = true;
    try {
      const res = await getPointsLog({ page: logPage.value, size: pageSize });
      pointsLogList.value.push(...(res.data || []));
      hasMorePointsLog.value = pointsLogList.value.length < res.total;
      logPage.value++;
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || t('account.points.logFetchFail'));
    } finally {
      pointsLogLoading.value = false;
    }
  };

  /**
   * 重置分页状态并加载第一页积分交易日志。
   */
  const resetAndFetchPointsLog = () => {
    pointsLogList.value = [];
    logPage.value = 1;
    hasMorePointsLog.value = true;
    fetchPointsLog();
  };

  /**
   * 滚动到底部时加载下一页积分交易日志。
   * @param {Event} e 滚动事件
   */
  const handlePointsLogScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;
    if (scrollHeight - scrollTop - clientHeight < scrollBottomOffset) {
      fetchPointsLog();
    }
  };

  return {
    pointsLogList,
    pointsLogLoading,
    logPage,
    hasMorePointsLog,
    resetAndFetchPointsLog,
    handlePointsLogScroll,
  };
}