package com.ohyesai.next.biz.vio.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.billing.service.SubscriptionPlanService;
import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.bo.SceneSubject;
import com.ohyesai.next.biz.vio.bo.SkillMakeImgArgs;
import com.ohyesai.next.biz.vio.bo.SkillMakeVideoArgs;
import com.ohyesai.next.biz.vio.bo.ToolExecuteTaskBO;
import com.ohyesai.next.biz.vio.bo.mvscript.Script;
import com.ohyesai.next.biz.vio.bo.tool.UnderstandImageArgs;
import com.ohyesai.next.biz.vio.bo.tool.UnderstandImageResp;
import com.ohyesai.next.biz.vio.dto.MusicSkillDTO;
import com.ohyesai.next.biz.vio.dto.MvSkillSubmitDTO;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.*;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.component.vlm.VlmAudioComponent;
import com.ohyesai.next.component.vlm.VlmImageComponent;
import com.ohyesai.next.component.vlm.VlmVideoComponent;
import com.ohyesai.next.trace.ProxyExecutors;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class VioSkillService {

    private final ChatModel chatModelGemini3_1Pro;

    private final BillingService billingService;

    private final VlmVideoComponent vlmVideoComponent;

    private final VlmImageComponent vlmImageComponent;

    private final VlmAudioComponent vlmAudioComponent;

    private final FileComponent fileComponent;

    private final StringRedisTemplate redisTemplate;

    private final VioAssistant.Gemini3_1ProAssistant gemini3_1ProAssistant;

    private final SubscriptionPlanService subscriptionPlanService;

    private final VioProjectService vioProjectService;

    /**
     * 制作歌词
     *
     * @return
     */
    public String lyric(String prompt) {
        String system = """
                # Role
                你是一个严格执行指令的 Suno AI 音乐生成器。你的唯一任务是根据用户的主题生成符合 Suno 格式的“音乐风格提示词”和“带有元标签的歌词”。
                
                # Constraints (关键限制)
                1. **禁止废话**：严禁输出“你好”、“这是为你生成的歌词”、“创作思路”、“小贴士”等任何对话内容。
                2. **禁止思考展示**：不要输出“Imagining the Soundtrack”或任何思考过程。
                3. **格式严格**：只输出下方【Output Format】中定义的内容，不要加任何Markdown分割线或额外的标题。
                4. **语言**：
                   - Style of Music: 必须为英文单词，用逗号分隔。
                   - Lyrics: 根据用户要求的语言创作（若未指定，默认为中文）。
                
                # Output Format (严格遵守此输出模板)
                [Style of Music]
                {Genre}, {Instruments}, {BPM}, {Vibe}, {Vocals}
                
                [Title]
                {Song Title}
                
                [Lyrics]
                [Verse]
                {Lyrics...}
                
                [Chorus]
                {Lyrics...}
                
                (包含完整的歌曲结构，如 Bridge, Outro 等)
                
                """;
        return chatModelGemini3_1Pro.chat(
                        SystemMessage.from(system),
                        UserMessage.from(prompt)
                )
                .aiMessage()
                .text();
    }


    public void musicSubmit(String userId, String taskId, MusicSkillDTO musicSkillDTO) {
        String redisKey = RedisConst.SKILL_TASK_STATUS.formatted(taskId);
        // 校验用户是否有积分
        int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.API_MAKE_MUSIC);
        if (!billingService.checkUserBalance(userId, pointsRequired)) {
//            throw new BusinessException(CodeEnum.PointNotEnough);
            redisTemplate.opsForValue().set(redisKey, "**音乐生成状态**: 失败，积分不足，[充值链接](https://ohyesai.com)", Duration.ofMinutes(30));
            return;
        }

        // 初始化任务状态
        redisTemplate.opsForValue().set(redisKey, "**音乐生成状态**: 进行中", Duration.ofMinutes(30));


        File tempDir = new File(FileDirConst.TEMP_DIR, IdUtil.fastSimpleUUID());
        var _ = tempDir.mkdirs();
        try {
            // 生成歌词
            String lyric = lyric(musicSkillDTO.getPrompt());

            List<VlmAudioComponent.AudioResp> musicResps = vlmAudioComponent.generateMusicBySuno(
                    lyric,
                    musicSkillDTO.getTitle(),
                    musicSkillDTO.getStyles(),
                    musicSkillDTO.getInstrumental(),
                    null
            );

            StringBuilder sb = new StringBuilder();
            sb.append("**音乐生成状态**: 已完成\n");
            for (int i = 0; i < musicResps.size(); i++) {
                VlmAudioComponent.AudioResp audioResp = musicResps.get(i);

                // 下载文件
                File musicFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp3");
                HttpUtil.downloadFile(audioResp.audioUrl(), musicFile);
                // 上传文件
                String musicFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp3");
                fileComponent.upload(musicFile, musicFileId);
                String musicUrl = fileComponent.shareUrl(musicFileId);

//                String musicCoverFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "jpg");
//                fileComponent.uploadByUrl(sunoResp.imageUrl(), musicCoverFileId);
                // 拼接 md 格式的链接
                sb.append("[").append(audioResp.title()).append("_").append(i + 1).append("](").append(musicUrl).append(")\n");
            }

            // 消费积分
            billingService.consumePoints(userId, ModelEnum.NONE, TaskType.API_MAKE_MUSIC, pointsRequired);
//            billingService.mapSessionWithPointsTransactionLog(sseMessageHelper.getChatSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

            redisTemplate.opsForValue().set(redisKey, sb.toString(), Duration.ofMinutes(30));

        } catch (Exception e) {
            redisTemplate.opsForValue().set(redisKey, "**音乐生成状态**: 失败", Duration.ofMinutes(30));
            log.error("music skill task 失败 taskId: {}", taskId, e);
        } finally {
            FileUtil.del(tempDir);

        }
    }

    public String musicQuery(@NotBlank String taskId) {
        String status = redisTemplate.opsForValue().get(RedisConst.SKILL_TASK_STATUS.formatted(taskId));
        if (StrUtil.isBlank(status)) {
            return "**任务不存在**";
        }
        return status;
    }

    public void mvSubmit(MvSkillSubmitDTO mvSkillSubmitDTO, String taskId, String userId) {
        String redisKey = RedisConst.SKILL_TASK_STATUS.formatted(taskId);
        ModelEnum model = ModelEnum.VIDUQ2;

        ToolExecuteTaskBO toolExecuteTaskBO = new ToolExecuteTaskBO(StateEnum.RUNNING);
        // 初始化任务状态 进行中
        redisTemplate.opsForValue().set(redisKey, JsonUtil.toJson(toolExecuteTaskBO), Duration.ofMinutes(30));
        File tempDir = new File(FileDirConst.TEMP_DIR, IdUtil.fastSimpleUUID());
        var _ = tempDir.mkdirs();
        try {

            // 制作脚本
            Script mvScript = mvScriptGenerate(tempDir, mvSkillSubmitDTO.getAudioFileId(), mvSkillSubmitDTO.getStyle(), mvSkillSubmitDTO.getAspectRatio());
            // 校验积分
            int[] mvPointsRequired = mvPointsRequired(mvScript, userId, model);
            // 制作参考图
            List<SkillMakeImgArgs> skillMakeImgArgs = mvScript.getSubjects().stream().map(definedSubject -> new SkillMakeImgArgs(definedSubject.getDescription(), definedSubject.getId(), definedSubject.getRefImgs())).toList();
            Map<String, String> makeImgResp = makeImg(userId, skillMakeImgArgs, mvPointsRequired[0]);
            // 制作分镜视频
            List<SkillMakeVideoArgs> skillMakeVideoArgs = mvScript.getScenes().stream().map(scene -> {
                List<SceneSubject> subjects = scene.getSubjectRefs().stream().map(subjectRef ->
                        new SceneSubject(subjectRef, makeImgResp.get(subjectRef))
                ).toList();
                return new SkillMakeVideoArgs(scene.getVisualPrompt(), scene.getDuration(), subjects, mvSkillSubmitDTO.getAspectRatio());
            }).toList();
            List<String> makeVideoResp = makeVideo(tempDir, userId, skillMakeVideoArgs, model, mvPointsRequired[1]);
            // 合并视频
            String mergeVideoFileId = mergeAudioVideo(tempDir, userId, makeVideoResp, mvSkillSubmitDTO.getAudioFileId());

            // 换取视频链接
            String mergeVideoUrl = fileComponent.shareUrl(mergeVideoFileId);
            toolExecuteTaskBO.setState(StateEnum.SUCCESS);
            toolExecuteTaskBO.setData(mergeVideoUrl);
            redisTemplate.opsForValue().set(redisKey, JsonUtil.toJson(toolExecuteTaskBO), Duration.ofMinutes(30));

            // 扣除积分
//            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, model, TaskType.MAKE_VIDEO, totalPoints).pointsTransactionLog();

        } finally {
            FileUtil.del(tempDir);
        }

    }

    /**
     * 基于脚本计算积分
     *
     * @return [imgTotalPoints, videoTotalPoints]
     */
    public int[] mvPointsRequired(Script mvScript, String userId, ModelEnum model) {
        // 计算图片所需积分
        int imgPointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.MAKE_IMAGE); // 积分单价
        int imgCount = mvScript.getSubjects().size(); // 图片数量
        int imgTotalPoints = imgPointsRequired * imgCount;
