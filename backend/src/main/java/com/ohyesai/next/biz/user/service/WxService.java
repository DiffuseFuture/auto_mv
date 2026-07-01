package com.ohyesai.next.biz.user.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.user.bo.WxUserInfo;
import com.ohyesai.next.biz.user.entity.UserOauthBind;
import com.ohyesai.next.biz.user.mapper.UserOauthBindMapper;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.WxProperties;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 微信相关操作
 */
@Service
@AllArgsConstructor
@Slf4j
public class WxService {

    private final WxProperties wxProperties;

    private final RestClient restClient;

    private final StringRedisTemplate redisTemplate;

    private final UserOauthBindMapper userOauthBindMapper;

    /**
     * 获取 access_token
     *
     * @return
     */
    public String getMpAccessToken() {
        WxProperties.MiniProgram mp = wxProperties.mp();
        // 缓存key
        String cacheKey = RedisConst.WX_ACCESS_TOKEN.formatted(mp.appid());
        String accessToken = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        // getAccessToken
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential" + "&appid=" + mp.appid() + "&secret=" + mp.secret();
        String res = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);

        JsonNode body = JsonUtil.readTree(res);
        if (body == null) {
            throw new BusinessException(CodeEnum.ParameterError);
        }
        accessToken = body.path("access_token").asText();
        long expiresIn = body.path("expires_in").asLong();
        if (StrUtil.isBlank(accessToken)) {
            log.error("获取 access_token 失败");
            throw new BusinessException(CodeEnum.ParameterError);
        }
        // 缓存
        redisTemplate.opsForValue().set(cacheKey, accessToken, Duration.ofSeconds(expiresIn));
        return accessToken;
    }

    /**
     * 获取小程序用户信息
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/api/open-api/login/wx.login.html">doc</a>
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html">doc</a>
     *
     * @param code wx.login 对应的code
     * @return
     */
    public WxUserInfo getMpUserInfo(String code) {
        // 换取小程序临时信息
        WxProperties.MiniProgram mp = wxProperties.mp();
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + mp.appid() + "&secret=" + mp.secret() + "&js_code=" + code + "&grant_type=authorization_code";
        String res = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);

        JsonNode body = JsonUtil.readTree(res);
        if (body == null) {
            throw new BusinessException(CodeEnum.ParameterError);
        }

        String openId = body.path("openid").asText();
        String unionid = body.path("unionid").asText();

        if (StrUtil.isAllBlank(openId, unionid)) {
            log.error("获取小程序用户信息失败 {}", res);
            throw new BusinessException(CodeEnum.ParameterError);
        }
        return new WxUserInfo(openId, unionid);
    }

    /**
     * 获取微信平台用户信息
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/api/open-api/login/wx.login.html">doc</a>
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html">doc</a>
     *
     * @param code wx.login 对应的code
     * @return
     */
    public WxUserInfo getWxUserInfo(String code) {
        // 换取小程序临时信息
        WxProperties.MiniProgram mp = wxProperties.mp();
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + mp.appid() + "&secret=" + mp.secret() + "&js_code=" + code + "&grant_type=authorization_code";
        String res = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);

        JsonNode body = JsonUtil.readTree(res);
        if (body == null) {
            throw new BusinessException(CodeEnum.ParameterError);
        }

        String openId = body.path("openid").asText();
        String unionid = body.path("unionid").asText();

        if (StrUtil.isAllBlank(openId, unionid)) {
            log.error("获取小程序用户信息失败");
            throw new BusinessException(CodeEnum.ParameterError);
        }
        return new WxUserInfo(openId, unionid);
    }

    /**
     * 获取手机号
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/getPhoneNumber.html">doc</a>
     *
     * @param phoneCode 获取手机号对应code
     * @return
     */
    public String getMpPhoneNumber(String phoneCode) {
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + getMpAccessToken();

        String bodyJson = JsonUtil.object().put("code", phoneCode).toString();
        String res = HttpUtil.post(url, bodyJson);
//        String res = restClient.post()
//                .uri(URI.create(url))
//                .body(JsonUtil.object()
//                        .put("code", phoneCode)
//                )
//                .retrieve()
//                .body(String.class);

        JsonNode body = JsonUtil.readTree(res);
        if (body == null) {
            throw new BusinessException(CodeEnum.ParameterError);
        }
        String phoneNumber = body.path("phone_info").path("purePhoneNumber").asText();
        if (StrUtil.isBlank(phoneNumber)) {
            log.error("获取手机号失败: {}", res);
            throw new BusinessException(CodeEnum.ParameterError);
        }
        return phoneNumber;
    }

    public UserOauthBind getUserOauthBind(WxUserInfo wxUserInfo) {
        return ChainWrappers.lambdaQueryChain(userOauthBindMapper)
                .eq(UserOauthBind::getOpenid, wxUserInfo.openId())
                .eq(UserOauthBind::getUnionid, wxUserInfo.unionid())
                .eq(UserOauthBind::getPlatform, UserOauthBind.Platform.WX_MP)
                .one();
//        // 查询用户信息是否存在
//        LambdaQueryWrapper<UserOauthBind> userOauthBindQuery = new LambdaQueryWrapper<UserOauthBind>()
//                .eq(UserOauthBind::getOpenid, wxUserInfo.openId())
//                .eq(UserOauthBind::getUnionid, wxUserInfo.unionid())
//                .eq(UserOauthBind::getPlatform, UserOauthBind.Platform.WX_MP);
//
//        return userOauthBindMapper.selectOne(userOauthBindQuery);
    }

    /**
     * 根据userId 查询 UserOauthBind
     *
     * @param userId
     * @return
     */
    public UserOauthBind getUserOauthBind(String userId, UserOauthBind.Platform platform) {
        return ChainWrappers.lambdaQueryChain(userOauthBindMapper)
                .eq(UserOauthBind::getUserId, userId)
                .eq(UserOauthBind::getPlatform, platform)
                .one();
    }

    public UserOauthBind updateUserOauthBind(String userId, WxUserInfo wxUserInfo, UserOauthBind.Platform platform) {
        // 移除原始数据
        LambdaQueryWrapper<UserOauthBind> userOauthBindQuery = Wrappers.<UserOauthBind>lambdaQuery()
                .eq(UserOauthBind::getUserId, userId)
                .eq(UserOauthBind::getPlatform, platform);
        userOauthBindMapper.delete(userOauthBindQuery);

        // 添加新数据
        UserOauthBind userOauthBind = new UserOauthBind();
        userOauthBind.setUserId(userId);
        userOauthBind.setPlatform(platform);
        userOauthBind.setOpenid(wxUserInfo.openId());
        userOauthBind.setUnionid(wxUserInfo.unionid());
        userOauthBind.setCreateTime(LocalDateTime.now());
        userOauthBindMapper.insert(userOauthBind);
        return userOauthBind;
    }
}
