package com.ohyesai.next.config;

import cn.dev33.satoken.apikey.template.SaApiKeyUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.common.properties.MvAnalyzerProperties;
import com.ohyesai.next.common.properties.PayProperties;
import com.ohyesai.next.common.properties.WxProperties;
import com.ohyesai.next.trace.TraceInterceptor;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        WxProperties.class,
        ModelApiProperties.class,
        PayProperties.class,
        MvAnalyzerProperties.class
})
@EnableScheduling
public class AppConfiguration implements WebMvcConfigurer {

    private final HttpServletRequest request;

    public AppConfiguration(HttpServletRequest request) {
        this.request = request;
    }

    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // trace拦截器
        registry.addInterceptor(new TraceInterceptor())
                .addPathPatterns("/**");

        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 跳过预检请求
                    if (request.getMethod().equals("OPTIONS")) {
                        return;
                    }
                    if (request.getDispatcherType() == DispatcherType.REQUEST) {
                        // sa token bug  暂时先这么解决 等官方修复
                        // https://github.com/dromara/Sa-Token/issues/778
//                        StpUtil.checkLogin();

                        // 常规鉴权
                        SaRouter
                                .match("/**")    // 拦截的 path 列表，可以写多个 */
                                .notMatch("/api/**")        // 排除掉的 path 列表，可以写多个
                                .check(r -> StpUtil.checkLogin());

                        // api鉴权
                        SaRouter
                                .match("/api/**")    // 拦截的 path 列表，可以写多个 */
                                .check(r -> SaApiKeyUtil.currentApiKey());
                    }
                }))
                .addPathPatterns("/**")
                // 排除swagger接口
                .excludePathPatterns("/doc.html", "/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs/**", "/swagger-ui/index.html", "/webjars/**", "/error");
    }
}
