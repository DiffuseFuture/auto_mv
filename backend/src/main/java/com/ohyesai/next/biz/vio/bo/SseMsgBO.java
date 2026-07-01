package com.ohyesai.next.biz.vio.bo;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.bo.tool.SceneScript2UiBO;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.util.JsonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Data
@Schema(description = "sse 消息结构")
public class SseMsgBO<T> {

    @Schema(description = "类型")
    private Type type;

    @Schema(description = "数据")
    private T data;

    public SseMsgBO() {
    }

    private SseMsgBO(Type type, T data) {
        this.type = type;
        this.data = data;
    }

    // region 各种 of 快捷方法

    /**
     * 猜测data对应的type类型
     *
     * @param data
     */
    public static Type data2Type(Object data) {
        return switch (data) {
            case SseMsgBO.Init _ -> Type.INIT;
            case SseMsgBO.Text _ -> Type.TEXT;
            case SseMsgBO.Img _ -> Type.IMG;
            case SseMsgBO.Subject _ -> Type.SUBJECT;
            case SseMsgBO.Scene _ -> Type.SCENE;
            case SseMsgBO.Video _ -> Type.VIDEO;
            case SseMsgBO.Complete _ -> Type.COMPLETE;
            default -> throw new IllegalArgumentException("Invalid data type");
        };
    }

    public static SseMsgBO<Object> ofPayload(JsonNode payload, FileComponent fileComponent) {
        Type type = JsonUtil.tree2Object(payload.path("type"), Type.class);
        JsonNode data = payload.path("data");
        Object val = switch (type) {
            case INIT -> JsonUtil.tree2Object(data, Init.class);
            case TEXT, THINKING -> {
                Text text = JsonUtil.tree2Object(data, Text.class);
                List<Img> imgs = text.imgs == null ? null : text.imgs.stream().map(img -> new Img(img.imgFileId(), fileComponent.shareUrl(img.imgFileId()))).toList();
                List<Audio> audios = text.audios == null ? null : text.audios.stream().map(audio -> new Audio(
                        audio.title(),
                        audio.style(),
                        audio.lyrics(),
                        audio.audioFileId(),
                        fileComponent.shareUrl(audio.audioFileId()),
                        audio.coverFileId(),
                        fileComponent.shareUrl(audio.coverFileId()),
                        audio.projectId()
                )).toList();

                yield new Text(text.text(), imgs, audios);
            }
            case LYRICS -> JsonUtil.tree2Object(data, Lyrics.class);
            case IMG -> {
                // 有附件的需要回显
                List<Img> imgs = JsonUtil.tree2ListObject(data, Img.class);
                yield imgs.stream().map(img -> new Img(img.imgFileId(), fileComponent.shareUrl(img.imgFileId()))).toList();
            }
            case SCENE_SCRIPT -> {
                List<SceneScript> scenes = JsonUtil.tree2ListObject(data, SceneScript.class);
                for (SceneScript scene : scenes) {
                    if (scene.getSubjectRefs() != null) {
                        for (SceneScript.SubjectRef subjectRef : scene.getSubjectRefs()) {
                            subjectRef.setUrl(fileComponent.shareUrl(subjectRef.getFileId()));
                        }
                    }
                }
                yield scenes;
            }
            case VIDEO -> {
                // 有附件的需要回显
                List<Video> videos = JsonUtil.tree2ListObject(data, Video.class);
                yield videos.stream()
                        .map(video -> new Video(
                                video.videoFileId(),
                                fileComponent.shareUrl(video.videoFileId()),
                                video.coverFileId,
                                fileComponent.shareUrl(video.coverFileId()),
                                video.projectId()
                        ))
                        .toList();
            }
            case SUBJECT -> {
                Subject subject = JsonUtil.tree2Object(data, Subject.class);
                for (Subject.VersionedImg v : subject.getVersions()) {
                    v.setImgUrl(fileComponent.shareUrl(v.getImgFileId()));
                    if (v.getRefImages() != null) {
                        for (Subject.RefImg ref : v.getRefImages()) {
                            ref.setUrl(fileComponent.shareUrl(ref.getFileId()));
                        }
                    }
                }
                yield subject;
            }
            case SCENE -> {
                Scene scene = JsonUtil.tree2Object(data, Scene.class);
                for (Scene.VersionedVideo v : scene.getVersions()) {
                    v.setVideoUrl(fileComponent.shareUrl(v.getVideoFileId()));
                    v.setCoverUrl(fileComponent.shareUrl(v.getCoverFileId()));
                    if (v.getSubjects() != null) {
                        for (Scene.SubjectRef ref : v.getSubjects()) {
                            ref.setImageUrl(fileComponent.shareUrl(ref.getImageFileId()));
                        }
                    }
                }
                yield scene;
            }
            case AUDIO -> {
                // 有附件的需要回显
                List<Audio> audios = JsonUtil.tree2ListObject(data, Audio.class);
                yield audios.stream()
                        .map(audio -> new Audio(
                                audio.title(),
                                audio.style(),
                                audio.lyrics(),
//                                audio.lrcLyrics(),
                                audio.audioFileId(),
                                fileComponent.shareUrl(audio.audioFileId()),
                                audio.coverFileId(),
                                fileComponent.shareUrl(audio.coverFileId()),
                                audio.projectId()
                        )).toList();
            }
//            case USER_INPUT -> {
//                // 有附件的需要回显
//                UserInput userInput = JsonUtil.toObject(data, UserInput.class);
//                if (StrUtil.isBlank(userInput.fileId())) {
//                    yield userInput;
//                }
//                yield new UserInput(userInput.text(), userInput.fileId(), fileComponent.shareUrl(userInput.fileId()));
//            }


            case ERROR -> JsonUtil.tree2Object(data, Error.class);
            case COMPLETE -> JsonUtil.tree2Object(data, Complete.class);
//            case PING -> null;
        };
        return SseMsgBO.ofData(type, val);
    }

