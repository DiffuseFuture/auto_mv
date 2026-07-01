package com.ohyesai.next.biz.vio.bo.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ohyesai.next.common.enums.AspectRatio;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("mv 脚本参数")
@Data
public class MvScriptPlanArgs {

    @Description("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask。")
    private String taskId;

    @Description("音频文件 Id; 文件时长必须小于 300 秒")
    private String audioFileId;

    @Description("音频文件歌词。如果该音频没有歌词，请务必传入忽略属性，不要编造内容")
    @JsonProperty(required = false)
    private String audioLyrics;

    @Description("风格")
    private String style;

    // 新增字段：用于接收用户五花八门的具体要求
    @Description("""
            用户对 MV 内容的自定义要求或额外约束（可选）。
            当用户在对话中提出了对人物角色（如性别/年龄/数量）、故事情节、镜头表现、画面元素限制等具体要求时，请将这些要求提取并总结到此字段。
            示例1：用户说"生成个全是男性的mv"，此字段填入"所有出场人物必须均为男性"。
            示例2：用户说"想要一个关于猫咪流浪的悲伤故事"，此字段填入"主角是一只流浪猫，整体故事氛围悲伤"。
            示例3：用户说"不要出现任何现代交通工具"，此字段填入"画面中禁止出现现代交通工具"。
            如果用户没有提出具体的定制要求，请传空字符串。
            """)
    @JsonProperty(required = false)
    private String additionalRequirements;

//    @Description("""
//            音频节拍分析数据（可选，由音频分析服务提供）。
//            当可用时，包含 JSON 格式的节拍分析的原始结果。
//            无分析数据时不传或传空字符串。
//            """)
//    @JsonProperty(required = false)
//    private String beatAnalysis;

    //    @Description("用户上传的参考图描述信息（JSON格式），包含每张图的意图和内容摘要")

    @Description("""
            用户上传的参考图上下文信息（可选，无参考图时不传或传空字符串）。
            此字段的值来自 understandImage 工具的返回结果拼装，每张参考图占一行，格式为：
            [INTENTION] description (fileId: xxx)
            
            示例：
            [CHARACTER] 年轻亚洲女性，圆脸，大眼睛，黑色波波头短发，皮肤白皙，身穿白色连衣裙。 (fileId: mv/202603/abc.jpg)
            [ENVIRONMENT] 黄昏时分的埃菲尔铁塔，天空呈暖橙色，塞纳河倒映着光影。 (fileId: mv/202603/def.jpg)
            [STYLE] 赛博朋克霓虹美学，高饱和的品红与青色，雨水浸湿的路面反光。 (fileId: mv/202603/ghi.jpg)
            
            其中 INTENTION 为意图标签枚举：CHARACTER(人物)、COSTUME(服装)、ENVIRONMENT(环境)、PROP(道具)、STYLE(风格)。
            description 为 understandImage 返回的中文视觉描述。
            fileId 为原始图片文件Id，后续 makeImg 时需要用 fileId 获取图片 URL 传入 refImageUrls。
            """)
    @JsonProperty(required = false)
    private String refImageContext;

    @Description("画面比例")
    private AspectRatio aspectRatio;

}
