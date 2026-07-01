package com.ohyesai.next.biz.user.service;

import com.ohyesai.next.biz.user.mapper.UserMapper;
import com.ohyesai.next.util.StpApiUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ApiUserService {

    private final StringRedisTemplate redisTemplate;

    private final UserMapper userMapper;


    public boolean check(String apiKey) {
        return false;
    }
}
