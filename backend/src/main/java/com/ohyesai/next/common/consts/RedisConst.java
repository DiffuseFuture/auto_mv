package com.ohyesai.next.common.consts;

/**
 * redis 常量
 */
public interface RedisConst {

    String REDIS_PREFIX = "ohyesai-next";

    /**
     * 用户级 计费模块锁
     */
    String BILLING_LOCK_USER = REDIS_PREFIX + ":billing-lock:%s";

    /**
     * 支付 订单信息
     */
    String PAY_ORDER = REDIS_PREFIX + ":pay-order:%s";

    /**
     * 微信 access_token
     */
    String WX_ACCESS_TOKEN = REDIS_PREFIX + ":wx-access-token:%s";

    /**
     * 手机号验证码缓存
     * 参数：手机号
     */
    String MOBILE_CODE = REDIS_PREFIX + ":mobile-code:%s";

    /**
     * 验证码发送频率限制
     */
    String MOBILE_CODE_RATE = REDIS_PREFIX + ":mobile-code-rate:%s";

    /**
     * 智能体对话历史，用来实现恢复聊天功能
     * 仅存储 chatId 下最后一轮对话（一问一答）
     * <p>
     * %s:%s = chatSessionId:historyMessageId
     */
    String AGENT_CHAT_HISTORY = REDIS_PREFIX + ":agent-chat-history:%s:%s";

    /**
     * session 级别对话所
     */
    String AGENT_CHAT_SESSION_LOCK = REDIS_PREFIX + ":agent-chat-session-lock:%s";

    /**
     * 分享链接缓存
     */
    String SHARE_LINK_CACHE = REDIS_PREFIX + ":share-link-cache:%s";

    /**
     * 积分发放锁
     */
    String POINTS_GRANT_LOCK = REDIS_PREFIX + ":points-grant-lock";

    /**
     * 积分到期锁
     */
    String POINTS_EXPIRE_LOCK = REDIS_PREFIX + ":points-expire-lock";

    /**
     * 分镜修改(reMakeVideo) 任务状态
     */
    String REMAKE_VIDEO_TASK_STATUS = REDIS_PREFIX + ":remake-video-task-status:%s";

    /**
     * 直接编辑 任务状态
     */
    String DIRECT_EDIT_TASK_STATUS = REDIS_PREFIX + ":direct-edit-task-status:%s";

    /**
     * 技能 任务 状态
     */
    String SKILL_TASK_STATUS = REDIS_PREFIX + ":skill-task-status:%s";

    /**
     * 邀请人积分限制
     * %s: inviterUserId 邀请人的UserId，记录邀请人的积分限制
     */
    String INVITER_POINTS_LIMIT = REDIS_PREFIX + ":inviter-points-limit:%s";

    /**
     * 给邀请人发放积分所需信息
     * %s: inviteeUserId  受邀人的UserId，受邀人在消费积分的时候 检查是否有他需要发放积分的事件； 注意这里是受邀人的，因为触发事件拿到的是受邀人的 UserId 更方便查找
     */
    String INVITER_GRANT_INFO = REDIS_PREFIX + ":inviter-grant-info:%s";

    /**
     * 受邀人已执行的积分发放事件
     * %s: inviteeUserId  受邀人的UserId，记录受邀人的已执行积分发放事件
     */
    String INVITEE_GRANTED_EVENT = REDIS_PREFIX + ":invitee-granted-event:%s";

    /**
     * agent tool 结果缓存
     * historyMessageId: 控制缓存尽在当前轮次生效
     * historyMessageId: CacheKey
     */
    String AGENT_TOOL_RESULT_CACHE = REDIS_PREFIX + ":agent-tool-result-cache:%s:%s";
}
