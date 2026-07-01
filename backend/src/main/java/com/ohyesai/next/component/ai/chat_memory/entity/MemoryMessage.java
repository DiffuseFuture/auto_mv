package com.ohyesai.next.component.ai.chat_memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 持久化存储内存记忆
 */
@Data
public class MemoryMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String sessionId;

    private String content;

    private int seqNo;

    private LocalDateTime createTime;

}
