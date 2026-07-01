package com.ohyesai.next.biz.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送短信验证码参数")
public class SendSmsCodeDTO {

    @Schema(description = "手机号; 如果为空则给当前登录用户发送")
    private String mobile;

}