    public static <T> SseMsgBO<T> ofData(Type type, @Nullable T data) {
        return new SseMsgBO<>(type, data);
    }


    public static SseMsgBO<Init> ofInit(String id, String name) {
        return new SseMsgBO<>(Type.INIT, new Init(id, name));
    }

    public static SseMsgBO<Text> ofText(String text) {
        return new SseMsgBO<>(Type.TEXT, new Text(text, null, null));
    }

    public static SseMsgBO<Text> ofText(String text, List<Img> imgs, List<Audio> audios) {
        return new SseMsgBO<>(Type.TEXT, new Text(text, imgs, audios));
    }

    public static SseMsgBO<Lyrics> ofLyrics(String lyrics) {
        return new SseMsgBO<>(Type.LYRICS, new Lyrics(lyrics));
    }

    public static SseMsgBO<Text> ofThinking(String text) {
        return new SseMsgBO<>(Type.THINKING, new Text(text, null, null));
    }

    public static SseMsgBO<List<Img>> ofImg(String imgFileId, String imgUrl) {
        return ofImgs(List.of(new Img(imgFileId, imgUrl)));
    }

    public static SseMsgBO<List<Img>> ofImgs(List<Img> imgs) {
        return new SseMsgBO<>(Type.IMG, imgs);
    }

    public static SseMsgBO<List<Video>> ofVideo(String videoFileId, String videoUrl, String coverFileId, String coverUrl, String projectId) {
        return ofVideos(List.of(new Video(videoFileId, videoUrl, coverFileId, coverUrl, projectId)));
    }

    public static SseMsgBO<Subject> ofSubject(Subject subject) {
        return new SseMsgBO<>(Type.SUBJECT, subject);
    }

    public static SseMsgBO<Scene> ofScene(Scene scene) {
        return new SseMsgBO<>(Type.SCENE, scene);
    }

    public static SseMsgBO<List<SceneScript>> ofSceneScript(List<SceneScript> sceneScripts) {
        return new SseMsgBO<>(Type.SCENE_SCRIPT, sceneScripts);
    }

    @Deprecated // 使用 ofScene(Scene scene) 替代
    public static SseMsgBO<List<Video>> ofScene(String videoFileId, String videoUrl, String coverFileId, String coverUrl) {
        return ofScenes(List.of(new Video(videoFileId, videoUrl, coverFileId, coverUrl, null)));
    }


    public static SseMsgBO<List<Video>> ofVideos(List<Video> videos) {
        return new SseMsgBO<>(Type.VIDEO, videos);
    }

    public static SseMsgBO<List<Video>> ofScenes(List<Video> videos) {
        return new SseMsgBO<>(Type.SCENE, videos);
    }

    public static SseMsgBO<List<Audio>> ofAudio(
            String title,
            String style,
            String lyrics,
//            String lrcLyrics,
            String audioFileId,
            String audioUrl,
            String coverFileId,
            String coverUrl,
            String projectId) {
        return ofAudios(List.of(new Audio(title, style, lyrics, audioFileId, audioUrl, coverFileId, coverUrl, projectId)));
    }

    public static SseMsgBO<List<Audio>> ofAudios(List<Audio> audios) {
        return new SseMsgBO<>(Type.AUDIO, audios);
    }

    public static SseMsgBO<Error> ofError(CodeEnum code, String error) {
        return new SseMsgBO<>(Type.ERROR, new Error(code.code, error));
    }

    public static SseMsgBO<Error> ofError(CodeEnum code) {
        return new SseMsgBO<>(Type.ERROR, new Error(code.code, code.message));
    }

    public static SseMsgBO<Complete> ofComplete() {
        return new SseMsgBO<>(Type.COMPLETE, new Complete(null, null));
    }

    // endregion

    /**
     * 消息类型
     * 消息类型与数据类型是任意组合的关系
     */
    public enum Type {

