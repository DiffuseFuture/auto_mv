package com.ohyesai.next.biz.vio.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CREATE TABLE `chat_message` (
 * `id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
 * `chat_session_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'chat_session_id',
 * `content` json DEFAULT NULL COMMENT '对话内容',
 * `seq_no` int NOT NULL AUTO_INCREMENT COMMENT '序号; 自动递增',
 * `sender_type` int NOT NULL COMMENT '1: user; 2 AI;',
 * `like_state` tinyint NOT NULL COMMENT '0 未点赞；1 点赞； 2 点踩；',
 * `create_time` datetime NOT NULL,
 * PRIMARY KEY (`id`) USING BTREE,
 * UNIQUE KEY `seq_no` (`seq_no`) USING BTREE,
 * KEY `chat_session_id` (`chat_session_id`) USING BTREE
 * ) ENGINE=InnoDB AUTO_INCREMENT=371 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='历史对话消息表，存储全量对话记录';
 */
@Data
public class HistoryMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String chatSessionId;

    private String content;

    /**
     * 递增字段
     */
    private int seqNo;

    private SenderType senderType;

    private LikeState likeState;

    /**
     * 对话回复状态
     * 0 false 未完成
     * 1 true 已完成
     */
    private Boolean finish;

    private LocalDateTime createTime;

    public enum LikeState {

        @Schema(description = "无")
        NONE(0),

        @Schema(description = "喜欢")
        LIKE(1),

        @Schema(description = "不喜欢")
        DISLIKE(2);

        @EnumValue
        public final int value;

        LikeState(int value) {
            this.value = value;
        }
    }

    public enum SenderType {

        USER(1),

        MODEL(2);

        @EnumValue
        public final int value;

        SenderType(int value) {
            this.value = value;
        }
    }
}
