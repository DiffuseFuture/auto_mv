package com.ohyesai.next.biz.vio.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;


@Data
@Description("合并音频和视频参数")
public class MergeAudioVideoArgs {

    @Description("须从 initTask 返回或历史记录中提取，严禁伪造；若无有效ID请先调用 initTask")
    private String taskId;

    @Description("视频文件名称，结合上下文进行总结")
    private String videoFileName;

    @Description("是否需要嘴形同步, 该操作为付费操作，仅当用户明确要求嘴形同步时才传入true")
    @JsonProperty(required = false)
    private Boolean lipSync;

    @Description("""
            仅当 lipSync = true 时该参数生效。用于控制是否复用历史已生成的嘴形同步缓存：
            - 传入 true：【复用缓存】。适用于用户仅想修改/重新生成字幕或背景音，但希望“保持上次生成的嘴形效果不变”或“避免重复扣费”的场景。
            - 传入 false：【重新生成】。适用于“首次为该视频进行嘴形同步”，或“用户明确对之前的口型效果不满意，要求重新对嘴形”的场景。
            【决策指引】：若用户是首次对该视频对口型，或未表达过“保留/复用上次口型”的意图，Agent 应默认传入 false。
            """)
    @JsonProperty(required = false)
    private Boolean lipSyncCache;

    @Description("是否添加字幕，true 为添加字幕，默认不添加。注意：如果 subtitleReference 有值，此项必须设为 true。")
    @JsonProperty(required = false)
    private Boolean subtitleFlag;

    @Description("""
            辅助生成字幕的参考内容。可接受任何格式的辅助文本（如：原始歌词、视频剧本、提示词方案、翻译稿或大纲等，格式不限）。
            注意：
            1. 仅在用户【本次发言直接给出新文本】时填写。若仅命令“开启字幕”但未给文本，【必须留空(null)】。
            2. 严禁从任何历史上下文（包括往轮对话历史、历史任务数据等）中捞取旧文本。
            3. 如果填写了此字段，请务必将 subtitleFlag 设为 true。
            """)
    @JsonProperty(required = false)
    private String subtitleReference;

    @Description("""
            二次确认标识。
            【重要】平时请勿传入此参数（忽略此参数）。
            仅当系统上一次调用返回 NEED_CONFIRM 错误，且您已获得用户明确同意消耗积分后，方可设为 true 重新发起请求。
            """)
    @JsonProperty(required = false)
    private Boolean confirmed;
}
