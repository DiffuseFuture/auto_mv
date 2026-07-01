package com.ohyesai.next.biz.billing.enums;

/**
 * 积分操作类型枚举
 * 枚举的顺序决定了 扣费顺序
 * <p/>
 * 枚举的顺序很重要！！！
 * 枚举的顺序很重要！！！
 * 枚举的顺序很重要！！！
 */
public enum PointsOperation {
    /**
     * 注册赠送
     */
    REGISTER,
    /**
     * 邀请赠送
     */
    INVITE,
    /**
     * 订阅充值
     */
    RECHARGE_SUBSCRIPTION,
    /**
     * 积分包充值
     */
    RECHARGE_POINT_PACKAGE,
    /**
     * 消费积分
     */
    CONSUME,
    /**
     * 积分过期
     */
    EXPIRED,

    ;
}
