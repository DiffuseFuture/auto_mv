package com.ohyesai.next.biz.vio.bo.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ohyesai.next.common.enums.GenderEnum;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("音频制作工具参数")
public class MakeMusicArgs {

    @Description("歌词标题")
    private String title;

    @Description("自定义的完整歌词。**注意：歌词的字数长短与最终生成的音频时长没有直接线性关系。请勿为了缩短时长而强行删减歌词，这会导致生成效果劣化。**")
    private String lyrics;

    @Description("歌曲风格")
    private String styles;

    @Description("是否为纯音乐（无歌词/无人声）。true代表纯音乐，false代表有歌词的音乐。注意：除非用户在输入中明确要求生成'纯音乐'、'伴奏'或'无人声'，否则请默认传入false。")
    private boolean instrumental;

    @Description("期望的人声性别。仅在用户明确指定了性别要求时才设置此参数；若用户未提及，请勿设置此参数，不要尝试根据语境推测性别。")
    @JsonProperty(required = false)
    private GenderEnum vocalGender;
}
