package com.ohyesai.next.biz.billing.vo;

import com.ohyesai.next.biz.billing.entity.BillingPointRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "查询用户账单参数")
public class QueryBillingRecordVO {

    private String id;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "积分(变动积分,负数为扣除)")
    private Integer bp;

    @Schema(description = "剩余积分")
    private Integer rp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static QueryBillingRecordVO from(BillingPointRecord billingPointRecord) {
        QueryBillingRecordVO queryBillingRecordVO = new QueryBillingRecordVO();
        queryBillingRecordVO.setId(billingPointRecord.getId());
        queryBillingRecordVO.setModuleCode(billingPointRecord.getModuleCode());
        queryBillingRecordVO.setModuleName(billingPointRecord.getModuleName());
        queryBillingRecordVO.setBp(billingPointRecord.getBp());
        queryBillingRecordVO.setRp(billingPointRecord.getRp());
        queryBillingRecordVO.setCreateTime(billingPointRecord.getCreateTime());
        return queryBillingRecordVO;
    }
}
