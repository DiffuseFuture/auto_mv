package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户积分明细 (控制过期与扣减顺序)
 */
@Data
public class UserPointsBatch {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String userId;

    private PointsOperation pointsType;

    /**
     * 获取的总积分数
     */
    private Integer totalAmount;

    /**
     * 当前剩余可用积分数
     */
    private Integer remainingAmount;

    /**
     * 过期时间 (永不过期为超大时间 2126-05-25 00:00:00)
     */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
