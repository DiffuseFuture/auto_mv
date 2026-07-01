package com.ohyesai.next.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 状态码常量
 * 数据库使用
 */
public enum StateEnum {
    // 状态：0：完成； 1 进行中； 2 失败； 3 等待；

    /**
     * 已生成 成功
     */
    SUCCESS(0),

    /**
     * 生成中
     */
    RUNNING(1),

    /**
     * 生成失败
     */
    FAIL(2),

    /**
     * 待生成/无需生成
     */
    WAIT(3),
    ;

    StateEnum(int code) {
        this.code = code;
    }

    @EnumValue
    public final int code;
}
