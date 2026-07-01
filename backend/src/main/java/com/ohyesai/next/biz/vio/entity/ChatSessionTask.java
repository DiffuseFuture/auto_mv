package com.ohyesai.next.biz.vio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ChatSessionTask {

    public static final String AGENT_FRIENDLY_TASK_ID_PREFIX = "Task_";

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String chatSessionId;

    private String payload;

    /**
     * 获取 agent 友好的 taskId
     */
    public String agentFriendlyTaskId() {
        return AGENT_FRIENDLY_TASK_ID_PREFIX + id;
    }
}
