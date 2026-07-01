package com.ohyesai.next.biz.vio.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileMagicNumber;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.billing.service.SubscriptionPlanService;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.bo.*;
import com.ohyesai.next.biz.vio.bo.tool.DoMergeAudioVideoBO;
import com.ohyesai.next.biz.vio.dto.*;
import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import com.ohyesai.next.biz.vio.entity.HistoryMessageChunk;
import com.ohyesai.next.biz.vio.entity.VioProject;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.biz.vio.mapper.ChatSessionMapper;
import com.ohyesai.next.biz.vio.mapper.VioProjectMapper;
import com.ohyesai.next.biz.vio.vo.*;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.dto.PageDTO;
import com.ohyesai.next.common.enums.*;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.component.vlm.VlmImageComponent;
import com.ohyesai.next.component.vlm.VlmVideoComponent;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.MiscUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class VioProjectService {

    private final VioAssistant.DeepSeekV4FlashAssistant deepSeekV4FlashAssistant;

    private final VioProjectMapper vioProjectMapper;

    private final FileComponent fileComponent;

    private final VlmVideoComponent vlmVideoComponent;

    private final VlmImageComponent vlmImageComponent;

    private final HistoryMessageService historyMessageService;

    private final StringRedisTemplate redisTemplate;

    private final BillingService billingService;

    private final ChatSessionMapper chatSessionMapper;

    private final SubscriptionPlanService subscriptionPlanService;

    private final SessionTaskService sessionTaskService;

    private final UserMapper userMapper;

    private final VioAssistant.Gemini3_5FlashAssistant gemini3_5FlashAssistant;

    public MvMetaVO getVideoMeta(String mvFileId) {
        VioProject mvVioProject = queryByFileId(mvFileId, TaskType.MAKE_MV);

        // 解析参数
        DoMergeAudioVideoBO mergeAudioVideoArgs = JsonUtil.toObject(mvVioProject.getArgs(), DoMergeAudioVideoBO.class);
        // 根据参数数据 查询分镜数据
        List<String> scenesFileIds = mergeAudioVideoArgs.getVideoFileIds().stream().map(DigestUtil::md5Hex).toList();
        List<VioProject> scenesFiles = vioProjectMapper.selectByIds(scenesFileIds);
        scenesFiles.sort(Comparator.comparing(v -> scenesFileIds.indexOf(v.getId()))); // 按照查询顺序排序

        List<MvMetaVO.Scene> sceneMetas = new ArrayList<>();
        for (VioProject scenesFile : scenesFiles) {
            // 解析分镜工具 makeVideo 参数
            DoMakeVideoBO doMakeVideoBO = JsonUtil.toObject(scenesFile.getArgs(), DoMakeVideoBO.class);

            MvMetaVO.Scene scene = new MvMetaVO.Scene();
            scene.setSceneId(scenesFile.getId()); // 镜头id
            scene.setVisualPrompt(doMakeVideoBO.getVisualPrompt());
            scene.setDuration(doMakeVideoBO.getDuration());
            scene.setModel(scenesFile.getModelName());
            // 参考图
            if (CollUtil.isNotEmpty(doMakeVideoBO.getSubject())) {
                List<MvMetaVO.FileInfo> refImgs = doMakeVideoBO.getSubject().stream().map(sceneSubject -> {
                    MvMetaVO.FileInfo fileInfo = new MvMetaVO.FileInfo();
                    fileInfo.setSubjectId(sceneSubject.getId());
                    fileInfo.setFileId(sceneSubject.getFileId());
                    fileInfo.setFileUrl(fileComponent.shareUrl(sceneSubject.getFileId()));
                    return fileInfo;
                }).toList();
                scene.setSubject(refImgs);
            }
            // 分镜视频文件
            scene.setVideoFile(new MvMetaVO.FileInfo(null, scenesFile.getFileId(), fileComponent.shareUrl(scenesFile.getFileId())));
            // 分镜视频封面文件
            scene.setCoverFile(new MvMetaVO.FileInfo(null, scenesFile.getPreviewFileId(), fileComponent.shareUrl(scenesFile.getPreviewFileId())));

            sceneMetas.add(scene);
        }

        MvMetaVO videoMetaVO = new MvMetaVO();
        videoMetaVO.setMvId(mvVioProject.getId());
        videoMetaVO.setFileId(mvVioProject.getFileId());
        videoMetaVO.setFileUrl(fileComponent.shareUrl(mvVioProject.getFileId()));
        videoMetaVO.setScenes(sceneMetas);

        return videoMetaVO;

    }

    private VioProject queryById(String id, TaskType fromTool) {
        return queryById(id, StpUtil.getLoginIdAsString(), fromTool);
    }

    private VioProject queryById(String id, String userId, TaskType fromTool) {
        VioProject vioProject = ChainWrappers.lambdaQueryChain(vioProjectMapper)
                .eq(VioProject::getId, id) // 传入id后其他参数 仅用来做过滤，确保数据类型正确
                .eq(VioProject::getUserId, userId)
                .eq(VioProject::getTaskType, fromTool)
                .one();

        if (vioProject == null) {
            throw new BusinessException(CodeEnum.ParameterError, "文件不存在");
        }
        return vioProject;
    }

    private VioProject queryByFileId(String fileId, TaskType fromTool) {
        return queryById(DigestUtil.md5Hex(fileId), fromTool);
    }

    /**
     * 项目表不应该存在删除逻辑
     *
     * @return
     */
    public VioProject save(SaveVioProjectBO saveVioProjectBO) {

        VioProject vioProject = new VioProject();
        vioProject.setId(DigestUtil.md5Hex(saveVioProjectBO.getFileId()));
        vioProject.setUserId(saveVioProjectBO.getUserId());
        vioProject.setProjectName(saveVioProjectBO.getProjectName());
        vioProject.setSessionId(saveVioProjectBO.getSessionId());
        vioProject.setMessageId(saveVioProjectBO.getMessageId());
        vioProject.setFileId(saveVioProjectBO.getFileId());
        vioProject.setPreviewFileId(saveVioProjectBO.getPreviewFileId());
        vioProject.setMediaDuration(saveVioProjectBO.getDuration());
        vioProject.setLyrics(saveVioProjectBO.getLyrics());
        vioProject.setArgs(JsonUtil.toJson(saveVioProjectBO.getArgs()));
        vioProject.setTaskType(saveVioProjectBO.getTaskType());
        vioProject.setShowProject(true);
        vioProject.setCreateTime(LocalDateTime.now());
        vioProject.setModelName(saveVioProjectBO.getModel());
        // 插入新数据
        vioProjectMapper.insert(vioProject);

        // 检查对应session是否存在封面 如果不存在则更新封面
        if (StrUtil.isNotBlank(vioProject.getPreviewFileId())) {
            chatSessionMapper.updateCover4Null(vioProject.getSessionId(), vioProject.getPreviewFileId());
        }
//        ChainWrappers.lambdaUpdateChain(chatSessionMapper)
//                .eq(ChatSession::getId, saveVioProjectBO.getSessionId())
//                .isNull(ChatSession::getCover)
//                .set(ChatSession::getCover, vioProject.getPreviewFileId())
//                .update();

        return vioProject;
    }

    /**
     * 复制原有mv参数信息 生成新mv
     *
     * @param reMakeVideoDTO
     * @return
     */
    public void reMakeVideoSubmit(ReMakeVideoDTO reMakeVideoDTO, String taskKey, String userId) {
        log.info("reMakeVideoSubmit run");
        File tempDir = new File(FileDirConst.TEMP_DIR, IdUtil.fastSimpleUUID());
        var _ = tempDir.mkdirs();
        try {
            // 获取 old mv
            VioProject oldMvVioProject = queryById(reMakeVideoDTO.getMvId(), userId, TaskType.MAKE_MV);
            // 获取 old mv 参数
            DoMergeAudioVideoBO oldMergeAudioVideoArgs = JsonUtil.toObject(oldMvVioProject.getArgs(), DoMergeAudioVideoBO.class);

            // 遍历新参数
            List<String> sceneFileIds = new ArrayList<>();
            for (ReMakeVideoDTO.Scene scene : reMakeVideoDTO.getScenes()) {
                // 获取历史分镜信息
                VioProject oldSceneVioProject = queryById(scene.getSceneId(), userId, TaskType.MAKE_VIDEO);
                DoMakeVideoBO oldDoMakeVideo = JsonUtil.toObject(oldSceneVioProject.getArgs(), DoMakeVideoBO.class);

                // 优先使用前端传入的 duration，为空时沿用历史值
                Double effectiveDuration = scene.getDuration() != null ? scene.getDuration() : oldDoMakeVideo.getDuration();
                if (scene.isReMake()) { // 需要重跑的分镜
                    DoMakeVideoBO newDoMakeVideo = scene.toMakeVideoArgs(effectiveDuration, oldDoMakeVideo.getAspectRatio(), oldDoMakeVideo.getResolution());
                    // 重跑分镜  获取新分镜结果
                    VioProject newSceneVioProject = doMakeVideo(
                            tempDir,
                            userId,
                            oldSceneVioProject.getSessionId(),
                            oldSceneVioProject.getMessageId(),
                            scene.getModel(),
                            newDoMakeVideo
                    );

                    // 消费积分
                    int pointsRequired = billingService.getPointsRequired(scene.getModel(), Objects.requireNonNullElse(oldDoMakeVideo.getResolution(), Resolution.P720), TaskType.MAKE_MV);
                    int totalPoints = (int) Math.ceil(newDoMakeVideo.getDuration()) * pointsRequired;
                    PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, scene.getModel(), TaskType.MAKE_VIDEO, totalPoints).pointsTransactionLog();
                    billingService.mapSessionWithPointsTransactionLog(oldSceneVioProject.getSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

                    sceneFileIds.add(newSceneVioProject.getFileId());
                } else {
                    // 没有修改的直接复用历史
                    sceneFileIds.add(oldSceneVioProject.getFileId());
                }
            }

            // 构造 new MergeAudioVideoArgs 参数
            DoMergeAudioVideoBO newMergeAudioVideoArgs = new DoMergeAudioVideoBO();
            newMergeAudioVideoArgs.setAudioFileId(oldMergeAudioVideoArgs.getAudioFileId());
            newMergeAudioVideoArgs.setChatSessionTaskId(oldMergeAudioVideoArgs.getChatSessionTaskId());
            newMergeAudioVideoArgs.setVideoFileIds(sceneFileIds);
            newMergeAudioVideoArgs.setSubtitleFlag(false);
            // 重跑mv
            VioProject newMvVioProject = doMergeAudioVideo(
                    tempDir,
                    userId,
                    oldMvVioProject.getSessionId(),
                    oldMvVioProject.getMessageId(),
                    oldMvVioProject.getProjectName(),
                    newMergeAudioVideoArgs,
                    null
            ).vioProject();

            // 将视频消息结果推送到聊天窗口中
            SseMsgBO<List<SseMsgBO.Video>> videoSseMsgBo = SseMsgBO.ofVideo(
                    newMvVioProject.getFileId(),
                    fileComponent.shareUrl(newMvVioProject.getFileId()),
                    newMvVioProject.getPreviewFileId(),
                    fileComponent.shareUrl(newMvVioProject.getPreviewFileId()),
                    newMvVioProject.getId()
            );

            // 存储在数据库  以便可以在聊天历史中进行回显
            HistoryMessage historyMessage = historyMessageService.createHistoryMessage(newMvVioProject.getSessionId(), HistoryMessage.SenderType.MODEL, true);
            historyMessageService.putMessageChunk(historyMessage.getId(), newMergeAudioVideoArgs.getChatSessionTaskId(), videoSseMsgBo);

            // 更新任务结果到redis
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new ReMakeVideoTaskBO(StateEnum.SUCCESS, videoSseMsgBo)), Duration.ofMinutes(30));

        } catch (Exception e) {
            log.error("reMakeVideo error", e);
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new ReMakeVideoTaskBO(StateEnum.FAIL)), Duration.ofMinutes(30));
        } finally {
            FileUtil.del(tempDir);
        }
        log.info("reMakeVideoSubmit end");
    }


    /**
     * 生成图片（从 VioMvTool.makeImg 提取的核心逻辑，供直接编辑 API 复用）
     *
     * @return 生成的图片 fileId
     */
    public String doMakeImg(String prompt, List<String> refImageFileIds) {
        List<String> refImageUrls = refImageFileIds == null ? List.of()
                : refImageFileIds.stream().map(fileComponent::shareUrl).toList();
        List<byte[]> imgBytes = vlmImageComponent.seedreamGenerateImage(prompt, refImageUrls, 1);
        byte[] imgByte = imgBytes.getFirst();
        String extension = FileMagicNumber.getMagicNumber(imgByte).getExtension();
        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, extension);
        fileComponent.upload(imgByte, fileId);
        return fileId;
    }

    /**
     * 参数时长如果为小数，会向上取整 生成后在裁剪回实际长度
     *
     * @param tempDir
     * @param userId
     * @param makeVideoArgs
     * @return
     */
    public VioProject doMakeVideo(File tempDir,
                                  String userId,
                                  String sessionId,
                                  String messageId,
                                  ModelEnum model,
                                  DoMakeVideoBO makeVideoArgs) {

        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        String coverFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "jpg");

        // 参数转换
        List<VlmVideoComponent.Subject> subjects = null;
        if (CollUtil.isNotEmpty(makeVideoArgs.getSubject())) {
            // 主体参数转换，将文件id换成文件url
            subjects = makeVideoArgs.getSubject().stream().map(subject -> {
                VlmVideoComponent.Subject subject_a = new VlmVideoComponent.Subject();
                subject_a.setId(subject.getId());
                subject_a.setImages(List.of(fileComponent.shareUrl(subject.getFileId())));
                return subject_a;
            }).toList();

        }

        // 映射比例
        AspectRatio aspectRatio = makeVideoArgs.getAspectRatio();
        // 分辨率
        Resolution resolution = makeVideoArgs.getResolution();

        // 计算视频生成时长：向上取整（API只支持整数秒）
        double exactDuration = makeVideoArgs.getDuration() != null ? makeVideoArgs.getDuration() : 5.0;
        int apiDuration = (int) Math.ceil(exactDuration);

        VlmVideoComponent.VideoResp videoResp = switch (model) {
            case VIDUQ2 -> {
                if (CollUtil.isEmpty(subjects)) { // vidu 参考生视频必须传入图片，需要配合文生解决无参考图问题
                    yield vlmVideoComponent.viduTextToVideo(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, resolution);
                } else {
                    yield vlmVideoComponent.viduReference2video(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, resolution);
                }
            }
            case SEEDANCE_2 ->
                    vlmVideoComponent.seedance2Video(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, resolution);
            case SEEDANCE_2_FAST ->
                    vlmVideoComponent.seedance2FastVideo(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, resolution);
            case KLING_V3_OMNI ->
                    vlmVideoComponent.klingTextToVideo(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, resolution);
            default -> throw new BusinessException(CodeEnum.Unknow, "暂不支持该模型");

        };
        // 生成视频
