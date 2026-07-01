package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.enums.ModelEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * String fileId,
 * String previewFileId,
 * String projectName,
 * Integer duration,
 * Object args,
 * TaskType fromTool,
 * String userId,
 * String sessionId,
 * String messageId
 */
@Builder
@Getter
public class SaveVioProjectBO {

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 预览文件id
     */
    private String previewFileId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 视频文件 时长
     */
    private Integer duration;

    /**
     * 音频文件歌词
     */
    private String lyrics;

    /**
     * 参数
     */
    private Object args;

    /**
     * 任务类型
     */
    private TaskType taskType;

    /**
     * 用户id
     */
    private String userId;

    /**
     * session id
     */
    private String sessionId;

    /**
     * 消息id
     */
    private String messageId;

    /**
     * 模型
     */
    private ModelEnum model;

}
