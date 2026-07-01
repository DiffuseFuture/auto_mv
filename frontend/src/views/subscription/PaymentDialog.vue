<template>
  <el-dialog
      v-model="visible"
      width="min(420px, 92vw)"
      :show-close="false"
      align-center
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="payment-dialog"
  >
    <div class="bg-white rounded-[24px] overflow-hidden p-8 relative">
      <!-- 关闭按钮 -->
      <div class="absolute top-4 right-4 cursor-pointer hover:opacity-70 transition-opacity" @click="handleClose">
        <svg-icon name="gy-closure" size="32" color="#999999"></svg-icon>
      </div>

      <!-- 标题：订单创建后金额从 createTransaction 返回的 amountF 覆盖，未下单前用 props.price 占位 -->
      <div class="text-center mb-6">
        <div class="text-[22px] font-bold text-[#000000] mb-1">{{ planName }}</div>
        <div class="text-[32px] font-bold text-[#000000]">¥{{ displayAmount }}</div>
      </div>

      <!-- 支付方式 -->
      <div class="text-center mb-6">
        <span class="inline-flex items-center px-4 py-2 rounded-[20px] bg-[#000000] text-[#C2FF00] text-[14px] font-medium">{{ t('subscription.paymentDialog.wechatPay') }}</span>
      </div>

      <!-- 二维码区域 -->
      <div class="flex-center mx-auto w-[240px] h-[240px] bg-[#F5F5F0] rounded-[16px] mb-4">
        <div v-if="orderLoading" class="text-[#A7A7A6] text-[14px]">{{ t('subscription.paymentDialog.creatingOrder') }}</div>
        <div v-else-if="orderError" class="text-center px-4">
          <div class="text-[#000000] text-[14px] mb-2">{{ orderError }}</div>
          <button
            v-if="orderErrorCode !== 4004"
            class="text-[14px] text-[#C2FF00] bg-[#000000] rounded-lg px-4 py-1 cursor-pointer"
            @click="createOrder"
          >
            {{ t('subscription.paymentDialog.retry') }}
          </button>
        </div>
        <img v-else-if="qrCodeDataUrl" :src="qrCodeDataUrl" alt="pay-qrcode" class="w-[208px] h-[208px] rounded-[8px]"/>
        <div v-else class="text-[#A7A7A6] text-[14px]">{{ t('subscription.paymentDialog.preparingPaymentInfo') }}</div>
      </div>

      <!-- 提示文字 -->
      <div class="text-center text-[14px] text-[#A7A7A6]">{{ isMobile ? t('subscription.paymentDialog.saveImageTip') : t('subscription.paymentDialog.scanTip') }}</div>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, ref, watch, onBeforeUnmount} from 'vue';
import {ElMessage} from 'element-plus';
import QRCode from 'qrcode';
import {createTransaction, getTransactionState} from '@/api/creation';
import {useI18nText} from '@/i18n';
import {reportBaiduConvert, BAIDU_CONVERT_TYPE} from '@/utils/baiduTrack';
import {isMobileClient} from '@/utils/index.js';

const isMobile = isMobileClient();

const props = defineProps({
  modelValue: Boolean,
  /** 套餐编码 */
  tierCode: String,
  /** 套餐名称（用于显示） */
  planName: String,
  /** 价格（用于显示） */
  price: [String, Number],
});
const emit = defineEmits(['update:modelValue', 'success']);
const {t} = useI18nText();

const visible = ref(false);
const orderLoading = ref(false);
const orderError = ref('');
const orderErrorCode = ref(null);
const tradeNo = ref('');
const qrCodeDataUrl = ref('');
/** 订单接口返回的 amountF（单位：分），下单成功后覆盖标题金额；未下单前用 props.price 占位 */
const orderAmount = ref('');

/**
 * 分转元，按 zh-CN 本地化格式化（千分位 + 至多 2 位小数，整数不补零，类似 Java DecimalFormat "0.##"）。
 *
 * 金额最佳实践：
 * - 后端按整数"分"传，前端按整数运算，不引入浮点；
 * - 必要的舍入交给 Intl.NumberFormat 内部处理，roundingMode: 'halfEven' 是银行家舍入
 *   （会计 / 金融标准，避免 Math.round 那种"0.5 总向上"的系统性偏差，以及负数舍入不对称问题）。
 * - 不用 Math.round 兜底"后端误传带小数的分"——后端契约就是整数分，遵循类型契约信任原则。
 */
const yuanFormatter = new Intl.NumberFormat('zh-CN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
  roundingMode: 'halfEven',
});

const formatYuanFromFen = (fen) => yuanFormatter.format(Number(fen) / 100);

const displayAmount = computed(() => (orderAmount.value ? formatYuanFromFen(orderAmount.value) : props.price));
let pollTimer = null;

/** 创建微信支付订单并生成二维码 */
const createOrder = async () => {
  orderLoading.value = true;
  orderError.value = '';
  orderErrorCode.value = null;
  qrCodeDataUrl.value = '';
  orderAmount.value = '';
  stopPolling();

  try {
    const res = await createTransaction({
      tierCode: props.tierCode,
      payKind: 'WX_PC',
    });
    tradeNo.value = res.tradeNo;
    orderAmount.value = res.amountF;

    const payContent = (res.payContent || '').trim();
    if (!payContent) {
      orderError.value = t('subscription.paymentDialog.invalidPaymentInfo');
      return;
    }

    qrCodeDataUrl.value = await QRCode.toDataURL(payContent, {
      width: 208,
      margin: 2,
      color: {dark: '#000000', light: '#F5F5F0'},
    });

    // 开始轮询支付状态
    startPolling();
  } catch (error) {
    console.error(error);
    orderErrorCode.value = Number(error?.code);
    orderError.value = error?.message || t('subscription.paymentDialog.createOrderFailed');
    if (Number(error?.code) !== 4004) {
      ElMessage.error(error?.message || t('subscription.paymentDialog.createOrderFailed'));
    }
  } finally {
    orderLoading.value = false;
  }
};

/** 轮询支付状态（每 2 秒一次） */
const startPolling = () => {
  stopPolling();
  pollTimer = setInterval(async () => {
    if (!tradeNo.value) return;
    try {
      const state = await getTransactionState({tradeNo: tradeNo.value});
      if (state === 0) {
        stopPolling();
        // 百度推广转化上报（订阅支付成功）
        reportBaiduConvert(BAIDU_CONVERT_TYPE.SERVICE_SUCCESS);
        ElMessage.success(t('subscription.paymentDialog.paySuccess'));
        visible.value = false;
        emit('success');
      } else if (state === 2) {
        stopPolling();
        orderError.value = t('subscription.paymentDialog.payFailedOrTimeout');
      }
      // state === 3 继续轮询
    } catch (error) {
      console.error(error);
    }
  }, 2000);
};

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

/** 关闭弹窗 */
const handleClose = () => {
  stopPolling();
  tradeNo.value = '';
  orderError.value = '';
  qrCodeDataUrl.value = '';
  orderAmount.value = '';
  visible.value = false;
};

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val && props.tierCode) {
    createOrder();
  } else {
    stopPolling();
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
  if (!val) {
    stopPolling();
    tradeNo.value = '';
    orderError.value = '';
    qrCodeDataUrl.value = '';
    orderAmount.value = '';
  }
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style lang="scss">
.payment-dialog {
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
</style>
