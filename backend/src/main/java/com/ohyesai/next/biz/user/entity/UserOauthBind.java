package com.ohyesai.next.biz.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CREATE TABLE `user_oauth_bind` (
 * `id` char(32) NOT NULL,
 * `user_id` char(32) NOT NULL,
 * `platform` varchar(20) NOT NULL COMMENT '平台: WX ',
 * `openid` varchar(64) NOT NULL,
 * `unionid` varchar(64) NOT NULL,
 * `create_time` datetime NOT NULL,
 * PRIMARY KEY (`id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户三方oahtu登录信息绑定表';
 */

@Data
public class UserOauthBind {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private Platform platform;

    private String openid;

    private String unionid;

    private LocalDateTime createTime;

    public enum Platform {
        /**
         * 微信小程序
         */
        WX_MP,
        /**
         * 微信开放平台
         */
        WX,
    }
}
