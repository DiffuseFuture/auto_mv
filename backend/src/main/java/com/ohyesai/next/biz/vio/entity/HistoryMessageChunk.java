package com.ohyesai.next.biz.vio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class HistoryMessageChunk {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String historyMessageId;

    /**
     * chat_session_task_id
     */
    private Integer chatSessionTaskId;

    /**
     * json数据
     * 结构遵循：com.ohyesai.next.biz.vio.bo.SseMsgBO
     */
    private String chunk;
}
