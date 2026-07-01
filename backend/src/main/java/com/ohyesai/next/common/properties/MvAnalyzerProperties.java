package com.ohyesai.next.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mv-analyzer")
public class MvAnalyzerProperties {

    /**
     * mv-audio-analyzer 服务地址
     */
    private String baseUrl = "http://localhost:8010";

    /**
     * 请求超时（秒）
     */
    private int timeoutSeconds = 30;
}
