package com.ohyesai.next.biz.vio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "音乐制作参数")
@Data
public class MusicSkillDTO {

    @NotBlank
    @Schema(description = "歌曲标题")
    private String title;

    @Schema(description = "用户提示词")
    @NotBlank
    private String prompt;

    @NotBlank
    @Schema(description = "歌曲风格")
    private String styles;

    @NotNull
    @Schema(description = "是否为纯音乐（无歌词/无人声）。true代表纯音乐，false代表有歌词的音乐")
    private Boolean instrumental;


}
