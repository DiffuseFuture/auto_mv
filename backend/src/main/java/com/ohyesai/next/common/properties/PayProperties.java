package com.ohyesai.next.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "pay")
public record PayProperties(Wx wx, Ali ali, Set<String> payWhiteList) {

    public record Wx(
            String merchantId,
            String privateKey,
            String merchantSerialNumber,
            String apiV3Key,
            String notifyUrl,
            String appId,
            String productNotifyUrl
    ) {
    }

    public record Ali(
            String appid,
            String privateKey,
            String publicKey,
            String notifyUrl
    ) {
    }
}
