package com.ohyesai.next.biz.billing.bo;

/**
 * 升级会员套餐响应
 * @param usedPoints 用户以消耗的积分数
 * @param diffAmountF 需要补齐的差价
 */
public record UpdateSubscriptionPlanResp(int usedPoints, int diffAmountF) {
}
