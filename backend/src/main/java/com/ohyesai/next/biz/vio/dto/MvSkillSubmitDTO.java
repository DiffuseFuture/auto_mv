package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.common.enums.AspectRatio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "工具执行提交参数")
public class MvSkillSubmitDTO {

    @Schema(description = "音频文件id")
    @NotNull(message = "音频文件不能为空")
    private String audioFileId;

    @Schema(description = "风格")
    @NotNull(message = "风格不能为空")
    private String style;

    @Schema(description = "画面比例")
    @NotNull(message = "画面比例不能为空")
    private AspectRatio aspectRatio;
}
