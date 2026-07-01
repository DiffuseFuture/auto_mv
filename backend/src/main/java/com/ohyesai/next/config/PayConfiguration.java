package com.ohyesai.next.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;

import com.ohyesai.next.common.properties.PayProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PayProperties.class)
public class PayConfiguration {
    /**
     * wx 支付身份
     */
    @Bean
    public RSAAutoCertificateConfig wxConfig(PayProperties properties) {
        PayProperties.Wx wx = properties.wx();
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(wx.merchantId())
                .privateKey(wx.privateKey())
                .merchantSerialNumber(wx.merchantSerialNumber())
                .apiV3Key(wx.apiV3Key())
                .build();
    }
    /**
     * wx native 支付客户端
     */
    @Bean
    public NativePayService nativePayService(RSAAutoCertificateConfig config) {
        return new NativePayService.Builder().config(config).build();
    }
    /**
     * 初始化微信 jsapi pay service
     * <p>
     * 小程序支付使用
     */
    @Bean
    public JsapiServiceExtension wxJsapiService(RSAAutoCertificateConfig config) {
        // 构建service
        return new JsapiServiceExtension.Builder().config(config).build();
    }
    /**
     * 支付宝支付客户端
     */
    @Bean
    public AlipayClient alipayClient(PayProperties properties) throws AlipayApiException {

        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
        alipayConfig.setAppId(properties.ali().appid());
        alipayConfig.setPrivateKey(properties.ali().privateKey());
        alipayConfig.setFormat("json");
        alipayConfig.setAlipayPublicKey(properties.ali().publicKey());
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setSignType("RSA2");
        return new DefaultAlipayClient(alipayConfig);
    }

}
