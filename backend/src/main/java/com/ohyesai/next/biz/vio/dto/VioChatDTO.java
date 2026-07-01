package com.ohyesai.next.biz.vio.dto;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.component.FileComponent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Vio Chat DTO")
public class VioChatDTO {

    @Schema(description = "聊天id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String sessionId;

    @Schema(description = "prompt", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "prompt不能为空")
    private String prompt;

    @Schema(description = "选择使用的模型", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotNull(message = "model不能为空")
    private ModelEnum model;

    @Schema(description = "分辨率; 同一个session首次对话时必填")
    private Resolution resolution;

    @Schema(description = "mp3文件id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String audioFileId;

    @Schema(description = "audioFileId 对应的歌词", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String audioLyrics;

    @Schema(description = "主体图片 文件id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<RefImage> subjectImgs;

    @Schema(description = "对口型", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean lipSync;

    @Schema(description = "用户编辑摘要（前端在用户确认继续时传入，描述 PAUSE 期间的编辑操作）")
    private String editContext;

    @Schema(description = "加字幕", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean subtitle;

    // 新增内部类
    @Data
    public static class RefImage {
        @Schema(description = "图片文件id")
        private String fileId;

        @Schema(description = "意图标签: CHARACTER(人物), COSTUME(服装), ENVIRONMENT(环境), PROP(道具), STYLE(风格)")
        private RefImageIntention intention;
    }

    // 新增枚举
    public enum RefImageIntention {
        CHARACTER,   // 人物
        COSTUME,     // 服装
        ENVIRONMENT, // 环境
        PROP,        // 道具
        STYLE        // 风格
    }

    /**
     * 转为SSE消息
     *
     * @param fileComponent
     * @return
     */
    public SseMsgBO<SseMsgBO.Text> toSseMsgBO(FileComponent fileComponent) {
        List<SseMsgBO.Img> imgs = null;
        if (CollUtil.isNotEmpty(getSubjectImgs())) {
            imgs = getSubjectImgs().stream().map(img -> new SseMsgBO.Img(img.fileId, fileComponent.shareUrl(img.fileId))).toList();
        }

        List<SseMsgBO.Audio> audios = null;
        if (StrUtil.isNotBlank(getAudioFileId())) {
            audios = List.of(new SseMsgBO.Audio(
                    null,
                    null,
                    getAudioLyrics(),
                    getAudioFileId(),
                    fileComponent.shareUrl(getAudioFileId()),
                    null,
                    null,
                    null
            ));
        }
        return SseMsgBO.ofText(prompt, imgs, audios);
    }
}
