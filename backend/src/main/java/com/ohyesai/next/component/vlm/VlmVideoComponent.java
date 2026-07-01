package com.ohyesai.next.component.vlm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.component.FeiShuNotifyComponent;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class VlmVideoComponent {

    private final RestClient restClient;

    private final ModelApiProperties modelApiProperties;

    private final FeiShuNotifyComponent feiShuNotifyComponent;

//    private final FileComponent fileComponent;

    /**
     * vidu 参考生视频
     * <a href="https://platform.vidu.cn/docs/reference-to-video">doc</a>
     *
     * @param prompt
     * @param duration
     * @param aspectRatio
     * @param subjects
     * @return
     */
    public VideoResp viduReference2video(String prompt, int duration, AspectRatio aspectRatio, List<Subject> subjects, Resolution resolution) {
        duration = durationCheck(duration, 1, 10, "vidu 最大时长不能超过10秒");

        String aspectRatioStr = switch (aspectRatio) {
            case LANDSCAPE -> "16:9";
            case PORTRAIT -> "9:16";
        };

        String resolutionStr = switch (resolution) {
            case Resolution.P720 -> "720p";
            case Resolution.P1080 -> "1080p";
        };

        ObjectNode requestBody = JsonUtil.object();
        requestBody.put("model", "viduq2");
        requestBody.put("prompt", prompt);
        requestBody.put("duration", duration);
        requestBody.put("aspect_ratio", aspectRatioStr);
        requestBody.put("audio", false);
        requestBody.put("resolution", resolutionStr);
        requestBody.put("movement_amplitude", "auto");
        requestBody.put("seed", 0);

        if (CollUtil.isNotEmpty(subjects)) {
            ArrayNode subjectsArray = JsonUtil.array();
            for (Subject subject : subjects) {
                ObjectNode subjectNode = JsonUtil.object();
                subjectNode.put("name", subject.getId());
                subjectNode.set("images", JsonUtil.value2Tree(subject.getImages()));
                subjectsArray.add(subjectNode);
            }
            requestBody.set("subjects", subjectsArray);
        }

        JsonNode response = restClient.post()
                .uri("https://api.vidu.cn/ent/v2/reference2video")
                .header("Authorization", "Token " + modelApiProperties.vidu().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "vidu 生成视频错误");
        }

        String taskId = response.path("task_id").asText();
        if (StrUtil.isBlank(taskId)) {
            feiShuNotifyComponent.sendNotifyAtAll("vidu 错误", "无法获取 task_id: " + response);
            throw new BusinessException(CodeEnum.Unknow, "无法获取 task_id: " + response);
        }

        while (true) {
            String videoUrl = viduVideoQuery(taskId);
            if (StrUtil.isNotBlank(videoUrl)) {
                return new VideoResp(videoUrl, duration);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }
    }

    /**
     * vidu 文生视频 <p>
     * 相对于参考生 第一秒积分由25变为15 <p>
     *
     * <a href="https://platform.vidu.cn/docs/text-to-video">doc</a>
     *
     * @param prompt      视频生成的提示词
     * @param duration    视频时长（秒），viduq2 支持 1-10秒
     * @param aspectRatio 比例：16:9、9:16、3:4、4:3、1:1
     * @return 生成的视频URL和时长
     */
    public VideoResp viduTextToVideo(String prompt, int duration, AspectRatio aspectRatio, Resolution resolution) {
        duration = durationCheck(duration, 1, 10, "vidu 最大时长不能超过10秒");

        String aspectRatioStr = switch (aspectRatio) {
            case LANDSCAPE -> "16:9";
            case PORTRAIT -> "9:16";
        };

        String resolutionStr = switch (resolution) {
            case Resolution.P720 -> "720p";
            case Resolution.P1080 -> "1080p";
        };

        ObjectNode requestBody = JsonUtil.object();
        requestBody.put("model", "viduq2");
        requestBody.put("style", "general");
        requestBody.put("prompt", prompt);
        requestBody.put("duration", duration);
        requestBody.put("seed", 0);
        requestBody.put("aspect_ratio", aspectRatioStr);
        requestBody.put("resolution", resolutionStr);
        requestBody.put("movement_amplitude", "auto");
        requestBody.put("audio", false);
        requestBody.put("off_peak", false);

        JsonNode response = restClient.post()
                .uri("https://api.vidu.cn/ent/v2/text2video")
                .header("Authorization", "Token " + modelApiProperties.vidu().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "vidu 生成视频错误");
        }

        String taskId = response.path("task_id").asText();
        if (StrUtil.isBlank(taskId)) {
            feiShuNotifyComponent.sendNotifyAtAll("vidu 错误", "无法获取 task_id: " + response);
            throw new BusinessException(CodeEnum.Unknow, "无法获取 task_id: " + response);
        }

        while (true) {
            String videoUrl = viduVideoQuery(taskId);
            if (StrUtil.isNotBlank(videoUrl)) {
                return new VideoResp(videoUrl, duration);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }
    }

    private String viduVideoQuery(String taskId) {
        JsonNode response = restClient.get()
                .uri("https://api.vidu.cn/ent/v2/tasks/" + taskId + "/creations")
                .header("Authorization", "Token " + modelApiProperties.vidu().ak())
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "vidu 错误");
        }

        String state = response.path("state").asText();
        log.info("轮训 vidu 视频生成 结果 taskId {}; state {}", taskId, state);

        if ("failed".equals(state)) {
            feiShuNotifyComponent.sendNotifyAtAll("vidu 视频异常", response.toString());
            throw new BusinessException(CodeEnum.Unknow, "vidu 错误: " + response);
        }

        if ("success".equals(state)) {
            JsonNode creations = response.path("creations");
            if (creations.isArray() && !creations.isEmpty()) {
                return creations.path(0).path("url").asText();
            }
        }

        return null;
    }

    /**
     * 文生视频方法
     * <a href="https://platform.vidu.cn/docs/reference-to-video">doc</a>
     *
     * @param prompt      视频生成的提示词
     * @param duration    视频时长（秒），默认为5秒 [1-10]
     * @param subjects    主体对象列表（可选）
     * @param aspectRatio 比例默认 16:9，可选值如下：16:9、9:16、1:1  注：q2模型 支持任意宽高比
     * @return 生成的视频URL
     * @throws Exception 请求或解析异常
     */
    public VideoResp omniViduTextToVideo(String prompt, int duration, String aspectRatio, List<Subject> subjects) {
        // 确保模型最小值
        duration = Math.max(1, duration);

        String stype = "general";
        String resolution = "720p";
        int seed = 0;
        String movementAmplitude = "auto";
        boolean bgm = false;

        // 构建 metadata
        var metadata = JsonUtil.object();
        metadata.put("seed", seed);
        metadata.put("resolution", resolution);
        metadata.put("movement_amplitude", movementAmplitude);
        metadata.put("bgm", bgm);
        // 比例默认 16:9，可选值如下：16:9、9:16、1:1  注：q2模型 支持任意宽高比
        metadata.put("aspect_ratio", aspectRatio);

        if (CollUtil.isNotEmpty(subjects)) {
            metadata.set("subjects", JsonUtil.value2Tree(subjects));
        }

        // 构建请求体
        var requestBody = JsonUtil.object();
        requestBody.put("model", ModelEnum.VIDUQ2.modelName);
        requestBody.put("stype", stype);
        requestBody.put("prompt", prompt);
        requestBody.put("duration", duration);
        requestBody.set("metadata", metadata);

        // 发送 POST 请求并获取 task_id
        String response = restClient.post()
                .uri(CommonConst.OMNI_BASE_URL + "/v1/video/generations")
                .header("Authorization", "Bearer " + modelApiProperties.omnimaas().ak())
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        String taskId = jsonResponse.path("task_id").asText();
        if (StrUtil.isBlank(taskId)) {
            String errorMsg = "无法获取 task_id: " + response;
            feiShuNotifyComponent.sendNotifyAtAll("vidu 异常", errorMsg);
            throw new BusinessException(CodeEnum.Unknow, errorMsg);
        }

        // 循环查询结果
        while (true) {
            String videoUrl = omniVideoQuery(taskId);
            if (StrUtil.isNotBlank(videoUrl)) {
                return new VideoResp(videoUrl, duration);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }
    }

    /**
     * 查询视频生成结果
     *
     * @param taskId 任务ID
     * @return 视频URL（如果生成成功）
     */
    private String guoyanVideoQuery(String taskId) {
        String response = restClient.get()
                .uri(CommonConst.GUOYAN_BASE_URL + "/v1/video/generations/" + taskId)
                .header("Authorization", "Bearer " + modelApiProperties.guoyan().ak())
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        JsonNode data = jsonResponse.path("data").path("data");

        /**
         * created 创建成功
         * queueing 任务排队中
         * processing 任务处理中
         * success 任务成功
         * failed 任务失败
         */
        String status = data.path("state").asText();

        log.info("轮训 guoyan 视频生成 结果 taskId {}; status {}", taskId, status);

        if ("failed".equals(status)) {
            feiShuNotifyComponent.sendNotifyAtAll("轮训 omni 视频异常", response);
            throw new BusinessException(CodeEnum.Unknow, response);
        }

        if ("success".equals(status)) {
            return data.path("creations").path(0).path("url").asText();
        }

        return null; // 仍在处理中
    }

    /**
     * 查询视频生成结果
     *
     * @param taskId 任务ID
     * @return 视频URL（如果生成成功）
     */
    private String omniVideoQuery(String taskId) {
        String response = restClient.get()
                .uri(CommonConst.OMNI_BASE_URL + "/v1/video/generations/" + taskId)
                .header("Authorization", "Bearer " + modelApiProperties.omnimaas().ak())
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        JsonNode data = jsonResponse.get("data");

        String status = data.get("status").asText();

        log.info("轮训 omni 视频生成 结果 taskId {}; status {}", taskId, status);

        if ("failed".equals(status)) {
            feiShuNotifyComponent.sendNotifyAtAll("轮训 omni 视频异常", response);
            throw new BusinessException(CodeEnum.Unknow, response);
        }

        if ("succeeded".equals(status)) {
            return data.get("url").asText();
        }

        return null; // 仍在处理中
    }

    /**
     * 示例：
     * {
     * "model": "kling-v3-omni-pro",
     * "duration": 5,
     * "prompt": "<<<image_1>>> 在 <<<image_2>>>散步",
     * "metadata": {
     * "multi_shot": false,
     * "aspect_ratio": "9:16",
     * "image_list": [
     * {
     * "image_url":  "https://oss-workplace-prod.innoecos.cn/picture/0be2b49767400000_%E7%8C%AB.jpg"
     * },
     * {
     * "image_url":  "https://oss-workplace-prod.innoecos.cn/picture/0beb787f83c00000_%E5%AE%87%E5%AE%99.jpeg"
     * }
     * ],
     * "sound": "on"
     * }
     * }
     *
     * <a href="https://klingai.com/document-api/apiReference/model/OmniVideo">doc</a>
     *
     * @param prompt
     * @param duration    3-15 秒
     * @param aspectRatio 16:9 9:16 1:1
     * @param subjects
     * @return
     */
    public VideoResp klingTextToVideo(String prompt, int duration, AspectRatio aspectRatio, List<Subject> subjects, Resolution resolution) {
        // 确保模型最小值
        duration = durationCheck(duration, 3, 15, "keling 最大时长不能超过15秒");

        String aspectRatioStr = switch (aspectRatio) {
            case LANDSCAPE -> "16:9";
            case PORTRAIT -> "9:16";
        };

        // 通过 模式控制分辨率
        String mode = switch (resolution) {
            case Resolution.P720 -> "std";
            case Resolution.P1080 -> "pro";
        };

        ObjectNode requestBody = JsonUtil.object();
        // 转换提示词与主体
        ArrayNode imageList = JsonUtil.array();
        if (CollUtil.isNotEmpty(subjects)) {
            for (int i = 0; i < subjects.size(); i++) {
                Subject subject = subjects.get(i);
                // 将提示词中的id替换换为对应的图片标识
                prompt = prompt.replaceAll("@?" + Pattern.quote(subject.getId()), "<<<image_%d>>>".formatted(i + 1));
//                prompt = prompt.replace(subject.getId(), "<<<image_%d>>>".formatted(i + 1));
                // 仅支持一张首帧图
                if (subject.getImages().size() > 1) {
                    log.warn("一个主体仅支持1张图,本次大于一张仅取第一张 {}", subject.getImages());
                }
                imageList.add(JsonUtil.object()
                        .put("image_url", subject.getImages().getFirst())
                );
            }
        }


        requestBody.put("model", ModelEnum.KLING_V3_OMNI.modelName + "-" + mode);
        requestBody.put("duration", duration);
        requestBody.put("prompt", prompt);

        // 元数据
        ObjectNode metadata = JsonUtil.object();
        metadata.put("multi_shot", false);
        metadata.put("aspect_ratio", aspectRatioStr);
        metadata.set("image_list", imageList);
        // 关闭音效
        metadata.put("sound", "off");
        requestBody.set("metadata", metadata);

        // 发送 POST 请求并获取 task_id
        String response = restClient.post()
                .uri(CommonConst.OMNI_BASE_URL + "/v1/video/generations")
                .header("Authorization", "Bearer " + modelApiProperties.omnimaas().ak())
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        String taskId = jsonResponse.path("task_id").asText();
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(CodeEnum.Unknow, "无法获取 task_id: " + response);
        }

        // 循环查询结果
        while (true) {
            String videoUrl = omniVideoQuery(taskId);
            if (StrUtil.isNotBlank(videoUrl)) {
                return new VideoResp(videoUrl, duration);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }

    }

    public VideoResp seedance2Video(String prompt, Integer duration, AspectRatio aspectRatio, List<Subject> subjects, Resolution resolution) {
        return seedance2ByModelName(prompt, ModelEnum.SEEDANCE_2.modelName, duration, aspectRatio, subjects, resolution);
    }

    public VideoResp seedance2FastVideo(String prompt, Integer duration, AspectRatio aspectRatio, List<Subject> subjects, Resolution resolution) {
        if (resolution == Resolution.P1080) {
            throw new BusinessException(CodeEnum.Unknow, "seedance2_fast 1080p 不支持");
        }
        return seedance2ByModelName(prompt, ModelEnum.SEEDANCE_2_FAST.modelName, duration, aspectRatio, subjects, resolution);
    }

    /**
     * 使用 Seedance 2.0 生成视频
     * <a href="https://www.volcengine.com/docs/82379/1520757?lang=zh">doc</a>
     * <a href="https://www.volcengine.com/docs/82379/1544106?_vtm_=a106466.b106468.0_0.0_0.0.195_7610611322983646755&lang=zh#83af2aad">模型计费</a>
     *
     * @param prompt      提示词，可包含 @subjectId 引用图片
     * @param duration    视频时长（秒）  [4,15]
     * @param aspectRatio 宽高比 16:9 9:16 1:1
     * @param subjects    参考图片主体列表
     * @return 视频URL
     */
    private VideoResp seedance2ByModelName(String prompt, String modelName, int duration, AspectRatio aspectRatio, List<Subject> subjects, Resolution resolution) {
        // 确保模型最小值
        duration = durationCheck(duration, 4, 15, "seedance2 最大时长不能超过15秒");

        String aspectRatioStr = switch (aspectRatio) {
            case LANDSCAPE -> "16:9";
            case PORTRAIT -> "9:16";
        };

        String resolutionStr = switch (resolution) {
            case Resolution.P720 -> "720p";
            case Resolution.P1080 -> "1080p";
        };

        // 构建 content 数组
        ArrayNode contentArray = JsonUtil.array();

        // 转换提示词中的主体引用，并添加参考图片
        ArrayNode imagesArray = JsonUtil.array();
        if (CollUtil.isNotEmpty(subjects)) {
            for (int i = 0; i < subjects.size(); i++) {
                Subject subject = subjects.get(i);
                prompt = prompt.replaceAll("@?" + Pattern.quote(subject.getId()), "图片%d".formatted(i + 1));
                if (subject.getImages().size() > 1) {
                    log.warn("seedance2ByModelName 一个主体仅支持1张图,本次大于一张仅取第一张 {}", subject.getImages());
                }
                imagesArray.add(JsonUtil.object()
                        .put("type", "image_url")
                        .put("role", "reference_image")
                        .set("image_url", JsonUtil.object().put(
                                "url", subject.getImages().getFirst()
                        ))
                );
            }
        }

        // 添加文本提示词
        contentArray.add(JsonUtil.object()
                .put("type", "text")
                .put("text", prompt)
        );
        contentArray.addAll(imagesArray);


        // 构建请求体
        ObjectNode requestBody = JsonUtil.object();

        requestBody.put("model", modelName);
//        requestBody.put("model", "doubao-seedance-2-0-260128");
        requestBody.set("content", contentArray);
        requestBody.put("generate_audio", false);
        requestBody.put("ratio", aspectRatioStr);
        requestBody.put("duration", duration);
        requestBody.put("watermark", false);
        requestBody.put("resolution", resolutionStr);

        // 发送 POST 请求
        String response = restClient.post()
                .uri("https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks")
                .header("Authorization", "Bearer " + modelApiProperties.volcFangzhou().ak())
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        String taskId = jsonResponse.path("id").asText();
        if (StrUtil.isBlank(taskId)) {
            log.error("seedance2 无法获取 task_id: {}", response);
            feiShuNotifyComponent.sendNotifyAtAll("seedance 2 异常", response);

            String code = jsonResponse.path("error").path("code").asText();
            if ("InputImageSensitiveContentDetected.PrivacyInformation".equals(code)) {
                // 触发真人禁用
                throw new BusinessException(CodeEnum.Unknow, "生成失败，" + modelName + " 不支持真人");
            }
            throw new BusinessException(CodeEnum.Unknow, "生成失败，请重试");
        }

        // 循环查询结果
        while (true) {
            String videoUrl = seedance2VideoQuery(taskId);
            if (StrUtil.isNotBlank(videoUrl)) {
                return new VideoResp(videoUrl, duration);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new BusinessException(e);
            }
        }
    }

    private String seedance2VideoQuery(String taskId) {
        String response = restClient.get()
                .uri("https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks/" + taskId)
                .header("Authorization", "Bearer " + modelApiProperties.volcFangzhou().ak())
                .retrieve()
                .body(String.class);

        JsonNode jsonResponse = JsonUtil.readTree(response);
        String status = jsonResponse.get("status").asText();

        log.info("轮训 seedance视频生成 结果 taskId {}; status {}", taskId, status);

        // cancelled failed expired
        if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
            feiShuNotifyComponent.sendNotifyAtAll("seedance 2 异常", response);
            throw new BusinessException(CodeEnum.Unknow, "seedance 错误: " + response);
        }

        if ("succeeded".equals(status)) {
            return jsonResponse.path("content").path("video_url").asText();
        }

        return null; // 仍在处理中
    }

    @Data
    public static class Subject {
        private String id;
        /**
         * 文件url
         */
        private List<String> images;
    }

    public record VideoResp(
            String videoUrl,
            int duration) {
    }

    /**
     * 校验时长范围，
     * - 低于最低时长会以模型的最低时长为准（过长可以裁剪），
     * - 高于最高时长则抛出异常（过短无法拉长）
     *
     * @param duration
     * @param maxDuration
     * @param minDuration
     * @param errorMsg
     * @return
     */
    private int durationCheck(int duration, int minDuration, int maxDuration, String errorMsg) {
        if (duration < minDuration) {
            return minDuration;
        }
        if (duration > maxDuration) {
            throw new BusinessException(CodeEnum.Unknow, errorMsg);
        }
        return duration;
    }


}
