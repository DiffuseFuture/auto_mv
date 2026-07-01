package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.biz.vio.bo.DoMakeVideoBO;
import com.ohyesai.next.biz.vio.bo.SceneSubject;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "mv元数据")
@Data
public class ReMakeVideoDTO {

//    @Schema(description = "chatSessionId")
//    @NotBlank(message = "chatSessionId 不能为空")
//    private String chatSessionId;

    @Schema(description = "mv Id")
    @NotBlank(message = "mv Id 不能为空")
    private String mvId;

    @Schema(description = "分镜数据")
    @NotNull(message = "scenes 不能为空")
    private List<Scene> scenes;

    @Data
    public static class Scene {

        @Schema(description = "模型名称")
        @NotNull(message = "模型名称 不能为空")
        private ModelEnum model;

        @Schema(description = "是否重新生成")
        private boolean reMake = false;

        @Schema(description = "分镜 Id")
        private String sceneId;

        @Schema(description = "视频提示词")
        private String visualPrompt;

        @Schema(description = "参考图/参考主体")
        private List<FileInfo> subject;

        @Schema(description = "分镜时长（秒，支持毫秒精度），为空时使用原始时长")
        private Double duration;

        public DoMakeVideoBO toMakeVideoArgs(Double duration, AspectRatio aspectRatio, Resolution resolution) {
            if (resolution == null) {
                resolution = Resolution.P720; // 兼容老数据，给个720P
            }

            DoMakeVideoBO makeVideoArgs = new DoMakeVideoBO();
            makeVideoArgs.setVisualPrompt(visualPrompt);
            makeVideoArgs.setDuration(duration);
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

    @Data
    public static class FileInfo {

        @Schema(description = "subject id")
        private String subjectId;

        @Schema(description = "文件 Id")
        private String fileId;

        @Schema(description = "文件 URL")
        private String fileUrl;

        public FileInfo() {
        }

        public FileInfo(String subjectId, String fileId, String fileUrl) {
            this.subjectId = subjectId;
            this.fileId = fileId;
            this.fileUrl = fileUrl;
        }
    }
}
