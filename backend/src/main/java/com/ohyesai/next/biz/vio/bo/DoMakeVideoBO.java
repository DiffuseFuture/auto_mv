package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DoMakeVideoBO {

    @Schema(description = "场景序号，对应脚本中 scene 的 id")
    private String sceneIdx;

    @Schema(description = "画面描述。**关键规则**：若要保持角色一致性，必须在描述中使用 `@subject_id` 的格式引用主体（例如：'@hero_01 哭泣的特写'）")
    private String visualPrompt;

    @Schema(description = "时长（秒，支持小数如 4.350），不超过 10 秒。精确到毫秒以对齐音乐节拍。")
    private Double duration;

    @Schema(description = "该分镜的起始时间（秒）")
    private Double startTime;

    @Schema(description = "该分镜的结束时间（秒）")
    private Double endTime;

    @Schema(description = "可选的主体列表。包含 `id`（如 'hero_01'）和 `imageFileId`（参考图id）")
    private List<SceneSubject> subject;

    @Schema(description = "（已有的消息片段 chunkId。重新生成同一分镜时，传入之前 makeVideo 返回的 chunkId，新版本会追加到该记录中；不存在则传入 -1")
    private Integer chunkId;

    @Schema(description = "画面比例")
    private AspectRatio aspectRatio;

    @Schema(description = "画面分辨率")
    private Resolution resolution;

    public static DoMakeVideoBO from(ChatSessionTaskPayload.PayloadScene payloadScene,
                                     AspectRatio aspectRatio,
                                     Resolution resolution,
                                     Integer chunkId){
        DoMakeVideoBO doMakeVideoBO = new DoMakeVideoBO();
        doMakeVideoBO.setSceneIdx(payloadScene.getId());
        doMakeVideoBO.setVisualPrompt(payloadScene.getVisualPrompt());
        doMakeVideoBO.setDuration(payloadScene.getDuration());
        doMakeVideoBO.setStartTime(payloadScene.getStartTime());
        doMakeVideoBO.setEndTime(payloadScene.getEndTime());
        doMakeVideoBO.setSubject(payloadScene.getSceneSubjects());
        doMakeVideoBO.setChunkId(chunkId);
        doMakeVideoBO.setAspectRatio(aspectRatio);
        doMakeVideoBO.setResolution(resolution);

        return doMakeVideoBO;
    }
}
