package com.ohyesai.next.biz.vio.bo.tool;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MakeImgResp {

    @Schema(description = "主体id")
    private String subjectId;

    @Schema(description = "生成的图片 fileId")
    @JsonIgnore
    private String imageFileId;

    @Schema(description = "消息片段 chunkId，后续重新生成同一主体时必须回传此值")
    private Integer chunkId;

    public MakeImgResp() {
    }

    public MakeImgResp(String imageFileId, Integer chunkId) {
        this.imageFileId = imageFileId;
        this.chunkId = chunkId;
    }

    public MakeImgResp(String subjectId, String imageFileId, Integer chunkId) {
        this.subjectId = subjectId;
        this.imageFileId = imageFileId;
        this.chunkId = chunkId;
    }
}
