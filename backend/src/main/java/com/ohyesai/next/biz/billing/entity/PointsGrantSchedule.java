package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CREATE TABLE `points_grant_schedule` (
 *   `id` int NOT NULL AUTO_INCREMENT,
 *   `user_id` char(32) NOT NULL,
 *   `trade_no` varchar(64) NOT NULL COMMENT '关联的年费充值订单号; payOrder 表字段',
 *   `period_index` int NOT NULL COMMENT '期数 (例如：1到12)',
 *   `points_to_grant` int NOT NULL COMMENT '本期应发积分数量',
 *   `expected_grant_date` date NOT NULL COMMENT '预计发放日期 (如: 2026-03-15)',
 *   `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待发放), GRANTED(已发放)',
 *   `actual_grant_time` datetime DEFAULT NULL COMMENT '实际发放成功的时间',
 *   `created_at` datetime NOT NULL,
 *   PRIMARY KEY (`id`),
 *   KEY `idx_status_date` (`status`,`expected_grant_date`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='包年套餐按月积分发放计划表';
 */
@Data
public class PointsGrantSchedule {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 关联的年费充值订单号; payOrder 表字段
     */
    private String tradeNo;

    /**
     * 关联的订阅计划
     */
    private String tierCode;

    /**
     * 期数 (例如：1到12)
     */
    private Integer periodIndex;

    /**
     * 本期应发积分数量
     */
    private Integer pointsToGrant;

    /**
     * 预计发放日期 (如: 2026-03-15)
     */
    private LocalDateTime expectedGrantDate;

    /**
     * 状态: PENDING(待发放), GRANTED(已发放)
     */
    private Status status;

    /**
     * 实际发放成功的时间
     */
    private LocalDateTime actualGrantTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    public enum Status {
        /**
         * 待发放
         */
        PENDING,

        /**
         * 已发放
         */
        GRANTED,

        /**
         * 废弃；不在发放
         */
        ABANDONED
    }
}
