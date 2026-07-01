package com.ohyesai.next.biz.vio.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.vio.bo.DirectEditTaskBO;
import com.ohyesai.next.biz.vio.bo.ReMakeVideoTaskBO;
import com.ohyesai.next.biz.vio.bo.ShareCacheBO;
import com.ohyesai.next.biz.vio.dto.*;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.biz.vio.service.VioProjectService;
import com.ohyesai.next.biz.vio.vo.*;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.StateEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.common.vo.Result;
import com.ohyesai.next.trace.ProxyThread;
import com.ohyesai.next.util.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/vio/resource")
@Tag(name = "Vio 资源模块")
@AllArgsConstructor
@Slf4j
public class VioProjectController {

    private final VioProjectService vioProjectService;

    private final StringRedisTemplate redisTemplate;

    private final BillingService billingService;

//    @GetMapping("/test")
//    public Result<String> test() {
//        log.error("123213",new BusinessException(CodeEnum.Unknow,"异常的message信息"));
//        return Result.success("测试成功");
//    }

    @Operation(summary = "获取视频元信息")
    @GetMapping("/video-meta")
    public Result<MvMetaVO> getVideoMeta(@Validated @NotBlank(message = "mvFileId不能为空") String mvFileId) {
        MvMetaVO videoMeta = vioProjectService.getVideoMeta(mvFileId);
        return Result.success(videoMeta);
    }

