package com.ohyesai.next.biz.billing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.ZonedDateTime;

@Schema(description = "用户订阅计划")
@Data
public class UserPlanVO {

    @Schema(description = "用户剩余积分")
    private Integer pointsBalance;

    @Schema(description = "订阅计划code")
    private String tierCode;

    @Schema(description = "订阅计划名称")
    private String tierName;

    @Schema(description = "订阅计划有效期; null：无订阅； 小于当前时间：订阅过期")
    private ZonedDateTime expireTime;

}