        @Schema(description = "初始化")
        INIT,

        @Schema(description = "文本")
        TEXT,

        @Schema(description = "思考数据")
        THINKING,

        @Schema(description = "图片")
        IMG,

        @Schema(description = "主体参考图（带版本）")
        SUBJECT,

        @Schema(description = "镜头视频数据（带版本）")
        SCENE,

        @Schema(description = "分镜/镜头 脚本")
        SCENE_SCRIPT,

        @Schema(description = "视频；代码最终结果视频")
        VIDEO,

        @Schema(description = "音频")
        AUDIO,

        @Schema(description = "歌词")
        LYRICS,

        @Schema(description = "错误")
        ERROR,

        @Schema(description = "结束")
        COMPLETE,

    }

    // region 数据结构

    public record Init(String id, String name) {
    }

    /**
     *
     * @param text
     * @param imgs audios 部分场景 会有文本和图片同是展示的，例如用户发送带图片的消息,主要用于回显附件数据
     */
    public record Text(String text, @Nullable List<Img> imgs, @Nullable List<Audio> audios) {
    }

    public record Lyrics(String lyrics) {
    }

    public record Img(String imgFileId, String imgUrl) {
    }

    public record Video(String videoFileId, String videoUrl, String coverFileId, String coverUrl, String projectId) {
    }

    /**
     * 主体参考图（带版本管理）
     */
    @Data
    public static class Subject {

        /**
         * subject Id
         */
        private String assetKey;

//        private String description;

        /**
         * 当前激活版本号（0-based），versions.get(activeVersion) 定位
         */
        private int activeVersion;

        private SubjectType type;

        private List<VersionedImg> versions;  // 版本列表

        @Data
        public static class VersionedImg {
            private int version;
            private String imgFileId;
            private String imgUrl;            // ofPayload 时回填
            private String prompt;            // 生成时的提示词（编辑时回显用）
            private List<RefImg> refImages;   // 参考图列表
        }

        @Data
        public static class RefImg {
            private String fileId;
            private String url;               // ofPayload 时回填
        }

        public enum SubjectType {
            character,   // 角色
            object,      // 物体
            environment  // 环境

            ;

            public static SubjectType of(com.ohyesai.next.biz.vio.bo.mvscript.Subject.SubjectType type) {
                return switch (type) {
                    case character -> SubjectType.character;
                    case object -> SubjectType.object;
                    case environment -> SubjectType.environment;
                };
            }
        }
    }

    /**
     * 分镜/镜头 视频（带版本管理）
     */
    @Data
    public static class Scene {

        /**
         * sceneId
         */
        private String assetKey;

//        private String description;

        /**
         * 当前激活版本号
         */
        private int activeVersion;
        private double startTime;
        private double endTime;
        private double duration;
        private List<VersionedVideo> versions; // 版本列表

        @Data
        public static class VersionedVideo {
            private int version;
            private String videoFileId;
            private String videoUrl;          // ofPayload 时回填
            private String coverFileId;
            private String coverUrl;          // ofPayload 时回填
            private String visualPrompt;      // 视觉提示词
            // 画面比例
            private AspectRatio aspectRatio;
            // 清晰度
            private Resolution resolution;
            private List<SubjectRef> subjects; // 使用的参考图
            // 模型名称
            private ModelEnum model;
        }

        @Data
        public static class SubjectRef {
            private String id;                // subject id
            private String imageFileId;       // 参考图 fileId
            private String imageUrl;          // ofPayload 时回填
        }
    }

    public record Audio(
            String title,
            String style,
            // 纯文本歌词
            String lyrics,
//            String lrcLyrics,
            String audioFileId,
            String audioUrl,
            String coverFileId,
            String coverUrl,
            String projectId) {
    }

    public record Error(int code, String text) {
    }

    public record Complete(String userMsgId, String aiMsgId) {
    }

    /**
     * 场景/镜头脚本
     */
    @Data
    public static class SceneScript {
        /**
         * sceneId
         */
        private String id;
        private double startTime;
        private double endTime;
        private double duration;
        private List<SubjectRef> subjectRefs;
        private String description;
        private String visualPrompt;

        @Data
        public static class SubjectRef {
            private String subjectId;  // 主体id
            private String fileId;        // 主体图片对应id
            private String url;       // ofPayload 时回填
        }

        public static SceneScript of(SceneScript2UiBO.SceneScript sourceSceneScript, FileComponent fileComponent) {
            SceneScript targetSceneScript = new SceneScript();
            BeanUtil.copyProperties(sourceSceneScript, targetSceneScript);

            if (sourceSceneScript.getSubjectRefs() != null) {
                targetSceneScript.getSubjectRefs().forEach(subjectRef -> {
                    subjectRef.setUrl(fileComponent.shareUrl(subjectRef.getFileId()));
                });
            }
            return targetSceneScript;
        }
    }

    // endregion
}
