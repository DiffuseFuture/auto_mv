package com.ohyesai.next.biz.vio.entity;

import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.enums.ModelEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VioProject {

    private String id;

    private String userId;

    private String sessionId;

    private String messageId;

    /**
     * 项目名称
     */
    private String projectName;

    private String args;

    private String fileId;

    private String previewFileId;

    /**
     * 媒体时长
     */
    private Integer mediaDuration;

    /**
     * 歌词
     */
    private String lyrics;

    private TaskType taskType;

    /**
     * 是否显示项目
     */
    private boolean showProject;

    /**
     * 使用的模型名称
     */
    private ModelEnum modelName;

    private LocalDateTime createTime;
}
