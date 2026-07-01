package com.ohyesai.next.biz.vio.controller;

import cn.dev33.satoken.apikey.template.SaApiKeyUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.ohyesai.next.biz.vio.bo.ToolExecuteTaskBO;
import com.ohyesai.next.biz.vio.dto.MusicSkillDTO;
import com.ohyesai.next.biz.vio.dto.MvSkillSubmitDTO;
import com.ohyesai.next.biz.vio.service.VioSkillService;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.Result;
import com.ohyesai.next.trace.ProxyThread;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/vio/skill")
@Tag(name = "技能模块")
@AllArgsConstructor
@Slf4j
public class VioSkillController {

    private final VioSkillService vioSkillService;

    @Operation(summary = "安装技能")
    @GetMapping(value = "/install")
    public ResponseEntity<String> install() {
        try (InputStream inputStream = VioSkillController.class.getResourceAsStream("/static/music_skill.md")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_MARKDOWN)
                    .body(IoUtil.readUtf8(inputStream));
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Operation(summary = "生成歌曲-提交")
    @PostMapping("/music-submit")
    public ResponseEntity<String> musicSubmit(@Validated @RequestBody MusicSkillDTO musicSkillDTO) {
        String taskId = IdUtil.fastSimpleUUID();
        String userId = SaApiKeyUtil.currentApiKey().getLoginId().toString();
        ProxyThread.startVirtualThread(() -> vioSkillService.musicSubmit(userId, taskId, musicSkillDTO));
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(taskId);
    }

    @Operation(summary = "生成歌曲-查询")
    @GetMapping("/music-query")
    public ResponseEntity<String> musicQuery(@Validated @NotBlank String taskId) {
        String result = vioSkillService.musicQuery(taskId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(result);
    }

    @Operation(summary = "mv skill 提交")
    @PostMapping("/mv-submit")
    public Result<String> mvSubmit(@Validated @RequestBody MvSkillSubmitDTO mvSkillSubmitDTO) {
        String taskId = IdUtil.fastSimpleUUID();
        String userId = SaApiKeyUtil.currentApiKey().getLoginId().toString();
        Thread.startVirtualThread(() -> vioSkillService.mvSubmit(mvSkillSubmitDTO, taskId, userId));
        return Result.success(taskId);
    }

    @Operation(summary = "mv skill 查询")
    @GetMapping("/mv-query")
    public Result<ToolExecuteTaskBO> mvQuery(@Validated @NotBlank String taskId) {
        ToolExecuteTaskBO result = vioSkillService.mvQuery(taskId);
        return Result.success(result);
    }

    @Operation(summary = "上传文件换取 fileId")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        String fileId = vioSkillService.upload(file);
        return Result.success(fileId);
    }


}
