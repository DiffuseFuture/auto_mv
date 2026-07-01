package com.ohyesai.next.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tos")
public record TosProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {

}
