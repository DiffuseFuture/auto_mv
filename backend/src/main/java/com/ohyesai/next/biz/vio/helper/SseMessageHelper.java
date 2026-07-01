package com.ohyesai.next.biz.vio.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.bo.SseMsgBO;
import com.ohyesai.next.biz.vio.bo.tool.SceneScript2UiBO;
import com.ohyesai.next.biz.vio.entity.HistoryMessageChunk;
import com.ohyesai.next.biz.vio.service.HistoryMessageService;
import com.ohyesai.next.biz.vio.vo.SseMsgVO;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 构造sse 消息的组件
 * 可以存储数据到库中
 */
@Data
@AllArgsConstructor
public class SseMessageHelper {

    private final String userId;

    private final String chatSessionId;

    private final String historyMessageId;

    private final HistoryMessageService historyMessageService;

    private final StringRedisTemplate redisTemplate;


    /**
     * 初始化对话记录
     *
     * @param chatName
     * @return
     */
    public void ofInit(String chatName) {
        SseMsgBO<SseMsgBO.Init> initSseMsgBO = SseMsgBO.ofInit(chatSessionId, chatName);
        // 存储
        Integer chunkId = saveMessageChunk(initSseMsgBO);
        sendQueue(initSseMsgBO, chunkId);
    }

    /**
     * 推动到队列 用于异步取出渲染前端
     * 注意 前端使用的 SseMsgVO 并不是 SseMsgBO
     *
     * @param object
     * @param chunkId
     */
    private void sendQueue(SseMsgBO<?> object, Integer chunkId) {
        String keyHistory = RedisConst.AGENT_CHAT_HISTORY.formatted(chatSessionId, historyMessageId);
        // 记录当前对话的缓存与偏移量
//        redisTemplate.opsForZSet().add(keyHistory, JsonUtil.toJson(object), sort);
        redisTemplate.opsForList().rightPush(keyHistory, JsonUtil.toJson(new SseMsgVO(historyMessageId, chunkId, JsonUtil.object2Tree(object))));
        // 设置最大存活时间
        redisTemplate.expire(keyHistory, 30, TimeUnit.MINUTES);
    }

    public void ofText(String text) {
        SseMsgBO<SseMsgBO.Text> textSseMsgBO = SseMsgBO.ofText(text);
        Integer chunkId = saveMessageChunk(textSseMsgBO);
        sendQueue(textSseMsgBO, chunkId);
    }

    public void ofLyrics(String lyrics) {
        SseMsgBO<SseMsgBO.Lyrics> textSseMsgBO = SseMsgBO.ofLyrics(lyrics);
        Integer chunkId = saveMessageChunk(textSseMsgBO);
        sendQueue(textSseMsgBO, chunkId);
    }

    public void ofImg(String imgFileId, String imgUrl) {
        SseMsgBO<List<SseMsgBO.Img>> imgSseMsgBO = SseMsgBO.ofImg(imgFileId, imgUrl);
        Integer chunkId = saveMessageChunk(imgSseMsgBO);
        sendQueue(imgSseMsgBO, chunkId);
    }

    public void ofImgs(List<SseMsgBO.Img> imgs) {
        SseMsgBO<List<SseMsgBO.Img>> imgSseMsgBO = SseMsgBO.ofImgs(imgs);
        Integer chunkId = saveMessageChunk(imgSseMsgBO);
        sendQueue(imgSseMsgBO, chunkId);
    }

    public void ofAudios(List<SseMsgBO.Audio> audios) {
        SseMsgBO<List<SseMsgBO.Audio>> _audios = SseMsgBO.ofAudios(audios);
        Integer chunkId = saveMessageChunk(_audios);
        sendQueue(_audios, chunkId);
    }

    public void ofVideo(String videoFileId, String videoUrl, String coverFileId, String coverUrl) {
        ofVideo(videoFileId, videoUrl, coverFileId, coverUrl, null);
    }

    public void ofVideo(String videoFileId, String videoUrl, String coverFileId, String coverUrl, String projectId) {
        SseMsgBO<List<SseMsgBO.Video>> _videos = SseMsgBO.ofVideo(videoFileId, videoUrl, coverFileId, coverUrl, projectId);
        Integer chunkId = saveMessageChunk(_videos);
        sendQueue(_videos, chunkId);
    }

    @Deprecated // 使用 ofScene(SseMsgBO.Scene) 替代
    public void ofScene(String videoFileId, String videoUrl, String coverFileId, String coverUrl) {
        SseMsgBO<List<SseMsgBO.Video>> _scenes = SseMsgBO.ofScene(videoFileId, videoUrl, coverFileId, coverUrl);
        Integer chunkId = saveMessageChunk(_scenes);
        sendQueue(_scenes, chunkId);
    }

