package com.ohyesai.next.biz.vio.bo.mvscript;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ohyesai.next.common.enums.AspectRatio;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("MV分镜脚本，包含音频分析摘要、主体定义和分镜列表")
public class Script {

    @Description("简短描述风格、BPM及剧情大纲")
    @JsonProperty(required = true)
    private String analysisSummary;

    @Description("音频总时长校验值（秒），必须等于所有分镜 duration 之和")
    @JsonProperty(required = true)
    private int totalDurationSeconds;

    @Description("分镜总数")
    @JsonProperty(required = true)
    private int sceneCount;

    @Description("剧情中的关键主体定义列表（角色/物品/环境），最多7个")
    @JsonProperty(required = true)
    private List<Subject> subjects;

    @Description("分镜列表，按时间轴顺序排列，时间连续且总时长等于音频时长")
    @JsonProperty(required = true)
    private List<Scene> scenes;

    @Description("画面比例")
    @JsonProperty(required = true)
    private AspectRatio aspectRatio;
}
