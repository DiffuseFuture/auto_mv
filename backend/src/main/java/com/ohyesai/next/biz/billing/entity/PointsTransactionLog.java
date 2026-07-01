package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsTransactionLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 交易类型
     */
    private PointsOperation transactionType;

    /**
     * 变动数量 (正数或负数)
     */
    private Integer amount;

    /**
     * 变动后的余额
     */
    private Integer balanceAfter;

    /**
     * 描述 (如：开通基础版月卡，或 调用Mureka O2生成音乐)
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

//    /**
//     * RECHARGE(充值), DEDUCT(扣减), SYSTEM_GRANT(系统赠送)
//     */
//    public enum TransactionType {
//        /**
//         * 充值
//         */
//        RECHARGE,
//
//        /**
//         * 扣减
//         */
//        DEDUCT,
//
//        /**
//         * 系统赠送
//         */
//        SYSTEM_GRANT
//    }

}
