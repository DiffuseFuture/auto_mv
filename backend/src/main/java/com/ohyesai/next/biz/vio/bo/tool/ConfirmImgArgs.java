package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("确认图片参数")
public class ConfirmImgArgs {

    private List<Img> imgs;

    @Data
    public static class Img{
        @Description("文件索引；文件的id")
        private String imageIdx;

        @Description("图片 fileId")
        private String fileId;

        @Description("图片描述")
        private String prompt;

        @Description("参考图 fileId ")
        private List<String> refImageFileId;

        @Description("图片类型：character（角色）、object（物品）、environment（环境场景）")
        private SseMsgBO.Subject.SubjectType type;

    }
}
