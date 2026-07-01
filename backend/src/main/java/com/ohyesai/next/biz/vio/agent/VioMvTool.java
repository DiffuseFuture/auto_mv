package com.ohyesai.next.biz.vio.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileMagicNumber;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.billing.enums.InvitationPointsRule;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.vio.bo.*;
import com.ohyesai.next.biz.vio.bo.mvscript.Script;
import com.ohyesai.next.biz.vio.bo.tool.*;
import com.ohyesai.next.biz.vio.entity.VioProject;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.biz.vio.helper.SseMessageHelper;
import com.ohyesai.next.biz.vio.service.SessionTaskService;
import com.ohyesai.next.biz.vio.service.VioProjectService;
import com.ohyesai.next.biz.vio.service.VioService;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.component.MvAudioAnalyzerClient;
import com.ohyesai.next.component.vlm.VlmAudioComponent;
import com.ohyesai.next.component.vlm.VlmImageComponent;
import com.ohyesai.next.component.vlm.VlmLipSyncComponent;
import com.ohyesai.next.trace.ProxyExecutors;
import com.ohyesai.next.trace.TraceInterceptor;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.MiscUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.invocation.InvocationParameters;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class VioMvTool {

    private final VioAssistant.Gemini3_1ProAssistant gemini3_1ProAssistant;

    private final VioAssistant.DeepSeekV4FlashAssistant deepSeekV4FlashAssistant;

    private final VlmLipSyncComponent vlmLipSyncComponent;

    private final VlmImageComponent vlmImageComponent;

    private final VlmAudioComponent vlmAudioComponent;

    private final FileComponent fileComponent;

    private final MvAudioAnalyzerClient mvAudioAnalyzerClient;

    private final VioProjectService vioProjectService;

    private final BillingService billingService;

    private final StringRedisTemplate redisTemplate;

    private final SessionTaskService sessionTaskService;

//    /**
//     * 音频节拍分析工具 —— 调用 mv-audio-analyzer 微服务提取 BPM、强拍、段落结构。
//     * 必须在 mvScriptGenerate 之前调用，将返回的 beatAnalysis 数据传入脚本生成工具。
//     */
//    @Tool("""
//            **音频节拍分析工具（Music Beat Analyzer）**
//
//            **功能定义：**
//            对音频文件进行专业级节拍分析，提取 BPM（每分钟节拍数）、强拍位置（downbeats）和音乐段落结构（intro/verse/chorus/bridge/outro）。
//
//            **使用场景：**
//            在调用 mvScriptGenerate 生成 MV 脚本**之前**，必须先调用此工具对音频进行节拍分析。分析结果将作为 mvScriptGenerate 的 beatAnalysis 参数传入，确保镜头切换与音乐节奏精准对齐。
//
//            **返回值：**
//            JSON 格式的节拍分析数据，包含 bpm、downbeats（强拍时间点数组）、segments（段落结构数组）。
//            此返回值应直接作为 mvScriptGenerate 的 beatAnalysis 参数使用。
//
//            **调用协议：**
//            1. 在调用前向用户反馈："正在分析音频节奏结构，请稍候..."
//            2. 获取结果后用自然语言总结："音频分析完成，BPM 为 [数值]，识别到 [数量] 个段落（[段落标签列表]）"
//            3. 将返回的 JSON 数据传入 mvScriptGenerate 的 beatAnalysis 字段
//            """)
//    public String analyzeMusic(@P("音频文件 Id") String audioFileId, InvocationParameters parameters) {
//        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
//            log.info("Run 音频节拍分析工具: audioFileId={}", audioFileId);
//            Objects.requireNonNull(audioFileId, "audioFileId 不能为空");
//            // 调用 mv-audio-analyzer 微服务
//            return mvAudioAnalyzerClient.musicSegment(fileComponent.shareUrl(audioFileId));
//        } catch (Exception e) {
//            log.warn("音频节拍分析失败，将降级为无节拍数据模式: {}", e.getMessage());
//            return "{\"degraded\":true,\"error\":\"音频分析服务暂时不可用，请通过聆听音频自行分析节奏和段落结构\"}";
//        }
//    }

    /**
     * **调用协议（必须遵守）：**
     * 1.  **前置反馈（Pre-action）：** 在调用此工具**之前**，必须向用户输出正在进行的操作状态。例如："正在对音频进行多模态分析，提取BPM与情绪基调，请稍候..."
     * 2.  **执行工具：** 传入音频与时长参数。
     * 3.  **后置总结（Post-action）：** 获取工具返回的 JSON 后，**禁止**直接通过代码块抛出原始数据。必须先用自然语言总结关键信息（如："分析完成，识别到歌曲为[情绪]基调，BPM为[数值]，共生成了[数量]个分镜脚本..."），随后再根据用户需求展示详细内容。
     *
     * @param mvScriptPlanArgs
     * @param parameters
     * @return
     * @throws IOException
     */
    @Tool("""
            **工具描述：** 专业级多模态 MV 脚本规划引擎
            **功能定义：** 基于音频文件及其精确时长，进行深度多模态分析，规划 MV 脚本。
            **持久化声明：** 生成的数据已由后端根据 taskId 自动存入数据库
            **调用协议（必须遵守）：** 工具如果出现异常将是致命的,后续步骤均无法继续完成
            """)
    public String mvScriptPlan(MvScriptPlanArgs mvScriptPlanArgs, InvocationParameters parameters) {
        ProxyExecutors executors = ProxyExecutors.newVirtualThreadPerTaskExecutor();
        var _mdcCloseable = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID));
        try (executors; _mdcCloseable) {
            MiscUtil.toolArgsRequireNonNull(mvScriptPlanArgs, "mvScriptPlanArgs", MvScriptPlanArgs.class);

            log.info("Run MV 脚本生成工具: {}", mvScriptPlanArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            Resolution resolution = parameters.get(VioService.RESOLUTION);

            sessionTaskService.checkTaskIdFormatIsLegal(mvScriptPlanArgs.getTaskId(), sseMessageHelper.getChatSessionId()); // 校验 taskId 格式

            File tempDir = parameters.get(VioService.TEMP_DIR);

            // 下载音频文件
            File audioFile = new File(tempDir, mvScriptPlanArgs.getAudioFileId());
            fileComponent.download(mvScriptPlanArgs.getAudioFileId(), audioFile);
            // 获取音频总时长
            CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(audioFile);
            // 校验音频时长
            if (mediaInfo.duration().toSeconds() > 301) {
                throw new BusinessException(CodeEnum.ParameterError, "音频时长不能超过5分钟");
            }

            sseMessageHelper.ofText("\n> 1. 分析音乐数据\n"); // 双换行 让后面元素脱离引用块

            String audioFileUrl = fileComponent.shareUrl(mvScriptPlanArgs.getAudioFileId());

            // 分析音乐节奏
            Future<String> futureMusicSegment = executors.submit(() -> mvAudioAnalyzerClient.musicSegment(audioFileUrl));
            // 规范化风格提示词
            Future<String> futureNormalizeStyle = executors.submit(() -> deepSeekV4FlashAssistant.normalizeStyle(mvScriptPlanArgs.getStyle()));
            // 识别音频歌词
            Future<SubtitleTranscribed> futureSubtitleTranscribed = executors.submit(() -> {
                SubtitleTranscribed subtitleTranscribed = vlmAudioComponent.volcAsr(audioFileUrl);
                // 方法会返回一个新的列表，所以需要重新赋值
                List<SubtitleTranscribed.SubtitleLine> subtitleLines = vioProjectService.subtitleTranscribedFix(mvScriptPlanArgs.getAudioLyrics(), subtitleTranscribed.getLines());
                subtitleTranscribed.setLines(subtitleLines);

                return subtitleTranscribed;
            });

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

            String normalizeStyle = futureNormalizeStyle.get();
            SubtitleTranscribed subtitleTranscribed = futureSubtitleTranscribed.get();
            String musicSegment = futureMusicSegment.get();

            sseMessageHelper.ofText("> 2. 执行编剧创作\n\n"); // 双换行 让后面元素脱离引用块
            Script mvScript = MiscUtil.reTry(() -> gemini3_1ProAssistant.mvScriptGenerate(
                    mediaInfo.duration().toSeconds(),
                    normalizeStyle,
                    StrUtil.blankToDefault(JsonUtil.toJson(subtitleTranscribed), "无"),
                    StrUtil.blankToDefault(mvScriptPlanArgs.getRefImageContext(), "无"),
                    StrUtil.blankToDefault(mvScriptPlanArgs.getAdditionalRequirements(), "无"),
                    StrUtil.blankToDefault(musicSegment, "无（请通过聆听音频自行分析节奏和段落结构）"),
                    mvScriptPlanArgs.getAspectRatio(),
                    audioContent
            ), 3, _ -> new BusinessException(CodeEnum.Unknow, "发生致命错误，必须停止本次任务并告知用户稍后再试"));

            // 存储任务数据
            sessionTaskService.updateTaskPayload(mvScriptPlanArgs.getTaskId(), sseMessageHelper.getChatSessionId(), ChatSessionTaskPayload.from(mvScript, mvScriptPlanArgs.getAudioFileId(), resolution, subtitleTranscribed.getLines()));

            return MiscUtil.toolResp(JsonUtil.toJson(mvScript.getSubjects()), "禁止总结工具结果，因为数据量过大且用户不可读; 整体风格为“" + normalizeStyle + "”,后续如果有新增主体、分镜需要保持风格一致");
        } catch (ExecutionException e) {
            throw new BusinessException(e.getCause());
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 仅用来回显脚本等待用户确认
     *
     * @param taskId
     * @param parameters
     */
    @Tool("""
            同步 scene 到 UI 展示
            **注意**: 仅用来推送scene数据到ui，无法展示subject数据
            """)
    public String syncSceneScript2Ui(@P("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask") String taskId, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            log.info("Run syncSceneScript2Ui args: {}", taskId);
            Objects.requireNonNull(taskId, CommonConst.ARGS_NULL_ERROR);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(taskId, sseMessageHelper.getChatSessionId());
            // 获取任务数据
            ChatSessionTaskPayload payload = sessionTaskService.getTaskPayload(taskId, sseMessageHelper.getChatSessionId());
            Map<String, ChatSessionTaskPayload.PayloadSubject> subjectMap = payload.subject2Map();
            // 校验是否存在没有生成的主体
            String missingSubjects = subjectMap.entrySet().stream()
                    .filter(entry -> StrUtil.isBlank(entry.getValue().getResultFileId()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(","));
            if (StrUtil.isNotBlank(missingSubjects)) {
                return MiscUtil.toolResp("主体结果缺失，无法推送分镜脚本", "以下主体没有生成结果，请确认当前环节是否应该推送分镜(Scene)脚本，如果必须推送请先完成主体生成：" + missingSubjects);
            }


            List<SceneScript2UiBO.SceneScript> sceneScripts = payload.getScenes().stream().map(scene -> {
                SceneScript2UiBO.SceneScript sceneScript = new SceneScript2UiBO.SceneScript();
                sceneScript.setId(scene.getId());
                sceneScript.setStartTime(scene.getStartTime());
                sceneScript.setEndTime(scene.getEndTime());
                sceneScript.setDuration(scene.getDuration());
                sceneScript.setVisualPrompt(scene.getVisualPrompt());
                sceneScript.setSubjectRefs(SceneScript2UiBO.SceneScript.SubjectRef.from(scene.getSubjectRefs(), subjectMap));
                return sceneScript;
            }).toList();

            sseMessageHelper.ofSceneScript(sceneScripts, fileComponent, sessionTaskService.desugarTaskId(taskId));

            return MiscUtil.toolResp(JsonUtil.toJson(payload.getScenes()), "必须停止生成并询问用户是否满意。严禁在获得用户明确‘确认’回复前私自进行下一步。禁止总结工具结果，因为数据量过大且用户不可读。");
        }
    }


    @Tool("""
            本工具能够根据用户的文字描述生成高质量图片。并允许上传参考图片以控制生成内容的构图或风格
            **并发与批量能力**：本工具原生支持数组级批量入参，工具内置高性能异步排队与动态限流机制，无需考虑并发阈值限制，可根据任务需求发起任意规模的生成请求，系统将自动实现任务的无限量并发调度与并行产出。
            **【参数提交与重试规范（极其重要）】**：
            你需要严格区分以下两种场景，来决定提交入参的范围：
            *   **场景一：工具执行报错后的自动重试（需保证原子性）**
                  当且仅当你调用本工具并**收到报错信息**（如部分生成失败）时，你需要分析失败原因并调整报错片段的参数。此时，**必须将调整后的参数与同批次其他成功的参数合并，【全量完整提交】整个批次的参数**以保证原子性。
            *   **场景二：响应用户的主动修改指令（需按需生成）**
                  当**用户在对话中明确要求**“重新生成第X个主体”、“修改某个主体”或“新增主体”时，你**只需要将该主体通过 updateSubject 更新后，将对应 subjectId + chunkId 作为单独的入参提交即可**。此时**绝对不要**提交其他无需修改的主体，以避免资源浪费。
            """)
    public String makeImg(MakeImgArgs makeImgArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            MiscUtil.toolArgsRequireNonNull(makeImgArgs, "makeImgArgs", MakeImgArgs.class);

            log.info("Run makeImg args: {}", makeImgArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);

            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(makeImgArgs.getTaskId(), sseMessageHelper.getChatSessionId());

            String userId = parameters.get(VioService.USER_ID);

            // 获取任务数据
            ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(makeImgArgs.getTaskId(), sseMessageHelper.getChatSessionId());
            Map<String, Integer> subjectChunkIdMap; // 主体列表与chunkId映射
            List<ChatSessionTaskPayload.PayloadSubject> subjects;
            if (CollUtil.isEmpty(makeImgArgs.getSubjects())) { // 如果没有传入任何主体则默认生成所有主体
                subjectChunkIdMap = Map.of();
                subjects = taskPayload.getSubjects();
            } else {
                subjectChunkIdMap = MiscUtil.listToMap(makeImgArgs.getSubjects(), MakeImgArgs.Subject::getSubjectId, MakeImgArgs.Subject::getChunkId);
                subjects = taskPayload.getSubjects().stream().filter(subject -> subjectChunkIdMap.containsKey(subject.getId())).toList();
            }

            if (CollUtil.isEmpty(subjects)) {
                return MiscUtil.toolResp("传入的 subjectId 错误，无法找到有效数据", "请检查输入参数");
            }

            // 校验用户是否有积分
            int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.MAKE_IMAGE); // 积分单价
            int imgCount = subjects.size(); // 图片数量
            int totalPoints = pointsRequired * imgCount;
            int gapBalance = billingService.compareBalance(userId, totalPoints);
            if (gapBalance < 0) {
                int userPoints = gapBalance + totalPoints;
                throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(totalPoints, userPoints, Math.abs(gapBalance)));
            }

            // 判断是否需要与用户确认积分; 重新生成+没有确认
            if (subjectChunkIdMap.values().stream().anyMatch(chunkId -> chunkId != null && chunkId != -1) && !Boolean.TRUE.equals(makeImgArgs.getConfirmed())) {
                return MiscUtil.toolResp(CommonConst.TOOL_RESULT_NEED_CONFIRM, CommonConst.CONFIRM_POINTS_PROMPT_TEMPLATE.formatted(totalPoints));
            }

            List<Future<Tuple>> result = new ArrayList<>();
            try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
                for (ChatSessionTaskPayload.PayloadSubject subject : subjects) {
                    Future<Tuple> future = executorService.submit(() -> {
                        List<String> refImageUrls = CollUtil.isEmpty(subject.getRefImgs()) ? List.of() : subject.getRefImgs().stream().map(fileComponent::shareUrl).toList();
                        List<byte[]> imgBytes = vlmImageComponent.seedreamGenerateImage(subject.getDescription(), refImageUrls, 1);

                        // 获取第一个图片
                        byte[] imageBytes = imgBytes.getFirst();
                        // 上传图片
                        String extension = FileMagicNumber.getMagicNumber(imageBytes).getExtension();
                        String fileId = fileComponent.genObjectName(FileDirConst.MV_DIR, extension);
                        fileComponent.upload(imageBytes, fileId);

                        // 回更payload结果
                        subject.setResultFileId(fileId);

                        // 推送 SUBJECT 类型消息（带版本管理）
                        SseMsgBO.Subject.VersionedImg versionImg = new SseMsgBO.Subject.VersionedImg();
                        versionImg.setVersion(0);
                        versionImg.setImgFileId(fileId);
                        versionImg.setImgUrl(fileComponent.shareUrl(fileId));
                        versionImg.setPrompt(subject.getDescription());
                        if (subject.getRefImgs() != null) {
                            versionImg.setRefImages(subject.getRefImgs().stream().map(fid -> {
                                SseMsgBO.Subject.RefImg ref = new SseMsgBO.Subject.RefImg();
                                ref.setFileId(fid);
                                ref.setUrl(fileComponent.shareUrl(fid));
                                return ref;
                            }).toList());
                        }

                        SseMsgBO.Subject sseMsgSubject = new SseMsgBO.Subject();
                        sseMsgSubject.setAssetKey(subject.getId());
//                        sseMsgSubject.setDescription(subject.getDescription());
                        sseMsgSubject.setActiveVersion(0);
                        sseMsgSubject.setVersions(List.of(versionImg));
                        sseMsgSubject.setType(SseMsgBO.Subject.SubjectType.of(subject.getType()));

                        return new Tuple(sseMsgSubject, subjectChunkIdMap.get(subject.getId()), fileId, subject.getId());
                    });

                    result.add(future);

                }
            }

            List<Tuple> tuples = errorHandler(result);

            // 回更payload结果到数据库
            sessionTaskService.updateTaskPayload(makeImgArgs.getTaskId(), sseMessageHelper.getChatSessionId(), taskPayload);

            List<MakeImgResp> resps = tuples.stream().map(tuple -> {
                Integer chunkId = sseMessageHelper.ofSubject(tuple.get(0), tuple.get(1), sessionTaskService.desugarTaskId(makeImgArgs.getTaskId()));
                return new MakeImgResp(tuple.get(3), tuple.get(2), chunkId);
            }).toList();

            // 积分扣除
            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, ModelEnum.NONE, TaskType.MAKE_IMAGE, totalPoints).pointsTransactionLog();
            billingService.mapSessionWithPointsTransactionLog(sseMessageHelper.getChatSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

            return MiscUtil.toolResp(JsonUtil.toJson(resps), "图片已渲染到UI。必须停止生成并询问用户是否满意。严禁在获得用户明确‘确认’回复前私自进行下一步");
        }
    }

    /**
     * 图片理解工具 —— 分析用户上传的参考图内容
     * <p>
     * 调用协议：
     * 1. 在处理用户上传的参考图（subjectImgs）时，对每张图调用此工具以理解其视觉内容。
     * 2. 根据返回的 description 和 matchScore，决定是否需要向用户二次确认用途。
     * 3. 将返回的 description 用于后续 mvScriptGenerate 的 refImageContext 参数拼接。
     */
    @Tool("""
            **图片内容理解工具（Reference Image Analyzer）**
            
            **功能定义：**
            分析用户上传的参考图片内容，输出图片的详细视觉描述，并评估图片内容与用户标注的意图标签的匹配程度。
            
            **使用场景：**
            当用户上传了参考图（subjectImgs）时，在生成MV脚本之前，对每张参考图调用此工具进行内容理解。
            
            **返回值说明：**
            - `description`：图片的详细中文描述，可直接用于后续 prompt 构建
            - `matchScore`：图片与意图标签的匹配度（HIGH / MEDIUM / LOW）
            - `suggestion`：当匹配度为 LOW 时，给出更合适的意图标签建议
            
            **调用协议（必须遵守）：**
            1. 如果 matchScore 为 LOW，必须在对话中向用户确认："您上传的这张图片看起来是[description摘要]，但标注为[intention]，请问是否继续使用？或者您希望将它改为[suggestion]？"
            2. 如果 matchScore 为 HIGH 或 MEDIUM，无需额外确认，直接使用返回的 description 用于后续流程。
            3. 禁止将原始返回结果输出给用户，仅用自然语言总结。
            """)
    public List<UnderstandImageResp> understandImage(List<UnderstandImageArgs> understandImageList, InvocationParameters parameters) throws IOException {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            Objects.requireNonNull(understandImageList, CommonConst.ARGS_NULL_ERROR);

            log.info("Run understandImage args: {}", understandImageList);
//        SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            File tempDir = parameters.get(VioService.TEMP_DIR);
//        String userId = parameters.get(VioService.USER_ID);

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

    }

    @Tool("""
            支持角色一致性的视频生成工具。
            **功能**：根据文本提示词、时长和可选的角色参考图生成视频片段。本工具支持批量入参以实现多片段并行生成。
            **并发与批量能力**：本工具原生支持数组级批量入参，工具内置高性能异步排队与动态限流机制，无需考虑并发阈值限制，可根据任务需求发起任意规模的生成请求，系统将自动实现任务的无限量并发调度与并行产出。
            **【参数提交与重试规范（极其重要）】**：
            你需要严格区分以下两种场景，来决定提交入参的范围：
            *   **场景一：工具执行报错后的自动重试（需保证原子性）**
                  当且仅当你调用本工具并**收到报错信息**（如部分生成失败）时，你需要分析失败原因并调整报错片段的参数。此时，**必须将调整后的参数与同批次其他成功的参数合并，【全量完整提交】整个批次的参数**以保证原子性。
            *   **场景二：响应用户的主动修改指令（需按需生成）**
                  当**用户在对话中明确要求**“重新生成第X个分镜”、“修改某个分镜”或“新增分镜”时，你**只需要将该分镜通过 updateScene 更新后，将对应 sceneId + chunkId 作为单独的入参提交即可**。此时**绝对不要**提交其他无需修改的分镜，以避免资源浪费。
            """)
    public String makeVideo(MakeVideoArgs makeVideoArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            MiscUtil.toolArgsRequireNonNull(makeVideoArgs, "makeVideoArgs", MakeVideoArgs.class);

            ModelEnum model = parameters.get(VioService.MODEL);

            log.info("Run makeVideo model:{}  args:{}", model, makeVideoArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);

            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(makeVideoArgs.getTaskId(), sseMessageHelper.getChatSessionId());

            File tempDir = parameters.get(VioService.TEMP_DIR);
            String userId = parameters.get(VioService.USER_ID);

            // 获取任务数据
            ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(makeVideoArgs.getTaskId(), sseMessageHelper.getChatSessionId());
            taskPayload.initSceneSubject(); // 初始化分镜主体数据

            Map<String, Integer> sceneChunkIdMap; // 分镜列表与chunkId映射
            List<ChatSessionTaskPayload.PayloadScene> scenes;
            if (CollUtil.isEmpty(makeVideoArgs.getScenes())) {
                sceneChunkIdMap = Map.of();
                scenes = taskPayload.getScenes();
            } else {
                sceneChunkIdMap = MiscUtil.listToMap(makeVideoArgs.getScenes(), MakeVideoArgs.Scene::getSceneId, MakeVideoArgs.Scene::getChunkId);
                scenes = taskPayload.getScenes().stream().filter(scene -> sceneChunkIdMap.containsKey(scene.getId())).toList();
            }

            if (CollUtil.isEmpty(scenes)) {
                return MiscUtil.toolResp("传入的 sceneId 错误，无法找到有效数据", "请检查输入参数");
            }

            // 校验用户是否有积分
            int pointsRequired = billingService.getPointsRequired(model, taskPayload.getResolution(), TaskType.MAKE_MV); // 积分单价
            int sumDuration = scenes.stream().mapToInt(v -> (int) Math.ceil(v.getDuration())).sum(); // 总时长
            int totalPoints = pointsRequired * sumDuration; // 本次消耗总积分
            int gapBalance = billingService.compareBalance(userId, totalPoints);
            if (gapBalance < 0) {
                int userPoints = gapBalance + totalPoints;
                throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(totalPoints, userPoints, Math.abs(gapBalance)));
            }

            // 判断是否需要与用户确认积分; 重新生成+没有确认
            if (sceneChunkIdMap.values().stream().anyMatch(chunkId -> chunkId != null && chunkId != -1) && !Boolean.TRUE.equals(makeVideoArgs.getConfirmed())) {
                return MiscUtil.toolResp(CommonConst.TOOL_RESULT_NEED_CONFIRM, CommonConst.CONFIRM_POINTS_PROMPT_TEMPLATE.formatted(totalPoints));
            }

            // 开始批量生成视频
            List<Future<Tuple>> result = new ArrayList<>();
            try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
                Semaphore semaphore = new Semaphore(10);
                for (ChatSessionTaskPayload.PayloadScene payloadScene : scenes) {
                    semaphore.acquire();// 获取信号量
                    Future<Tuple> future = executorService.submit(() -> {
                        try {
                            Integer chunkId = sceneChunkIdMap.get(payloadScene.getId());
                            // 缓存策略 仅在本轮生效
                            String cacheKey = RedisConst.AGENT_TOOL_RESULT_CACHE.formatted(sseMessageHelper.getHistoryMessageId(), payloadScene.getId());
                            String cacheRes = redisTemplate.opsForValue().get(cacheKey);
                            VioProject vioProject;
                            if (StrUtil.isNotBlank(cacheRes)) {
                                log.info("makeVideo 生成命中缓存 {}", payloadScene);
                                vioProject = JsonUtil.toObject(cacheRes, VioProject.class);
                            } else {
                                vioProject = vioProjectService.doMakeVideo(
                                        tempDir,
                                        userId,
                                        sseMessageHelper.getChatSessionId(),
                                        sseMessageHelper.getHistoryMessageId(),
                                        model,
                                        DoMakeVideoBO.from(payloadScene, taskPayload.getAspectRatio(), taskPayload.getResolution(), chunkId)
                                );
                                // 缓存
                                redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(vioProject), Duration.ofHours(5));
                            }

                            // 回更payload结果
                            payloadScene.setResultFileId(vioProject.getFileId());


                            // 推送版本化 SCENE 消息
                            SseMsgBO.Scene.VersionedVideo versionVideo = new SseMsgBO.Scene.VersionedVideo();
                            versionVideo.setVersion(0);
                            versionVideo.setVideoFileId(vioProject.getFileId());
                            versionVideo.setVideoUrl(fileComponent.shareUrl(vioProject.getFileId()));
                            versionVideo.setCoverFileId(vioProject.getPreviewFileId());
                            versionVideo.setCoverUrl(fileComponent.shareUrl(vioProject.getPreviewFileId()));
                            versionVideo.setVisualPrompt(payloadScene.getVisualPrompt());
                            versionVideo.setModel(model);
                            versionVideo.setAspectRatio(taskPayload.getAspectRatio());
                            versionVideo.setResolution(taskPayload.getResolution());


                            if (payloadScene.getSceneSubjects() != null) {
                                versionVideo.setSubjects(payloadScene.getSceneSubjects().stream().map(sceneSubject -> {
                                    SseMsgBO.Scene.SubjectRef ref = new SseMsgBO.Scene.SubjectRef();
                                    ref.setId(sceneSubject.getId());
                                    ref.setImageFileId(sceneSubject.getFileId());
                                    ref.setImageUrl(fileComponent.shareUrl(sceneSubject.getFileId()));
                                    return ref;
                                }).toList());
                            }

                            SseMsgBO.Scene scene = new SseMsgBO.Scene();
                            scene.setAssetKey(payloadScene.getId());
//                            scene.setDescription(payloadScene.getVisualPrompt());
                            scene.setVersions(List.of(versionVideo));
                            scene.setActiveVersion(0);
                            // 设置时间信息
                            scene.setStartTime(payloadScene.getStartTime());
                            scene.setEndTime(payloadScene.getEndTime());
                            scene.setDuration(payloadScene.getDuration());

                            return new Tuple(scene, chunkId, vioProject.getFileId(), payloadScene.getId());
                        } finally {
                            semaphore.release(); // 释放信号量
                        }

                    });

                    result.add(future);
                }
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }

            List<Tuple> tuples = errorHandler(result);

            // 回更payload结果到数据库
            sessionTaskService.updateTaskPayload(makeVideoArgs.getTaskId(), sseMessageHelper.getChatSessionId(), taskPayload);

            List<MakeVideoResp> resps = tuples.stream().map(tuple -> {
                Integer chunkId = sseMessageHelper.ofScene(tuple.get(0), tuple.get(1), sessionTaskService.desugarTaskId(makeVideoArgs.getTaskId()));
                return new MakeVideoResp(tuple.get(3), tuple.get(2), chunkId);
            }).toList();

            // 积分扣除
            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, model, taskPayload.getResolution(), TaskType.MAKE_VIDEO, totalPoints).pointsTransactionLog();
            billingService.mapSessionWithPointsTransactionLog(sseMessageHelper.getChatSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

            return MiscUtil.toolResp(JsonUtil.toJson(resps), "视频已渲染到UI。必须停止生成并询问用户是否满意。严禁在获得用户明确‘确认’回复前私自进行下一步");
        }
    }

    private List<Tuple> errorHandler(List<Future<Tuple>> result) {
        // 先轮训一遍确保所有分镜都正常完成
        StringBuilder errMsg = new StringBuilder();
        List<Tuple> tupleList = new ArrayList<>();
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
                    【工具执行内部报错】批量生成任务部分失败！触发[原子性重试]规则：
                    1. 请分析下方失败原因，仅调整失败片段的参数。
                    2. 本次操作无效，数据回滚，必须将同批次所有参数（含之前成功的和本次调整的） 全量重新提交！
                    失败详情如下：
                    %s
                    """.formatted(errMsg);

            // 抛出异常返回给 Agent（或者封装进你的标准 Result/Response 对象中返回）
            throw new BusinessException(CodeEnum.Unknow, finalErrorMessage);
        }

        return tupleList;
    }

    @Tool("""
            音视频合并工具，将音频文件和视频文件合并为一个新的音视频文件。支持可选的口型同步、字幕烧录功能。
            """)
    public String mergeAudioVideo(MergeAudioVideoArgs mergeAudioVideoArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            MiscUtil.toolArgsRequireNonNull(mergeAudioVideoArgs, "mergeAudioVideoArgs", MergeAudioVideoArgs.class);

            String taskId = mergeAudioVideoArgs.getTaskId();

            log.info("Run mergeAudioVideo args: {}", mergeAudioVideoArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            File tempDir = parameters.get(VioService.TEMP_DIR);
            String userId = parameters.get(VioService.USER_ID);

            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(taskId, sseMessageHelper.getChatSessionId());

            // 获取任务数据
            ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(taskId, sseMessageHelper.getChatSessionId());
            List<ChatSessionTaskPayload.PayloadScene> payloadScenes = taskPayload.getScenes().stream().filter(scene -> StrUtil.isNotBlank(scene.getResultFileId())).toList(); // 需要处理的分镜

            // 获取需要合并的分镜
            List<String> videoFileIds;
            int totalPoints;
            if (Boolean.TRUE.equals(mergeAudioVideoArgs.getLipSync())) {
                // 校验用户是否有积分(预计消耗积分)
                int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.LIP_SYNC); // 积分单价
                int sumDuration = payloadScenes.stream().filter(ChatSessionTaskPayload.PayloadScene::isLipSync).mapToInt(v -> (int) Math.ceil(v.getDuration())).sum(); // 需要对口型的总时长
                int preTotalPoints = pointsRequired * sumDuration; // 预计消耗总积分
                int gapBalance = billingService.compareBalance(userId, preTotalPoints);
                if (gapBalance < 0) {
                    int userPoints = gapBalance + preTotalPoints;
                    throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(preTotalPoints, userPoints, Math.abs(gapBalance)));
                }

                // 判断是否需要与用户确认积分; 不走缓存+重新生成(已存在口型结果)+没有确认
                if (!Boolean.TRUE.equals(mergeAudioVideoArgs.getLipSyncCache()) && taskPayload.getScenes().stream().anyMatch(scene -> StrUtil.isNotBlank(scene.getLipSyncFileId())) && !Boolean.TRUE.equals(mergeAudioVideoArgs.getConfirmed())) {
                    return MiscUtil.toolResp(CommonConst.TOOL_RESULT_NEED_CONFIRM, CommonConst.CONFIRM_POINTS_PROMPT_TEMPLATE.formatted(preTotalPoints));
                }

                LipSyncSceneBO lipSyncSceneBO = lipSyncScene(payloadScenes, taskPayload.getAudioFileId(), tempDir, pointsRequired, Boolean.TRUE.equals(mergeAudioVideoArgs.getLipSyncCache()));
                videoFileIds = lipSyncSceneBO.videoFileIds();
                totalPoints = lipSyncSceneBO.totalPoints(); // 实际消耗的总积分
            } else {
                totalPoints = 0; // 不对口型则不需要积分
                videoFileIds = payloadScenes.stream().map(ChatSessionTaskPayload.PayloadScene::getResultFileId).toList();
            }

            if (CollUtil.isEmpty(videoFileIds)) {
                return MiscUtil.toolResp("合并失败: 无法获取分镜结果视频", "请确认分镜是否完成");
            }

            DoMergeAudioVideoBO doMergeAudioVideoBO = new DoMergeAudioVideoBO();
            doMergeAudioVideoBO.setChatSessionTaskId(sessionTaskService.desugarTaskId(taskId));
            doMergeAudioVideoBO.setVideoFileName(mergeAudioVideoArgs.getVideoFileName());
            doMergeAudioVideoBO.setVideoFileIds(videoFileIds);
            doMergeAudioVideoBO.setAudioFileId(taskPayload.getAudioFileId());
            doMergeAudioVideoBO.setSubtitleFlag(mergeAudioVideoArgs.getSubtitleFlag());
            doMergeAudioVideoBO.setSubtitleReference(mergeAudioVideoArgs.getSubtitleReference());

            sseMessageHelper.ofText("\n> 正在收集数据做最终合并...\n\n");
            // 开始合并
            DoMergeAudioVideoResp doMergeAudioVideoResp = vioProjectService.doMergeAudioVideo(
                    tempDir,
                    userId,
                    sseMessageHelper.getChatSessionId(),
                    sseMessageHelper.getHistoryMessageId(),
                    doMergeAudioVideoBO.getVideoFileName(),
                    doMergeAudioVideoBO,
                    taskPayload
            );

            // 发放积分事件
            billingService.inviteRegisterEvent(userId, InvitationPointsRule.MAKE_MV);

            // 回更payload结果到数据库
            sessionTaskService.updateTaskPayload(taskId, sseMessageHelper.getChatSessionId(), taskPayload);

            String outputFileId = doMergeAudioVideoResp.vioProject().getFileId();
            String coverFileId = doMergeAudioVideoResp.vioProject().getPreviewFileId();
            String projectId = doMergeAudioVideoResp.vioProject().getId();
            // 推送到页面
            String coverUrl = fileComponent.shareUrl(coverFileId);
            sseMessageHelper.ofVideo(outputFileId, fileComponent.shareUrl(outputFileId), coverFileId, coverUrl, projectId);

            // 积分扣除
            if (totalPoints > 0) {
                PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, ModelEnum.NONE, TaskType.LIP_SYNC, totalPoints).pointsTransactionLog();
                billingService.mapSessionWithPointsTransactionLog(sseMessageHelper.getChatSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联
            }

            return MiscUtil.toolResp(outputFileId, "执行结果已推送给前端展示");
        }
    }

    /**
     * 给分镜对口型，并且校验积分 但不会扣除积分
     * 该方法会将对成功的视频 写会 PayloadScene#lipSyncFileId 字段
     *
     * @param payloadScenes
     * @param audioFileId
     * @param tempDir
     * @param cacheFlag     是否使用缓存 开启后如果没有对应缓存会报错
     * @return
     */
    public LipSyncSceneBO lipSyncScene(List<ChatSessionTaskPayload.PayloadScene> payloadScenes, String audioFileId, File tempDir, int pointsRequired, boolean cacheFlag) {
        // 获取需要对口型 但没有需要对的视频的分镜 直接返回原始视频
        if (payloadScenes.isEmpty()) {
            return new LipSyncSceneBO(List.of(), 0);
        }

        // 复用缓存 直接使用缓存文件返回，不会消耗积分
        if (cacheFlag) {
            List<String> lipSyncFileIds = payloadScenes.stream()
                    .map(scene -> {
                        if (scene.isLipSync() && StrUtil.isBlank(scene.getLipSyncFileId())) { // 需要对口型，但口型又没有缓存结果
                            throw new BusinessException(CodeEnum.ParameterError, "分镜 " + scene.getId() + " 没有可用的缓存文件，确认是该分镜是否已经对过口型");
                        }
                        return scene.isLipSync() ? scene.getLipSyncFileId() : scene.getResultFileId();
                    })
                    .toList();
            return new LipSyncSceneBO(lipSyncFileIds, 0);
        }


        try (ExecutorService executorService = ProxyExecutors.newVirtualThreadPerTaskExecutor()) {
            // 下载音频文件
            String extName = FileUtil.extName(audioFileId);
            File audioFile = new File(tempDir, IdUtil.fastSimpleUUID() + "." + extName);
            fileComponent.download(audioFileId, audioFile);

            // 信号量
            Semaphore semaphore = new Semaphore(5);

            // 循环分镜，根据每个分镜的起始和结束时间裁剪音频 进行对口型
            List<Future<Tuple>> futures = payloadScenes.stream().map(lipSyncScene -> executorService.submit(() -> {
                try {
                    semaphore.acquire();
                    return processLipSync4Scene(
                            tempDir,
                            pointsRequired,
                            lipSyncScene,
                            audioFile
                    );
                } finally {
                    semaphore.release();
                }
            })).toList();

            // 循环等待异步结果 整理返回
            List<String> lipSyncFileIds = new ArrayList<>(payloadScenes.size());
            int totalPoints = 0; // 总积分
            for (Future<Tuple> tupleFuture : futures) {
                String lipSyncVideoFileId = tupleFuture.get().get(0);
                int points = tupleFuture.get().get(1);

                lipSyncFileIds.add(lipSyncVideoFileId);
                totalPoints += points;
            }

            return new LipSyncSceneBO(lipSyncFileIds, totalPoints);


        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     *
     * @param tempDir
     * @param pointsRequired
     * @param lipSyncScene
     * @param audioFile
     * @return 0: lipSyncVideoFileId； 1: points
     */
    private Tuple processLipSync4Scene(File tempDir, int pointsRequired, ChatSessionTaskPayload.PayloadScene lipSyncScene, File audioFile) {
        if (!lipSyncScene.isLipSync()) { // 跳过不需要对口型的分镜 直接返回
            return new Tuple(lipSyncScene.getResultFileId(), 0);
        }
        // 裁剪指定片段音频 并上传换取url
        File trimAudioFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".wav");
        CvUtil.trimAudio(audioFile, trimAudioFile, lipSyncScene.getStartTime(), lipSyncScene.getDuration());
        String trimAudioFileId = fileComponent.genObjectName(FileDirConst.TEMP_24H, "wav");
        fileComponent.upload(trimAudioFile, trimAudioFileId);
        String trimAudioUrl = fileComponent.shareUrl(trimAudioFileId);

        // 进行对口型
        String videoUrl = fileComponent.shareUrl(lipSyncScene.getResultFileId());
        String lipSyncVideoUrl = vlmLipSyncComponent.lipSync4PixverseTryCatch(videoUrl, trimAudioUrl); // 音视频进行对口型
        if (lipSyncVideoUrl == null) {
            log.warn("口型失败，回退到常规视频 sceneId:{}", lipSyncScene.getId());
            return new Tuple(lipSyncScene.getResultFileId(), 0);
        }

        // 下载口型视频
        File lipSyncVideoFile = new File(tempDir, IdUtil.fastSimpleUUID() + "_lipSync.mp4");
        HttpUtil.downloadFile(lipSyncVideoUrl, lipSyncVideoFile);

        CvUtil.MediaInfo LipSyncVideoMediaInfo = CvUtil.mediaInfo(lipSyncVideoFile, false);
        double lipSyncVideoDuration = MiscUtil.millisToSeconds(LipSyncVideoMediaInfo.duration().toMillis()); // 统一单位
        if (lipSyncVideoDuration > lipSyncScene.getDuration()) { // 口型视频可能比原是时间长 需要裁剪
            File lipSyncVideoFileCut = new File(tempDir, IdUtil.fastSimpleUUID() + "_lipSync_trim.mp4");
            CvUtil.trimVideo(lipSyncVideoFile, lipSyncVideoFileCut, 0, lipSyncScene.getDuration());
            lipSyncVideoFile = lipSyncVideoFileCut;
        }

        // 将对口型结果上传到文件系统
        String lipSyncVideoFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp4");
        fileComponent.upload(lipSyncVideoFile, lipSyncVideoFileId);
        lipSyncScene.setLipSyncFileId(lipSyncVideoFileId);

        log.info("对口型成功 sceneId: {}; audioFileId:{} videoFileId:{} lipSyncFileId:{}", lipSyncScene.getId(), trimAudioFileId, lipSyncScene.getResultFileId(), lipSyncVideoFileId);
        return new Tuple(lipSyncVideoFileId, pointsRequired * (int) Math.ceil(lipSyncScene.getDuration()));
    }

    @Tool("""
            创建mv任务，每当开始一个新的mv任务时都需要通过此工具初始化任务
            创建成功返回 taskId, 例如 Task_1
            """)
    public String initTask(InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            log.info("Run initTask");
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            // 创建并返回 agent 友好格式 id
            String taskId = sessionTaskService.initTaskBySession(sseMessageHelper.getChatSessionId()).agentFriendlyTaskId();
            return MiscUtil.toolResp(taskId, "mv任务初始化成功，后续属于本次任务的工具调用统一使用相同taskId。taskId 为内部值，禁止输出");
        }
    }

    @Tool("""
            更新 Subject(主体) 数据；当要修改 Subject 数据时有且仅能通过此工具进行更新
            """)
    public String updateSubject(UpdateSubjectArgs updateSubjectArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            MiscUtil.toolArgsRequireNonNull(updateSubjectArgs, "updateSubjectArgs", UpdateSubjectArgs.class);

            log.info("Run updateSubject args: {}", updateSubjectArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);


            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(updateSubjectArgs.getTaskId(), sseMessageHelper.getChatSessionId());

            // 获取原始task数据
            ChatSessionTaskPayload payload = sessionTaskService.getTaskPayload(updateSubjectArgs.getTaskId(), sseMessageHelper.getChatSessionId());

            String subjectIdxErrMsg = MiscUtil.toolResp("传入的 subjectId 错误，无法找到有效数据", "请检查输入参数");
            return switch (updateSubjectArgs.getUpdateType()) {
                case UPDATE -> {
                    // 根据 id 找到原始数据所在位置
                    int subjectIndex = findSubjectIndex(payload.getSubjects(), updateSubjectArgs.getSubjectId());
                    if (subjectIndex != -1) {
                        ChatSessionTaskPayload.PayloadSubject oldPayloadSubject = payload.getSubjects().get(subjectIndex);

                        payload.getSubjects().set(subjectIndex, ChatSessionTaskPayload.PayloadSubject.from(updateSubjectArgs.getSubject()));
                        sessionTaskService.updateTaskPayload(updateSubjectArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);
                        String guidance = null;
                        if (StrUtil.isNotBlank(oldPayloadSubject.getResultFileId())) {
                            guidance = "主体 " + updateSubjectArgs.getSubjectId() + " 的主要参数被修改，主体结果已清空，重新生成主体才能继续使用";
                        }

                        yield MiscUtil.toolResp("Success", guidance);
                    }
                    yield subjectIdxErrMsg;
                }
                case DELETE -> {
                    boolean removeIf = payload.getSubjects().removeIf(subject -> subject.getId().equals(updateSubjectArgs.getSubjectId()));
                    if (removeIf) {
                        sessionTaskService.updateTaskPayload(updateSubjectArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);
                        yield "Success";
                    }
                    yield subjectIdxErrMsg;
                }
                case ADD -> {
                    // 根据 id 找到原始数据所在位置
                    int subjectIndex = StrUtil.isBlank(updateSubjectArgs.getSubjectId()) ? payload.getSubjects().size() : findSubjectIndex(payload.getSubjects(), updateSubjectArgs.getSubjectId());
                    if (subjectIndex != -1) {
                        payload.getSubjects().add(subjectIndex, ChatSessionTaskPayload.PayloadSubject.from(updateSubjectArgs.getSubject()));
                        sessionTaskService.updateTaskPayload(updateSubjectArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);
                        yield "Success";
                    }
                    yield subjectIdxErrMsg;
                }
            };
        }
    }

    @Tool("""
            更新 Scene (分镜) 数据；当要修改 Scene 数据时有且仅能通过此工具进行更新
            """)
    public String updateScene(UpdateSceneArgs updateSceneArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            MiscUtil.toolArgsRequireNonNull(updateSceneArgs, "updateSceneArgs", UpdateSceneArgs.class);

            log.info("Run updateScene args: {}", updateSceneArgs);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(updateSceneArgs.getTaskId(), sseMessageHelper.getChatSessionId());
            // 获取原始task数据
            ChatSessionTaskPayload payload = sessionTaskService.getTaskPayload(updateSceneArgs.getTaskId(), sseMessageHelper.getChatSessionId());

            String sceneIdxErrMsg = MiscUtil.toolResp("传入的 sceneId 错误，无法找到有效数据", "请检查输入参数");
            return switch (updateSceneArgs.getUpdateType()) {
                case UPDATE -> {
                    // 根据 id 找到原始数据所在位置
                    int sceneIndex = findSceneIndex(payload.getScenes(), updateSceneArgs.getSceneId());

                    if (sceneIndex != -1) {
                        // 如果没有修改提示词 则不清空结果
                        ChatSessionTaskPayload.PayloadScene oldPayloadScene = payload.getScenes().get(sceneIndex);
                        ChatSessionTaskPayload.PayloadScene newPayloadScene = ChatSessionTaskPayload.PayloadScene.from(updateSceneArgs.getScene());

                        String guidance = null;
                        if (oldPayloadScene.isReMakeScene(newPayloadScene) && StrUtil.isNotBlank(newPayloadScene.getResultFileId())) { // 修改对结果有影响 并且有结果
                            guidance = "分镜 " + updateSceneArgs.getSceneId() + " 的主要参数被修改，分镜结果已清空，重新生成分镜才能继续使用";
                        } else { // 如果不需要重新生成则保留 old 结果
                            newPayloadScene.setResultFileId(oldPayloadScene.getResultFileId());
                            newPayloadScene.setLipSyncFileId(oldPayloadScene.getLipSyncFileId());
                        }

                        payload.getScenes().set(sceneIndex, newPayloadScene);
                        sessionTaskService.updateTaskPayload(updateSceneArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);

                        yield MiscUtil.toolResp("Success", guidance);
                    }
                    yield sceneIdxErrMsg;
                }
                case DELETE -> {
                    boolean removeIf = payload.getScenes().removeIf(scene -> scene.getId().equals(updateSceneArgs.getSceneId()));
                    if (removeIf) {
                        sessionTaskService.updateTaskPayload(updateSceneArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);
                        yield "Success";
                    }
                    yield sceneIdxErrMsg;
                }
                case ADD -> {
                    // 根据 id 找到原始数据所在位置
                    int sceneIndex = StrUtil.isBlank(updateSceneArgs.getSceneId()) ? payload.getScenes().size() : findSceneIndex(payload.getScenes(), updateSceneArgs.getSceneId());
                    if (sceneIndex != -1) {
                        payload.getScenes().add(sceneIndex, ChatSessionTaskPayload.PayloadScene.from(updateSceneArgs.getScene()));
                        sessionTaskService.updateTaskPayload(updateSceneArgs.getTaskId(), sseMessageHelper.getChatSessionId(), payload);
                        yield "Success";
                    }
                    yield sceneIdxErrMsg;
                }
            };
        }
    }

    @Tool("""
            通过 taskId 获取任务最新数据
            """)
    public String getTaskPayload(@P("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask") String taskId, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            log.info("Run getTaskPayload taskId: {}", taskId);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            // 校验 taskId 格式
            sessionTaskService.checkTaskIdFormatIsLegal(taskId, sseMessageHelper.getChatSessionId());

            ChatSessionTaskPayload taskPayload = sessionTaskService.getTaskPayload(taskId, sseMessageHelper.getChatSessionId());

//            return MiscUtil.toolResp(JsonUtil.toJson(taskPayload),"");
            return JsonUtil.toJson(taskPayload);
        }
    }

    /**
     * 根据 id 找到主体所在索引
     *
     * @param subjects
     * @param subjectId
     * @return
     */
    private int findSubjectIndex(List<ChatSessionTaskPayload.PayloadSubject> subjects, String subjectId) {
        return findIndex(subjects, subject -> subject.getId().equals(subjectId));

//        for (int i = 0; i < subjects.size(); i++) {
//            if (subjects.get(i).getId().equals(subjectId)) {
//                return i;
//            }
//        }
//        return -1;
    }

    private int findSceneIndex(List<ChatSessionTaskPayload.PayloadScene> scenes, String sceneId) {
        return findIndex(scenes, scene -> scene.getId().equals(sceneId));
//        for (int i = 0; i < scenes.size(); i++) {
//            if (scenes.get(i).getId().equals(sceneId)) {
//                return i;
//            }
//        }
//        return -1;
    }

    private <T> int findIndex(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

}
