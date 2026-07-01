package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.agent.VioAssistant;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {
 * "description": "详细的中文视觉描述...",
 * "matchScore": "HIGH | MEDIUM | LOW",
 * "suggestion": "仅当 matchScore 为 LOW 时填写更合适的意图标签（如 ENVIRONMENT），否则为 null"
 * }
 */
@Data
@Description("图片理解返回值")
@EqualsAndHashCode(callSuper = true)
public class UnderstandImageResp extends VioAssistant.Gemini3_1ProAssistant.UnderstandImage {

    @Description("序号")
    private Integer index;

}
