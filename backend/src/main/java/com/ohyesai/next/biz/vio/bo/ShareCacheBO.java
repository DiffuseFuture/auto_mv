package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;

public record ShareCacheBO(
        String userId,
        String nickName,
        String sessionId,
        String projectName,
        String prompt,
        String lyrics,
        Type type,
        String fileId,
        String fileUrl,
        String coverId,
        String coverUrl
) {

    public enum Type {
        MUSIC,
        VIDEO;

        public static Type fromTaskType(TaskType taskType) {
            return switch (taskType) {
                case MAKE_MUSIC -> MUSIC;
                case MAKE_MV -> VIDEO;
                default -> throw new BusinessException(CodeEnum.ParameterError, "toolName error");
            };
        }
    }
}
