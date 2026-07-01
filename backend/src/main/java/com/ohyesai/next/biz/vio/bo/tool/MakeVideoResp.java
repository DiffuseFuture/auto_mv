package com.ohyesai.next.biz.vio.bo.tool;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MakeVideoResp {

    @Schema(description = "场景序号，对应脚本中 scene 的 id; 回声字段")
    private String sceneId;

    @Schema(description = "生成的视频 fileId")
    @JsonIgnore
    private String videoFileId;

    @Schema(description = "消息片段 chunkId，后续重新生成同一分镜时必须回传此值")
    private Integer chunkId;

    public MakeVideoResp(String videoFileId, Integer chunkId) {
        this.videoFileId = videoFileId;
        this.chunkId = chunkId;
    }

    public MakeVideoResp(String sceneId, String videoFileId, Integer chunkId) {
        this.sceneId = sceneId;
        this.videoFileId = videoFileId;
        this.chunkId = chunkId;
    }
}
