package com.ohyesai.next.biz.vio.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ohyesai.next.biz.vio.dto.*;
import com.ohyesai.next.biz.vio.service.VioService;
import com.ohyesai.next.biz.vio.vo.ChatHistoryVO;
import com.ohyesai.next.biz.vio.vo.ChatSessionVO;
import com.ohyesai.next.biz.vio.vo.UploadVO;
import com.ohyesai.next.common.dto.PageDTO;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.common.vo.Result;
import com.ohyesai.next.util.SseEmitterWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse/vio")
@Tag(name = "Vio Mv创作模块")
@AllArgsConstructor
@Slf4j
public class VioController {

    private final VioService vioService;

    private final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    @Operation(summary = "开始对话")
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody @Validated VioChatDTO vioChatDTO) {
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);
        String userId = StpUtil.getLoginIdAsString();
        vioService.chat(new SseEmitterWrapper(sseEmitter), vioChatDTO, userId);
        return sseEmitter;
    }

    @Operation(summary = "继续对话")
    @PostMapping("/resume-chat")
    public SseEmitter resumeChat(@RequestBody @Validated ResumeChatDTO resumeChatDTO) {
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);
        // 开始消费数据
        Thread.startVirtualThread(() -> {
            SseEmitterWrapper sseEmitterWrapper = new SseEmitterWrapper(sseEmitter);
            vioService.consumerSseMsg(resumeChatDTO.getSessionId(), resumeChatDTO.getHistoryMessageId(), sseEmitterWrapper);
            sseEmitterWrapper.complete();
        });

        return sseEmitter;
    }

    @Operation(summary = "获取对话历史消息")
    @GetMapping("/history-message")
    @SaIgnore // 该接口无需鉴权，允许匿名访问; 分享、首页展示时查看创作过程会使用该接口
    public Result<ChatHistoryVO> historyMessage(@RequestParam @ParameterObject String chatSessionId) {
        ChatHistoryVO chatHistoryVO = vioService.historyMessage(chatSessionId);
        return Result.success(chatHistoryVO);
    }

    @Operation(summary = "对话列表")
    @GetMapping("/chat-list")
    public PageResult<ChatSessionVO> chatList(@ParameterObject ChatListDTO chatListDTO) {
        return vioService.chatList(chatListDTO);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<UploadVO> upload(@Validated @NotNull(message = "文件为必填") @RequestParam("file") MultipartFile file) {
        UploadVO uploadVO = vioService.upload(file);
        return Result.success(uploadVO);
    }

    @Operation(summary = "重命名对话")
    @PostMapping("/rename")
    public Result<Void> rename(@Validated @RequestBody RenameDTO renameDTO) {
        vioService.rename(renameDTO);
        return Result.success();
    }

    @Operation(summary = "删除对话")
    @PostMapping("/delete")
    public Result<Void> delete(@Validated @RequestBody DeleteChatDTO deleteChatDTO) {
        vioService.delete(deleteChatDTO);
        return Result.success();
    }

}
