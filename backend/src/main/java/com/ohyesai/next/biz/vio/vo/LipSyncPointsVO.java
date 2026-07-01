package com.ohyesai.next.biz.vio.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "对口型积分明细")
@Data
public class LipSyncPointsVO {

    @Schema(description = "总积分")
    private Integer totalPoints;

    @Schema(description = "总时长")
    private Integer totalDuration;

    @Schema(description = "分镜明细")
    private List<Item> scenes;


    @Data
    public static class Item {
        @Schema(description = "分镜Id")
        private String sceneId;

        @Schema(description = "时长/秒")
        private Integer duration;

        @Schema(description = "积分")
        private Integer points;
    }
}
