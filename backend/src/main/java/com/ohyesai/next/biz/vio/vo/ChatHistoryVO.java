package com.ohyesai.next.biz.vio.vo;

import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import com.ohyesai.next.common.enums.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "聊天记录")
@Data
public class ChatHistoryVO {

    @Schema(description = "分辨率")
    private Resolution resolution;

    @Schema(description = "是否是继续对话; 继续对话：true")
    private Boolean resumeChat;

    @Schema(description = "会话id")
    private String sessionId;

    @Schema(description = "最后一个消息id")
    private String lastMessageId;

    @Schema(description = "历史消息记录")
    private List<HistoryMessageVO> chatMessages;


    @Schema(description = "历史消息记录")
    @Data
    public static class HistoryMessageVO {
        @Schema(description = "消息id")
        private String messageId;

        @Schema(description = "序号")
        private Integer seqNo;

        @Schema(description = "消息发送者")
        private HistoryMessage.SenderType senderType;

        @Schema(description = "消息块")
        private List<HistoryMessageChunkVO> messageChunks;

    }

    @Data
    @Schema(description = "历史消息块")
    public static class HistoryMessageChunkVO {

        @Schema(description = "消息块id")
        private String messageChunkId;

        @Schema(description = "块内容")
        private SseMsgBO<Object> content;

    }
}
