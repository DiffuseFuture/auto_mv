package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "重命名参数")
@Data
public class RenameDTO {

    @Schema(description = "session id")
    @NotBlank(message = "sessionId不能为空")
    private String sessionId;

    @Schema(description = "新名称name")
    @NotBlank(message = "名称不能为空")
    private String name;
}
