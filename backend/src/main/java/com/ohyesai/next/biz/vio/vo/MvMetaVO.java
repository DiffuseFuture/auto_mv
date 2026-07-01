package com.ohyesai.next.biz.vio.vo;

import com.ohyesai.next.common.enums.ModelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "mv元数据")
@Data
public class MvMetaVO {

    @Schema(description = "mv Id")
    private String mvId;

    @Schema(description = "分镜数据")
    private List<Scene> scenes;

    @Schema(description = "文件 Id")
    private String fileId;

    @Schema(description = "文件 URL")
    private String fileUrl;

    @Data
    public static class Scene {

        @Schema(description = "分镜 Id")
        private String sceneId;

        @Schema(description = "视频提示词")
        private String visualPrompt;

        @Schema(description = "参考图/参考主体")
        private List<FileInfo> subject;

        @Schema(description = "分镜时长/秒（支持毫秒精度小数）")
        private Double duration;

        @Schema(description = "视频文件")
        private FileInfo videoFile;

        @Schema(description = "封面文件")
        private FileInfo coverFile;

        @Schema(description = "模型名称")
        private ModelEnum model;

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
