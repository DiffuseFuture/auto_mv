package com.ohyesai.next.biz.vio.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
public class SubtitleTranscribed {
    @Description("音频时长（秒）")
    private double duration;

    @Description("字幕行列表，按时间顺序排列")
    @JsonProperty(required = true)
    private List<SubtitleLine> lines;

    @Data
    @Description("单行字幕，包含起止时间和文本")
    public static class SubtitleLine {

        @Description("该行字幕的起始时间（秒），精确到毫秒，如 1.500")
        @JsonProperty(required = true)
        private double startTime;

        @Description("该行字幕的结束时间（秒），精确到毫秒，如 3.200")
        @JsonProperty(required = true)
        private double endTime;

        @Description("该时间段内的转录文本内容")
        @JsonProperty(required = true)
        private String text;

        public SubtitleLine() {
        }

        public SubtitleLine(double startTime, double endTime, String text) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.text = text;
        }


        public SubtitleLine(SubtitleLine subtitleLine) {
            this.startTime = subtitleLine.startTime;
            this.endTime = subtitleLine.endTime;
            this.text = subtitleLine.text;
        }
    }
}
