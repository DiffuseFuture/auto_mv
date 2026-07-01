package com.ohyesai.next.biz.vio.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.biz.vio.entity.ChatSession;
import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import com.ohyesai.next.biz.vio.entity.HistoryMessageChunk;
import com.ohyesai.next.biz.vio.mapper.ChatSessionMapper;
import com.ohyesai.next.biz.vio.mapper.HistoryMessageChunkMapper;
import com.ohyesai.next.biz.vio.mapper.HistoryMessageMapper;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class HistoryMessageService {

    private final ChatSessionMapper chatSessionMapper;

    private final HistoryMessageMapper historyMessageMapper;

    private final HistoryMessageChunkMapper historyMessageChunkMapper;

    public ChatSession initChatSession(String chatSessionId, String chatName, String userId, Resolution resolution) {
        if (StrUtil.isNotBlank(chatSessionId)) { // 已存在对话
            ChatSession chatSession = ChainWrappers.lambdaQueryChain(chatSessionMapper)
                    .eq(ChatSession::getId, chatSessionId)
                    .eq(ChatSession::getUserId, userId)
                    .one();
            if (chatSession == null) {
                throw new BusinessException(CodeEnum.ParameterError, "对话不存在");
            }
            // 更新对话记录 最后对话日期
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(chatSession);
            return chatSession;
        }

        // 不存在则创建
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId);
        // 确保名称不会太长
        chatSession.setName(chatName.substring(0, Math.min(99, chatName.length())));

        // 设置元数据
        ChatSession.Metadata metadata = new ChatSession.Metadata();
        metadata.setResolution(resolution);
        chatSession.setMetadata(metadata);

        LocalDateTime now = LocalDateTime.now();
        chatSession.setUpdateTime(now);
        chatSession.setCreateTime(now);
        chatSessionMapper.insert(chatSession);
        return chatSession;
    }

    /**
     * 插入单条消息
     * 通常只有 用户消息 是简单的； model 返回消息，尤其是流式+tool 调用 一次回复通常会产生大量的消息片段
     * 创建一条 history_message 和 history_message_chunk
     *
     * @param sessionId
     * @param sseMsgBO
     * @param senderType
     * @return
     */
    public void putSimpleHistoryMsg(String sessionId, SseMsgBO<?> sseMsgBO, HistoryMessage.SenderType senderType) {
        // 插入 history message
        HistoryMessage historyMessage = createHistoryMessage(sessionId, senderType, true);
        // 插入 message chunk
        putMessageChunk(historyMessage.getId(), null, sseMsgBO);
    }

    public HistoryMessageChunk putMessageChunk(String messageId, @Nullable Integer chatSessionTaskId, SseMsgBO<?> sseMsgBO) {
        // 插入 message chunk
        HistoryMessageChunk historyMessageChunk = new HistoryMessageChunk();
        historyMessageChunk.setHistoryMessageId(messageId);
        historyMessageChunk.setChatSessionTaskId(chatSessionTaskId);
        historyMessageChunk.setChunk(JsonUtil.toJson(sseMsgBO));
        historyMessageChunkMapper.insert(historyMessageChunk);
        return historyMessageChunk;
    }

    /**
     * 更新 chunk 内容（用于版本追加、版本切换等场景）
     */
    public void updateChunkContent(Integer chunkId, SseMsgBO<?> sseMsgBO) {
        ChainWrappers.lambdaUpdateChain(historyMessageChunkMapper)
                .eq(HistoryMessageChunk::getId, chunkId)
                .set(HistoryMessageChunk::getChunk, JsonUtil.toJson(sseMsgBO))
                .update();
    }

    /**
     * 根据 id 查询 chunk
     */
    public HistoryMessageChunk getChunkById(Integer chunkId) {
        return historyMessageChunkMapper.selectById(chunkId);
    }

    /**
     * 判断chunkId是否属于当前用户
     */
    public HistoryMessageChunk getChunkByIdAndUserId(Integer chunkId, String userId) {
        return historyMessageChunkMapper.selectByIdAndUser(chunkId, userId);
    }

    /**
     * 创建并插入 history message
     *
     * @param chatSessionId
     * @param senderType
     * @param finish        true 对话完成;
     * @return 插入数据库后的 尸体🧟
     */
    public HistoryMessage createHistoryMessage(String chatSessionId, HistoryMessage.SenderType senderType, boolean finish) {
        // 插入 history message
        HistoryMessage historyMessage = new HistoryMessage();
        historyMessage.setChatSessionId(chatSessionId);
        historyMessage.setSenderType(senderType);
        historyMessage.setCreateTime(LocalDateTime.now());
        historyMessage.setLikeState(HistoryMessage.LikeState.NONE);
        historyMessage.setFinish(finish);
        historyMessageMapper.insert(historyMessage);
        return historyMessage;
    }

    /**
     * 更新历史消息为 完成状态
     *
     * @param historyMessageId
     */
    public void finishHistoryMessage(String historyMessageId) {
        ChainWrappers.lambdaUpdateChain(historyMessageMapper)
                .set(HistoryMessage::getFinish, true)
                .eq(HistoryMessage::getId, historyMessageId)
                .update();
    }

    /**
     * 获取session 下 用户发送的第一个文本消息
     *
     * @param sessionId
     * @return
     */
    public SseMsgBO.Text findFirstUserText(String sessionId) {
        // 获取这个项目用户发送的第一个消息
        HistoryMessage historyMessage = ChainWrappers.lambdaQueryChain(historyMessageMapper)
                .eq(HistoryMessage::getChatSessionId, sessionId)
                .eq(HistoryMessage::getSenderType, HistoryMessage.SenderType.USER)
                .orderByAsc(HistoryMessage::getSeqNo)
                .last("limit 1")
                .one();

        if (historyMessage == null) {
            throw new BusinessException(CodeEnum.Unknow, "sessionId 无法获取对应数据：" + sessionId);
        }

        HistoryMessageChunk historyMessageChunk = ChainWrappers.lambdaQueryChain(historyMessageChunkMapper)
                .eq(HistoryMessageChunk::getHistoryMessageId, historyMessage.getId())
                .orderByAsc(HistoryMessageChunk::getId)
                .last("limit 1")
                .one();

        JsonNode payload = JsonUtil.readTree(historyMessageChunk.getChunk());
        SseMsgBO.Type type = JsonUtil.tree2Object(payload.path("type"), SseMsgBO.Type.class);
        if (type != SseMsgBO.Type.TEXT) {
            log.error("获取用户发送的第一个消息失败，类型不是文本 content: {}", historyMessageChunk);
            throw new BusinessException(CodeEnum.Unknow, "分享失败");
        }
        JsonNode data = payload.path("data");
        return JsonUtil.tree2Object(data, SseMsgBO.Text.class);
    }

}
