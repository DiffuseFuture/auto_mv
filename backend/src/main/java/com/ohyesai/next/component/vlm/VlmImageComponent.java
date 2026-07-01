package com.ohyesai.next.component.vlm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class VlmImageComponent {

    private final RestClient restClient;

    private final ModelApiProperties modelApiProperties;

    /**
     * 生成图像的方法
     * <a href="https://www.volcengine.com/docs/82379/1541523?lang=zh">doc</a>
     * @param prompt    图像生成的提示词
     * @param imageUrls 可选的参考图像 URL 列表
     * @param maxImages 最大生成图像数量
     * @return 生成的图像字节数组列表
     * @throws Exception 请求或解析异常
     */
    public List<byte[]> seedreamGenerateImage(String prompt, List<String> imageUrls, int maxImages) {
        // 判断是否启用连续图像生成
        String sequentialImageGeneration = maxImages < 2 ? "disabled" : "auto";

        // 构建请求体
        var requestBody = JsonUtil.object();
//        requestBody.put("model", "doubao-seedream-4-5-251128");
        requestBody.put("model", "doubao-seedream-5-0-lite-260128");
        requestBody.put("prompt", prompt);
        requestBody.put("sequential_image_generation", sequentialImageGeneration);
        requestBody.putObject("sequential_image_generation_options").put("max_images", maxImages);
        requestBody.put("size", "2K");
        requestBody.put("response_format", "url");
        requestBody.put("watermark", false);

        // 如果提供了参考图像 URL，则添加到请求体中
        if (CollUtil.isNotEmpty(imageUrls)) {
            requestBody.set("image", JsonUtil.value2Tree(imageUrls));
        }

        // 发送 POST 请求并解析响应
        JsonNode jsonResponse = restClient.post()
//                .uri(CommonConst.OMNI_BASE_URL + "/v1/images/generations")
//                .header("Authorization", "Bearer " + modelApiProperties.omnimaas().ak())

                // 这里必须使用官方api，5.0生成的真人图 必须是同一个账号下 才能被 seedance 2.0 使用
                .uri("https://ark.cn-beijing.volces.com/api/v3/images/generations")
                .header("Authorization", "Bearer " + modelApiProperties.volcFangzhou().ak())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (jsonResponse == null) {
            throw new BusinessException(CodeEnum.Unknow, "生图错误");
        }

        // 解析响应中的图像 URL 列表
        JsonNode dataNode = jsonResponse.path("data");
        if (dataNode == null || !dataNode.isArray()) {
            throw new BusinessException(CodeEnum.Unknow, "无法获取图片数据: " + jsonResponse);
        }
        List<byte[]> imageBytesList = new ArrayList<>();
        for (JsonNode jsonNode : dataNode) {
            String url = jsonNode.path("url").asText();
            byte[] imageBytes = HttpUtil.downloadBytes(url);
            imageBytesList.add(imageBytes);
        }
        return imageBytesList;
    }

    /**
     * 生成图像的方法 (Gemini 3 Pro Image Preview - Chat Completions)
     *
     * @param prompt    图像生成的提示词
     * @param imageUrls 可选的参考图像 URL 列表
     * @param maxImages 最大生成图像数量
     * @return 生成的图像字节数组列表
     * @throws Exception 请求或解析异常
     */
    public List<byte[]> geminiGenerateImage(String prompt, List<String> imageUrls, int maxImages) {
        // 构建请求体
        var requestBody = JsonUtil.object();
        requestBody.put("model", "gemini-3.1-flash-image-preview");

        // 构建参考图数组
        var contentNode = JsonUtil.array();
        if (CollUtil.isNotEmpty(imageUrls)) {
            for (String imageUrl : imageUrls) {
                contentNode.add(JsonUtil.object()
                        .put("type", "image_url")
                        .set("image_url", JsonUtil.object().put("url", imageUrl)));
            }
        }
        contentNode.add(JsonUtil.object()
                .put("type", "text")
                .put("text", prompt));

        // 构建prompt数组
        var messagesNode = JsonUtil.array();
        messagesNode.add(JsonUtil.object()
                .put("role", "user")
                .set("content", contentNode));
        requestBody.set("messages", messagesNode);

        // 发送 POST 请求并解析响应
        String response = restClient.post()
                .uri(CommonConst.OMNI_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + modelApiProperties.omnimaas().ak())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        // 解析响应中的图像 base64 数据
        JsonNode jsonResponse = JsonUtil.readTree(response);
        JsonNode choicesNode = jsonResponse.get("choices");
        if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
            throw new BusinessException(CodeEnum.Unknow, "无法获取响应数据: " + response);
        }

        JsonNode messageNodeResponse = choicesNode.get(0).get("message");
        String content = messageNodeResponse != null ? messageNodeResponse.get("content").asText() : "";

        // 从 Markdown 格式提取图像 base64 数据：![image](data:image/jpeg;base64,xxxx)
        List<byte[]> imageBytesList = new ArrayList<>();
        Pattern markdownPattern = Pattern.compile("!\\[image\\]\\(data:image/[^;]+;base64,([^)]+)\\)");
        Matcher matcher = markdownPattern.matcher(content);
        while (matcher.find()) {
            String base64Data = matcher.group(1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            imageBytesList.add(imageBytes);
        }
        return imageBytesList;
    }
}
