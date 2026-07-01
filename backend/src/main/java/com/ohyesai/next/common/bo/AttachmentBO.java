package com.ohyesai.next.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件 全局统一接受文件参数
 */
@Data
public class AttachmentBO {

    @Schema(description = "http 二进制文件")
    private MultipartFile file;

}
