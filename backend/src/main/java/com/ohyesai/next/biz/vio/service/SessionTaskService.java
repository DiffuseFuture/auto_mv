package com.ohyesai.next.biz.vio.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.vio.bo.ChatSessionTaskPayload;
import com.ohyesai.next.biz.vio.entity.ChatSessionTask;
import com.ohyesai.next.biz.vio.mapper.ChatSessionTaskMapper;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SessionTaskService {

    private final ChatSessionTaskMapper chatSessionTaskMapper;

    /**
     * 根据session Id 获取最新一条任务数据
     *
     * @param chatSessionId
     * @return
     */
    public ChatSessionTaskPayload findTaskPayloadBySession(String chatSessionId) {
        ChatSessionTask one = ChainWrappers.lambdaQueryChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getChatSessionId, chatSessionId)
                .orderByDesc(ChatSessionTask::getId)
                .last("limit 1")
                .one();

        if (one == null) {
            return null;
        }

        return ChatSessionTaskPayload.from(one);
    }

    /**
     * 初始化会话任务
     *
     * @param sessionId
     * @return
     */
    public ChatSessionTask initTaskBySession(String sessionId) {
        ChatSessionTask chatSessionTask = new ChatSessionTask();
        chatSessionTask.setChatSessionId(sessionId);
        chatSessionTaskMapper.insert(chatSessionTask);
        return chatSessionTask;
    }

    /**
     * 更新任务数据 payload
     *
     * @return false: 更新失败
     */
    public boolean updateTaskPayload(String taskId, String chatSessionId, ChatSessionTaskPayload payload) {
        // 移除 agent 友好的 taskId
        if (taskId.startsWith(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX)) {
            taskId = taskId.substring(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX.length());
        }

        int id = Integer.parseInt(taskId);
        return ChainWrappers.lambdaUpdateChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getId, id)
                .eq(ChatSessionTask::getChatSessionId, chatSessionId)
                .set(ChatSessionTask::getPayload, JsonUtil.toJson(payload))
                .update();
    }

    /**
     * 更新任务数据 payload
     * 方法不会校验数据范围，需要外部调用者保证
     *
     * @param taskId
     * @param payload
     * @return
     */
    public boolean updateTaskPayload(Integer taskId, ChatSessionTaskPayload payload) {
        return ChainWrappers.lambdaUpdateChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getId, taskId)
                .set(ChatSessionTask::getPayload, JsonUtil.toJson(payload))
                .update();
    }

    /**
     * 根据任务id获取任务数据 payload
     *
     * @param taskId
     * @return
     */
    public ChatSessionTaskPayload getTaskPayload(String taskId, String chatSessionId) {
        // 移除 agent 友好的 taskId
        int id = desugarTaskId(taskId);
        ChatSessionTask chatSessionTask = ChainWrappers.lambdaQueryChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getId, id)
                .eq(ChatSessionTask::getChatSessionId, chatSessionId)
                .one();

        return ChatSessionTaskPayload.from(chatSessionTask);
    }

    /**
     * 根据任务id获取任务数据 payload
     * 方法不会校验数据范围，需要外部调用者保证
     *
     * @param taskId
     * @return
     */
    public ChatSessionTaskPayload getTaskPayload(Integer taskId) {
        // 移除 agent 友好的 taskId
        ChatSessionTask chatSessionTask = ChainWrappers.lambdaQueryChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getId, taskId)
                .one();

        return ChatSessionTaskPayload.from(chatSessionTask);
    }

    /**
     * task id 脱糖
     * <p>本质是去掉 agent 友好的 taskId 前缀并转为int类型
     * <p>例如：Task_1 -> 1
     *
     * @param taskId
     * @return
     */
    public int desugarTaskId(String taskId) {
        // 移除 agent 友好的 taskId
        if (taskId.startsWith(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX)) {
            taskId = taskId.substring(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX.length());
        }

        return Integer.parseInt(taskId);
    }

    /**
     * 校验 任务 id 格式是否合法
     *
     * @param taskId
     * @return
     */
    public void checkTaskIdFormatIsLegal(String taskId, String chatSessionId) {
        if (StrUtil.isBlank(taskId) || !taskId.matches(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX + "\\d+")) {
            throw new IllegalArgumentException("任务ID(taskId) 格式错误");
        }
        // 移除 agent 友好的 taskId
        if (taskId.startsWith(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX)) {
            taskId = taskId.substring(ChatSessionTask.AGENT_FRIENDLY_TASK_ID_PREFIX.length());
        }
        // 判断taskid 是否存在
        if (!ChainWrappers.lambdaQueryChain(chatSessionTaskMapper)
                .eq(ChatSessionTask::getId, Integer.parseInt(taskId))
                .eq(ChatSessionTask::getChatSessionId, chatSessionId)
                .exists()) {
            throw new IllegalArgumentException("任务ID(taskId) 不存在");
        }
    }
}
