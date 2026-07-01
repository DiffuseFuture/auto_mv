package com.ohyesai.next.biz.vio.bo.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * 该数据结构会存储到 vioProject.args字段中
 */
@Data
public class DoMergeAudioVideoBO {

    @Schema(description = "session task id")
    private Integer chatSessionTaskId;

    @Schema(description = "视频文件名称，结合上下文进行总结")
    private String videoFileName;

    @Schema(description = "视频文件 Id 集合")
    private List<String> videoFileIds;

    @Schema(description = "音频文件 Id")
    private String audioFileId;

    @Schema(description = "是否开启字幕")
    private Boolean subtitleFlag;

    @Schema(description = "字幕参考内容")
    private String subtitleReference;

}
