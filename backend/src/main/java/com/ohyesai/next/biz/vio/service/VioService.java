package com.ohyesai.next.biz.vio.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.bo.ChatHistoryBO;
import com.ohyesai.next.biz.vio.bo.HistoryMessageBO;
import com.ohyesai.next.biz.vio.bo.HistoryMessageChunkBO;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.biz.vio.dto.ChatListDTO;
import com.ohyesai.next.biz.vio.dto.DeleteChatDTO;
import com.ohyesai.next.biz.vio.dto.RenameDTO;
import com.ohyesai.next.biz.vio.dto.VioChatDTO;
import com.ohyesai.next.biz.vio.entity.ChatSession;
import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import com.ohyesai.next.biz.vio.entity.VioProject;
import com.ohyesai.next.biz.vio.helper.SseMessageHelper;
import com.ohyesai.next.biz.vio.mapper.ChatSessionMapper;
import com.ohyesai.next.biz.vio.mapper.HistoryMessageChunkMapper;
import com.ohyesai.next.biz.vio.mapper.HistoryMessageMapper;
import com.ohyesai.next.biz.vio.mapper.VioProjectMapper;
import com.ohyesai.next.biz.vio.vo.ChatHistoryVO;
import com.ohyesai.next.biz.vio.vo.ChatSessionVO;
import com.ohyesai.next.biz.vio.vo.SseMsgVO;
import com.ohyesai.next.biz.vio.vo.UploadVO;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.trace.ProxyThread;
import com.ohyesai.next.trace.TraceInterceptor;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.SseEmitterWrapper;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class VioService {

    public static final String SSE_HELPER = "SSE_HELPER";

    public static final String TEMP_DIR = "TEMP_DIR";

    private final VioAssistant vioAssistant;

    private final StringRedisTemplate redisTemplate;

    private final FileComponent fileComponent;

    private final HistoryMessageService historyMessageService;

    private final HistoryMessageMapper historyMessageMapper;

    private final HistoryMessageChunkMapper historyMessageChunkMapper;

    private final ChatSessionMapper chatSessionMapper;

    private final VioProjectMapper vioProjectMapper;

    private final SessionTaskService sessionTaskService;

    /**
     * 原始参数
     */
    public static final String RAW_PARAM = "RAW_PARAM";

    /**
     * 模型名称
     */
    public static final String MODEL = "MODEL";

    /**
     * 模型分辨率
     */
    public static final String RESOLUTION = "RESOLUTION";

    /**
     * 用户id
     */
    public static final String USER_ID = "USER_ID";

    /**
     * 意图类型
     * PlanRouterType
     */
    public static final String PLAN_ROUTER_TYPE = "PLAN_ROUTER_TYPE";


    /**
     * TraceId
     */
    public static final String TRACE_ID = TraceInterceptor.TRACE_ID;


    public void chat(SseEmitterWrapper sseEmitterWrapper, VioChatDTO vioChatDTO, String userId) {
        // 获取 traceId
        String traceId = MDC.get(TraceInterceptor.TRACE_ID);
        // 清理逻辑在 send chat 中和异常捕获里
        File tempDir = new File(FileDirConst.TEMP_DIR, IdUtil.fastSimpleUUID());
        boolean _ = tempDir.mkdirs();

        // 如果分辨率为空则获取历史数据分辨率；目前分辨率一旦确认不可修改，否则会因为多个分辨率视频进行合并出现错误 当前实现逻辑由前端控制
        if (StrUtil.isBlank(vioChatDTO.getSessionId()) && vioChatDTO.getResolution() == null) {
            sseEmitterWrapper.completeWithJsonData(SseMsgBO.ofError(CodeEnum.ParameterError, "请选择分辨率"));
            return;
        }

        // 获取本次 session
        ChatSession chatSession = historyMessageService.initChatSession(vioChatDTO.getSessionId(), vioChatDTO.getPrompt(), userId, vioChatDTO.getResolution());
        String sessionId = chatSession.getId();

        // 赋值默认分辨率 后面方便取
        vioChatDTO.setResolution(chatSession.getMetadata().getResolution());

        // session id 加锁
        String sessionLockKey = RedisConst.AGENT_CHAT_SESSION_LOCK.formatted(sessionId);
        Boolean sessionNotUse = redisTemplate.opsForValue().setIfAbsent(sessionLockKey, "0", Duration.ofHours(1));
        if (Boolean.TRUE.equals(sessionNotUse)) {
            // 存储用户消息
            historyMessageService.putSimpleHistoryMsg(sessionId, vioChatDTO.toSseMsgBO(fileComponent), HistoryMessage.SenderType.USER);
            // 构造 model history_message; 后续chunk消息关联这个记录
            HistoryMessage modelHistoryMessage = historyMessageService.createHistoryMessage(sessionId, HistoryMessage.SenderType.MODEL, false);

            // 构造sse helper
            SseMessageHelper sseMessageHelper = new SseMessageHelper(
                    userId,
                    sessionId,
                    modelHistoryMessage.getId(),
                    historyMessageService,
                    redisTemplate
            );

            // 激活队列
            sseMessageHelper.ofInit(chatSession.getName());
            try {
                // 开始对话   注：此方法内部为异步；
                // 绝大部分异常都会进入 onError 中，但有些异常，例如动态system、持久化这部分 会直接同步抛出，所以需要捕获并清理
                sendChat(sseMessageHelper, vioChatDTO, tempDir, sessionId, sessionLockKey, chatSession, traceId);
            } catch (Exception e) {
                log.error("chat 启动错误", e);
                sseMessageHelper.ofError(CodeEnum.ChatError);
                chatCleaner(sessionLockKey, chatSession, tempDir, sseMessageHelper);
            }
            // 开始消费数据
            ProxyThread.startVirtualThread(() -> {
                try {
                    consumerSseMsg(sessionId, modelHistoryMessage.getId(), sseEmitterWrapper);
                } catch (Exception e) {
                    log.error("sse 消费数据错误: {}", e.getMessage());
                } finally {
                    sseEmitterWrapper.complete();
                }
            });
        } else {
            sseEmitterWrapper.completeWithJsonData(SseMsgBO.ofError(CodeEnum.ParameterError, "对话正在进行中"));
        }
    }

    private void sendChat(SseMessageHelper sseMessageHelper,
                          VioChatDTO vioChatDTO,
                          File tempDir,
                          String sessionId,
                          String sessionLockKey,
                          ChatSession chatSession,
                          String traceId) {


        // 开始对话
        InvocationParameters parameters = InvocationParameters.from(Map.of(
                        VioService.SSE_HELPER, sseMessageHelper,
                        VioService.TEMP_DIR, tempDir,
                        VioService.RAW_PARAM, vioChatDTO,
                        VioService.MODEL, vioChatDTO.getModel(),
                        VioService.RESOLUTION, vioChatDTO.getResolution(),
                        VioService.USER_ID, chatSession.getUserId(),
                        VioService.TRACE_ID, traceId
                )
        );

        StringBuilder xml = new StringBuilder();
        List<VioChatDTO.RefImage> subjectImgs = vioChatDTO.getSubjectImgs();
        if (CollUtil.isNotEmpty(subjectImgs)) {
            xml.append("<subject-imgs description=\"用户上传的参考图列表\">");
            for (int i = 0; i < subjectImgs.size(); i++) {
                VioChatDTO.RefImage ref = subjectImgs.get(i);
                xml.append("<img index=\"%d\" file-id=\"%s\" intention=\"%s\"/>"
                        .formatted(i + 1, ref.getFileId(), ref.getIntention()));
            }
            xml.append("</subject-imgs>");
        }

        if (Boolean.TRUE.equals(vioChatDTO.getLipSync())) {
            xml.append("<lip-sync description=\"合并视频是否启用对口型\">true</lip-sync>");
        }

        if (StrUtil.isNotBlank(vioChatDTO.getAudioFileId())) {
            long durationSeconds = getDurationSeconds(tempDir, vioChatDTO.getAudioFileId());
            xml.append("<audio description=\"音频数据\">");
            xml.append("<file-id description=\"音频文件\">%s</file-id>".formatted(vioChatDTO.getAudioFileId()));
            xml.append("<duration-seconds description=\"音频文件时长/秒\">%d</duration-seconds>".formatted(durationSeconds));

            if (StrUtil.isNotBlank(vioChatDTO.getAudioLyrics())) {
//                String audioLyrics = StrUtil.isBlank(vioChatDTO.getAudioLyrics()) ? "无" : vioChatDTO.getAudioLyrics();
                xml.append("<lyrics description=\"音频文件歌词\">%s</lyrics>".formatted(vioChatDTO.getAudioLyrics()));
            }
            xml.append("</audio>");
        }

//        if (StrUtil.isNotBlank(vioChatDTO.getEditContext())) {
//            xml.append("<edit-context description=\"以下为用户在界面操作后的最新数据镜像，仅供参考。是否应用到任务流请遵循全局一致性规则。\">");
//            xml.append(vioChatDTO.getEditContext());
//            xml.append("</edit-context>");
//        }
        if (Boolean.TRUE.equals(vioChatDTO.getSubtitle())) {
            xml.append("<subtitle description=\"合并视频是否启用加字幕\">true</subtitle>");
        }

        String attachArgs = xml.isEmpty() ? StrUtil.EMPTY : "<attach-params>" + xml + "</attach-params>";
        log.info("开始对话");
        TokenStream tokenStream = vioAssistant.chat(sessionId, vioChatDTO.getPrompt(), attachArgs, parameters);
        tokenStream
                // 流式回复的片段  用于实时输出到前段
                .onPartialResponseWithContext((partialResponse, partialResponseContext) -> {
                    try {
                        sseMessageHelper.ofText(partialResponse.text());
                    } catch (Exception e) {
                        partialResponseContext.streamingHandle().cancel(); // 取消接收
                        log.error("onPartialResponseWithContext send 出错");
                        throw new BusinessException(e);
                    }
                })
                .onCompleteResponse(chatResponse -> {
                    try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId)) {
                        log.info("对话完成");
                        // 对话标记为完成
                        sseMessageHelper.ofComplete();
                        // 清理对话
                        chatCleaner(sessionLockKey, chatSession, tempDir, sseMessageHelper);
                    }
                })
                .onToolExecuted(toolExecution -> {
                    try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId)) {
                        log.info("工具{}执行完毕", toolExecution.request().name());
                    }
                })
                .onError(e -> {
                    try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId)) {
                        log.error("对话报错", e);
                        // 对话标记为完成
                        sseMessageHelper.ofError(CodeEnum.ChatError);
                        // 清理对话
                        chatCleaner(sessionLockKey, chatSession, tempDir, sseMessageHelper);
                    }
                })
                .start();
    }

    private long getDurationSeconds(File tempDir, String audioFileId) {
        if (StrUtil.isBlank(audioFileId)) {
            return 0;
        }
        File audioFile = new File(tempDir, IdUtil.fastSimpleUUID());
        fileComponent.download(audioFileId, audioFile);
        return CvUtil.mediaInfo(audioFile).duration().toSeconds();
    }

    private void chatCleaner(String sessionLockKey, ChatSession chatSession, File tempDir, SseMessageHelper sseMessageHelper) {
        // 驱逐出内存
        vioAssistant.evictChatMemory(chatSession.getId());
        // 清空目录
        FileUtil.del(tempDir);
        // 解锁
        redisTemplate.delete(sessionLockKey);
    }

    /**
     * 消费sse 消息 进行推送
     */
    public void consumerSseMsg(String chatSessionId, String historyMessageId, SseEmitterWrapper sseEmitterWrapper) {
        String keyHistory = RedisConst.AGENT_CHAT_HISTORY.formatted(chatSessionId, historyMessageId);
        int offset = 0;
        while (true) {
            if (!redisTemplate.hasKey(keyHistory)) {
                return;
            }
            // 获取指定分数区间的对话记录
            List<String> range = redisTemplate.opsForList().range(keyHistory, offset, -1);
            if (range == null) {
                return;
            }

            for (String payload : range) {
                SseMsgVO sseMsgVO = JsonUtil.toObject(payload, SseMsgVO.class);
                // 获取content字段 刷新文件数据
                SseMsgBO<Object> sseAgentBO = SseMsgBO.ofPayload(sseMsgVO.getContent(), fileComponent);

                // 更新vo content数据
                sseMsgVO.setContent(JsonUtil.object2Tree(sseAgentBO));
                // 发送数据
                sseEmitterWrapper.sendJson(sseMsgVO);
                if (sseAgentBO.getType() == SseMsgBO.Type.COMPLETE || sseAgentBO.getType() == SseMsgBO.Type.ERROR) {
                    // 当所有任务都消费完 这个队列延迟删除
                    redisTemplate.expire(keyHistory, 5, TimeUnit.MINUTES);
                    return;
                }

            }
            // 每次输出一个结果后更新偏移量
            offset += range.size();
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }
    }

    /**
     * 获取历史消息内容
     *
     * @param chatSessionId
     * @return
     */
    public ChatHistoryVO historyMessage(String chatSessionId) {
        ChatHistoryBO chatHistoryBO = chatSessionMapper.selectChatHistory(chatSessionId);
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        if (chatHistoryBO == null || CollUtil.isEmpty(chatHistoryBO.getChatMessages())) {
            return null;
        }
        // 是否需要继续对话；换句话说 判断消息是否未完成
        HistoryMessageBO lastHistoryMessage = chatHistoryBO.getChatMessages().getLast();
        chatHistoryVO.setResumeChat(!lastHistoryMessage.getFinish());
        chatHistoryVO.setLastMessageId(lastHistoryMessage.getMessageId());
        chatHistoryVO.setSessionId(chatSessionId);

        // 获取本session对应的分辨率
        ChatSession chatSession = chatSessionMapper.selectById(chatSessionId);
        chatHistoryVO.setResolution(Objects.requireNonNullElse(chatSession.getMetadata().getResolution(), Resolution.P720));

        // 遍历赋值 BO -> VO
        List<ChatHistoryVO.HistoryMessageVO> historyMessageVOs = new ArrayList<>();
        chatHistoryVO.setChatMessages(historyMessageVOs);

        for (HistoryMessageBO chatMessage : chatHistoryBO.getChatMessages()) {

            if (!chatMessage.getFinish()) {
                // 跳过未结束的对话 防止重复输出
                // 未结束的对话应该由 resume-chat 继续输出
                continue;
            }

            // 构造 HistoryMessageVO
            ChatHistoryVO.HistoryMessageVO historyMessageVO = new ChatHistoryVO.HistoryMessageVO();
            historyMessageVO.setMessageId(chatMessage.getMessageId());
            historyMessageVO.setSeqNo(chatMessage.getSeqNo());
            historyMessageVO.setSenderType(chatMessage.getSenderType());


            List<ChatHistoryVO.HistoryMessageChunkVO> messageChunkVOs = new ArrayList<>();
            for (HistoryMessageChunkBO messageChunk : chatMessage.getMessageChunks()) {
                // 构造 HistoryMessageChunkVO
                ChatHistoryVO.HistoryMessageChunkVO historyMessageChunkVO = new ChatHistoryVO.HistoryMessageChunkVO();
                historyMessageChunkVO.setMessageChunkId(messageChunk.getMessageChunkId());

                // 模型返回的数据需要特殊转换
                JsonNode payloadNode = JsonUtil.readTree(messageChunk.getContent());
                historyMessageChunkVO.setContent(SseMsgBO.ofPayload(payloadNode, fileComponent));

                messageChunkVOs.add(historyMessageChunkVO);

            }
            historyMessageVO.setMessageChunks(messageChunkVOs);
            historyMessageVOs.add(historyMessageVO);

        }


//        log.info("chatHistoryBO: {}", chatHistoryBO);
        return chatHistoryVO;
    }

    /**
     * 查询历史session
     *
     * @param chatListDTO
     * @return
     */
    public PageResult<ChatSessionVO> chatList(ChatListDTO chatListDTO) {
        String userId = StpUtil.getLoginIdAsString();
        Page<ChatSession> page = Page.of(chatListDTO.getPage(), chatListDTO.getSize());
        ChainWrappers.lambdaQueryChain(chatSessionMapper)
                .eq(ChatSession::getUserId, userId)
                .like(StrUtil.isNotBlank(chatListDTO.getSessionName()), ChatSession::getName, chatListDTO.getSessionName())
                .orderByDesc(ChatSession::getUpdateTime)
                .page(page);

        List<ChatSessionVO> result = page.getRecords().stream().map(item -> {
            ChatSessionVO chatSessionVO = new ChatSessionVO();
            chatSessionVO.setSessionId(item.getId());
            chatSessionVO.setSessionName(item.getName());
            if (StrUtil.isNotBlank(item.getMetadata().getCover())) {
                chatSessionVO.setSessionCover(fileComponent.shareUrl(item.getMetadata().getCover()));
            }
            chatSessionVO.setUpdateTime(item.getUpdateTime().atZone(ZoneId.systemDefault()));
            return chatSessionVO;
        }).toList();

        return PageResult.success(page.getTotal(), result);
    }

    public UploadVO upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (StrUtil.isBlank(fileName)) {
            throw new BusinessException(CodeEnum.ParameterError, "无法获取文件名称");
        }
        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            fileComponent.upload(inputStream, fileId);
            return new UploadVO(fileId, fileComponent.shareUrl(fileId));
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    public void rename(RenameDTO renameDTO) {
        ChainWrappers.lambdaUpdateChain(chatSessionMapper)
                .eq(ChatSession::getId, renameDTO.getSessionId())
                .eq(ChatSession::getUserId, StpUtil.getLoginIdAsString())
                .set(ChatSession::getName, renameDTO.getName())
                .update();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(DeleteChatDTO deleteChatDTO) {
        // 校验 session 所属
        Long count = ChainWrappers.lambdaQueryChain(chatSessionMapper)
                .eq(ChatSession::getId, deleteChatDTO.getSessionId())
                .eq(ChatSession::getUserId, StpUtil.getLoginIdAsString())
                .count();

        if (count == 0) {
            throw new BusinessException(CodeEnum.ParameterError, "sessionId 不存在");
        }

        // 删除 消息块
        historyMessageChunkMapper.deleteByHistoryByChatSessionId(deleteChatDTO.getSessionId());
        // 删除消息
        LambdaQueryWrapper<HistoryMessage> historyMessageWrapper = new LambdaQueryWrapper<>();
        historyMessageWrapper.eq(HistoryMessage::getChatSessionId, deleteChatDTO.getSessionId());
        historyMessageMapper.delete(historyMessageWrapper);
        // 删除session
        chatSessionMapper.deleteById(deleteChatDTO.getSessionId());
        // 删除项目资产
        LambdaQueryWrapper<VioProject> vioProjectWrapper = new LambdaQueryWrapper<>();
        vioProjectWrapper.eq(VioProject::getSessionId, deleteChatDTO.getSessionId());
        vioProjectMapper.delete(vioProjectWrapper);

    }
}
