package com.ohyesai.next.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wx")
public record WxProperties(WebApp wa, MiniProgram mp) {

    public record WebApp(String appid, String secret) {
    }

    public record MiniProgram(
            String appid,
            String secret,
            /*
              订阅消息跳转状态
              跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
             */
            String subJumpState) {

    }
}
