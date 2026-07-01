package com.ohyesai.next.biz.vio.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 音频节拍分析结果 — 与 Python mv-audio-analyzer 服务的 BeatTimeline schema 对齐。
 */
@Data
public class BeatTimeline {

    @JsonProperty("audio_id")
    private String audioId;

    @JsonProperty("duration_sec")
    private double durationSec;

    /**
     * BPM (beats per minute)
     */
    private double bpm;

    /**
     * 所有节拍时间点（秒）
     */
    private List<Double> beats;

    /**
     * 强拍（每小节首拍）时间点（秒）
     */
    private List<Double> downbeats;

    /**
     * 音乐段落结构
     */
    private List<Segment> segments;

    @Data
    public static class Segment {
        private String label;
        private double start;
        private double end;
    }

    /**
     * 每小节时长（秒），假设 4/4 拍
     */
    public double barLength() {
        return bpm > 0 ? 60.0 / bpm * 4 : 2.0;
    }

    /**
     * 将 BeatTimeline 序列化为注入 prompt 的精简 JSON 字符串。
     * 只保留 bpm、downbeats、segments，省略 beats 全量（太长）。
     */
    public String toPromptJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"bpm\":").append(bpm).append(",");
        sb.append("\"bar_length_sec\":").append(String.format("%.3f", barLength())).append(",");

        // downbeats（只输出前 40 个，避免 prompt 太长）
        sb.append("\"downbeats\":[");
        int limit = Math.min(downbeats.size(), 40);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.3f", downbeats.get(i)));
        }
        if (downbeats.size() > 40) sb.append(",...");
        sb.append("],");

        // segments
        sb.append("\"segments\":[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append(",");
            Segment s = segments.get(i);
            sb.append("{\"label\":\"").append(s.getLabel())
                    .append("\",\"start\":").append(String.format("%.3f", s.getStart()))
                    .append(",\"end\":").append(String.format("%.3f", s.getEnd()))
                    .append("}");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }
}