    @Operation(summary = "重新生成分镜-提交")
    @PostMapping("/re-make-video-submit")
    public Result<String> reMakeVideoSubmit(@RequestBody @Validated ReMakeVideoDTO reMakeVideoDTO) {

        String userId = StpUtil.getLoginIdAsString();
        // 校验是否有要修改的分镜
        if (reMakeVideoDTO.getScenes().stream().noneMatch(ReMakeVideoDTO.Scene::isReMake)) {
            throw new BusinessException(CodeEnum.ParameterError, "至少修改一个分镜才可以重新生成");
        }

        // 校验用户是否有积分
        if (!billingService.checkUserBalance(userId, BillingService.MIN_POINTS)) {
            throw new BusinessException(CodeEnum.PointNotEnough, "积分不足");
        }

        String taskId = IdUtil.fastSimpleUUID();
        String taskKey = RedisConst.REMAKE_VIDEO_TASK_STATUS.formatted(taskId);
        // 初始化任务状态
        redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new ReMakeVideoTaskBO(StateEnum.RUNNING)), Duration.ofHours(24));
        ProxyThread.startVirtualThread(() -> vioProjectService.reMakeVideoSubmit(reMakeVideoDTO, taskKey, userId));

        return Result.success(taskId);
    }

    @Operation(summary = "重新生成分镜-查询")
    @GetMapping("/re-make-video-query")
    public Result<ReMakeVideoTaskBO> reMakeVideoQuery(@Validated @NotBlank(message = "taskId不能为空") String taskId) {
        String taskKey = RedisConst.REMAKE_VIDEO_TASK_STATUS.formatted(taskId);
        String taskStatus = redisTemplate.opsForValue().get(taskKey);

        if (StrUtil.isBlank(taskStatus)) {
            return Result.custom(CodeEnum.ParameterError, "任务不存在");
        }

        return Result.success(JsonUtil.toObject(taskStatus, ReMakeVideoTaskBO.class));
    }

    @Operation(summary = "查询mv资源")
    @GetMapping("/find-mv")
    public PageResult<FindMvVO> findMv(@ParameterObject @Validated FindMvDTO findMvDTO) {
        return vioProjectService.findMv(findMvDTO);
    }

    @Operation(summary = "查询音乐资源")
    @GetMapping("/find-music")
    public PageResult<FindMusicVO> findMusic(@ParameterObject @Validated FindMusicDTO findMusicDTO) {
        return vioProjectService.findMusic(findMusicDTO);
    }

    @Operation(summary = "首页公共预览资源")
    @GetMapping("/common-preview")
    @SaIgnore
    @Cacheable(cacheNames = "commonPreview", key = "#p0.cacheKey()")
    public PageResult<CommonPreviewVO> commonPreview(@ParameterObject @Validated FindMvDTO findMvDTO) {
        return vioProjectService.commonPreview(findMvDTO);
    }

    @Operation(summary = "删除资源")
    @PostMapping("/delete-project")
    public Result<Void> deleteProject(@Validated @RequestBody DeleteProjectDTO deleteProjectDTO) {
        vioProjectService.deleteProject(deleteProjectDTO);
        return Result.success();
    }

    @Operation(summary = "重命名资源")
    @PostMapping("/rename-project")
    public Result<Void> renameProject(@Validated @RequestBody RenameProjectDTO renameProjectDTO) {
        vioProjectService.renameProject(renameProjectDTO);
        return Result.success();
    }

    @Operation(summary = "获取分享链接")
    @GetMapping("/share-link")
    @SaIgnore // 用户分享数据无需登陆
    public Result<String> getShareLink(@Validated @NotBlank(message = "projectId不能为空") String projectId) {
        return Result.success(vioProjectService.getShareLink(projectId));
    }

    @Operation(summary = "获取分享数据")
    @GetMapping("/share-data")
    @SaIgnore // 无需登陆
    public Result<ShareCacheBO> getShareData(@Validated @NotBlank(message = "shareId不能为空") String shareId) {
        return Result.success(vioProjectService.getShareData(shareId));
    }

    @Operation(summary = "直接编辑主体参考图-提交")
    @PostMapping("/direct-edit-subject")
    public Result<String> directEditSubject(@RequestBody @Validated DirectEditSubjectDTO directEditSubjectDTO) {
        String userId = StpUtil.getLoginIdAsString();

        // 校验 session 锁（确保 agent 处于 PAUSE 状态，即锁不存在）
        String sessionLockKey = RedisConst.AGENT_CHAT_SESSION_LOCK.formatted(directEditSubjectDTO.getSessionId());
        if (redisTemplate.hasKey(sessionLockKey)) {
            throw new BusinessException(CodeEnum.ParameterError, "当前对话正在进行中，请等待 Agent 暂停后再编辑");
        }

        // 校验用户积分
        int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.MAKE_IMAGE); // 积分单价
        if (!billingService.checkUserBalance(userId, pointsRequired)) {
            throw new BusinessException(CodeEnum.PointNotEnough);
        }

        String taskId = IdUtil.fastSimpleUUID();
        String taskKey = RedisConst.DIRECT_EDIT_TASK_STATUS.formatted(taskId);
        redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.RUNNING)), Duration.ofHours(24));
        ProxyThread.startVirtualThread(() -> vioProjectService.directEditSubject(directEditSubjectDTO, taskKey, userId, pointsRequired));

        return Result.success(taskId);
    }

    @Operation(summary = "直接编辑分镜视频-提交")
    @PostMapping("/direct-edit-scene")
    public Result<String> directEditScene(@RequestBody @Validated DirectEditSceneDTO directEditSceneDTO) {
        String userId = StpUtil.getLoginIdAsString();

        // 校验 session 锁（确保 agent 处于 PAUSE 状态，即锁不存在）
        String sessionLockKey = RedisConst.AGENT_CHAT_SESSION_LOCK.formatted(directEditSceneDTO.getSessionId());
        if (redisTemplate.hasKey(sessionLockKey)) {
            throw new BusinessException(CodeEnum.ParameterError, "当前对话正在进行中，请等待 Agent 暂停后再编辑");
        }

        String taskId = IdUtil.fastSimpleUUID();
        String taskKey = RedisConst.DIRECT_EDIT_TASK_STATUS.formatted(taskId);
        redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.RUNNING)), Duration.ofHours(24));
        ProxyThread.startVirtualThread(() -> vioProjectService.directEditScene(directEditSceneDTO, taskKey, userId));

        return Result.success(taskId);
    }

    @Operation(summary = "直接编辑重新生成-查询")
    @GetMapping("/direct-edit-query")
    public Result<DirectEditTaskBO> directEditQuery(@Validated @NotBlank(message = "taskId不能为空") String taskId) {
        String taskKey = RedisConst.DIRECT_EDIT_TASK_STATUS.formatted(taskId);
        String taskStatus = redisTemplate.opsForValue().get(taskKey);

        if (StrUtil.isBlank(taskStatus)) {
            return Result.custom(CodeEnum.ParameterError, "任务不存在");
        }

        return Result.success(JsonUtil.toObject(taskStatus, DirectEditTaskBO.class));
    }

    @Operation(summary = "切换版本；主体图、分镜")
    @PostMapping("/switch-version")
    public Result<Void> switchVersion(@RequestBody @Validated SwitchVersionDTO switchVersionDTO) {
        vioProjectService.switchVersion(switchVersionDTO);
        return Result.success();
    }

    @Operation(summary = "更新分镜脚本")
    @PostMapping("/update-scene-script")
    public Result<Void> updateSceneScript(@RequestBody @Validated UpdateSceneScriptDTO updateSceneScriptDTO) {
        vioProjectService.updateSceneScript(updateSceneScriptDTO);
        return Result.success();
    }

    @Operation(summary = "获取对口型消耗积分")
    @GetMapping("/get-lipsync-points")
    public Result<LipSyncPointsVO> getLipSyncPoints(@Validated @NotNull(message = "messageChunkId 不能为空") Integer messageChunkId) {
        LipSyncPointsVO lipSyncPointsVO = vioProjectService.getLipSyncPoints(messageChunkId);
        return Result.success(lipSyncPointsVO);
    }


}
