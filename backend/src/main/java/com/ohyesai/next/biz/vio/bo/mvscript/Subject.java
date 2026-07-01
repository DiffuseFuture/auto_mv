package com.ohyesai.next.biz.vio.bo.mvscript;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("剧情中的关键主体，包括角色、物品或核心环境场景")
public class Subject {

    @Description("主体唯一标识，如 subject_1、subject_2")
    @JsonProperty(required = true)
    private String id;

    @Description("主体类型：character（角色）、object（物品）、environment（环境场景）")
    @JsonProperty(required = true)
    private SubjectType type;

    @Description("""
            主体的详尽定义，用于生成纯净的视觉参考图。
            注意：以下语句中出现的占位符 “<style_ref>” 必须替换由用户描述中指定的风格（如 “y2k风格,像素/低保真”），不可留空。
            
            1. character (角色):
            描述其形态特征（物种、体型）、视觉细节（毛色/肤质、面部/头部特征）及外部装束（服饰、配饰、或功能性构件）。必须包含：‘<style_ref> 角色展示图，极致纯净背景，单体聚焦，排除任何非主体生物或环境干扰’。
            
            2. object (物体):
            描述材质、几何形体与纹理细节。必须包含：‘<style_ref> 独立物件视角，极致纯净背景。严禁出现任何交互行为、严禁出现任何依附载体（如生物、手部或底座），仅呈现物件本体的静态结构’。
            
            3. environment (环境):
            描述空间格局、建筑特征与光影感。必须包含：‘<style_ref> 空场景摄影，无人值守感。排除一切动态生物干扰，仅聚焦于静态空间的比例、材质与光影氛围’。
            """)
    @JsonProperty(required = true)
    private String description;

    @Description("关联的用户参考图 fileId 列表，来自 refImageContext；无关联参考图时为空数组")
    private List<String> refImgs;

    public enum SubjectType {
        character,   // 角色
        object,      // 物体
        environment  // 环境
    }
}
