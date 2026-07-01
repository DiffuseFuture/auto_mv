package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "删除聊天记录参数")
@Data
public class DeleteChatDTO {

    @Schema(description = "session id")
    @NotBlank(message = "sessionId不能为空")
    private String sessionId;
}
