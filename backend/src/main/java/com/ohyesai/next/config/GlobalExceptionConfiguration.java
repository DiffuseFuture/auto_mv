package com.ohyesai.next.config;

import cn.dev33.satoken.exception.SaTokenException;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

/**
 * 全局异常配置
 */
@RestControllerAdvice
@Slf4j
@AllArgsConstructor
public class GlobalExceptionConfiguration {

    private final HttpServletRequest httpServletRequest;

    // 捕获参数验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        // 打印异常信息
        log.warn("参数验证异常", e);
        // 返回统一的错误信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            return Result.custom(CodeEnum.ParameterError, fieldError.getDefaultMessage());
        }

        return Result.custom(CodeEnum.ParameterError);
    }

    // 捕获自定义异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> customExceptionHandler(BusinessException e) {
        // 打印异常信息
        log.warn("自定义异常: {} {}", httpServletRequest.getRequestURI(), e.getMessage());
        if (e.getCause() != null) {
            log.warn("自定义异常 cause", e.getCause());
        }
        // 返回统一的错误信息
        return Result.custom(e.getCode(), e.getMessage(), null);
    }

    // 捕获 satoken 异常
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> notLoginExceptionHandler(SaTokenException e) {
        // 打印异常信息
        log.warn("鉴权错误：{} {}", httpServletRequest.getRequestURI(), e.getMessage());
        // 返回统一的错误信息
        return Result.custom(CodeEnum.AuthError);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> exceptionHandler(Exception e) {
        // 打印异常信息
        log.error("全局异常信息", e);
        // 返回统一的错误信息
        return Result.error();
    }

    @ExceptionHandler({AsyncRequestNotUsableException.class, AsyncRequestTimeoutException.class})
    public void asyncRequestNotUsableException(AsyncRequestNotUsableException e) {
        log.error("异步请求异常, 客户端强制断开链接 {}", e.getMessage());
    }

}
