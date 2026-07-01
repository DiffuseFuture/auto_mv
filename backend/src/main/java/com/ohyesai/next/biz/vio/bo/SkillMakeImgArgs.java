package com.ohyesai.next.biz.vio.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SkillMakeImgArgs {
    @Schema(description = "生成图片的描述提示词；请务必包含画面内容及图片的横竖屏比例要求（如：横屏、竖屏、16:9、9:16等）")
    private String prompt;

    @Schema(description = "主体序号，对应脚本中 definedSubject 的 id")
    private String subjectIdx;

    @Schema(description = "参考图 fileId ")
    private List<String> refImageFileId;

    public SkillMakeImgArgs() {
    }

    public SkillMakeImgArgs(String prompt, String subjectIdx, List<String> refImageFileId) {
        this.prompt = prompt;
        this.subjectIdx = subjectIdx;
        this.refImageFileId = refImageFileId;
    }
}
