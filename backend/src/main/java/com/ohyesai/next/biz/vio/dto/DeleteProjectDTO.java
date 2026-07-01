package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "删除项目参数")
@Data
public class DeleteProjectDTO {

    @Schema(description = "项目id")
    @NotBlank(message = "项目id不能为空")
    private String projectId;
}
