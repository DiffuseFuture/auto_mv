package com.ohyesai.next.biz.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InvitationRecords {

//    /**
//     * 邀请获得积分数量
//     * 1级赠送：用户生成mv时
//     */
//    public static final int INVITATION_POINTS_LEVEL_1 = 1500;
//
//    /**
//     * 邀请获得积分数量
//     * 2级赠送：用户充值时
//     */
//    public static final int INVITATION_POINTS_LEVEL_2 = 3000;
//
//
//    /**
//     * 每次最大获取积分数量
//     */
//    public static final int MAX_INVITATION_POINTS = 7500;

    /**
     * 主键ID，自增
     */
    private Integer id;

    /**
     * 邀请人ID
     */
    private String inviterId;

    /**
     * 受邀人ID
     */
    private String inviteeId;

    /**
     * 使用的邀请码
     */
    private String inviteCode;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
