package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.entity.VioProject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MakeMusicResp {

    private String title;

    private String musicFileId;

    private String musicCoverFileId;

    private String musicLyrics;

    private long durationSeconds;

    private String projectId;

    public MakeMusicResp() {
    }
}
