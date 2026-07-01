package com.ohyesai.next.config;

import com.ohyesai.next.trace.ProxyExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * 全局http 客户端配置
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class RestClientConfiguration {

    /**
     * 全局通用rest客户端
     *
     * @param builder
     * @return
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) throws Exception {
//        SSLContext sslContext = sslContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .executor(ProxyExecutors.newVirtualThreadPerTaskExecutor()) // 在虚拟线程上执行并发
                // 抓包调试时 需要设置忽略信任证书，mac系统证书加到系统也不好使
//                .sslContext(sslContext())
//                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 9090)))
                .build();

        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);
        jdkClientHttpRequestFactory.setReadTimeout(Duration.ofMinutes(5));

        return builder
                .requestFactory(jdkClientHttpRequestFactory)
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    // do nothing
                    log.warn("{} 接口4xx错误", request.getURI());
                })
                .build();
    }

    private static SSLContext sslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }
}
