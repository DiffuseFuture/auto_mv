package com.ohyesai.next.biz.billing.vo;

import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.util.JsonUtil;
import dev.langchain4j.agent.tool.P;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Schema(description = "订阅计划")
@Data
public class SubscriptionPlanVO {

    @Schema(description = "免费")
    private List<PLan> free;

    @Schema(description = "月付")
    private List<PLan> monthly;

    @Schema(description = "年付")
    private List<PLan> yearly;


    public SubscriptionPlanVO(List<PLan> free, List<PLan> monthly, List<PLan> yearly) {
        this.free = free;
        this.monthly = monthly;
        this.yearly = yearly;
    }

    @Data
    public static class PLan {

        @Schema(description = "订阅编码")
        private String tierCode;

        @Schema(description = "会员套餐名称")
        private String tierName;

        @Schema(description = "订阅价格/元")
        private String price;

        @Schema(description = "订阅价值 元/月")
        private String priceMonth;

        @Schema(description = "每月赠送积分数")
        private Integer grantedPoints;

        @Schema
        private SubscriptionPlan.Description description;

        public static PLan from(SubscriptionPlan subscriptionPlan) {
            SubscriptionPlanVO.PLan plan = new SubscriptionPlanVO.PLan();
            plan.setTierCode(subscriptionPlan.getTierCode());
            plan.setTierName(subscriptionPlan.getTierName());

            BigDecimal subPrice = new BigDecimal(subscriptionPlan.getPrice());
            // 将分 换算 元
            plan.setPrice(subPrice.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toString());

            // 除12 计算一个月的价钱 并转为 元
            switch (subscriptionPlan.getSubscriptionType()) {
                // 月订阅 总价就是月单价
                case MONTH, FREE -> plan.setPriceMonth(plan.getPrice());
                // 年订阅 总价除12 就是月单价 1200 是算单价并转为元
                case YEAR ->
                        plan.setPriceMonth(subPrice.divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP).toString());
            }


            plan.setGrantedPoints(subscriptionPlan.getGrantedPoints());

            if (StrUtil.isNotBlank(subscriptionPlan.getDescription())) {
                plan.setDescription(JsonUtil.toObject(subscriptionPlan.getDescription(), SubscriptionPlan.Description.class));
            }

            return plan;
        }
    }
}
