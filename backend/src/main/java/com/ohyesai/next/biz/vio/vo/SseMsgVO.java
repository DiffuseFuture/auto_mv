package com.ohyesai.next.biz.vio.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于推到前端使用的数据格式
 * 扩展了  SseMsgBO 的数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseMsgVO {

    private String messageId;

    private Integer messageChunkId;

    /**
     * SseMsgBO 的数据,由于是范型 无法序列化,所以这里使用JsonNode
     */
    private JsonNode content;
}
