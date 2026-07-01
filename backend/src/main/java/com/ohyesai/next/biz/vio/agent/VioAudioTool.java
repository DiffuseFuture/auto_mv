package com.ohyesai.next.biz.vio.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.vio.bo.tool.MakeMusicArgs;
import com.ohyesai.next.biz.vio.bo.tool.MakeMusicResp;
import com.ohyesai.next.biz.vio.bo.SaveVioProjectBO;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.biz.vio.entity.VioProject;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.biz.vio.helper.SseMessageHelper;
import com.ohyesai.next.biz.vio.service.VioProjectService;
import com.ohyesai.next.biz.vio.service.VioService;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.component.FeiShuNotifyComponent;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.component.vlm.VlmAudioComponent;
import com.ohyesai.next.trace.TraceInterceptor;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.MiscUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Component
@AllArgsConstructor
public class VioAudioTool {

    private final ChatModel chatModelGemini3_1Pro;

    private final VlmAudioComponent vlmAudioComponent;

    private final FileComponent fileComponent;

    private final VioProjectService vioProjectService;

    private final BillingService billingService;

    private final FeiShuNotifyComponent feiShuNotifyComponent;


    @Tool("""
            歌词创作工具。根据用户提供的主题、故事背景或情感描述（prompt）创作完整的歌词文本。
            """)
    public String makeLyrics(@P("prompt") String prompt, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            log.info("Run make lyrics， prompt: {}", prompt);
            Objects.requireNonNull(prompt, CommonConst.ARGS_NULL_ERROR);
            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);

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
            String lyrics = chatModelGemini3_1Pro.chat(
                            SystemMessage.from(system),
                            UserMessage.from(prompt)
                    )
                    .aiMessage()
                    .text();
            // 推送歌词到页面
            sseMessageHelper.ofLyrics(lyrics);
            return lyrics;
        }
    }

    @Tool("""
            根据提供的歌词和风格标签生成定制音乐。工具会调用 AI 模型生成多首（通常为 2 首）备选歌曲。
            **关于时长限制：** 生成时长由模型算法固定决定。即使用户要求特定时长（如1分钟），也必须直接生成完整歌曲，并在回复结尾附加固定话术：“*注：AI音乐生成时长由模型决定，无法精确指定，已为您生成完整版本。*”
            """)
    public String makeMusic(MakeMusicArgs makeMusicArgs,
                            InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            log.info("Run make music {}", makeMusicArgs);
            Objects.requireNonNull(makeMusicArgs, CommonConst.ARGS_NULL_ERROR);

            String title = makeMusicArgs.getTitle();
            String lyrics = makeMusicArgs.getLyrics();
            String styles = makeMusicArgs.getStyles();


            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            String userId = parameters.get(VioService.USER_ID);
            File tempDir = parameters.get(VioService.TEMP_DIR);

            // 校验用户是否有积分
            int pointsRequired = billingService.getPointsRequired(ModelEnum.NONE, TaskType.MAKE_MUSIC); // 积分单价
            int gapBalance = billingService.compareBalance(userId, pointsRequired); // 扣费后还剩多少积分
            if (gapBalance < 0) {
                int userPoints = gapBalance + pointsRequired; // 用户总积分
                throw new BusinessException(CodeEnum.PointNotEnough, CodeEnum.PointNotEnough.message.formatted(pointsRequired, userPoints, Math.abs(gapBalance)));
            }

            // 执行任务
            List<VlmAudioComponent.AudioResp> musicResps = vlmAudioComponent.generateMusicLoadBalanced(lyrics, title, styles, makeMusicArgs.isInstrumental(),makeMusicArgs.getVocalGender());
            // 构造工具返回结果
            List<MakeMusicResp> toolResult = musicResps.stream()
                    .map(audioResp -> {
                        // 下载文件
                        File musicFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp3");
                        HttpUtil.downloadFile(audioResp.audioUrl(), musicFile);
                        // 上传文件
                        String musicFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "mp3");
                        fileComponent.upload(musicFile, musicFileId);

                        String musicCoverFileId = fileComponent.genObjectName(FileDirConst.MV_DIR, "jpg");
                        fileComponent.uploadByUrl(audioResp.imageUrl(), musicCoverFileId);
                        // 解析媒体信息
                        CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(musicFile);
                        // 存储入库
                        SaveVioProjectBO saveVioProjectBO = SaveVioProjectBO.builder()
                                .fileId(musicFileId)
                                .previewFileId(musicCoverFileId)
                                .projectName(audioResp.title())
                                .duration((int) mediaInfo.duration().toSeconds())
                                .lyrics(audioResp.lrcLyrics())
                                .args(makeMusicArgs)
                                .taskType(TaskType.MAKE_MUSIC)
                                .userId(userId)
                                .sessionId(sseMessageHelper.getChatSessionId())
                                .messageId(sseMessageHelper.getHistoryMessageId())
                                .build();
                        VioProject vioProject = vioProjectService.save(saveVioProjectBO);

                        return new MakeMusicResp(audioResp.title(), musicFileId, musicCoverFileId, audioResp.lyrics(), mediaInfo.duration().toSeconds(), vioProject.getId());
                    })
                    .toList();

            // 扣费
            PointsTransactionLog pointsTransactionLog = billingService.consumePoints(userId, ModelEnum.NONE, TaskType.MAKE_MUSIC, pointsRequired).pointsTransactionLog();
            billingService.mapSessionWithPointsTransactionLog(sseMessageHelper.getChatSessionId(), pointsTransactionLog.getId()); // 记录 session 与积分交易记录的关联

            // 构造sse返回结果
            final List<VlmAudioComponent.AudioResp> musicRespsFinal = musicResps;
            int size = Math.min(toolResult.size(), musicResps.size());
            List<SseMsgBO.Audio> audios = IntStream.range(0, size)
                    .mapToObj(i -> {
                        MakeMusicResp musicResp = toolResult.get(i);
                        VlmAudioComponent.AudioResp audioResp = musicRespsFinal.get(i);
                        return new SseMsgBO.Audio(
                                musicResp.getTitle(),
                                audioResp.style(),
                                audioResp.lyrics(),
                                musicResp.getMusicFileId(),
                                fileComponent.shareUrl(musicResp.getMusicFileId()),
                                musicResp.getMusicCoverFileId(),
                                fileComponent.shareUrl(musicResp.getMusicCoverFileId()),
                                musicResp.getProjectId()
                        );
                    }).toList();
            sseMessageHelper.ofAudios(audios);

            return MiscUtil.toolResp("Success", """
                    1. 音乐制作已完成。音乐数据已推送至前端页面并展示,用户可以进行试听。
                    2. 引导用户点击音乐试听页面右下角的【制作MV】按钮截取精华片段，以继续制作 MV。
                    **【硬性要求】**：如果用户使用此音频制作MV，只能通过页面的【制作MV】按钮操作进行。
                    """);
        }
    }

}
