package com.ohyesai.next.biz.billing.enums;

public enum BillingModule {

    /**
     * 积分充值
     * point 此时代表倍率
     */
    PAYMENT("积分充值", 1),

    /**
     * 赠送积分
     */
    GIVE_POINT("赠送积分", 1),

    /**
     * 表情制作
     */
    EMOJI_CREATION("表情制作", -9),

    /**
     * 首次登录
     */
    FIRST_LOGIN("首次登录", 30),

    /**
     * 首次生成
     */
    FIRST_CREATE("首次生成", 10),

    /**
     * 分享到聊天
     */
    SHARE_TO_CHAT("分享到聊天", 10),

    /**
     * 分享到朋友圈
     */
    SHARE_TO_MOMENT("分享到朋友圈", 10),

    ;

    /**
     * 消耗积分点
     * 单价（次、秒等，倍率等具体取决于功能模块）
     */
    public final int point;

    /**
     * 模块名称
     */
    public final String moduleName;

    BillingModule(String moduleName, int point) {
        this.moduleName = moduleName;
        this.point = point;
    }
}
