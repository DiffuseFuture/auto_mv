package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "直接编辑主体参考图")
@Data
public class DirectEditSubjectDTO {

    @Schema(description = "会话id")
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @Schema(description = "要编辑的 chunk id")
    @NotNull(message = "chunkId 不能为空")
    private Integer chunkId;

    @Schema(description = "图片生成提示词")
    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    @Schema(description = "参考图 fileId 列表")
    private List<String> refImageFileIds;
}
