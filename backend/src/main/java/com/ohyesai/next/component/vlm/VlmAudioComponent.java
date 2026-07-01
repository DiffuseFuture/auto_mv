package com.ohyesai.next.component.vlm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.biz.vio.bo.SubtitleTranscribed;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.GenderEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.component.FeiShuNotifyComponent;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.MiscUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class VlmAudioComponent {

    private final RestClient restClient;

    private final ModelApiProperties modelApiProperties;

    private final FeiShuNotifyComponent feiShuNotifyComponent;

    private final FileComponent fileComponent;

    private final String defaultMusicCover = "static/defaultMusicCover.png";

    /**
     * 带负载均衡的音乐生成
     *
     * @param lyrics
     * @param title
     * @param style
     * @param instrumental
     * @param vocalGender  期望的人声性别
     * @return
     */
    public List<AudioResp> generateMusicLoadBalanced(String lyrics, String title, String style, boolean instrumental, GenderEnum vocalGender) {
        List<AudioResp> musicResps;
        try {
            musicResps = generateMusicBySuno(lyrics, title, style, instrumental, vocalGender);
        } catch (Exception e) {
            feiShuNotifyComponent.sendNotifyAtAll("suno api 异常,切换 Mureka 重试", e.getMessage());
            try {
                musicResps = generateMusicByMureka(lyrics, title, style, instrumental);
            } catch (BusinessException ex) {
                feiShuNotifyComponent.sendNotifyAtAll("Mureka 异常", ex.getMessage());
                throw e;
            } catch (Exception ex) {
                feiShuNotifyComponent.sendNotifyAtAll("Mureka 异常", ex.getMessage());
                throw new BusinessException(CodeEnum.Unknow, e);
            }
        }
        return musicResps;
    }

    /**
     * 使用Suno生成音乐 <p>
     * <a href="https://docs.sunoapi.org/cn/suno-api/generate-music">sunoApi</a>
     *
     * @param lyrics       音乐生成的提示词
     * @param title        音乐标题
     * @param style        音乐标签
     * @param instrumental 是否为纯音乐
     * @return 生成的音乐URL列表
     */
    public List<AudioResp> generateMusicBySuno(String lyrics, String title, String style, boolean instrumental, @Nullable GenderEnum vocalGender) {
        String vocalGenderStr = switch (vocalGender) {
            case MALE -> "m";
            case FEMALE -> "f";
            case null -> null;
        };

        // 构建请求体
        var requestBody = JsonUtil.object();
        requestBody.put("prompt", lyrics);
        requestBody.put("customMode", true);
        requestBody.put("instrumental", instrumental); // 是否为纯音乐
        requestBody.put("model", "V5");
        requestBody.put("callBackUrl", "https://api.example.com/callback");
        if (vocalGenderStr != null) {
            requestBody.put("vocalGender", vocalGenderStr); // 性别
        }

        requestBody.put("title", title);
        requestBody.put("style", style);

        // 发送POST请求获取任务ID
        JsonNode response = restClient.post()
                .uri("https://api.sunoapi.org/api/v1/generate")
                .header("Authorization", "Bearer " + modelApiProperties.sunoapi().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "任务提交失败");
        }

        String taskId = response.path("data").path("taskId").asText();
        if (StrUtil.isBlank(taskId)) {
            String errorMsg = "无法获取任务ID:" + response;
//            feiShuNotifyComponent.sendNotifyAtAll("suno api 异常", errorMsg);
            throw new BusinessException(CodeEnum.Unknow, errorMsg);
        }

        // 轮询任务结果
        return querySunoMusicTask(taskId);
    }

    /**
     * Suno 上传并翻唱音乐<p>
     *
     * @param prompt
     * @param title
     * @param style
     * @param instrumental
     * @return
     */
    public List<AudioResp> uploadCoverMusicBySuno(String refAudioUrl, String prompt, String title, String style, boolean instrumental) {
        // 构建请求体
        var requestBody = JsonUtil.object();
        requestBody.put("uploadUrl", refAudioUrl);
        requestBody.put("prompt", prompt);
        requestBody.put("customMode", true);
        requestBody.put("instrumental", instrumental); // 是否为纯音乐
        requestBody.put("model", "V5");
        requestBody.put("callBackUrl", "https://api.example.com/callback");

        requestBody.put("title", title);
        requestBody.put("style", style);

        // 发送POST请求获取任务ID
        JsonNode response = restClient.post()
                .uri("https://api.sunoapi.org/api/v1/generate/upload-cover")
                .header("Authorization", "Bearer " + modelApiProperties.sunoapi().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "任务提交失败");
        }

        String taskId = response.path("data").path("taskId").asText();
        if (StrUtil.isBlank(taskId)) {
            String errorMsg = "无法获取任务ID:" + response;
            feiShuNotifyComponent.sendNotifyAtAll("suno api 异常", errorMsg);
            throw new BusinessException(CodeEnum.Unknow, errorMsg);
        }

        // 轮询任务结果
        return querySunoMusicTask(taskId);
    }

    /**
     * 查询音乐生成任务结果
     *
     * @param taskId 任务ID
     * @return 音乐URL列表
     */
    private List<AudioResp> querySunoMusicTask(String taskId) {
        while (true) {
            JsonNode response = restClient.get()
                    .uri("https://api.sunoapi.org/api/v1/generate/record-info?taskId=" + taskId)
                    .header("Authorization", "Bearer " + modelApiProperties.sunoapi().ak())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(CodeEnum.Unknow, "任务查询失败,taskId: " + taskId);
            }

            JsonNode dataNode = response.path("data");
            String status = dataNode.path("status").asText();

            log.info("轮询音乐 taskId {}; status {}", taskId, status);

            if ("CREATE_TASK_FAILED".equals(status) || "GENERATE_AUDIO_FAILED".equals(status) || "SENSITIVE_WORD_ERROR".equals(status)) {
                String errorMsg = String.format("音乐生成失败 状态查询参数：%s 返回值：%s", taskId, response);
//                feiShuNotifyComponent.sendNotifyAtAll("suno api 异常", errorMsg);
                throw new BusinessException(CodeEnum.Unknow, errorMsg);
            }

            if ("SUCCESS".equals(status)) {
                List<AudioResp> musicUrls = new ArrayList<>();
                JsonNode sunoDataArray = dataNode.path("response").path("sunoData");

                if (!sunoDataArray.isArray()) {
                    throw new BusinessException(CodeEnum.Unknow, "音乐生成失败");
                }
                for (JsonNode sunoData : sunoDataArray) {
                    String audioId = sunoData.path("id").asText();
                    String title = sunoData.path("title").asText();
                    String audioUrl = sunoData.path("audioUrl").asText();
                    String imageUrl = sunoData.path("imageUrl").asText();
                    String tags = sunoData.path("tags").asText();
                    String lyrics = sunoData.path("prompt").asText();
                    if (StrUtil.isAllNotBlank(audioUrl, imageUrl)) {
                        // 获取歌词信息
                        String lrcLyrics = queryTimestampedLyrics(taskId, audioId);
                        musicUrls.add(new AudioResp(title, tags, lyrics, lrcLyrics, audioUrl, imageUrl));
                    }
                }
                if (CollUtil.isEmpty(musicUrls)) {
                    throw new BusinessException(CodeEnum.Unknow, "音乐生成失败,没有产出音频");
                }
                return musicUrls;
            }

            // 等待5秒后重试
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(e);
            }
        }
    }

    // ... existing code ...

    /**
     * 查询带时间戳的歌词
     * 注意：截止到 20260304，sunoapi返回的歌词时间轴信息并不总是准确的，有时候偏差会非常大
     *
     * @param taskId  任务 ID
     * @param audioId 音频 ID
     * @return lrc 格式的歌词
     */
    public String queryTimestampedLyrics(String taskId, String audioId) {
        var requestBody = JsonUtil.object();
        requestBody.put("taskId", taskId);
        requestBody.put("audioId", audioId);

        JsonNode response = restClient.post()
                .uri("https://api.sunoapi.org/api/v1/generate/get-timestamped-lyrics")
                .header("Authorization", "Bearer " + modelApiProperties.sunoapi().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "获取歌词失败，taskId: " + taskId + ", audioId: " + audioId);
        }

        JsonNode alignedWordsNode = response.path("data").path("alignedWords");
        if (!alignedWordsNode.isArray()) {
            log.warn("queryTimestampedLyrics 失败，歌词为空 taskId:{} audioId:{}", taskId, audioId);
            return null;
        }

        return MiscUtil.convertToAgentLrc(alignedWordsNode);
    }

    /**
     * 音乐生成 mureka ，根据 instrumental 控制是否有歌词
     *
     * @param lyrics
     * @param title
     * @param style
     * @param instrumental
     * @return
     */
    public List<AudioResp> generateMusicByMureka(String lyrics, String title, String style, boolean instrumental) {
        if (instrumental) {
            return generateInstrumentalByMureka(title, style);
        } else {
            return generateSongByMureka(lyrics, title, style);
        }
    }

    /**
     * 歌曲生成(歌词)，mureka </p>
     * <a href="https://platform.mureka.cn/docs/api/operations/post-v1-song-generate.html">doc</a>
     *
     * @param lyrics
     * @param title
     * @param style
     * @return
     */
    public List<AudioResp> generateSongByMureka(String lyrics, String title, String style) {
        // 1. 构建请求体
        var requestBody = JsonUtil.object();
        requestBody.put("lyrics", lyrics);
        requestBody.put("model", "auto");
        requestBody.put("n", 2);
        requestBody.put("prompt", "title: " + title + ", style: " + style);  // 固定模式拼接

        // 2. 发送生成请求
        JsonNode response = restClient.post()
                .uri("https://api.mureka.cn/v1/song/generate")
                .header("Authorization", "Bearer " + modelApiProperties.mureka().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "mureka song 任务提交失败");
        }

        String songId = response.path("id").asText();
        if (StrUtil.isBlank(songId)) {
            throw new BusinessException(CodeEnum.Unknow, "mureka song 无法获取 songId: " + response);
        }

        // 3. 轮询查询结果
        return querySongMurekaTask(songId, title, style, lyrics);
    }

    private List<AudioResp> querySongMurekaTask(String songId, String title, String style, String lyrics) {
        while (true) {
            JsonNode response = restClient.get()
                    .uri("https://api.mureka.cn/v1/song/query/" + songId)
                    .header("Authorization", "Bearer " + modelApiProperties.mureka().ak())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(CodeEnum.Unknow, "mureka song 任务查询失败");
            }

            String status = response.path("status").asText();
            log.info("轮询 mureka song 任务 songId {}; status {}", songId, status);

            // 失败状态
            if ("failed".equals(status) || "timeouted".equals(status) || "cancelled".equals(status)) {
                String failedReason = response.path("failed_reason").asText();
                throw new BusinessException(CodeEnum.Unknow, "mureka song 错误: " + failedReason);
            }

            // 成功状态
            if ("succeeded".equals(status)) {
                List<AudioResp> result = new ArrayList<>();
                JsonNode choices = response.path("choices");
                if (choices.isArray()) {
                    for (JsonNode choice : choices) {
                        String url = choice.path("url").asText();
                        String imageUrl = fileComponent.shareUrl(defaultMusicCover); // Mureka 没有封面图 使用默认封面
                        if (StrUtil.isNotBlank(url)) {
                            url = trimMurekaAudioWatermark(url); // 去掉水印
                            result.add(new AudioResp(title, style, lyrics, null, url, imageUrl));
                        }
                    }
                }
                if (CollUtil.isEmpty(result)) {
                    throw new BusinessException(CodeEnum.Unknow, "mureka song 没有产出音频");
                }
                return result;
            }

            // 等待5秒后重试
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(e);
            }
        }
    }

    /**
     * 歌曲生成（纯音乐） mureka</p>
     * <a href="https://platform.mureka.cn/docs/api/operations/post-v1-instrumental-generate.html">doc</a>
     *
     * @param title
     * @param style
     * @return
     */
    public List<AudioResp> generateInstrumentalByMureka(String title, String style) {
        // 1. 构建请求体
        ObjectNode requestBody = JsonUtil.object();
        requestBody.put("model", "auto");
        requestBody.put("n", 2);
        requestBody.put("prompt", "title: " + title + ", style: " + style);  // 固定模式拼接

        // 2. 发送生成请求
        JsonNode response = restClient.post()
                .uri("https://api.mureka.cn/v1/instrumental/generate")
                .header("Authorization", "Bearer " + modelApiProperties.mureka().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "mureka instrumental 任务提交失败");
        }

        String songId = response.path("id").asText();
        if (StrUtil.isBlank(songId)) {
            throw new BusinessException(CodeEnum.Unknow, "mureka instrumental 无法获取 songId: " + response);
        }

        // 3. 轮询查询结果
        return queryInstrumentalMurekaTask(songId, title, style);
    }

    private List<AudioResp> queryInstrumentalMurekaTask(String songId, String title, String style) {
        while (true) {
            JsonNode response = restClient.get()
                    .uri("https://api.mureka.cn/v1/instrumental/query/" + songId)
                    .header("Authorization", "Bearer " + modelApiProperties.mureka().ak())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(CodeEnum.Unknow, "mureka instrumental 任务查询失败");
            }

            String status = response.path("status").asText();
            log.info("轮询 mureka instrumental 任务 songId {}; status {}", songId, status);

            // 失败状态
            if ("failed".equals(status) || "timeouted".equals(status) || "cancelled".equals(status)) {
                String failedReason = response.path("failed_reason").asText();
                throw new BusinessException(CodeEnum.Unknow, "mureka instrumental 错误: " + failedReason);
            }

            // 成功状态
            if ("succeeded".equals(status)) {
                List<AudioResp> result = new ArrayList<>();
                JsonNode choices = response.path("choices");
                if (choices.isArray()) {
                    for (JsonNode choice : choices) {
                        String url = choice.path("url").asText();
                        String imageUrl = fileComponent.shareUrl(defaultMusicCover); // Mureka 没有封面图 使用默认封面
                        if (StrUtil.isNotBlank(url)) {
                            url = trimMurekaAudioWatermark(url); // 去掉水印
                            result.add(new AudioResp(title, style, null, null, url, imageUrl));
                        }
                    }
                }
                if (CollUtil.isEmpty(result)) {
                    throw new BusinessException(CodeEnum.Unknow, "mureka instrumental 没有产出音频");
                }
                return result;
            }

            // 等待5秒后重试
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(e);
            }
        }
    }

    /**
     * 火山 asr 识别
     * 识别音频中字幕时间戳信息
     * <a href="https://www.volcengine.com/docs/6448/2386124?lang=zh">提交语音转字幕（ASR）任务 API</a>
     */
    public SubtitleTranscribed volcAsr(String audioUrl) {
        // 1. 构建请求体
        ObjectNode requestBody = JsonUtil.object();
        requestBody.put("audio_url", audioUrl);
        // content_type speech：普通说话;  singing：唱歌
        requestBody.put("content_type", "singing");

        // 2. 发送生成请求
        JsonNode response = restClient.post()
                .uri("https://mediakit.cn-beijing.volces.com/api/v1/tools/asr-subtitles")
                .header("Authorization", "Bearer " + modelApiProperties.volcMediakit().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "volcAsrSubmit 任务提交失败");
        }

        String taskId = response.path("task_id").asText();
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(CodeEnum.Unknow, "volcAsrSubmit 无法获取 task_id: " + response);
        }

        // 轮训结果
        JsonNode resultNode = volcMediaKitQuery(taskId).path("result");

        // 封装返回结构体，结构体没有按照官方文档定义，需自行封装
        SubtitleTranscribed subtitleTranscribed = new SubtitleTranscribed();
        subtitleTranscribed.setDuration(resultNode.path("duration").asDouble());

        List<SubtitleTranscribed.SubtitleLine> lines = new ArrayList<>();
        for (JsonNode subtitle : resultNode.withArrayProperty("subtitles")) {
            SubtitleTranscribed.SubtitleLine subtitleLine = new SubtitleTranscribed.SubtitleLine();
            subtitleLine.setStartTime(subtitle.path("start_time").asDouble());
            subtitleLine.setEndTime(subtitle.path("end_time").asDouble());
            subtitleLine.setText(subtitle.path("subtitle_text").asText());
            lines.add(subtitleLine);
        }
        subtitleTranscribed.setLines(lines);

        return subtitleTranscribed;
    }

    /**
     * 火山 mediakit 平台通用查询
     * <a href="https://www.volcengine.com/docs/6448/2278532?lang=zh&_vtm_=a106466.b106468.0_0.0_0.0.361_7610611322983646755">doc</a>
     *
     * @param taskId
     * @return result jsonNOde
     */
    public JsonNode volcMediaKitQuery(String taskId) {
        while (true) {
            JsonNode response = restClient.get()
                    .uri("https://mediakit.cn-beijing.volces.com/api/v1/tasks/" + taskId)
                    .header("Authorization", "Bearer " + modelApiProperties.volcMediakit().ak())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(CodeEnum.Unknow, "volcAsrQuery 任务查询失败");
            }

            String status = response.path("status").asText();
            log.info("轮询 volcAsrQuery 任务 taskId {}; status {}", taskId, status);


            // 失败状态 failed
            if ("failed".equals(status)) {
                throw new BusinessException(CodeEnum.Unknow, "volcAsrQuery 错误: " + response);
            }

            // 成功状态 completed
            if ("completed".equals(status)) {
                // 不同任务类型 成功时result的结构不同，外部调用者自行解析
                return response;
            }

            // 等待5秒后重试
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(e);
            }
        }
    }


    /**
     * 移除 mureka 末尾5秒的音频水印
     *
     * @param url
     * @return
     */
    private String trimMurekaAudioWatermark(String url) {

        File tempDir = new File(FileDirConst.TEMP_DIR);
        var _ = tempDir.mkdirs();
        try {
            // 下载文件
            File oldAudioFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".mp3");
            HttpUtil.downloadFile(url, oldAudioFile);
            // 移除水印
            File newAudioFile = new File(tempDir, IdUtil.fastSimpleUUID() + ".wav");
            CvUtil.MediaInfo mediaInfo = CvUtil.mediaInfo(oldAudioFile, false);
            CvUtil.trimAudio(oldAudioFile, newAudioFile, 0, mediaInfo.duration().toSeconds() - 5);
            // 上传文件
            String objectName = fileComponent.genObjectName(FileDirConst.TEMP_24H, IdUtil.fastSimpleUUID() + ".wav");
            fileComponent.upload(newAudioFile, objectName);
            // 返回文件url
            return fileComponent.shareUrl(objectName);
        } finally {
            FileUtil.del(tempDir);
        }
    }


    public record AudioResp(
            String title,
            String style,
            String lyrics,
            String lrcLyrics,
            String audioUrl,
            String imageUrl) {
    }
}
