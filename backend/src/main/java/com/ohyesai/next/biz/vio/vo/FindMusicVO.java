package com.ohyesai.next.biz.vio.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.ZonedDateTime;

@Schema(description = "音乐资源返回值")
@Data
public class FindMusicVO {

    @Schema(description = "所属会话id")
    private String sessionId;

    @Schema(description = "所属会话下的消息id")
    private String messageId;

    @Schema(description = "项目id")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "文件id")
    private String fileId;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件封面id")
    private String fileCoverId;

    @Schema(description = "文件封面地址")
    private String fileCoverUrl;

    @Schema(description = "创建时间")
    private ZonedDateTime createTime;
}
