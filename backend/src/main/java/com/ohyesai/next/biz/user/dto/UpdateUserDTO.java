package com.ohyesai.next.biz.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新用户信息DTO")
public class UpdateUserDTO {

    @Schema(description = "用户id; 管理员使用时该字段必须有值,普通用户可以为空")
    private String userId;

    @Schema(description = "昵称")
    private String nickName;

//    @Schema(description = "手机号")
//    private String mobile;
//
//    @Schema(description = "验证码; 当修改手机号时需要传入验证码; 管理员修改时无需传入验证码")
//    private String mobileCode;
}
