package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "重命名项目DTO")
@Data
public class RenameProjectDTO {
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目新名称")
    private String projectName;
}
