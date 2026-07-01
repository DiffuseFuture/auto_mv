package com.ohyesai.next.biz.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.user.dto.AddFeedbackDTO;
import com.ohyesai.next.biz.user.entity.UserFeedback;
import com.ohyesai.next.biz.user.mapper.UserFeedbackMapper;
import com.ohyesai.next.biz.user.vo.QueryFeedbackVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class FeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;

    /**
     * 添加反馈
     *
     * @param feedback
     */
    public void addFeedback(AddFeedbackDTO feedback) {
        UserFeedback userFeedback = ChainWrappers.lambdaQueryChain(userFeedbackMapper)
                .eq(UserFeedback::getMessageId, feedback.getMessageId())
                .one();
        if (userFeedback != null) { // 如果存在则先删除
            userFeedbackMapper.deleteById(userFeedback.getId());
            return;
        }

        userFeedback = new UserFeedback();
        userFeedback.setUserId(StpUtil.getLoginIdAsString());
        userFeedback.setMessageId(feedback.getMessageId());
        userFeedback.setAttitudeType(feedback.getAttitudeType());
        userFeedback.setContent(feedback.getContent());
        userFeedback.setCreateTime(LocalDateTime.now());
        userFeedbackMapper.insert(userFeedback);
    }

    public QueryFeedbackVO queryFeedback(String messageId) {
        UserFeedback userFeedback = ChainWrappers.lambdaQueryChain(userFeedbackMapper)
                .eq(UserFeedback::getMessageId, messageId)
                .one();
        if (userFeedback == null) {
            return null;
        }
        QueryFeedbackVO queryFeedbackVO = new QueryFeedbackVO();
        queryFeedbackVO.setContent(userFeedback.getContent());
        queryFeedbackVO.setAttitudeType(userFeedback.getAttitudeType());

        return queryFeedbackVO;
    }
}
