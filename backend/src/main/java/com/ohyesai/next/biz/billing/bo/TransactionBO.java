package com.ohyesai.next.biz.billing.bo;


import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.common.enums.StateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单信息; 用于缓存订单然后在回调中使用")
public class TransactionBO {

    @Schema(description = "订单号")
    private String tradeNo;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "套餐类型")
    private SubscriptionPlan subscriptionPlan;

    @Schema(description = "订单状态; 待支付、已支付")
    private StateEnum status;

    @Schema(description = "当前月已经使用的积分；充值时本月需要扣减的积分; 适用于升级套餐")
    private int usedPoints;

    @Schema(description = "升级、首次 订阅")
    private boolean upgrade;
}
