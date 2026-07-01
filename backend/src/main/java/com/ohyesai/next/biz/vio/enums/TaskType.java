package com.ohyesai.next.biz.vio.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum TaskType {

    /**
     * makeMusic
     */
    MAKE_MUSIC("音乐生成"),

    API_MAKE_MUSIC("API-音乐生成"),

    MAKE_VIDEO("视频生成"),

    /**
     * mergeAudioVideo
     */
    MAKE_MV("MV 制作"),

    /**
     * api make mv
     */
    API_MAKE_MV("API-MV 制作"),

    /**
     * 图片生成
     */
    MAKE_IMAGE("图片生成"),

    /**
     * api make image
     */
    API_MAKE_IMAGE("API-图片生成"),

    /**
     * 对口型
     */
    LIP_SYNC("对口型"),
    ;

    public final String taskName;

    TaskType(String taskName) {
        this.taskName = taskName;
    }
}
