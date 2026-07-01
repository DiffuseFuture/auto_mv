package com.ohyesai.next.common.vo;

import com.ohyesai.next.common.enums.CodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通用返回结果")
public class Result<T> {

    @Schema(description = "状态码")
    private int code;

    @Schema(description = "描述")
    private String message;

    @Schema(description = "数据")
    private T data;

    public static <T> Result<T> success(T data) {
        return custom(CodeEnum.Success.code, CodeEnum.Success.message, data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error() {
        return custom(CodeEnum.Unknow);
    }


    public static <T> Result<T> custom(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> custom(CodeEnum errorCode) {
        return custom(errorCode.code, errorCode.message, null);
    }

    public static <T> Result<T> custom(CodeEnum errorCode, String message) {
        return custom(errorCode.code, message, null);
    }

    public static <T> Result<T> custom(CodeEnum errorCode, T data) {
        return custom(errorCode.code, errorCode.message, data);
    }
}
