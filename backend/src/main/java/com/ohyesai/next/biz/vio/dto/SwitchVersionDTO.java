package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "切换版本")
@Data
public class SwitchVersionDTO {

    @Schema(description = "chunk id")
    @NotNull(message = "chunkId 不能为空")
    private Integer chunkId;

    @Schema(description = "要切换到的版本号")
    @NotNull(message = "activeVersion 不能为空")
    private Integer activeVersion;
}
