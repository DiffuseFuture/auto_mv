package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.bo.mvscript.Subject;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("主体增量更新工具参数")
public class UpdateSubjectArgs {

    @Description("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask。")
    private String taskId;

    @Description("更新类型：ADD-新增，UPDATE-更新/修改，DELETE-删除")
    private UpdateType updateType;

    @Description("""
            目标标识 传入 subject id（如 "subject_1"）
            - 当 updateType 为 UPDATE 或 DELETE
              - 代表要更新的主体
            - 当 updateType 为 ADD
              - 传入 null 或空字符串：追加到列表末尾
              - 传入目标 id（如 "subject_1"）：插入到该主体之前
            """)
    private String subjectId;

    @Description("新增或更新的主体数据，updateType 为 DELETE 时可传 null")
    private Subject subject;
}
