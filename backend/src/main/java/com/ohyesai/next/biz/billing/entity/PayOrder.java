package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import com.ohyesai.next.common.enums.StateEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayOrder {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String tradeNo;

    private String tierCode;

    private StateEnum state;

    private int amount;

    private LocalDateTime createTime;
}
