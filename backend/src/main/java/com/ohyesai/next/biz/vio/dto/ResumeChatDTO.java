package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "ResumeChatDTO")
public class ResumeChatDTO {

    @Schema(description = "聊天id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "聊天id不能为空")
    private String sessionId;

    @Schema(description = "聊天消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "聊天消息ID不能为空")
    private String historyMessageId;
}
