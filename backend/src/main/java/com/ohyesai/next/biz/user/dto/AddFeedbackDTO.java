package com.ohyesai.next.biz.user.dto;

import com.ohyesai.next.biz.user.entity.UserFeedback;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "添加反馈信息")
public class AddFeedbackDTO {

    @Schema(description = "messageId")
    @NotBlank(message = "messageId 不能为空")
    private String messageId;

    @Schema(description = "态度类型")
    @NotNull(message = "态度类型不能为空")
    private UserFeedback.AttitudeType attitudeType;

    @Schema(description = "反馈内容")
    private String content;
}
