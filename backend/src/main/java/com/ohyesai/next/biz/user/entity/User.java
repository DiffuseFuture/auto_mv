package com.ohyesai.next.biz.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String account;

    private String mobile;

    private String password;

    private String nickName;

    private Integer gender;

    private LocalDateTime birthDate;

    private String avatarImg;

//    private Integer pointsBalance;

    /**
     * 用户订阅计划 code
     */
    private String subscriptionPlanTierCode;

    /**
     * 用户订阅到期时间
     */
    private LocalDateTime subscriptionEndTime;

    /**
     * 用户api密钥
     */
    private String apiKey;

    /**
     * 用户专属邀请码
     */
    private String inviteCode;

    private LocalDateTime createTime;

}