package com.ohyesai.next.biz.vio.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Schema(description = "会话信息")
public class ChatSessionVO {
    @Schema(description = "会话id")
    private String sessionId;

    @Schema(description = "会话名称")
    private String sessionName;

    @Schema(description = "会话封面")
    private String sessionCover;

    @Schema(description = "更新日志")
    private ZonedDateTime updateTime;
}
