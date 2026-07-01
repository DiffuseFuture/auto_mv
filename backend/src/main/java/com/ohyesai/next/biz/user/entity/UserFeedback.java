package com.ohyesai.next.biz.user.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CREATE TABLE `user_feedback` (
 * `id` int NOT NULL AUTO_INCREMENT,
 * `user_id` char(32) NOT NULL,
 * `session_id` char(32) NOT NULL,
 * `message_id` char(32) NOT NULL,
 * `satisfaction_type` int NOT NULL COMMENT '1 满意；2 不满意',
 * `content` varchar(500) DEFAULT NULL COMMENT '反馈内容',
 * `create_time` datetime NOT NULL,
 * PRIMARY KEY (`id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈表';
 */
@Data
public class UserFeedback {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String userId;

    private String messageId;

    private AttitudeType attitudeType;

    private String content;

    private LocalDateTime createTime;

    public enum AttitudeType {

        /**
         * 满意
         */
        SATISFIED(1),

        /**
         * 不满意
         */
        NOT_SATISFIED(2);

        @EnumValue
        public final Integer value;

        AttitudeType(Integer value) {
            this.value = value;
        }
    }
}
