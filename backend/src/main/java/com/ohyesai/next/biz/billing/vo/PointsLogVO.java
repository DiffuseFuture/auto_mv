package com.ohyesai.next.biz.billing.vo;

import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Schema(description = "积分日志")
@Data
public class PointsLogVO {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 交易类型：RECHARGE(充值), DEDUCT(扣减), SYSTEM_GRANT(系统赠送)
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
    private ZonedDateTime createTime;

    public static PointsLogVO from(PointsTransactionLog pointsTransactionLog) {
        PointsLogVO pointsLogVO = new PointsLogVO();
        pointsLogVO.setId(pointsTransactionLog.getId());
        pointsLogVO.setTransactionType(pointsTransactionLog.getTransactionType());
        pointsLogVO.setAmount(pointsTransactionLog.getAmount());
        pointsLogVO.setBalanceAfter(pointsTransactionLog.getBalanceAfter());
        pointsLogVO.setDescription(pointsTransactionLog.getDescription());
        pointsLogVO.setCreateTime(ZonedDateTime.of(pointsTransactionLog.getCreateTime(), ZoneId.systemDefault()));
        return pointsLogVO;
    }
}
