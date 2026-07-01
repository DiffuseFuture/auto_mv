package com.ohyesai.next.util;

import com.ohyesai.next.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * sse emitter 包装类  主要是把 io 异常转为业务异常
 *
 * @param sseEmitter
 */
public record SseEmitterWrapper(SseEmitter sseEmitter) {

    public void sendJson(Object object) {
        try {
            sseEmitter.send(SseEmitter.event().data(object, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    public void complete() {
        sseEmitter.complete();
    }

    /**
     * 关闭的同时推送一条消息
     *
     * @param object
     */
    public void completeWithJsonData(Object object) {
        try {
            sendJson(object);
        } finally {
            sseEmitter.complete();

        }
    }

}
