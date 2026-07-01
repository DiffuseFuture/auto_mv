package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.dto.VioChatDTO;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("图片理解工具参数")
public class UnderstandImageArgs {

    @Description("文件索引(从用户上传的参考图中获取)")
    private Integer index;

    @Description("图片文件 Id（从用户上传的参考图中获取）")
    private String imageFileId;

    @Description("图片的意图标签，由用户在前端选择。枚举值：CHARACTER(人物)、COSTUME(服装)、ENVIRONMENT(环境)、PROP(道具)、STYLE(风格)")
    private VioChatDTO.RefImageIntention intention;

}
