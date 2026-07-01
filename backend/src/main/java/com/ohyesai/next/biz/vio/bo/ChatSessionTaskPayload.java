package com.ohyesai.next.biz.vio.bo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ohyesai.next.biz.vio.bo.mvscript.Scene;
import com.ohyesai.next.biz.vio.bo.mvscript.Script;
import com.ohyesai.next.biz.vio.bo.mvscript.Subject;
import com.ohyesai.next.biz.vio.entity.ChatSessionTask;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.interfaces.GenCacheKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 聊天会话任务参数
 * 用在 chat session task 表的 payload 字段中
 */
@Data
public class ChatSessionTaskPayload {

    /**
     * payload 版本号 相同大版本遵循想下兼容
     * 1.5 兼容 1.4 1.3
     * 2.0 无法兼容 1.5
     */
//    private String version = "1.0";

    private List<PayloadSubject> subjects;

    private List<PayloadScene> scenes;

    private AspectRatio aspectRatio;

    private Resolution resolution = Resolution.P720; // 给个默认值 兼容历史数据

    private String audioFileId;

    private List<SubtitleTranscribed.SubtitleLine> subtitleLines;

    /**
     * 便捷方法 形成以 subject_id 为key的map 便于搜索
     *
     * @return subjectId: PayloadSubject
     */
    public Map<String, PayloadSubject> subject2Map() {
        return subjects.stream().collect(Collectors.toMap(PayloadSubject::getId, Function.identity()));
    }

    public static ChatSessionTaskPayload from(Script mvScript, String audioFileId, Resolution resolution, List<SubtitleTranscribed.SubtitleLine> subtitleLines) {
        ChatSessionTaskPayload chatSessionTaskPayload = new ChatSessionTaskPayload();
        chatSessionTaskPayload.setSubjects(PayloadSubject.from(mvScript.getSubjects()));
        chatSessionTaskPayload.setScenes(PayloadScene.from(mvScript.getScenes()));
        chatSessionTaskPayload.setAspectRatio(mvScript.getAspectRatio());
        chatSessionTaskPayload.setAudioFileId(audioFileId);
        chatSessionTaskPayload.setResolution(resolution);
        chatSessionTaskPayload.setSubtitleLines(subtitleLines);
        return chatSessionTaskPayload;
    }

    public static ChatSessionTaskPayload from(ChatSessionTask chatSessionTask) {
        return JSON.parseObject(chatSessionTask.getPayload(), ChatSessionTaskPayload.class);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PayloadSubject extends Subject {

        /**
         * 主体结果文件id
         */
        private String resultFileId;

//        /**
//         * 对应的消息块id
//         * 用于回更消息数据
//         */
//        private Integer chunkId;

        public static List<PayloadSubject> from(List<Subject> subject) {
            return subject.stream().map(PayloadSubject::from).toList();
        }

        public static PayloadSubject from(Subject subject) {
            PayloadSubject payloadSubject = new PayloadSubject();
            payloadSubject.setId(subject.getId());
            payloadSubject.setType(subject.getType());
            payloadSubject.setDescription(subject.getDescription());
            payloadSubject.setRefImgs(subject.getRefImgs());
            return payloadSubject;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true) // 必须继承父级 cacheKey
    @ToString(callSuper = true)
    public static class PayloadScene extends Scene implements GenCacheKey {

        /**
         * 镜头结果文件id
         */
        private String resultFileId;

        /**
         * 对口型结果文件id
         */
        private String lipSyncFileId;

        /**
         * 直接参考主体 用户直接指定的分镜参考图片
         * 这里面的 subjectId 与 PayloadSubject.id 没有任何关联，subjectId 仅作为视频生成时参考用的 @subjectId
         */
        private List<SceneSubject> directSubjects;

        /**
         * 引用的主体图片
         * 这里面的 subjectId 与 PayloadSubject.id 存在关联;且必须可以从PayloadSubject 中获取
         */
        private List<SceneSubject> sceneSubjects;

        public static List<PayloadScene> from(List<Scene> scene) {
            return scene.stream().map(PayloadScene::from).toList();
        }

        public static PayloadScene from(Scene scene) {
            PayloadScene payloadScene = new PayloadScene();
            payloadScene.setId(scene.getId());
            payloadScene.setStartTime(scene.getStartTime());
            payloadScene.setEndTime(scene.getEndTime());
            payloadScene.setDuration(scene.getDuration());
            payloadScene.setSubjectRefs(scene.getSubjectRefs());
            payloadScene.setVisualPrompt(scene.getVisualPrompt());
            payloadScene.setLipSync(scene.isLipSync());
            return payloadScene;
        }

        /**
         * 判断新 PayloadScene 的改动是否需要会清空 resultFileId 结果
         *
         * @return
         */
        public boolean isReMakeScene(PayloadScene newPayloadScene) {
            // 时长不同 则需要重新生成
            if (this.getDuration() != newPayloadScene.getDuration()) {
                return true;
            }
            // 主体不同
            if (!this.getSubjectRefs().equals(newPayloadScene.getSubjectRefs())) {
                return true;
            }
            // 视觉提示不同
            if (!this.getVisualPrompt().equals(newPayloadScene.getVisualPrompt())) {
                return true;
            }
            return false;
        }

        @Override
        public String cacheKey() {
            return String.valueOf(hashCode());
        }
    }

    /**
     * <p>初始化镜头关联的主体数据
     * <p>把 subjectId 与 主体结果fileId 映射起来
     */
    public void initSceneSubject() {
        // 整理当前分镜主体与fileId映射
        Map<String, PayloadSubject> subjectMap = this.subject2Map();
        // 循环分镜数据 填充分镜主体
        for (PayloadScene scene : this.scenes) {
            List<SceneSubject> sceneSubjects = new ArrayList<>();

            if (CollUtil.isNotEmpty(scene.getSubjectRefs())) {
                // 收集 主体
                List<SceneSubject> subjects = scene.getSubjectRefs().stream()
                        .map(subjectId -> {
                            String fileId = subjectMap.get(subjectId).getResultFileId();
                            return new SceneSubject(subjectId, fileId);
                        })
                        .peek(subject -> {
                            // 增加异常校验
                            if (StrUtil.isBlank(subject.getFileId())) {
                                throw new BusinessException(CodeEnum.ParameterError, subject.getId() + " (主体图) fileId 为空，请先生成主体图");
                            }
                        })
                        .toList();

                sceneSubjects.addAll(subjects);
            }

            if (CollUtil.isNotEmpty(scene.getDirectSubjects())) {
                // 收集“直接”参考主体
                List<SceneSubject> directSubjects = scene.getDirectSubjects().stream()
                        .map(directSubject -> {
                            return new SceneSubject(directSubject.getId(), directSubject.getFileId());
                        })
                        .toList();

                sceneSubjects.addAll(directSubjects);
            }


            if (!sceneSubjects.isEmpty()) {
                scene.setSceneSubjects(sceneSubjects);
            }
        }
    }

}
