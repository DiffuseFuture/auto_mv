package com.ohyesai.next.biz.baidu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ohyesai.next.biz.baidu.dto.UploadCovertDataDTO;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@AllArgsConstructor
public class ConvertDataService {

    private final String baseUrl = "https://ocpc.baidu.com/ocpcapi/api/uploadConvertData";

    private final RestClient restClient;

    /**
     * 上传转化数据
     *
     * @param uploadCovertDataDTO
     */
    public void uploadCovertData(UploadCovertDataDTO uploadCovertDataDTO) {
        ObjectNode body = JsonUtil.object();
        // token
        body.put("token", "？？？？");
        // conversionType
        ObjectNode conversionType = JsonUtil.object();
        conversionType.put("logidUrl", uploadCovertDataDTO.getLogidUrl());
        conversionType.put("newType", uploadCovertDataDTO.getNewType().code);
        body.set("conversionTypes", JsonUtil.array().add(conversionType));

        String response = restClient.post()
                .uri(baseUrl)
                .body(body)
                .retrieve()
                .body(String.class);
        log.info("上传转化数据结果：{}", response);
    }
}
