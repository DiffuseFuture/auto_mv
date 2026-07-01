package com.ohyesai.next.biz.vio.agent;

import com.ohyesai.next.biz.vio.bo.ReportStatusArgs;
import com.ohyesai.next.biz.vio.helper.SseMessageHelper;
import com.ohyesai.next.biz.vio.service.VioService;
import com.ohyesai.next.trace.TraceInterceptor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class VioBaseTool {

    @Tool("""
            【后台状态广播工具】
            用于向前端 UI 汇报 Agent 的后台执行计划或异步任务的进度状态。
            在调用任何耗时的功能性工具（如视频/图片/脚本生成）之前，必须并发调用此工具来刷新界面的 Loading 状态。
            【🚫警告】：这不是对话通道！严禁将“向用户的提问”、“索要参数”或“寒暄”写入此工具，与用户的对话请直接使用标准的自然语言文本输出！
            """)
    public void reportStatus(ReportStatusArgs reportStatusArgs, InvocationParameters parameters) {
        try (var _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, parameters.get(VioService.TRACE_ID))) {
            if (reportStatusArgs == null) {
                log.warn("reportStatus 传入参数为null，不执行任何动作");
                return;
            }

            SseMessageHelper sseMessageHelper = parameters.get(VioService.SSE_HELPER);
            String content = String.join("\n> ", reportStatusArgs.getContent().split("\n"));
            sseMessageHelper.ofText("""
                    
                    **%s: %s**
                    > %s
                    
                    ----
                    
                    """.formatted(reportStatusArgs.getType(), reportStatusArgs.getTitle(), content));
        }
    }

}
