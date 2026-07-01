package com.ohyesai.next.biz.billing.vo;

import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Schema(description = "积分包")
public class PointsPackageVO {

    @Schema(description = "积分包编码")
    private String tierCode;

    @Schema(description = "积分包名称")
    private String tierName;

    @Schema(description = "价格/元")
    private String price;

    public static PointsPackageVO from(SubscriptionPlan subscriptionPlan) {
        PointsPackageVO pointsPackageVO = new PointsPackageVO();
        pointsPackageVO.setTierCode(subscriptionPlan.getTierCode());
        pointsPackageVO.setTierName(subscriptionPlan.getTierName());
        // 将分 换算 元
        BigDecimal subPrice = new BigDecimal(subscriptionPlan.getPrice());
        pointsPackageVO.setPrice(subPrice.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toString());
        return pointsPackageVO;
    }
}
