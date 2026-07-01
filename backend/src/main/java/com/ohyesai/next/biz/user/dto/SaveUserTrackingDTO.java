package com.ohyesai.next.biz.user.dto;

import com.ohyesai.next.biz.user.entity.UserTracking;
import com.ohyesai.next.common.enums.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户行为追踪")
@Data
public class SaveUserTrackingDTO {

    /**
     * 来源；从那个网站跳过来的
     */
    @Schema(description = "来源；从那个网站跳过来的")
    private String referer;

    /**
     * 目标，访问的是哪个功能点
     */
    @Schema(description = "目标，访问的是哪个功能点")
    @NotNull(message = "目标不能为空")
    private UserTracking.Target target;

    @Schema(description = "平台")
    @NotNull(message = "平台不能为空")
    private Platform platform;
}
