package com.ohyesai.next.biz.vio.bo.tool;

import com.ohyesai.next.biz.vio.bo.ChatSessionTaskPayload;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Description("确认MV脚本参数")
public class SceneScript2UiBO {

    @Description("分镜参数列表")
    private List<SceneScript> sceneScripts;

    @Data
    @Description("场景/镜头脚本")
    public static class SceneScript {
        @Description("分镜序号，对应脚本中 scene 的 id（如 1、2、3）")
        private String id;
        @Description("该分镜的起始时间（秒），对应音频时间轴")
        private double startTime;
        @Description("该分镜的结束时间（秒），对应音频时间轴")
        private double endTime;
        @Description("该分镜的持续时长（秒），等于 endTime - startTime")
        private double duration;
//        @Description("该分镜的剧情描述")
//        private String description;
        @Description("该分镜的 AI 视频生成视觉提示词，使用 @subject_id 格式引用主体（如 @subject_1）")
        private String visualPrompt;
        @Description("该分镜引用的主体列表，映射到 defined_subjects 中的主体及其参考图")
        private List<SubjectRef> subjectRefs;

        @Data
        @Description("分镜中引用的主体信息")
        public static class SubjectRef {
            @Description("主体id，对应 defined_subjects 中的 id（如 subject_1）")
            private String subjectId;
            @Description("该主体通过 makeImg 生成的参考图文件id")
            private String fileId;

            public static List<SubjectRef> from(List<String> subjectRefs, Map<String, ChatSessionTaskPayload.PayloadSubject> subjectMap){
                if (subjectRefs == null){
                    return null;
                }
                return subjectRefs.stream().map(subjectId -> {
                    ChatSessionTaskPayload.PayloadSubject payloadSubject = subjectMap.get(subjectId);
                    SubjectRef subjectRef = new SubjectRef();
                    subjectRef.setSubjectId(payloadSubject.getId());
                    subjectRef.setFileId(payloadSubject.getResultFileId());
                    return subjectRef;
                }).toList();
            }
        }

    }

}
