package com.ohyesai.next.biz.vio.bo;

import cn.hutool.crypto.digest.DigestUtil;
import com.ohyesai.next.biz.vio.bo.mvscript.Subject;
import com.ohyesai.next.biz.vio.bo.tool.MakeVideoArgs;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.interfaces.GenCacheKey;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SkillMakeVideoArgs implements GenCacheKey {

    @Schema(description = "画面描述。**关键规则**：若要保持角色一致性，必须在描述中使用 `@subject_id` 的格式引用主体（例如：'@hero_01 哭泣的特写'）")
    private String visualPrompt;

    @Schema(description = "时长（秒，支持小数如 4.350），不超过 10 秒。精确到毫秒以对齐音乐节拍。")
    private Double duration;

    @Schema(description = "可选的主体列表。包含 `id`（如 'hero_01'）和 `imageFileId`（参考图id）")
    private List<SceneSubject> subject;

    @Schema(description = "画面比例")
    private AspectRatio aspectRatio;

    public SkillMakeVideoArgs() {
    }

    public SkillMakeVideoArgs(String visualPrompt, Double duration, List<SceneSubject> subject, AspectRatio aspectRatio) {
        this.visualPrompt = visualPrompt;
        this.duration = duration;
        this.subject = subject;
        this.aspectRatio = aspectRatio;
    }

    @Override
    public String cacheKey() {
        return String.valueOf(hashCode());
    }
}
