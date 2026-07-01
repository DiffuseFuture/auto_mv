package com.ohyesai.next.biz.vio.bo;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("全局广播工具参数")
public class ReportStatusArgs {

    @Description("行动标题。用于前端进度条的标题。例如：'\uD83D\uDD0D 正在检索数据'、'\uD83C\uDFAC 启动视频渲染'。")
    private String title;

    @Description("详细的行动描述。用于向前端UI呈现内部逻辑：1. 后台处理的思维链概要（Chain of Thought）；2. 接下来要执行的步骤列表。支持 Markdown 格式。注意：绝对不能在此包含向用户提出的问题或对话引导！")
    private String content;

    @Description("消息类型，用于前端根据类型渲染不同的UI组件（'Plan' 规划任务，'Execution' 执行工具）")
    private Type type;

    public enum Type {
        Plan,
        Execution,
//        AskUser,
    }
}
