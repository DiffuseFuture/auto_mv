package com.ohyesai.next.biz.vio.bo.tool;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.ohyesai.next.common.interfaces.GenCacheKey;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("图片工具参数")
public class MakeImgArgs implements GenCacheKey {

    @Description("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask")
    private String taskId;

    @Description("""
            本次待处理的主体列表。遵循增量更新原则：
            1. 仅传入本次需要新增或修改的主体。
            2. 已确认满意的主体严禁重复传入，以避免重复计算。
            """)
    private List<Subject> subjects;

    @Description("""
            二次确认标识。
            【重要】平时请勿传入此参数（忽略此参数）。
            仅当系统上一次调用返回 NEED_CONFIRM 错误，且您已获得用户明确同意消耗积分后，方可设为 true 重新发起请求。
            """)
    @JsonProperty(required = false)
    private Boolean confirmed;


    @Data
    public static class Subject {

        @Description("主体序号，对应脚本中 subject 的 id")
        private String subjectId;

        @Description("""
                操作模式标识：
                1. 修改/重新生成主体：必须传入该主体原有的 chunkId，系统将覆盖旧记录（本质是原地修改历史）。
                2. 新增主体：固定传入 -1，系统将生成一条新的记录。
                """)
        private Integer chunkId;
    }

    @Override
    public String cacheKey() {
        return String.valueOf(hashCode());
    }
}
