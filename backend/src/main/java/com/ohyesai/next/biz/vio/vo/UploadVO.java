package com.ohyesai.next.biz.vio.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class UploadVO {
    @Schema(description = "文件id")
    private String fileId;

    @Schema(description = "文件url")
    private String fileUrl;

    public UploadVO(String fileId, String fileUrl) {
        this.fileId = fileId;
        this.fileUrl = fileUrl;
    }
}
