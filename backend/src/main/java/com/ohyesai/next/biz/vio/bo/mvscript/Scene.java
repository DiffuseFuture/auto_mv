package com.ohyesai.next.biz.vio.bo.mvscript;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("单个分镜/镜头脚本")
public class Scene {

    @Description("分镜唯一标识，如 scene_1、scene_2")
    @JsonProperty(required = true)
    private String id;

    @Description("该分镜的起始时间（秒），第1个镜头为0，后续镜头等于前一个镜头的 endTime")
    @JsonProperty(required = true)
    private double startTime;

    @Description("该分镜的结束时间（秒），最后一个镜头的 endTime 等于音频总时长")
    @JsonProperty(required = true)
    private double endTime;

    @Description("该分镜的持续时长（秒），单个镜头不超过10秒，建议3-6秒")
    @JsonProperty(required = true)
    private double duration;

    @Description("该分镜引用的主体 id 列表，如 [\"subject_1\", \"subject_2\"]")
    private List<String> subjectRefs;

    //    @Description("该分镜的 AI 视频生成视觉提示词，必须使用 @subject_id 格式引用主体（如 @subject_1），禁止使用通用名词")
    @Description("""
            该分镜的视频生成提示词，要求：
            1. 必须使用 @subject_id 格式引用主体（如 @subject_1），禁止使用通用名词。
            2. 严禁文字描述画面风格（如写实、动漫等），应使用"按照参考图像的风格"引导模型参考图片风格，文字仅专注于动作和场景。
            """)
    @JsonProperty(required = true)
    private String visualPrompt;

    @Description("true 表示该分镜音频时间窗内有人声，且画面明确包含脸部可见的人物；不确定时必须为 false")
    @JsonProperty(required = true)
    private boolean lipSync;
}
