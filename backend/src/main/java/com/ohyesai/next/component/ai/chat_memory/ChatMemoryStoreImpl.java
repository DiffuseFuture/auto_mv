package com.ohyesai.next.component.ai.chat_memory;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.ohyesai.next.biz.vio.entity.ChatSession;
import com.ohyesai.next.biz.vio.mapper.ChatSessionMapper;
import com.ohyesai.next.component.ai.chat_memory.entity.MemoryMessage;
import com.ohyesai.next.component.ai.chat_memory.mapper.MemoryMessageMapper;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.XmlUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//@Component
@AllArgsConstructor
@Slf4j
public class ChatMemoryStoreImpl implements ChatMemoryStore {

    private final ChatSessionMapper chatSessionMapper;

    private final MemoryMessageMapper memoryMessageMapper;

    /**
     * 标识主要用来记录  某个 memoryId 是否首次加载消息，
     * 如果是首次加载消息可以执行已经持久化数据的清理工作
     * 目前框架持久化的消息可能会出现 模型要调用某个工具，结果服务重启了 导致下次请求模型的时候没有工具返回结果 导致模型400错误
     */
    private final Set<Object> sanitizedIds = ConcurrentHashMap.newKeySet();

    /**
     * ChatMessageDeserializer.messageFromJson(String) and
     * ChatMessageDeserializer.messagesFromJson(String) helper methods can be used to
     * easily deserialize chat messages from JSON.
     *
     * @param memoryId The ID of the chat memory.
     * @return
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {

        // 查询数据库消息
        List<MemoryMessage> chatMessageList = ChainWrappers.lambdaQueryChain(memoryMessageMapper)
                .eq(MemoryMessage::getSessionId, memoryId)
                .orderByAsc(MemoryMessage::getSeqNo)
                .list();

        // 返回持久化消息
        return chatMessageList.stream()
                .map(chatMessage -> ChatMessageDeserializer.messageFromJson(chatMessage.getContent()))
//                .map(chatMessage -> {
//                    if (chatMessage instanceof AiMessage aiMessage) {
//                        // 临时解决方案，工具参数为异常时  移除改参数; 等langchain4j 修复词问题时删除该逻辑
//                        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests().stream()
//                                .map(toolExecutionRequest -> ToolExecutionRequest.builder()
//                                        .id(toolExecutionRequest.id())
//                                        .name(toolExecutionRequest.name())
//                                        .arguments(JsonUtil.isJson(toolExecutionRequest.arguments()) ? toolExecutionRequest.arguments() : null)
//                                        .build())
//                                .toList();
//
//                        return AiMessage.builder()
//                                .text(aiMessage.text())
//                                .thinking(aiMessage.thinking())
//                                .toolExecutionRequests(toolExecutionRequests)
//                                .attributes(aiMessage.attributes())
//                                .build();
//                    }
//                    return chatMessage;
//                })
                .toList();

//        if (!chatMessages.isEmpty() && sanitizedIds.add(memoryId)) { // 没有被检查过的数据
//            log.info("Sanitizing messages for memoryId: {}", memoryId);
//            chatMessages = sanitizeMessages(chatMessages);
//        }

//        return chatMessages;
    }

    /**
     * 清理数据
     *
     * @param messages
     * @return
     */
    private List<ChatMessage> sanitizeMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return messages;

        while (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.getLast();
            // 如果最后一条是 AI 消息且包含工具调用
            if (lastMessage instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                // 这是一条悬空的工具调用，删除它以保证下次请求合法
                // 这样 LLM 会看到上一条 UserMessage，并重新触发工具调用
                messages.removeLast();
            } else {
                break;
            }
        }

