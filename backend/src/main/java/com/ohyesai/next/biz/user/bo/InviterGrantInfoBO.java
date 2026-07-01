package com.ohyesai.next.biz.user.bo;


import lombok.Data;

/**
 * 邀请人发放积分所需信息
 */
@Data
public class InviterGrantInfoBO {

    /**
     * 邀请人ID
     */
    private String inviterUserId;

    /**
     * 受邀人ID
     */
    private String inviteeUserId;

    /**
     * 邀请码
     */
    private String inviteCode;
}
