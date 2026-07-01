package com.ohyesai.next.component.vlm;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class VlmLipSyncComponent {

    private final RestClient restClient;

    private final ModelApiProperties modelApiProperties;

    public String lipSync4PixverseTryCatch(String videoUrl, String audioUrl) {
        try {
            return lipSync4Pixverse(videoUrl, audioUrl);
        } catch (BusinessException e) {
            log.error("replicate lipsynq 异常：", e);
            return null;
        }
    }

    /**
     *
     * <a href="https://replicate.com/pixverse/lipsync/api">doc</a>
     *
     * @param videoUrl
     * @param audioUrl
     * @return
     */
    public String lipSync4Pixverse(String videoUrl, String audioUrl) {
        ObjectNode requestBody = JsonUtil.object();
        ObjectNode inputNode = JsonUtil.object()
                .put("video", videoUrl)
                .put("audio", audioUrl);
        requestBody.set("input", inputNode);

        JsonNode response = restClient.post()
                .uri("https://api.replicate.com/v1/models/pixverse/lipsync/predictions")
                .header("Authorization", "Bearer " + modelApiProperties.replicate().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        log.info("replicate lipsync response: {}", response);

        if (response == null) {
            throw new BusinessException(CodeEnum.Unknow, "replicate lipsync 创建任务失败");
        }


        String status = response.path("status").asText();
        String url = response.path("urls").path("get").asText();
        if (StrUtil.isBlank(url) || StrUtil.isBlank(status) || "failed".equals(status) || "canceled".equals(status)) {
            log.error("replicate lipsync 处理失败: {}", response);
            throw new BusinessException(CodeEnum.Unknow, "lipSync error");
        }

        return pollOutput(url);
    }

    private String pollOutput(String url) {
        while (true) {
            JsonNode response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + modelApiProperties.replicate().ak())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new BusinessException(CodeEnum.Unknow, "replicate lipsync 状态查询失败");
            }
            String status = response.path("status").asText();

            log.info("轮训 lipSync4Pixverse 结果: {}", status);

            if ("succeeded".equals(status)) {
                return response.path("output").asText();
            }
            if (StrUtil.isBlank(status) || "failed".equals(status) || "canceled".equals(status)) {
                String error = response.path("error").asText();
                throw new BusinessException(CodeEnum.Unknow, "replicate lipsync 处理失败, error=" + error);
            }
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(e);
            }
        }
    }

}
