package com.ohyesai.next.biz.vio.bo.tool;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * 工具的统一返回值格式，使用 xml 序列化
 */
@Data
public class ToolResp {

    /**
     * 如何使用返回值的提示信息
     */
    @JacksonXmlProperty(localName = "guidance")
    private String guidance;

    /**
     * 工具实际返回值
     */
    @JacksonXmlProperty(localName = "result")
    private String result;

    public ToolResp(String result, String guidance) {
        this.guidance = guidance;
        this.result = result;
    }
}
