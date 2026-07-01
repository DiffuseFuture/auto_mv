package com.ohyesai.next.biz.vio.bo;

import lombok.Data;

/**
 * 分镜使用的主体
 */
@Data
public class SceneSubject {

    private String id;

    private String fileId;

    public SceneSubject() {
    }

    public SceneSubject(String id, String fileId) {
        this.id = id;
        this.fileId = fileId;
    }
}
