package com.ohyesai.next.component;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.biz.vio.bo.BeatTimeline;
import com.ohyesai.next.common.properties.MvAnalyzerProperties;
import com.ohyesai.next.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP 客户端，调用 mv-audio-analyzer Python 微服务。
 */
@Slf4j
@Component
public class MvAudioAnalyzerClient {

    private final RestClient restClient;
    private final MvAnalyzerProperties properties;
//    private final ObjectMapper objectMapper;

    public MvAudioAnalyzerClient(RestClient restClient, MvAnalyzerProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        // Python 服务返回 snake_case，Java 侧用 camelCase
//        this.objectMapper = new ObjectMapper()
//                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 调用 analyzer 服务分析音频文件。
     *
     * @param audioPath 本地音频文件绝对路径
     * @return BeatTimeline 分析结果
     */
    @Deprecated
    public BeatTimeline analyze(String audioPath) {
        String url = properties.getBaseUrl() + "/analyze";
        log.info("调用 mv-audio-analyzer: {} audioPath={}", url, audioPath);

        String responseJson = HttpUtil.post(url, JsonUtil.object().put("audio_url", audioPath).toString());
//        String responseJson = restClient.post()
//                .uri(url)
//                .body(JsonUtil.object().put("audio_url", audioPath))
//                .retrieve()
//                .body(String.class);

        try {
            BeatTimeline timeline = JsonUtil.toObject(responseJson, BeatTimeline.class);
            log.info("mv-audio-analyzer 返回: bpm={}, beats={}, downbeats={}, segments={}",
                    timeline.getBpm(),
                    timeline.getBeats().size(),
                    timeline.getDownbeats().size(),
                    timeline.getSegments().size());
            return timeline;
        } catch (Exception e) {
            log.error("解析 mv-audio-analyzer 响应失败: {}", responseJson, e);
            throw new RuntimeException("音频分析服务响应解析失败", e);
        }
    }

    /**
     * 音乐节拍分析
     *
     * @param audioUrl
     * @return
     */
    public String musicSegment(String audioUrl) {

        String url = properties.getBaseUrl() + "/segment";
        log.info("调用 musicSegment: {} audioUrl={}", url, audioUrl);
        ObjectNode body = JsonUtil.object()
                .put("url", audioUrl)
                .put("min_segment_duration", 3.0)
                .put("degree", "medium");

        return HttpUtil.post(url, body.toString());
    }
}
