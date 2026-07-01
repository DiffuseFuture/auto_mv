package com.ohyesai.next.biz.billing.enums;

/**
 * 邀请奖励规则
 */
public enum InvitationPointsRule {


    /**
     * 用户生成mv时
     */
    MAKE_MV(1500, 7500),

    /**
     * 用户充值时
     */
    RECHARGE(3000, Integer.MAX_VALUE);


    /**
     * 赠送积分
     */
    public final int points;

    /**
     * 赠送积分上限
     */
    public final int limit;

    InvitationPointsRule(int points, int limit) {
        this.points = points;
        this.limit = limit;
    }
}