        return messages;
    }


    /**
     * ChatMessageSerializer.messageToJson(ChatMessage) and
     * ChatMessageSerializer.messagesToJson(List<ChatMessage>) helper methods can be used to
     * easily serialize chat messages into JSON.
     *
     * @param memoryId The ID of the chat memory.
     * @param messages List of messages for the specified chat memory, that represent the current state of the {@link ChatMemory}.
     *                 Can be serialized to JSON using {@link ChatMessageSerializer}.
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();
        List<MemoryMessage> chatMessageList = IntStream.range(0, messages.size())
                .mapToObj(idx -> {
                    MemoryMessage chatMessage = new MemoryMessage();
                    chatMessage.setSessionId(sessionId);
                    chatMessage.setContent(ChatMessageSerializer.messageToJson(messages.get(idx)));
                    chatMessage.setSeqNo(idx);
                    chatMessage.setCreateTime(LocalDateTime.now());
                    return chatMessage;
                })
                .toList();

        // 插入前先清空
        LambdaQueryWrapper<MemoryMessage> deleteWrapper = Wrappers.lambdaQuery();
        deleteWrapper.eq(MemoryMessage::getSessionId, sessionId);
        memoryMessageMapper.delete(deleteWrapper);
        // 插入数据库
        memoryMessageMapper.insert(chatMessageList);
        // 更新session日期
        ChainWrappers.lambdaUpdateChain(chatSessionMapper)
                .eq(ChatSession::getId, sessionId)
                .set(ChatSession::getUpdateTime, LocalDateTime.now())
                .update();


    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        // 删除chatSession
        chatSessionMapper.deleteById(sessionId);
        // 删除message
        LambdaQueryWrapper<MemoryMessage> deleteWrapper = Wrappers.lambdaQuery();
        deleteWrapper.eq(MemoryMessage::getSessionId, sessionId);
        memoryMessageMapper.delete(deleteWrapper);
    }

    /**
     * 提取模型、用户的精简版对话记录
     *
     * @param currentContent 当前轮次的对话输入; 因为当前轮次的输入并不能从 持久化存储中查询回来
     * @return 已纯文本的方式返回，主要用来给agent理解使用
     */
    public String extractSimpleMessage(Object memoryId, String currentContent, int maxLength) {
        List<ChatMessage> messages = getMessages(memoryId);
        // 获取最后过滤对话
        messages = messages.stream()
                .filter(message -> message.type() == ChatMessageType.USER || message.type() == ChatMessageType.AI)
                .toList();
        // 保留最后 maxLength 条对话
        messages = messages.subList(Math.max(messages.size() - maxLength, 0), messages.size());

        Transcript transcript = new Transcript();
        List<Turn> turns = new ArrayList<>();
        for (ChatMessage message : messages) {
            Turn turn = new Turn();
            if (message instanceof UserMessage userMessage) {
                turn.setRole("User");
                turn.setText(userMessage.contents().stream()
                        .filter(content -> content instanceof TextContent)
                        .map(textContent -> ((TextContent) textContent).text())
                        .collect(Collectors.joining("; "))
                );
            }
            if (message instanceof AiMessage aiMessage && StrUtil.isNotBlank(aiMessage.text())) {
                turn.setRole("AI");
                turn.setText(aiMessage.text());
            }
            turns.add(turn);
        }

        // 添加当前轮次的输入
        Turn currentTurn = new Turn();
        currentTurn.setRole("User");
        currentTurn.setText(currentContent);
        turns.add(currentTurn);

        transcript.setTurns(turns);

        return XmlUtil.toXml(transcript);

//        StringBuilder sb = new StringBuilder();
//        sb.append("<transcript>");
//        for (ChatMessage message : messages) {
//            if (message instanceof UserMessage userMessage) {
//                sb.append("<turn role=\"User\">");
//                sb.append(
//                        userMessage.contents().stream()
//                                .filter(content -> content instanceof TextContent)
//                                .map(textContent -> ((TextContent) textContent).text())
//                                .collect(Collectors.joining("; "))
//                );
//                sb.append("</turn>");
//            }
//            if (message instanceof AiMessage aiMessage && StrUtil.isNotBlank(aiMessage.text())) {
//                sb.append("<turn role=\"AI\">");
//                sb.append(aiMessage.text());
//                sb.append("</turn>");
//            }
//        }
//        sb.append("<turn role=\"User\">").append(currentContent).append("</turn>");
//
//        sb.append("</transcript>");
//
//        return sb.toString();
    }


    @Data
    public static class Transcript {

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "turn")
        private List<Turn> turns = new ArrayList<>();
    }

    @Data
    public static class Turn {

        @JacksonXmlProperty(isAttribute = true)
        private String role;

        @JacksonXmlText
        private String text;

    }
}
