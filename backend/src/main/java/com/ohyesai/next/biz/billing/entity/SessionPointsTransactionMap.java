package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SessionPointsTransactionMap {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String chatSessionId;

    /**
     * points_transaction_log_id
     */
    private Integer pointsTransactionLogId;
}