//        String videoUrl = vlmComponent.viduTextToVideo(makeVideoArgs.getVisualPrompt(), makeVideoArgs.getDuration(), aspectRatio, subjects);

        // 注意 当支持模型切换功能上线后，模型为kling的时候，由于无法指定分辨率，需要通过 cvUtil 缩放到720p
//        String videoUrl = vlmComponent.klinTextToVideo(makeVideoArgs.getVisualPrompt(), makeVideoArgs.getDuration(), aspectRatio, subjects);

        // 豆包 2.0
//        String videoUrl = vlmComponent.seedance2TextToVideo(makeVideoArgs.getVisualPrompt(), makeVideoArgs.getDuration(), aspectRatio, subjects);

        // 下载 存储
        File videoFile = new File(tempDir, IdUtil.fastSimpleUUID() + "_temp.mp4");
        HttpUtil.downloadFile(videoResp.videoUrl(), videoFile);

        // 当脚本规划时长非整数秒时，截断视频到精确时长
        if (exactDuration != videoResp.duration()) {
            File trimmedFile = new File(tempDir, IdUtil.fastSimpleUUID() + "_trimmed.mp4");
            CvUtil.trimVideo(videoFile, trimmedFile, 0, exactDuration);
            videoFile = trimmedFile;
        }

        // 缩放视频统一分辨率
        videoFile = scaleVideo(tempDir, makeVideoArgs.getAspectRatio(), resolution, videoFile);

        // 增加水印
        SubscriptionPlan subscriptionPlanByUser = subscriptionPlanService.getSubscriptionPlanByUser(userId);// 获取用户的订阅计划
        if (subscriptionPlanByUser == null || subscriptionPlanByUser.getSubscriptionType() == SubscriptionPlan.SubscriptionType.FREE) {
            videoFile = addWatermark(tempDir, videoFile);
        }

        fileComponent.upload(videoFile, fileId);
        // 封面文件
        CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(videoFile);
        // 上传封面
        fileComponent.upload(mediaInfo.cover(), coverFileId);

        // 存储入库
        SaveVioProjectBO saveVioProjectBO = SaveVioProjectBO.builder()
                .fileId(fileId)
                .previewFileId(coverFileId)
                .duration((int) mediaInfo.duration().toSeconds())
                .args(makeVideoArgs)
                .taskType(TaskType.MAKE_VIDEO)
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .model(model)
                .build();
        return save(saveVioProjectBO);
    }

    /**
     * 缩放视频
     *
     * @param tempDir
     * @param aspectRatio
     * @param videoFileTemp
     * @return
     */
    private File scaleVideo(File tempDir, AspectRatio aspectRatio, Resolution resolution, File videoFileTemp) {
        File videoFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        int[] resolutionArr = switch (aspectRatio) {
            case LANDSCAPE -> switch (resolution) {
                case P720 -> new int[]{1280, 720};
                case P1080 -> new int[]{1920, 1080};
            };
            case PORTRAIT -> switch (resolution) {
                case P720 -> new int[]{720, 1280};
                case P1080 -> new int[]{1080, 1920};
            };
        };
        int width = resolutionArr[0], height = resolutionArr[1];


        CvUtil.scale(videoFileTemp, videoFile, width, height);
        return videoFile;
    }

    /**
     *
     * @param tempDir
     * @param userId
     * @param doMergeAudioVideoBO
     * @return
     */
    public DoMergeAudioVideoResp doMergeAudioVideo(File tempDir,
                                                   String userId,
                                                   String sessionId,
                                                   String messageId,
                                                   String projectName,
                                                   DoMergeAudioVideoBO doMergeAudioVideoBO,
                                                   ChatSessionTaskPayload taskPayload) {
        String outputFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        String coverFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "jpg");

        // 下载视频片段
        List<File> videoFiles = doMergeAudioVideoBO.getVideoFileIds().parallelStream()
                .map(fileId -> {
                    File file = new File(tempDir, fileId);
                    fileComponent.download(fileId, file);
                    return file;
                }).toList();

        // 拼接视频片段（每个视频片段已在 doMakeVideo 中按脚本 duration 精确裁剪，此处直接拼接）
        File concatVideoFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.concatVideo(videoFiles, concatVideoFile);
        String concatVideoFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        fileComponent.upload(concatVideoFile, concatVideoFileId);

        // 最终结果文件
        File outputFile;

        // 音频文件
        File audioFIle = new File(tempDir, doMergeAudioVideoBO.getAudioFileId());
        fileComponent.download(doMergeAudioVideoBO.getAudioFileId(), audioFIle);
        // 合并
        File mergeOutputFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.mergeAudioVideo(concatVideoFile, audioFIle, mergeOutputFile);

        // 原始视频末尾 2 秒音频淡出
        CvUtil.MediaInfo mergeOutputMediaInfo = CvUtil.mediaInfo(mergeOutputFile, false);
        File fadeOutAudioFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.fadeOutAudio(mergeOutputFile, fadeOutAudioFile, (int) mergeOutputMediaInfo.duration().toSeconds() - 2, 2);

        // 增加水印
        SubscriptionPlan subscriptionPlanByUser = subscriptionPlanService.getSubscriptionPlanByUser(userId);// 获取用户的订阅计划
        if (subscriptionPlanByUser == null || subscriptionPlanByUser.getSubscriptionType() == SubscriptionPlan.SubscriptionType.FREE) {
            outputFile = addLogo(tempDir, fadeOutAudioFile, mergeOutputMediaInfo.width(), mergeOutputMediaInfo.height());
        } else {
            // 付费用户不增加水印
            outputFile = fadeOutAudioFile;
        }

        // 添加字幕
        if (Boolean.TRUE.equals(doMergeAudioVideoBO.getSubtitleFlag())) {
            outputFile = addSubtitles(
                    tempDir,
                    outputFile,
                    taskPayload,
                    doMergeAudioVideoBO.getSubtitleReference(),
                    mergeOutputMediaInfo.width(),
                    mergeOutputMediaInfo.height()
            );
        }

        // 上传最终结果
        fileComponent.upload(outputFile, outputFileId);

        // 上传封面
        CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(outputFile);
        fileComponent.upload(mediaInfo.cover(), coverFileId);

        // 存储入项目库
        SaveVioProjectBO saveVioProjectBO = SaveVioProjectBO.builder()
                .fileId(outputFileId)
                .previewFileId(coverFileId)
                .projectName(projectName)
                .duration((int) mediaInfo.duration().toSeconds())
                .args(doMergeAudioVideoBO)
                .taskType(TaskType.MAKE_MV)
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .build();
        VioProject vioProject = save(saveVioProjectBO);

        return new DoMergeAudioVideoResp(vioProject, mediaInfo.duration());
    }

    public File addSubtitles(File tempDir,
                             File videoFile,
                             ChatSessionTaskPayload taskPayload,
                             String subtitleReference,
                             int videoWidth,
                             int videoHeight) {
        try {
            List<SubtitleTranscribed.SubtitleLine> subtitleLines = taskPayload.getSubtitleLines();
            if (subtitleLines.isEmpty()) {
                throw new BusinessException(CodeEnum.ParameterError, "字幕缓存为空,无法使用");
            }
            // 如果传入参考数据则进行修正
            if (StrUtil.isNotBlank(subtitleReference)) {
                // 仅修改变量；更新后的字幕不会回更到数据库
                subtitleLines = subtitleTranscribedFix(subtitleReference, subtitleLines);
            }

            File srtFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".srt");
            Files.writeString(srtFile.toPath(), MiscUtil.toSrtContent(subtitleLines, videoWidth, videoHeight));

            File outputWithSubtitles = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
            CvUtil.addSubtitles(videoFile, srtFile, outputWithSubtitles, videoWidth > videoHeight ? MiscUtil.FONT_SIZE_LANDSCAPE : MiscUtil.FONT_SIZE_PORTRAIT);
            return outputWithSubtitles;
        } catch (Exception e) {
            log.warn("字幕添加失败，将降级为无字幕模式: {}", e.getMessage());
            return videoFile;
        }
    }

    /**
     * 修正字幕转化数据
     * 该方法返回一份新的数据 防止影响原始数据
     *
     * @param referenceData 字母参考数据
     * @param subtitleLines 原始字幕数据
     * @return 返回修改后的新列表（新列表是深度clone的）
     */
    public List<SubtitleTranscribed.SubtitleLine> subtitleTranscribedFix(String referenceData, List<SubtitleTranscribed.SubtitleLine> subtitleLines) {
        // clone 原始数据
        List<SubtitleTranscribed.SubtitleLine> subtitleLinesCopy = subtitleLines.stream().map(SubtitleTranscribed.SubtitleLine::new).toList();

        JsonNode linesArrayNode = JsonUtil.object2Tree(subtitleLinesCopy);
        for (int i = 0; i < linesArrayNode.size(); i++) {
            ObjectNode line = (ObjectNode) linesArrayNode.path(i);
            line.put("id", i);
        }

        AsrPatchResult asrPatchResult = deepSeekV4FlashAssistant.asrPatch(linesArrayNode.toString(), StrUtil.blankToDefault(referenceData, "无"));
        // 修正结果
        for (AsrPatchResult.AsrPatch patch : asrPatchResult.patches()) {
            subtitleLinesCopy.get(patch.id()).setText(patch.text());
        }
        return subtitleLinesCopy;
    }

    /**
     * 添加水印
     *
     * @param tempDir
     * @param videoFile
     * @return
     */
    public File addWatermark(File tempDir, File videoFile) {
        // 水印图片
        File watermarkFile = new File(tempDir, "shuiyin_2x.png");
        if (!watermarkFile.exists()) { // 不存在则释放
            try (InputStream inputStream = VioProjectService.class.getResourceAsStream("/static/shuiyin_2x.png");
                 OutputStream outputStream = Files.newOutputStream(watermarkFile.toPath())
            ) {
                IoUtil.copy(inputStream, outputStream);
            } catch (IOException e) {
                throw new BusinessException(CodeEnum.Unknow, e);
            }
        }
        // 免费用户增加水印
        File watermarkOutputFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.watermark(videoFile, watermarkFile, watermarkOutputFile);
        return watermarkOutputFile;
    }

    /**
     * 视频末尾拼接logo
     *
     * @return
     */
    public File addLogo(File tempDir, File videoFile, int videoWidth, int videoHeight) {

//        CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(videoFile, false);
        // 获取 logo video
        String logoVideoPath;
        File logoVideo;
        if (videoWidth > videoHeight) { // 横屏
            logoVideoPath = "/static/LOGO_LANDSCAPE.mp4";
            logoVideo = new File(tempDir, "LOGO_LANDSCAPE.mp4");
        } else { // 竖屏
            logoVideoPath = "/static/LOGO_PORTRAIT.mp4";
            logoVideo = new File(tempDir, "LOGO_PORTRAIT.mp4");
        }
        // 释放 logo video
        try (InputStream inputStream = VioProjectService.class.getResourceAsStream(logoVideoPath);
             OutputStream outputStream = Files.newOutputStream(logoVideo.toPath())
        ) {
            IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new BusinessException(CodeEnum.Unknow, e);
        }
        // 缩放水印到指定宽高，为了后期支持多分辨率做准备，logo video每次都需要释放+缩放
        File logoVideoResizeFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.scale(logoVideo, logoVideoResizeFile, videoWidth, videoHeight);

        // 末尾拼接 logo video
        File outputFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.concatVideoAudio(List.of(videoFile, logoVideoResizeFile), outputFile);
        return outputFile;
    }

    public PageResult<FindMvVO> findMv(FindMvDTO findMvDTO) {
        String userId = StpUtil.getLoginIdAsString();

        Page<VioProject> resultPage = getVioProjectPage(userId, findMvDTO.getProjectName(), TaskType.MAKE_MV, findMvDTO);

        List<FindMvVO> result = resultPage.getRecords().stream().map(item -> {
            FindMvVO findMvVO = new FindMvVO();
            findMvVO.setProjectId(item.getId());
            findMvVO.setSessionId(item.getSessionId());
            findMvVO.setMessageId(item.getMessageId());
            findMvVO.setProjectName(item.getProjectName());
            findMvVO.setFileId(item.getFileId());
            findMvVO.setFileUrl(fileComponent.shareUrl(item.getFileId()));
            findMvVO.setFileCoverId(item.getPreviewFileId());
            findMvVO.setFileCoverUrl(fileComponent.shareUrl(item.getPreviewFileId()));
            findMvVO.setCreateTime(ZonedDateTime.of(item.getCreateTime(), ZoneId.systemDefault()));
            return findMvVO;
        }).toList();

        return PageResult.success(resultPage.getTotal(), result);
    }