//        int imgGapBalance = billingService.compareBalance(userId, imgTotalPoints);
//        if (imgGapBalance < 0) {
//            throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(Math.abs(imgGapBalance)));
//        }
        // 计算视频所需积分
        int videoPointsRequired = billingService.getPointsRequired(model, Resolution.P720, TaskType.MAKE_MV); // 积分单价
        int sumDuration = mvScript.getScenes().stream().mapToInt(v -> (int) Math.ceil(v.getDuration())).sum(); // 总时长
        int videoTotalPoints = videoPointsRequired * sumDuration; // 本次消耗总积分
//        int videoGapBalance = billingService.compareBalance(userId, videoTotalPoints);
//        if (videoGapBalance < 0) {
//            throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(Math.abs(videoGapBalance)));
//        }

        // 校验一共所需积分
        int totalPoints = imgTotalPoints + videoTotalPoints;
        int gapBalance = billingService.compareBalance(userId, totalPoints);
        if (gapBalance < 0) {
            int userPoints = gapBalance + totalPoints;
            throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(totalPoints, userPoints, Math.abs(gapBalance)));
        }
        return new int[]{imgTotalPoints, videoTotalPoints};
    }


    public String mergeAudioVideo(File tempDir, String userId, List<String> videoFileIds, String audioFileId) {
        // 下载视频片段
        List<File> videoFiles = videoFileIds.parallelStream()
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

        // 音频文件
        File audioFIle = new File(tempDir, audioFileId);
        fileComponent.download(audioFileId, audioFIle);
        // 合并
        File mergeOutputFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        CvUtil.mergeAudioVideo(concatVideoFile, audioFIle, mergeOutputFile);

        // 增加水印
        SubscriptionPlan subscriptionPlanByUser = subscriptionPlanService.getSubscriptionPlanByUser(userId);// 获取用户的订阅计划

        // 最终结果文件
        File outputFile;
        if (subscriptionPlanByUser == null || subscriptionPlanByUser.getSubscriptionType() == SubscriptionPlan.SubscriptionType.FREE) {
            outputFile = vioProjectService.addWatermark(tempDir, mergeOutputFile);
        } else {
            // 付费用户不增加水印
            outputFile = mergeOutputFile;
        }

        // 上传最终结果
        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        fileComponent.upload(outputFile, fileId);
        return fileId;
    }


    public List<String> makeVideo(File tempDir, String userId, List<SkillMakeVideoArgs> makeVideoArgsArr, ModelEnum model, int totalPoints) {
        log.info("Run makeVideo args: {}", makeVideoArgsArr);
        Objects.requireNonNull(makeVideoArgsArr, CommonConst.ARGS_NULL_ERROR);

//        // 校验用户是否有积分
//        int pointsRequired = billingService.getPointsRequired(model, TaskType.API_MAKE_MV); // 积分单价
//        int sumDuration = makeVideoArgsArr.stream().mapToInt(v -> (int) Math.ceil(v.getDuration())).sum(); // 总时长
//        int totalPoints = pointsRequired * sumDuration; // 本次消耗总积分
//        int gapBalance = billingService.compareBalance(userId, totalPoints);
//        if (gapBalance < 0) {
//            throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(Math.abs(gapBalance)));
//        }

        List<Future<String>> result = new ArrayList<>();
        try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
            for (SkillMakeVideoArgs makeVideoArgs : makeVideoArgsArr) {
                Future<String> future = executorService.submit(() -> {
                    // 缓存策略 仅在本轮生效
                    String cacheKey = RedisConst.AGENT_TOOL_RESULT_CACHE.formatted("skill", makeVideoArgs.cacheKey());
                    String fileId = redisTemplate.opsForValue().get(cacheKey);
                    if (StrUtil.isBlank(fileId)) {
                        fileId = doMakeVideo(
                                tempDir,
                                model,
                                makeVideoArgs
                        );
                        // 缓存
                        redisTemplate.opsForValue().set(cacheKey, fileId, Duration.ofMinutes(30));
                    }
                    // 推送版本化 SCENE 消息
                    return fileId;
                });

                result.add(future);
            }
        }

        List<String> resps = errorHandler(result);
        // 积分扣除
        billingService.consumePoints(userId, model, TaskType.API_MAKE_MV, totalPoints);

        return resps;

    }

    private String doMakeVideo(File tempDir, ModelEnum model, SkillMakeVideoArgs makeVideoArgs) {
        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        // 参数转换
        List<VlmVideoComponent.Subject> subjects = null;
        if (CollUtil.isNotEmpty(makeVideoArgs.getSubject())) {
            // 主体参数转换，将文件id换成文件url
            subjects = makeVideoArgs.getSubject().stream().map(sceneSubject -> {
                VlmVideoComponent.Subject subject_a = new VlmVideoComponent.Subject();
                subject_a.setId(sceneSubject.getId());
                subject_a.setImages(List.of(fileComponent.shareUrl(sceneSubject.getFileId())));
                return subject_a;
            }).toList();
        }

        // 映射比例
        AspectRatio aspectRatio = makeVideoArgs.getAspectRatio();

        // 计算视频生成时长：向上取整（API只支持整数秒）
        double exactDuration = makeVideoArgs.getDuration() != null ? makeVideoArgs.getDuration() : 5.0;
        int apiDuration = (int) Math.ceil(exactDuration);

        VlmVideoComponent.VideoResp videoResp = switch (model) {
            case VIDUQ2 ->
                    vlmVideoComponent.viduReference2video(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, Resolution.P720);
            case SEEDANCE_2 ->
                    vlmVideoComponent.seedance2Video(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, Resolution.P720);
            case SEEDANCE_2_FAST ->
                    vlmVideoComponent.seedance2FastVideo(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, Resolution.P720);
            case KLING_V3_OMNI ->
                    vlmVideoComponent.klingTextToVideo(makeVideoArgs.getVisualPrompt(), apiDuration, aspectRatio, subjects, Resolution.P720);
            default -> throw new BusinessException(CodeEnum.Unknow, "暂不支持该模型");
        };
        // 下载 存储
        File videoFileTemp = new File(tempDir, IdUtil.fastSimpleUUID() + "_temp.mp4");
        HttpUtil.downloadFile(videoResp.videoUrl(), videoFileTemp);

        File videoFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp4");
        int width = 0, height = 0;
        switch (makeVideoArgs.getAspectRatio()) { // 目前分辨率统一 720p 计算
            case LANDSCAPE -> {
                height = 720;
                width = 1280;
            }
            case PORTRAIT -> {
                height = 1280;
                width = 720;
            }
        }
        CvUtil.scale(videoFileTemp, videoFile, width, height);

        // 当脚本规划时长非整数秒时，截断视频到精确时长
        if (exactDuration != apiDuration) {
            File trimmedFile = new File(tempDir, IdUtil.fastSimpleUUID() + "_trimmed.mp4");
            CvUtil.trimVideo(videoFile, trimmedFile, 0, exactDuration);
            videoFile = trimmedFile;
        }

        fileComponent.upload(videoFile, fileId);
        return fileId;
    }


    public Map<String, String> makeImg(String userId, List<SkillMakeImgArgs> makeImgArgsArr, int totalPoints) {
        log.info("Run makeImg args: {}", makeImgArgsArr);
        Objects.requireNonNull(makeImgArgsArr, CommonConst.ARGS_NULL_ERROR);
        // 校验用户是否有积分
//        int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.API_MAKE_IMAGE); // 积分单价
//        int imgCount = makeImgArgsArr.size(); // 图片数量
//        int totalPoints = pointsRequired * imgCount;
//        int gapBalance = billingService.compareBalance(userId, totalPoints);
//        if (gapBalance < 0) {
//            throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(Math.abs(gapBalance)));
//        }

        List<Future<Tuple>> result = new ArrayList<>();
        try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
            for (SkillMakeImgArgs makeImgArgs : makeImgArgsArr) {
                Future<Tuple> future = executorService.submit(() -> {
                    List<String> refImageUrls = makeImgArgs.getRefImageFileId().stream().map(fileComponent::shareUrl).toList();
                    List<byte[]> imgBytes = vlmImageComponent.seedreamGenerateImage(makeImgArgs.getPrompt(), refImageUrls, 1);
                    // 获取第一个图片
                    byte[] imageBytes = imgBytes.getFirst();
                    // 上传图片
                    String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "png");
                    fileComponent.upload(imageBytes, fileId);
                    return new Tuple(makeImgArgs.getSubjectIdx(), fileId);
                });
                result.add(future);
            }
        }

        List<Tuple> tuples = errorHandler(result);
        Map<String, String> resps = tuples.stream().collect(Collectors.toMap(v -> v.get(0), v -> v.get(1)));

        // 积分扣除
        billingService.consumePoints(userId, ModelEnum.NONE, TaskType.API_MAKE_IMAGE, totalPoints);

        return resps;
    }

    public Script mvScriptGenerate(File tempDir, String audioFileId, String style, AspectRatio aspectRatio) {
        // 下载音频文件
        File audioFile = new File(tempDir, audioFileId);
        fileComponent.download(audioFileId, audioFile);
        // 获取音频总时长
        CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(audioFile);
        // 校验音频时长
        if (mediaInfo.duration().toSeconds() > 301) {
            throw new BusinessException(CodeEnum.ParameterError, "音频时长不能超过5分钟");
        }

        try {

            // 将音频文件转为base64
            String audioBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(audioFile.toPath()));
        /*
            获取音频文件 mime type
            Gemini 支持以下音频格式 MIME 类型：
            WAV - audio/wav
            MP3 - audio/mp3
            AIFF - audio/aiff
            AAC - audio/aac
            OGG Vorbis - audio/ogg
            FLAC - audio/flac
         */

            String audioMimeType = Files.probeContentType(audioFile.toPath());

            AudioContent audioContent = AudioContent.from(audioBase64, audioMimeType);
            return gemini3_1ProAssistant.mvScriptGenerate(
                    mediaInfo.duration().toSeconds(),
                    style,
                    "无",
                    "无",
                    "无",
                    "无（请通过聆听音频自行分析节奏和段落结构）",
                    aspectRatio,
                    audioContent
            );

        } catch (Exception e) {
            log.error("工具执行失败", e);
            throw new BusinessException(CodeEnum.Unknow, "工具执行失败，" + e.getMessage());
        }

    }

    public List<UnderstandImageResp> understandImage(File tempDir, List<UnderstandImageArgs> understandImageList) {
        log.info("Run understandImage-skill args: {}", understandImageList);
        Objects.requireNonNull(understandImageList, CommonConst.ARGS_NULL_ERROR);

        List<Future<UnderstandImageResp>> featureResult = new ArrayList<>();
        try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
            for (UnderstandImageArgs understandImageArgs : understandImageList) {
                Future<UnderstandImageResp> future = executorService.submit(() -> {
                    // 1. 根据 understandImageArgs.getImageFileId() 下载或获取图片
                    File imageFile = new File(tempDir, understandImageArgs.getImageFileId());
                    fileComponent.download(understandImageArgs.getImageFileId(), imageFile);

                    // 2. 调用多模态模型分析图片内容
                    String intentionName = understandImageArgs.getIntention().name();
                    Path imagePath = imageFile.toPath();
                    String imageMimeType = Files.probeContentType(imagePath);
                    ImageContent imageContent = ImageContent.from(imagePath, imageMimeType);
                    VioAssistant.Gemini3_1ProAssistant.UnderstandImage understandImage = gemini3_1ProAssistant.understandImage(imageContent, intentionName);
                    // 转换返回值
                    UnderstandImageResp understandImageResp = new UnderstandImageResp();
                    understandImageResp.setDescription(understandImage.getDescription());
                    understandImageResp.setMatchScore(understandImage.getMatchScore());
                    understandImageResp.setSuggestion(understandImage.getSuggestion());
                    understandImageResp.setIndex(understandImageArgs.getIndex());
                    return understandImageResp;
                });

                featureResult.add(future);
            }
        }

        // 先轮训一遍确保所有分镜都正常完成
        StringBuilder errMsg = new StringBuilder();
        List<UnderstandImageResp> tupleList = new ArrayList<>();
        for (int i = 0; i < featureResult.size(); i++) {
            try {
                tupleList.add(featureResult.get(i).get());
            } catch (InterruptedException e) {
                log.error("makeVideo 执行失败", e);
                Thread.currentThread().interrupt();
                throw new BusinessException(CodeEnum.Unknow, "系统线程被中断，请直接使用原全量参数重新提交");
            } catch (ExecutionException e) {
                log.error("makeVideo 第{}个任务执行失败", i, e);

                // ExecutionException 包装了实际的异常，必须通过 getCause() 获取真实报错信息
                Throwable cause = e.getCause();
                String reason = (cause != null && cause.getMessage() != null)
                        ? cause.getMessage()
                        : e.getMessage();

                // 拼接给 Agent 看的信息：明确指出【入参索引/第几个】失败了，以及【失败原因】
                // Agent 对 JSON 数组的索引很敏感，告诉它索引值有助于它去修改对应的参数
                errMsg.append(String.format("- 第 %d 个片段 (入参索引 [%d]) 生成失败，原因: %s\n", i + 1, i, reason));
            }
        }

        // 循环结束后，判断是否有部分任务失败
        if (!errMsg.isEmpty()) {
            // 💡 核心技巧：在具体的错误列表前，加上一段【引导指令】，再次向 Agent 强调原子性重试规则
            String finalErrorMessage = """
                    【工具执行内部报错】批量生成任务部分失败：
                    - 请分析下方失败原因，调整失败片段的参数进行重试。
                    失败详情如下：
                    %s
                    """.formatted(errMsg);

            // 抛出异常返回给 Agent（或者封装进你的标准 Result/Response 对象中返回）
            throw new BusinessException(CodeEnum.Unknow, finalErrorMessage);
        }
        return tupleList;

    }

    public ToolExecuteTaskBO mvQuery(@NotBlank String taskId) {
        String taskData = redisTemplate.opsForValue().get(RedisConst.SKILL_TASK_STATUS.formatted(taskId));
        if (StrUtil.isBlank(taskData)) {
            ToolExecuteTaskBO taskBO = new ToolExecuteTaskBO();
            taskBO.setState(StateEnum.FAIL);
            taskBO.setData("任务不存在，请核对任务Id是否正确");
            return taskBO;
        }
        return JsonUtil.toObject(taskData, ToolExecuteTaskBO.class);
    }

    public String upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (StrUtil.isBlank(fileName)) {
            throw new BusinessException(CodeEnum.Unknow, "无法获取文件名称");
        }
        try (InputStream is = file.getInputStream()) {
            String fileObjectName = fileComponent.genObjectName(FileDirConst.TEMP_24H, fileName);
            fileComponent.upload(is, fileObjectName);
            return fileObjectName;
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    private <T> List<T> errorHandler(List<Future<T>> result) {
        // 先轮训一遍确保所有分镜都正常完成
        StringBuilder errMsg = new StringBuilder();
        List<T> tupleList = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            try {
                tupleList.add(result.get(i).get());
            } catch (InterruptedException e) {
                log.error("makeVideo 执行失败", e);
                Thread.currentThread().interrupt();
                throw new BusinessException(CodeEnum.Unknow, "系统线程被中断，请直接使用原全量参数重新提交");
            } catch (ExecutionException e) {
                log.error("makeVideo 第{}个任务执行失败", i, e);

                // ExecutionException 包装了实际的异常，必须通过 getCause() 获取真实报错信息
                Throwable cause = e.getCause();
                String reason = (cause != null && cause.getMessage() != null)
                        ? cause.getMessage()
                        : e.getMessage();

                // 拼接给 Agent 看的信息：明确指出【入参索引/第几个】失败了，以及【失败原因】
                // Agent 对 JSON 数组的索引很敏感，告诉它索引值有助于它去修改对应的参数
                errMsg.append(String.format("- 第 %d 个片段 (入参索引 [%d]) 生成失败，原因: %s\n", i + 1, i, reason));
            }
        }

        // 循环结束后，判断是否有部分任务失败
        if (!errMsg.isEmpty()) {
            // 💡 核心技巧：在具体的错误列表前，加上一段【引导指令】，再次向 Agent 强调原子性重试规则
            String finalErrorMessage = """
                    【工具执行内部报错】批量生成任务部分失败！触发[场景一：原子性重试]规则：
                    1. 请分析下方失败原因，仅调整失败片段的参数。
                    2. 必须将同批次所有参数（含之前成功的和本次调整的）合并，【全量重新提交】！
                    失败详情如下：
                    %s
                    """.formatted(errMsg);

            // 抛出异常返回给 Agent（或者封装进你的标准 Result/Response 对象中返回）
            throw new BusinessException(CodeEnum.Unknow, finalErrorMessage);
        }

        return tupleList;
    }
}
