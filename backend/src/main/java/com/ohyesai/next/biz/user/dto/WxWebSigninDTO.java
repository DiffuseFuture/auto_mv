package com.ohyesai.next.biz.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信网页登录参数")
public class WxWebSigninDTO {

    @Schema(description = "微信网页登录code")
    @NotBlank(message = "code不能为空")
    private String code;

    @Schema(description = "邀请码")
    private String inviteCode;
}
