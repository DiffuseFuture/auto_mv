package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import lombok.Data;

import java.util.List;

@Data
public class HistoryMessageBO {

    private String messageId;

    private Integer seqNo;

    private HistoryMessage.SenderType senderType;

    private Boolean finish;

    private List<HistoryMessageChunkBO> messageChunks;

}
