package com.ohyesai.next.biz.billing.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "支付参数")
public class TransactionsDTO {

    @Schema(description = "套餐编码")
    @NotBlank
    private String tierCode;

    @Schema(description = "支付方式")
    @NotNull
    private PayKind payKind;

    public enum PayKind {
        ALI_PC,
        WX_PC,
        WX_MP,
    }

}
