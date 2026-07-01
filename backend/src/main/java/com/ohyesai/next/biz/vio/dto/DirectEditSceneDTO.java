package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.biz.vio.bo.DoMakeVideoBO;
import com.ohyesai.next.biz.vio.bo.SceneSubject;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.component.FileComponent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "直接编辑分镜视频")
@Data
public class DirectEditSceneDTO {

    @Schema(description = "会话id")
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    // 用于获取历史数据
    @Schema(description = "要编辑的 chunk id")
    @NotNull(message = "chunkId 不能为空")
    private Integer chunkId;

    @Schema(description = "视觉提示词")
    @NotBlank(message = "visualPrompt 不能为空")
    private String visualPrompt;

    @Schema(description = "模型名称")
    @NotNull(message = "modelName 不能为空")
    private ModelEnum model;

    @Schema(description = "参考主体列表")
    private List<ReMakeVideoDTO.FileInfo> subject;

    public DoMakeVideoBO toMakeVideoArgs(Double duration, AspectRatio aspectRatio, Resolution resolution) {
        if(resolution == null){
            resolution = Resolution.P720; // 兼容老数据，给个720P
        }

        DoMakeVideoBO makeVideoArgs = new DoMakeVideoBO();
        makeVideoArgs.setVisualPrompt(visualPrompt);
        makeVideoArgs.setDuration(duration);
        makeVideoArgs.setChunkId(chunkId);
        if (subject != null) {
            List<SceneSubject> newSubjects = subject.stream().map(fileInfo ->
                    new SceneSubject(fileInfo.getSubjectId(), fileInfo.getFileId())
            ).toList();
            makeVideoArgs.setSubject(newSubjects);
        }
        makeVideoArgs.setAspectRatio(aspectRatio);
        makeVideoArgs.setResolution(resolution);
        return makeVideoArgs;
    }
}
