package com.ohyesai.next.biz.vio.bo;

import lombok.Data;

import java.util.List;

@Data
public class ChatHistoryBO {

    private String sessionId;

    private List<HistoryMessageBO> chatMessages;

}
