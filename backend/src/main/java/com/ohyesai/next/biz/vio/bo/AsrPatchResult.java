package com.ohyesai.next.biz.vio.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

@Description("asr patch 修正结果")
public record AsrPatchResult(

        @Description("所有需要修正的行列表。如果没有任何行需要修改，则返回空列表。")
        @JsonProperty(required = true)
        List<AsrPatchResult.AsrPatch> patches
) {

    @Description("asr patch 单条修改记录")
    public record AsrPatch(

            @Description("对应的行 id")
            @JsonProperty(required = true)
            int id,

            @Description("修改后的正确文本")
            @JsonProperty(required = true)
            String text
    ) {
    }
}
