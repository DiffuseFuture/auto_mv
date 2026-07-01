package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新场景脚本")
public class UpdateSceneScriptDTO {

    @Schema(description = "chunk id")
    @NotNull(message = "chunkId 不能为空")
    private Integer chunkId;

    @Schema(description = "要修改的脚本索引")
    @NotNull(message = "scriptIdx 不能为空")
    private Integer scriptIdx;

    @Schema(description = "主体参考图")
    private List<SseMsgBO.SceneScript.SubjectRef> subjectRefs;

    @Schema(description = "视频提示词")
    private String visualPrompt;

}