//    private final List<String> shareVioProjectIds = List.of(
//            "5a389acc28da5ec027595f292f60c1a9",
//            "c96f3f1d02e08ac712e0e99b7ad11131",
//            "36c98115a65962a7be063c1aa4359167",
//            "4af4d7b472f2c7a57733fdb048743be1",
//            "72f96507c3ed001393a4e5db833c6a42",
//            "82de19945740b3cb71d0660740555069",
//            "fa4e9505285d1a533dda7c9463a0e8c0",
//            "812fae63ae1020337d004e72bb5794aa",
//            "488cd02b4c8fff74d34fa5583bf29bc4",
//            "9a904bdffb993d63fd46a6023e9b71e5",
//            "d14b1b6ff8e3ee08a764b76dc7c608ed",
//            "1e6f3f3b2aaf3cb12453dcdfa721ded5",
//            "155d47ed5529d4649b1e981385edb729",
//            "c67cc25688399b24acb19d6636016534",
//            "78476c1ae8978ab6e095b09460af1836",
//            "d61e7818b79c7264a8398e8bb334af1e",
//            "605a148462ca5377e8a75bd3cdc29366",
//            "02efa47b188dfe934f5eaa64b8c0d2ff",
//            "0992c2263102e87d0dfda92786e63a19",
//            "60ae8ba59074fa4a2ea15b447e6f718b",
//            "46d803497769260e523b566512ee38a9",
//            "e596ed01d290ed773e8e4335e858b16c",
//            "09595d39943030f9672be755e3a24440",
//            "ed317941a1244abd4f39a97196804c8a",
//            "e0dca48e18a010d3f2de00e64c3a4d10",
//            "af2cbdd1aa6d81eb165a3140a4238052"
//    );

    public PageResult<CommonPreviewVO> commonPreview(FindMvDTO findMvDTO) {
        Page<VioProject> page = Page.of(findMvDTO.getPage(), findMvDTO.getSize());

//        List<String> shareVioProjectIds = shareVioProjectMapper.selectList(null).stream().map(ShareVioProject::getVioProjectId).toList();
        IPage<VioProjectJoinUser> sharVioProject = vioProjectMapper.selectPageJoinUser(page);

        List<CommonPreviewVO> result = sharVioProject.getRecords().stream().map(item -> {
            // 获取用户发送的第一个文本消息
            SseMsgBO.Text userText = historyMessageService.findFirstUserText(item.getSessionId());

            CommonPreviewVO commonPreviewVO = new CommonPreviewVO();
            commonPreviewVO.setPrompt(userText.text());
            commonPreviewVO.setUserId(item.getUserId());
            commonPreviewVO.setProjectId(item.getId());
            commonPreviewVO.setSessionId(item.getSessionId());
            commonPreviewVO.setMessageId(item.getMessageId());
            commonPreviewVO.setProjectName(item.getProjectName());
            commonPreviewVO.setFileId(item.getFileId());
            if (StrUtil.isNotBlank(item.getFileId())) {
                commonPreviewVO.setFileUrl(fileComponent.shareUrl(item.getFileId()));
            }
            commonPreviewVO.setFileCoverId(item.getPreviewFileId());
            if (StrUtil.isNotBlank(item.getPreviewFileId())) {
                commonPreviewVO.setFileCoverUrl(fileComponent.shareUrl(item.getPreviewFileId()));
            }
            commonPreviewVO.setDuration(item.getMediaDuration());
            commonPreviewVO.setNickName(item.getNickName());
            if (StrUtil.isNotBlank(item.getAvatarImg())) {
                commonPreviewVO.setAvatar(fileComponent.shareUrl(item.getAvatarImg()));
            }
            commonPreviewVO.setCreateTime(ZonedDateTime.of(item.getCreateTime(), ZoneId.systemDefault()));
            return commonPreviewVO;
        }).toList();

        return PageResult.success(sharVioProject.getTotal(), result);
    }

    private Page<VioProject> getVioProjectPage(String userId, String projectName, TaskType fromTool, PageDTO
            pageDTO) {
        Page<VioProject> page = Page.of(pageDTO.getPage(), pageDTO.getSize());

        return ChainWrappers.lambdaQueryChain(vioProjectMapper)
                .eq(VioProject::getUserId, userId)
                .eq(VioProject::getTaskType, fromTool)
                .eq(VioProject::isShowProject, true)
                .like(StrUtil.isNotBlank(projectName), VioProject::getProjectName, projectName)
                .orderByDesc(VioProject::getCreateTime)
                .page(page);
    }

    public PageResult<FindMusicVO> findMusic(FindMusicDTO findMusicDTO) {
        String userId = StpUtil.getLoginIdAsString();
        Page<VioProject> resultPage = getVioProjectPage(userId, findMusicDTO.getProjectName(), TaskType.MAKE_MUSIC, findMusicDTO);

        List<FindMusicVO> result = resultPage.getRecords().stream().map(item -> {
            FindMusicVO findMusicVO = new FindMusicVO();
            findMusicVO.setProjectId(item.getId());
            findMusicVO.setSessionId(item.getSessionId());
            findMusicVO.setMessageId(item.getMessageId());
            findMusicVO.setProjectName(item.getProjectName());
            findMusicVO.setFileId(item.getFileId());
            findMusicVO.setFileUrl(fileComponent.shareUrl(item.getFileId()));
            findMusicVO.setFileCoverId(item.getPreviewFileId());
            findMusicVO.setFileCoverUrl(fileComponent.shareUrl(item.getPreviewFileId()));
            findMusicVO.setCreateTime(ZonedDateTime.of(item.getCreateTime(), ZoneId.systemDefault()));
            return findMusicVO;
        }).toList();

        return PageResult.success(resultPage.getTotal(), result);
    }

    /**
     * show project 设置为false
     *
     * @param deleteProjectDTO
     */
    public void deleteProject(DeleteProjectDTO deleteProjectDTO) {
        ChainWrappers.lambdaUpdateChain(vioProjectMapper)
                .eq(VioProject::getId, deleteProjectDTO.getProjectId())
                .eq(VioProject::getUserId, StpUtil.getLoginIdAsString())
                .set(VioProject::isShowProject, false)
                .update();
    }

    public void renameProject(RenameProjectDTO renameProjectDTO) {
        ChainWrappers.lambdaUpdateChain(vioProjectMapper)
                .eq(VioProject::getId, renameProjectDTO.getProjectId())
                .eq(VioProject::getUserId, StpUtil.getLoginIdAsString())
                .set(VioProject::getProjectName, renameProjectDTO.getProjectName())
                .update();
    }

    /**
     * 返回分享id
     * 前端使用分享id调用无需鉴权的 分享详情接口  获取分享内容
     *
     * @param projectId
     * @return
     */
    public String getShareLink(String projectId) {
//        VioProject vioProject = ChainWrappers.lambdaQueryChain(vioProjectMapper)
//                .eq(VioProject::getId, projectId)
//                // 分享的数据应该是自己的 或者是首页的公共数据
//                .and(consumer -> consumer
//                        .eq(VioProject::getUserId, StpUtil.getLoginIdAsString())
//                        .or()
//                        .in(VioProject::getId, shareVioProjectIds)
//                )
//                .one();

        VioProject vioProject = vioProjectMapper.selectById(projectId);

        if (vioProject == null) {
            throw new BusinessException(CodeEnum.ParameterError, "项目不存在");
        }

        // 获取用户发送的第一个文本消息
        SseMsgBO.Text userText = historyMessageService.findFirstUserText(vioProject.getSessionId());
        String lyrics = null;
        if (TaskType.MAKE_MUSIC.equals(vioProject.getTaskType())) {
//            MakeMusicArgs makeMusicArgs = JsonUtil.toObject(vioProject.getArgs(), MakeMusicArgs.class);
//            lyrics = makeMusicArgs.getLyrics();
            lyrics = vioProject.getLyrics();
        }
        // 获取用户信息
        User sharer = userMapper.selectById(vioProject.getUserId());

        // 将待分享信息存入 缓存
        String shareId = IdUtil.fastSimpleUUID();
        ShareCacheBO shareCacheBO = new ShareCacheBO(
                vioProject.getUserId(),
                sharer.getNickName(),
                vioProject.getSessionId(),
                vioProject.getProjectName(),
                userText.text(),
                lyrics,
                ShareCacheBO.Type.fromTaskType(vioProject.getTaskType()),
                vioProject.getFileId(),
                null,
                vioProject.getPreviewFileId(),
                null
        );
        redisTemplate.opsForValue().set(RedisConst.SHARE_LINK_CACHE.formatted(shareId), JsonUtil.toJson(shareCacheBO), 3, TimeUnit.DAYS);

        return shareId;
    }

    public ShareCacheBO getShareData(String shareId) {
        String json = redisTemplate.opsForValue().get(RedisConst.SHARE_LINK_CACHE.formatted(shareId));
        if (StrUtil.isBlank(json)) {
            throw new BusinessException(CodeEnum.ParameterError, "分享内容不存在或已过期");
        }
        ShareCacheBO shareCacheBO = JsonUtil.toObject(json, ShareCacheBO.class);
        return new ShareCacheBO(
                shareCacheBO.userId(),
                shareCacheBO.nickName(),
                shareCacheBO.sessionId(),
                shareCacheBO.projectName(),
                shareCacheBO.prompt(),
                shareCacheBO.lyrics(),
                shareCacheBO.type(),
                shareCacheBO.fileId(),
                fileComponent.shareUrl(shareCacheBO.fileId()),
                shareCacheBO.coverId(),
                fileComponent.shareUrl(shareCacheBO.coverId())
        );

    }

    /**
     * 直接编辑主体参考图（异步执行）
     * 读取 chunk JSON → 生成新图片 → 追加版本 → 更新 chunk
     */
    public void directEditSubject(DirectEditSubjectDTO directEditSubjectDTO, String taskKey, String userId,
                                  int pointsRequired) {
        try {
            log.info("directEditSubject taskKey: {}", taskKey);
            HistoryMessageChunk historyMessageChunk = historyMessageService.getChunkByIdAndUserId(directEditSubjectDTO.getChunkId(), userId);
            if (historyMessageChunk == null) {
                throw new BusinessException(CodeEnum.ParameterError, "chunk 不存在");
            }

            JsonNode payload = JsonUtil.readTree(historyMessageChunk.getChunk());
            SseMsgBO.Type type = JsonUtil.tree2Object(payload.path("type"), SseMsgBO.Type.class);
            if (type != SseMsgBO.Type.SUBJECT) {
                throw new BusinessException(CodeEnum.ParameterError, "chunk 类型不匹配，期望 SUBJECT");
            }

            JsonNode data = payload.path("data");
            SseMsgBO.Subject subject = JsonUtil.tree2Object(data, SseMsgBO.Subject.class);

            // 生成新图片
            String newFileId = doMakeImg(directEditSubjectDTO.getPrompt(), directEditSubjectDTO.getRefImageFileIds());

            // 构造新版本
            SseMsgBO.Subject.VersionedImg newVersion = new SseMsgBO.Subject.VersionedImg();
            newVersion.setVersion(subject.getVersions().size()); // 0-based
            newVersion.setImgFileId(newFileId);
            newVersion.setImgUrl(fileComponent.shareUrl(newFileId));
            newVersion.setPrompt(directEditSubjectDTO.getPrompt());
            if (directEditSubjectDTO.getRefImageFileIds() != null) {
                newVersion.setRefImages(directEditSubjectDTO.getRefImageFileIds().stream().map(fid -> {
                    SseMsgBO.Subject.RefImg ref = new SseMsgBO.Subject.RefImg();
                    ref.setFileId(fid);
                    ref.setUrl(fileComponent.shareUrl(fid));
                    return ref;
                }).toList());
            }

            // 追加版本并更新 activeVersion
            subject.getVersions().add(newVersion);
            subject.setActiveVersion(newVersion.getVersion());

            SseMsgBO<SseMsgBO.Subject> updatedMsg = SseMsgBO.ofSubject(subject);

            // 更新 chunk
            historyMessageService.updateChunkContent(directEditSubjectDTO.getChunkId(), updatedMsg);

            // 更新 chat session task 数据
            if (historyMessageChunk.getChatSessionTaskId() != null) {
                updateTaskSubjectData(historyMessageChunk.getChatSessionTaskId(), newVersion, subject.getAssetKey());
            }

            // 更新任务结果
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.SUCCESS, updatedMsg)), Duration.ofMinutes(30));

            // 积分扣除
            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, ModelEnum.NONE, TaskType.MAKE_IMAGE, pointsRequired).pointsTransactionLog();
            billingService.mapSessionWithPointsTransactionLog(directEditSubjectDTO.getSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

        } catch (Exception e) {
            log.error("directEditSubject error", e);
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.FAIL, "生成失败，请重试")), Duration.ofMinutes(30));
        }
    }

    /**
     * 直接编辑分镜视频（异步执行）
     * 读取 chunk JSON → 生成新视频 → 追加版本 → 更新 chunk
     */
    public void directEditScene(DirectEditSceneDTO directEditSceneDTO,
                                String taskKey,
                                String userId) {
        File tempDir = new File(FileDirConst.TEMP_DIR, IdUtil.fastSimpleUUID());
        var _ = tempDir.mkdirs();
        try {
            log.info("directEditScene taskKey: {}", taskKey);
            // 获取历史数据
            HistoryMessageChunk historyMessageChunk = historyMessageService.getChunkByIdAndUserId(directEditSceneDTO.getChunkId(), userId);
            if (historyMessageChunk == null) {
                throw new BusinessException(CodeEnum.ParameterError, "chunk 不存在");
            }

            JsonNode payload = JsonUtil.readTree(historyMessageChunk.getChunk());
            SseMsgBO.Type type = JsonUtil.tree2Object(payload.path("type"), SseMsgBO.Type.class);
            if (type != SseMsgBO.Type.SCENE) {
                throw new BusinessException(CodeEnum.ParameterError, "chunk 类型不匹配，期望 SCENE");
            }

            JsonNode data = payload.path("data");
            SseMsgBO.Scene scene = JsonUtil.tree2Object(data, SseMsgBO.Scene.class);

            // 从已有版本获取 duration（保持不变）
            SseMsgBO.Scene.VersionedVideo activeVer = scene.getVersions().get(scene.getActiveVersion());
            DoMakeVideoBO doMakeVideoBO = directEditSceneDTO.toMakeVideoArgs(scene.getDuration(), activeVer.getAspectRatio(), activeVer.getResolution());

            // 校验用户积分
            int pointsRequired = billingService.getPointsRequired(directEditSceneDTO.getModel(), Objects.requireNonNullElse(activeVer.getResolution(), Resolution.P720), TaskType.MAKE_MV); // 积分单价
            int totalPoints = (int) Math.ceil(scene.getDuration()) * pointsRequired;
            if (!billingService.checkUserBalance(userId, totalPoints)) {
                throw new BusinessException(CodeEnum.PointNotEnough, "积分不足");
            }


            VioProject newSceneVioProject = doMakeVideo(
                    tempDir,
                    userId,
                    directEditSceneDTO.getSessionId(),
                    historyMessageChunk.getHistoryMessageId(),
                    directEditSceneDTO.getModel(),
                    doMakeVideoBO
            );

            // 构造新版本
            SseMsgBO.Scene.VersionedVideo newVersion = new SseMsgBO.Scene.VersionedVideo();
            newVersion.setVersion(scene.getVersions().size());
            newVersion.setVideoFileId(newSceneVioProject.getFileId());
            newVersion.setVideoUrl(fileComponent.shareUrl(newSceneVioProject.getFileId()));
            newVersion.setCoverFileId(newSceneVioProject.getPreviewFileId());
            newVersion.setCoverUrl(fileComponent.shareUrl(newSceneVioProject.getPreviewFileId()));
            newVersion.setVisualPrompt(directEditSceneDTO.getVisualPrompt());
            newVersion.setAspectRatio(doMakeVideoBO.getAspectRatio());
            newVersion.setModel(directEditSceneDTO.getModel());

            // 主体图转换
            if (CollUtil.isNotEmpty(directEditSceneDTO.getSubject())) {
                List<SseMsgBO.Scene.SubjectRef> subjectRefs = directEditSceneDTO.getSubject().stream()
                        .map(s -> {
                            SseMsgBO.Scene.SubjectRef ref = new SseMsgBO.Scene.SubjectRef();
                            ref.setId(s.getSubjectId());
                            ref.setImageFileId(s.getFileId());
                            ref.setImageUrl(fileComponent.shareUrl(s.getFileId()));
                            return ref;
                        }).toList();
                newVersion.setSubjects(subjectRefs);
            }

            // 追加版本并更新 activeVersion
            scene.getVersions().add(newVersion);
            scene.setActiveVersion(newVersion.getVersion());

            SseMsgBO<SseMsgBO.Scene> updatedMsg = SseMsgBO.ofScene(scene);

            // 更新 chunk
            historyMessageService.updateChunkContent(directEditSceneDTO.getChunkId(), updatedMsg);

            // 更新 chat session task 数据
            if (historyMessageChunk.getChatSessionTaskId() != null) {
                updateTaskSceneData(historyMessageChunk.getChatSessionTaskId(), newVersion, scene.getAssetKey());
            }

            // 更新任务结果
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.SUCCESS, updatedMsg)), Duration.ofMinutes(30));

            // 积分扣除
            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, directEditSceneDTO.getModel(), TaskType.MAKE_VIDEO, totalPoints).pointsTransactionLog();
            billingService.mapSessionWithPointsTransactionLog(directEditSceneDTO.getSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

        } catch (Exception e) {
            log.error("directEditScene error", e);
            redisTemplate.opsForValue().set(taskKey, JsonUtil.toJson(new DirectEditTaskBO(StateEnum.FAIL, e.getMessage())), Duration.ofMinutes(30));
        } finally {
            FileUtil.del(tempDir);
        }
    }

    /**
     * 切换版本（同步）
     * 读取 chunk JSON → 修改 activeVersion → 更新回去
     */
    public void switchVersion(SwitchVersionDTO switchVersionDTO) {
        HistoryMessageChunk historyMessageChunk = historyMessageService.getChunkByIdAndUserId(switchVersionDTO.getChunkId(), StpUtil.getLoginIdAsString());
        if (historyMessageChunk == null) {
            throw new BusinessException(CodeEnum.ParameterError, "chunk 不存在");
        }

        JsonNode payload = JsonUtil.readTree(historyMessageChunk.getChunk());
        SseMsgBO.Type type = JsonUtil.tree2Object(payload.path("type"), SseMsgBO.Type.class);
        JsonNode data = payload.path("data");

        SseMsgBO<?> updatedMsg;

        if (type == SseMsgBO.Type.SUBJECT) {
            SseMsgBO.Subject subject = JsonUtil.tree2Object(data, SseMsgBO.Subject.class);
            if (switchVersionDTO.getActiveVersion() < 0 || switchVersionDTO.getActiveVersion() >= subject.getVersions().size()) {
                throw new BusinessException(CodeEnum.ParameterError, "版本号超出范围");
            }
            subject.setActiveVersion(switchVersionDTO.getActiveVersion());
            updatedMsg = SseMsgBO.ofSubject(subject);

            if (historyMessageChunk.getChatSessionTaskId() != null) {
                // 找到新版本数据
                SseMsgBO.Subject.VersionedImg versionedImg = subject.getVersions().get(switchVersionDTO.getActiveVersion());
                // 更新 chat session task 数据
                updateTaskSubjectData(historyMessageChunk.getChatSessionTaskId(), versionedImg, subject.getAssetKey());
            }

        } else if (type == SseMsgBO.Type.SCENE) {
            SseMsgBO.Scene scene = JsonUtil.tree2Object(data, SseMsgBO.Scene.class);
            if (switchVersionDTO.getActiveVersion() < 0 || switchVersionDTO.getActiveVersion() >= scene.getVersions().size()) {
                throw new BusinessException(CodeEnum.ParameterError, "版本号超出范围");
            }
            scene.setActiveVersion(switchVersionDTO.getActiveVersion());
            updatedMsg = SseMsgBO.ofScene(scene);

            if (historyMessageChunk.getChatSessionTaskId() != null) {
                // 找到新版本数据
                SseMsgBO.Scene.VersionedVideo versionedVideo = scene.getVersions().get(switchVersionDTO.getActiveVersion());
                // 更新 chat session task 数据
                updateTaskSceneData(historyMessageChunk.getChatSessionTaskId(), versionedVideo, scene.getAssetKey());
            }

        } else {
            throw new BusinessException(CodeEnum.ParameterError, "该 chunk 不支持版本切换");
        }

        historyMessageService.updateChunkContent(switchVersionDTO.getChunkId(), updatedMsg);
    }

    /**
     * 更新任务数据中的 某个主体
     */
    private void updateTaskSubjectData(Integer taskId, SseMsgBO.Subject.VersionedImg versionedImg, String subjectId) {
        // 更新 chat session task 数据
        if (taskId == null) {
            log.warn("taskId 为空，无法更新任务数据 subjectId:{}", subjectId);
            return;
        }
        // 获取任务数据
        ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(taskId); // 此时chunk已经被校验过所属当前用户，查询task数据时直接使用id查询即可
        // 找到新版本数据
        // 找到主体 更新数据
        Optional<ChatSessionTaskPayload.PayloadSubject> first = taskPayload.getSubjects().stream()
                .filter(payloadSubject -> payloadSubject.getId().equals(subjectId))
                .findFirst();

        first.ifPresent(payloadSubject -> {
            payloadSubject.setDescription(versionedImg.getPrompt());
            payloadSubject.setRefImgs(versionedImg.getRefImages().stream().map(SseMsgBO.Subject.RefImg::getFileId).toList());
            payloadSubject.setResultFileId(versionedImg.getImgFileId());
            // 更新数据库
            sessionTaskService.updateTaskPayload(taskId, taskPayload);
        });
    }

    /**
     * 更新任务数据中的 某个场景
     * 根据 SseMsgBO.Scene.VersionedVideo 更新场景数据
     */
    private void updateTaskSceneData(@NonNull Integer taskId, SseMsgBO.Scene.VersionedVideo versionedVideo, String
            sceneId) {
        // 获取任务数据
        ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(taskId); // 此时chunk已经被校验过所属当前用户，查询task数据时直接使用id查询即可
        // 找到主体 更新数据
        Optional<ChatSessionTaskPayload.PayloadScene> first = taskPayload.getScenes().stream()
                .filter(payloadScene -> payloadScene.getId().equals(sceneId))
                .findFirst();

        first.ifPresent(payloadScene -> {
            payloadScene.setVisualPrompt(versionedVideo.getVisualPrompt());
            payloadScene.setResultFileId(versionedVideo.getVideoFileId());
//            payloadScene.setSubjectRefs(versionedVideo.getSubjects().stream().map(SseMsgBO.Scene.SubjectRef::getId).toList());
            // 提取主体id 用来校验传入的新主体是否为新增
            Map<String, ChatSessionTaskPayload.PayloadSubject> subjectMap = taskPayload.subject2Map();
            // 循环装入主体数据
            payloadScene.setDirectSubjects(new ArrayList<>());
            payloadScene.setSubjectRefs(new ArrayList<>());
            for (SseMsgBO.Scene.SubjectRef versionSubjectRef : versionedVideo.getSubjects()) {
                if (subjectMap.containsKey(versionSubjectRef.getId())) { // 存在主体列表中装入  setSubjectRefs
                    payloadScene.getSubjectRefs().add(versionSubjectRef.getImageFileId());
                } else { // 不存在主体列表中视为用户自定义参考引用，装入 DirectSubjects
                    payloadScene.getDirectSubjects().add(new SceneSubject(versionSubjectRef.getId(), versionSubjectRef.getImageFileId()));
                }
            }
            // 更新数据库
            sessionTaskService.updateTaskPayload(taskId, taskPayload);
        });
    }

    /**
     * 更新分镜脚本
     *
     * @param updateSceneScriptDTO
     */
    public void updateSceneScript(UpdateSceneScriptDTO updateSceneScriptDTO) {
        // 校验参数
        if (updateSceneScriptDTO.getScriptIdx() < 0) {
            throw new BusinessException(CodeEnum.ParameterError, "分镜脚本索引不能小于0");
        }
        if (CollUtil.isEmpty(updateSceneScriptDTO.getSubjectRefs()) && StrUtil.isBlank(updateSceneScriptDTO.getVisualPrompt())) {
            throw new BusinessException(CodeEnum.ParameterError, "修改参数不能为空");
        }

        HistoryMessageChunk historyMessageChunk = historyMessageService.getChunkByIdAndUserId(updateSceneScriptDTO.getChunkId(), StpUtil.getLoginIdAsString());
        if (historyMessageChunk == null) {
            throw new BusinessException(CodeEnum.ParameterError, "chunk 不存在");
        }
        // 提取旧 sseMsgBO 数据 开始修改
        JsonNode payload = JsonUtil.readTree(historyMessageChunk.getChunk());
        SseMsgBO.Type type = JsonUtil.tree2Object(payload.path("type"), SseMsgBO.Type.class);
        if (type != SseMsgBO.Type.SCENE_SCRIPT) {
            throw new BusinessException(CodeEnum.ParameterError, "该 chunk 不支持更新分镜脚本");
        }
        JsonNode data = payload.path("data");
        List<SseMsgBO.SceneScript> scenesScripts = JsonUtil.tree2ListObject(data, SseMsgBO.SceneScript.class);
        if (updateSceneScriptDTO.getScriptIdx() >= scenesScripts.size()) {
            throw new BusinessException(CodeEnum.ParameterError, "分镜脚本索引超出范围");
        }
        SseMsgBO.SceneScript sceneScript = scenesScripts.get(updateSceneScriptDTO.getScriptIdx());
        if (StrUtil.isNotBlank(updateSceneScriptDTO.getVisualPrompt())) {
            sceneScript.setVisualPrompt(updateSceneScriptDTO.getVisualPrompt());
        }
        if (updateSceneScriptDTO.getSubjectRefs() != null) {
            sceneScript.setSubjectRefs(updateSceneScriptDTO.getSubjectRefs());
        }


        // 更新任务数据
        if (historyMessageChunk.getChatSessionTaskId() != null) {
            // 获取任务数据
            ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(historyMessageChunk.getChatSessionTaskId());
            // 找到分镜 更新数据
            Optional<ChatSessionTaskPayload.PayloadScene> findPayloadScene = taskPayload.getScenes().stream()
                    .filter(payloadScene -> payloadScene.getId().equals(sceneScript.getId()))
                    .findFirst();

            findPayloadScene.ifPresent(payloadScene -> {
                payloadScene.setVisualPrompt(sceneScript.getVisualPrompt());
//                payloadScene.setSubjectRefs(sceneScript.getSubjectRefs().stream().map(SseMsgBO.SceneScript.SubjectRef::getSubjectId).toList());
                // 提取主体id 用来校验传入的新主体是否为新增
                Map<String, ChatSessionTaskPayload.PayloadSubject> subjectMap = taskPayload.subject2Map();
                // 循环装入主体数据
                payloadScene.setDirectSubjects(new ArrayList<>());
                payloadScene.setSubjectRefs(new ArrayList<>());
                for (SseMsgBO.SceneScript.SubjectRef subjectRef : sceneScript.getSubjectRefs()) {
                    if (subjectMap.containsKey(subjectRef.getSubjectId())) { // 存在主体列表中装入  setSubjectRefs
                        payloadScene.getSubjectRefs().add(subjectRef.getSubjectId());
                    } else { // 不存在主体列表中视为用户自定义参考引用，装入 DirectSubjects
                        payloadScene.getDirectSubjects().add(new SceneSubject(subjectRef.getSubjectId(), subjectRef.getFileId()));
                    }
                }

                // 更新数据库
                sessionTaskService.updateTaskPayload(historyMessageChunk.getChatSessionTaskId(), taskPayload);
            });
        }
        historyMessageService.updateChunkContent(updateSceneScriptDTO.getChunkId(), SseMsgBO.ofSceneScript(scenesScripts));
    }

    public LipSyncPointsVO getLipSyncPoints(Integer messageChunkId) {
        HistoryMessageChunk historyMessageChunk = historyMessageService.getChunkByIdAndUserId(messageChunkId, StpUtil.getLoginIdAsString());
        if (historyMessageChunk == null) {
            throw new BusinessException(CodeEnum.ParameterError, "chunk 不存在");
        }
        if (historyMessageChunk.getChatSessionTaskId() == null) {
            throw new BusinessException(CodeEnum.ParameterError, "该 chunk 不支持获取口型积分");
        }
        // 获取对应 session task 数据
        ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(historyMessageChunk.getChatSessionTaskId());

        // 获取对口型单价
        int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.LIP_SYNC); // 积分单价

        List<LipSyncPointsVO.Item> items = new ArrayList<>();
        int sumDuration = 0;
        int totalPoints = 0;
        for (ChatSessionTaskPayload.PayloadScene scene : taskPayload.getScenes()) {
            if (!scene.isLipSync()) {
                continue;
            }
            LipSyncPointsVO.Item item = new LipSyncPointsVO.Item();
            item.setSceneId(scene.getId());
            item.setDuration((int) Math.ceil(scene.getDuration()));
            item.setPoints(pointsRequired * item.getDuration());
            items.add(item);

            sumDuration += item.getDuration();
            totalPoints += item.getPoints();
        }

        LipSyncPointsVO lipSyncPointsVO = new LipSyncPointsVO();
        lipSyncPointsVO.setTotalDuration(sumDuration);
        lipSyncPointsVO.setTotalPoints(totalPoints);
        lipSyncPointsVO.setScenes(items);

        return lipSyncPointsVO;
    }
}
