package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BillingPointRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String moduleCode;

    private String moduleName;

    /**
     * 计费积分（本次变动积分）
     */
    private Integer bp;

    /**
     * 剩余积分
     */
    private Integer rp;

    private LocalDateTime createTime;
}
