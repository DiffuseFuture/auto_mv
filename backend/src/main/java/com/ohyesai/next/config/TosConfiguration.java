package com.ohyesai.next.config;

import com.ohyesai.next.common.properties.TosProperties;
import com.volcengine.tos.TOSClientConfiguration;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.credential.StaticCredentialsProvider;
import com.volcengine.tos.transport.TransportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TosProperties.class)
public class TosConfiguration {

    private final TosProperties tosProperties;

    public TosConfiguration(TosProperties tosProperties) {
        this.tosProperties = tosProperties;
    }

    @Bean
    public TOSV2 tosv2() {
        String region = "cn-beijing";
        TransportConfig config = TransportConfig.builder()
                // HTTP 读请求超时时间，单位毫秒，默认值为 30000，即 30 秒。
                .readTimeoutMills(30000)
                // HTTP 写请求超时时间，单位毫秒，默认值为 30000，即 30 秒。
                .writeTimeoutMills(30000)
                // 建立 HTTP 连接的超时时间，单位毫秒，默认值为 10000，即 10 秒。
                .connectTimeoutMills(10000)
                .build();

        TOSClientConfiguration configuration = TOSClientConfiguration.builder()
                .transportConfig(config)
                .region(region)
                .endpoint(tosProperties.endpoint())
                .credentialsProvider(new StaticCredentialsProvider(tosProperties.accessKey(), tosProperties.secretKey()))
                .build();

        return new TOSV2ClientBuilder().build(configuration);
    }
}
