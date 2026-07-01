package com.ohyesai.next.common.enums;

public enum CodeEnum {

    /**
     * 成功
     */
    Success(0, "success"),

    /**
     * 业务码 智能体需要恢复对话
     * 2001
     */
    ResumeChat(2001, "ResumeChat"),


    /**
     * 参数异常
     */
    ParameterError(4000, "参数异常"),

    /**
     * 账号密码错误
     */
    AccountPasswordError(4001, "账号或密码错误"),

    /**
     * 账号已存在
     */
    AccountExist(4002, "账号已存在"),

    /**
     * 鉴权错误
     */
    AuthError(4003, "鉴权错误"),

    /**
     * 重复订阅
     */
    RepeatSubscribe(4004, "重复订阅"),

    /**
     * 对话错误
     */
    ChatError(4005, "聊天的人太多了，请稍后再试！"),

    /**
     * 积分不足
     * 本次创作一共需要多少积分，当前您有多少积分，还需要多少积分
     */
    PointNotEnough(4006, """
            用户积分不足，无法继续完成任务
            
            **强制回复模板（请严格按照此格式回复用户）**
            积分余额不足，本次创作一共需要 %s 积分，当前您有 %s 积分，还需 %s 积分才能继续执行。请点击页面右上方头像中的【升级】图标补充积分。
            订阅会员后还可以购买积分附加包
            移动端暂不支持订阅，请到PC端订阅会员
            """),

    /**
     * 未知异常
     */
    Unknow(5000, "服务异常!"),


    /**
     * 潭柘智空 专用 code 码
     * 服务器不支持当前请求的功能
     *
     */
    UNSUPPORTED_FUNCTION(501001, "服务器不支持当前请求的功能"),
    ;


    public final int code;
    public final String message;

    CodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