    /**
     * 推送 SUBJECT 消息
     *
     * @param subject 新的 subject 数据（首次调用时包含 V1）
     * @param chunkId 已有的 chunkId，非 null 时追加版本到该 chunk，null 时新建
     * @return chunkId
     */
    public Integer ofSubject(SseMsgBO.Subject subject, Integer chunkId, Integer chatSessionTaskId) {
        // id 没有传，或者传了个不存在的id 都可以继续
        HistoryMessageChunk existingChunk = (chunkId != null && chunkId > 0) ? historyMessageService.getChunkById(chunkId) : null;
        if (existingChunk != null) {
            // 追加版本到已有 chunk
            JsonNode payload = JsonUtil.readTree(existingChunk.getChunk());
            SseMsgBO.Subject existing = JsonUtil.tree2Object(payload.path("data"), SseMsgBO.Subject.class);

            // 提取新版本
            SseMsgBO.Subject.VersionedImg newVer = subject.getVersions().getFirst();
            newVer.setVersion(existing.getVersions().size()); // 设置新版本的版本号，因为版本号从 0 开始

            // 向历史数据追加版本
            existing.getVersions().add(newVer);
            existing.setActiveVersion(newVer.getVersion()); // 设置激活版本号为刚添加的新版本

            SseMsgBO<SseMsgBO.Subject> subjectSseMsgBO = SseMsgBO.ofSubject(existing);
            historyMessageService.updateChunkContent(chunkId, subjectSseMsgBO);

            sendQueue(subjectSseMsgBO, chunkId);
            return chunkId;
        } else {
            SseMsgBO<SseMsgBO.Subject> msg = SseMsgBO.ofSubject(subject);
            Integer newChunkId = saveMessageChunk(msg, chatSessionTaskId);
            sendQueue(msg, newChunkId);
            return newChunkId;
        }
    }

    /**
     * 推送 SCENE 消息
     *
     * @param scene   新的 scene 数据（首次调用时包含 V1）
     * @param chunkId 已有的 chunkId，非 null 时追加版本到该 chunk，null 时新建
     * @return chunkId
     */
    public Integer ofScene(SseMsgBO.Scene scene, Integer chunkId, Integer chatSessionTaskId) {
        // id 没有传，或者传了个不存在的id 都可以继续
        HistoryMessageChunk existingChunk = (chunkId != null && chunkId > 0) ? historyMessageService.getChunkById(chunkId) : null;
        if (existingChunk != null) {
            JsonNode payload = JsonUtil.readTree(existingChunk.getChunk());
            SseMsgBO.Scene existing = JsonUtil.tree2Object(payload.path("data"), SseMsgBO.Scene.class);

            // 提取新版本
            SseMsgBO.Scene.VersionedVideo newVer = scene.getVersions().getFirst();
            newVer.setVersion(existing.getVersions().size()); // 设置新版本的版本号，因为版本号从 0 开始

            // 追加新版本
            existing.getVersions().add(newVer);
            existing.setActiveVersion(newVer.getVersion()); // 设置激活版本号为刚添加的新版本

            SseMsgBO<SseMsgBO.Scene> sceneSseMsgBO = SseMsgBO.ofScene(existing);
            updateChunkContent(chunkId, sceneSseMsgBO);

            sendQueue(sceneSseMsgBO, chunkId);
            return chunkId;
        } else {
            SseMsgBO<SseMsgBO.Scene> msg = SseMsgBO.ofScene(scene);
            Integer newChunkId = saveMessageChunk(msg, chatSessionTaskId);
            sendQueue(msg, newChunkId);
            return newChunkId;
        }
    }


    public void ofSceneScript(List<SseMsgBO.SceneScript> scenesScript, Integer taskId) {
        SseMsgBO<List<SseMsgBO.SceneScript>> _sceneScript = SseMsgBO.ofSceneScript(scenesScript);
        Integer chunkId = saveMessageChunk(_sceneScript, taskId);
        sendQueue(_sceneScript, chunkId);
    }

    public void ofSceneScript(List<SceneScript2UiBO.SceneScript> scenesScript, FileComponent fileComponent, Integer taskId) {
        ofSceneScript(scenesScript.stream().map(v -> SseMsgBO.SceneScript.of(v, fileComponent)).toList(), taskId);
    }

    public void ofThinking(String text) {
        SseMsgBO<SseMsgBO.Text> textSseMsgBO = SseMsgBO.ofThinking(text);
        Integer chunkId = saveMessageChunk(textSseMsgBO);
        sendQueue(textSseMsgBO, chunkId);
    }

    public void ofComplete() {
        SseMsgBO<SseMsgBO.Complete> voidSseMsgBO = SseMsgBO.ofComplete();
        // 存储
        Integer chunkId = saveMessageChunk(voidSseMsgBO);
        sendQueue(voidSseMsgBO, chunkId);
        // 当前消息标记为完成
        historyMessageService.finishHistoryMessage(historyMessageId);
    }

    public void ofError(CodeEnum codeEnum) {
        ofError(codeEnum, codeEnum.message);
    }

    public void ofError(CodeEnum codeEnum, String errorMsg) {
        SseMsgBO<SseMsgBO.Error> errorSseMsgBO = SseMsgBO.ofError(codeEnum, errorMsg);
        // 存储
        Integer chunkId = saveMessageChunk(errorSseMsgBO);
        sendQueue(errorSseMsgBO, chunkId);
        // 当前消息标记为完成
        historyMessageService.finishHistoryMessage(historyMessageId);
    }

    /**
     *
     * @param sseAgentBO
     * @return 返回插入数据库后的 chunk id； id为自增 也是偏移量
     */
    private Integer saveMessageChunk(SseMsgBO<?> sseAgentBO) {
        return historyMessageService.putMessageChunk(historyMessageId, null, sseAgentBO).getId();
    }

    private Integer saveMessageChunk(SseMsgBO<?> sseAgentBO, Integer chatSessionTaskId) {
        return historyMessageService.putMessageChunk(historyMessageId, chatSessionTaskId, sseAgentBO).getId();
    }

    private void updateChunkContent(Integer chunkId, SseMsgBO<SseMsgBO.Scene> sceneSseMsgBO) {
        historyMessageService.updateChunkContent(chunkId, sceneSseMsgBO);
    }


}
