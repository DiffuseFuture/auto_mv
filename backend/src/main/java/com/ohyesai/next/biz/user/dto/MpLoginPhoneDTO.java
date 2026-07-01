package com.ohyesai.next.biz.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "小程序登录参数")
public class MpLoginPhoneDTO {

    @Schema(description = "wx.login code")
    @NotBlank(message = "code不能为空")
    private String code;

    @Schema(description = "手机号code")
    @NotBlank(message = "手机号code不能为空")
    private String phoneCode;
}
