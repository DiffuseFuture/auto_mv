package com.ohyesai.next.trace;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

public class TraceInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID = "trace-id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String traceId = IdUtil.nanoId(6);
        // 获取用户 id 进行拼接
        if (request.getDispatcherType() == DispatcherType.REQUEST && StpUtil.isLogin()) {
            traceId += "-" + StpUtil.getLoginId();
        }
        response.addHeader(TRACE_ID, traceId);
        MDC.put(TRACE_ID, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(TRACE_ID);
    }
}
