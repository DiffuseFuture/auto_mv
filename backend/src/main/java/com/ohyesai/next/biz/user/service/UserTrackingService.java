package com.ohyesai.next.biz.user.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.user.dto.SaveUserTrackingDTO;
import com.ohyesai.next.biz.user.entity.UserTracking;
import com.ohyesai.next.biz.user.mapper.UserTrackingMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class UserTrackingService {

    private final UserTrackingMapper userTrackingMapper;

    public void save(SaveUserTrackingDTO saveUserTrackingDTO, String clientIp) {
        UserTracking userTracking = new UserTracking();
        if (StpUtil.isLogin()) {
            userTracking.setUserId(StpUtil.getLoginIdAsString());
        }
        if (StrUtil.isNotBlank(saveUserTrackingDTO.getReferer())) {
            userTracking.setReferer(StrUtil.maxLength(saveUserTrackingDTO.getReferer(), 400));
        }
        userTracking.setTarget(saveUserTrackingDTO.getTarget());
        userTracking.setFromIp(clientIp);
        userTracking.setPlatform(saveUserTrackingDTO.getPlatform());
        userTracking.setCreateTime(LocalDateTime.now());
        userTrackingMapper.insert(userTracking);
    }
}
