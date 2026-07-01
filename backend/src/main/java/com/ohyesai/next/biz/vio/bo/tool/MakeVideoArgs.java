package com.ohyesai.next.biz.vio.bo.tool;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.ohyesai.next.common.interfaces.GenCacheKey;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("视频制作工具参数")
public class MakeVideoArgs implements GenCacheKey {

    @Description("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask。")
    private String taskId;

    @Description("""
            本次待处理的分镜列表。遵循增量更新原则：
            1. 仅传入本次需要新增或修改的分镜。
            2. 已确认满意的分镜严禁重复传入，以避免重复计算。
            """)
    private List<Scene> scenes;

    @Description("""
            二次确认标识。
            【重要】平时请勿传入此参数（忽略此参数）。
            仅当系统上一次调用返回 NEED_CONFIRM 错误，且您已获得用户明确同意消耗积分后，方可设为 true 重新发起请求。
            """)
    @JsonProperty(required = false)
    private Boolean confirmed;


    @Override
    public String cacheKey() {
        return String.valueOf(hashCode());
    }

    @Data
    public static class Scene {

        @Description("主体序号，对应脚本中 scene 的 id")
        private String sceneId;

        @Description("""
                操作模式标识：
                1. 修改/重新生成分镜：必须传入该分镜原有的 chunkId，系统将覆盖旧记录（本质是原地修改历史）。
                2. 新增分镜：固定传入 -1，系统将生成一条新的记录。
                """)
        private Integer chunkId;
    }
}
