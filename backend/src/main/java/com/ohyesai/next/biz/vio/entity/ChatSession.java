package com.ohyesai.next.biz.vio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ohyesai.next.common.enums.Resolution;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CREATE TABLE `chat_session` (
 * `id` char(32) NOT NULL,
 * `user_id` char(32) NOT NULL,
 * `name` varchar(200) NOT NULL COMMENT '对话名称',
 * `create_time` datetime NOT NULL COMMENT '创建日期',
 * PRIMARY KEY (`id`),
 * KEY `user_id` (`user_id`) USING BTREE
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对话表';
 */
@Data
@TableName(autoResultMap = true)
public class ChatSession {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String name;

    /**
     * 对话封面 文件id
     */
//    @Deprecated
//    private String cover;

    /**
     * 元数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Metadata metadata;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;


    @Data
    public static class Metadata {

        /**
         * 分辨率
         */
        private Resolution resolution;

        /**
         * 对话封面 文件id
         */
        private String cover;
    }
}
