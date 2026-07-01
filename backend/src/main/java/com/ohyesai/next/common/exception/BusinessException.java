package com.ohyesai.next.common.exception;

import com.ohyesai.next.common.enums.CodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(Throwable cause) {
        super(CodeEnum.Unknow.message, cause);
        this.code = CodeEnum.Unknow.code;
    }

    public BusinessException(CodeEnum CodeEnum) {
        super(CodeEnum.message);
        this.code = CodeEnum.code;
    }

    public BusinessException(CodeEnum CodeEnum, String message) {
        super(message);
        this.code = CodeEnum.code;
    }

    public BusinessException(CodeEnum CodeEnum, Throwable cause) {
        super(CodeEnum.message, cause);
        this.code = CodeEnum.code;
    }

    public BusinessException(CodeEnum CodeEnum, String message, Throwable cause) {
        super(message, cause);
        this.code = CodeEnum.code;
    }
}
