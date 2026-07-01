package com.ohyesai.next.biz.user.service;

import cn.dev33.satoken.apikey.template.SaApiKeyUtil;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.billing.service.SubscriptionPlanService;
import com.ohyesai.next.biz.user.bo.InviterGrantInfoBO;
import com.ohyesai.next.biz.user.bo.UserSessionBO;
import com.ohyesai.next.biz.user.bo.WxUserInfo;
import com.ohyesai.next.biz.user.dto.*;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.entity.UserOauthBind;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import com.ohyesai.next.biz.user.mapper.UserOauthBindMapper;
import com.ohyesai.next.biz.user.vo.UserInfoVO;
import com.ohyesai.next.common.consts.CommonConst;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.WxProperties;
import com.ohyesai.next.component.FileComponent;
import com.ohyesai.next.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
//@AllArgsConstructor
@Slf4j
public class UserService {

    private final String profile;

    private final UserMapper userMapper;

    private final UserOauthBindMapper userOauthBindMapper;

    private final SubscriptionPlanService subscriptionPlanService;

    private final FileComponent fileComponent;

    private final WxService wxService;

    private final WxProperties wxProperties;

    private final RestClient restClient;

    private final StringRedisTemplate redisTemplate;

    private final AsyncClient asyncClient;

    private final BillingService billingService;

    public UserService(@Value("${spring.profiles.active}") String profile, UserMapper userMapper, UserOauthBindMapper userOauthBindMapper, SubscriptionPlanService subscriptionPlanService, FileComponent fileComponent, WxService wxService, WxProperties wxProperties, RestClient restClient, StringRedisTemplate redisTemplate, AsyncClient asyncClient, BillingService billingService) {
        this.profile = profile;
        this.userMapper = userMapper;
        this.userOauthBindMapper = userOauthBindMapper;
        this.subscriptionPlanService = subscriptionPlanService;
        this.fileComponent = fileComponent;
        this.wxService = wxService;
        this.wxProperties = wxProperties;
        this.restClient = restClient;
        this.redisTemplate = redisTemplate;
        this.asyncClient = asyncClient;
        this.billingService = billingService;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO mpLoginByPhone(MpLoginPhoneDTO mpLoginDTO) {
        WxUserInfo mpUserInfo = wxService.getMpUserInfo(mpLoginDTO.getCode());
        String phoneNumber = wxService.getMpPhoneNumber(mpLoginDTO.getPhoneCode());
        // 使用手机号查询是否存在
        User user = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(User::getMobile, phoneNumber)
                .one();

        UserOauthBind userOauthBind;
        if (user != null) { // 用户存在更新微信绑定后登录
            // 更新手机号用户对应的微信绑定
            userOauthBind = wxService.updateUserOauthBind(user.getId(), mpUserInfo, UserOauthBind.Platform.WX_MP);
            return loginByUserId(user.getId(), userOauthBind);
        }
        // 如果不存在则创建用户
        user = new User();
        user.setNickName(genNickName());
        user.setMobile(phoneNumber);
//        user.setPointsBalance(0);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 设置免费订阅计划
        setFreeSubscriptionPlan(user);

        // 创建绑定
        userOauthBind = wxService.updateUserOauthBind(user.getId(), mpUserInfo, UserOauthBind.Platform.WX_MP);

        return loginByUser(user, userOauthBind);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO mpLogin(MpLoginDTO mpLoginDTO) {
        WxUserInfo mpUserInfo = wxService.getMpUserInfo(mpLoginDTO.getCode());

        // 查询用户信息是否存在
        UserOauthBind userOauthBind = wxService.getUserOauthBind(mpUserInfo);
        if (userOauthBind != null) { // 用户存在直接登录
            return loginByUserId(userOauthBind.getUserId(), userOauthBind);
        }

        // 如果不存在则创建用户
        User user = new User();
        user.setNickName(genNickName());
//        user.setPointsBalance(0);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 设置免费订阅计划
        setFreeSubscriptionPlan(user);

        // 创建绑定
        userOauthBind = wxService.updateUserOauthBind(user.getId(), mpUserInfo, UserOauthBind.Platform.WX_MP);

        return loginByUser(user, userOauthBind);
    }


    public UserInfoVO loginByUserId(String userId, @Nullable UserOauthBind userOauthBind) {
        // 查询 user 信息
        User user = userMapper.selectById(userId);
        return loginByUser(user, userOauthBind);
    }

    public UserInfoVO loginByUser(User user, @Nullable UserOauthBind userOauthBind) {
        // 登录
        StpUtil.login(user.getId());
        // 缓存session
        putUserSession(user, userOauthBind);
        // 设置token
        UserInfoVO userInfoVO = UserInfoVO.from(user, fileComponent);
        userInfoVO.setToken(StpUtil.getTokenInfo().getTokenValue());

        return userInfoVO;
    }

    /**
     * 仅通过 user 更新session
     * UserSessionBO 使用原值
     *
     * @param user
     */
    private void putUserSession(User user) {
        UserSessionBO userSessionBO = getUserSession();
        UserOauthBind userOauthBind = null;
        if (userSessionBO != null) {
            userOauthBind = userSessionBO.getUserOauthBind();
        }

        putUserSession(user, userOauthBind);
    }

    private void putUserSession(User user, @Nullable UserOauthBind userOauthBind) {
        /*
            在 Sa-Token 中，Session 分为三种，分别是：
            Account-Session: 指的是框架为每个 账号id 分配的 Session
            Token-Session: 指的是框架为每个 token 分配的 Session
            Custom-Session: 指的是以一个 特定的值 作为SessionId，来分配的 Session
         */

        // 用户信息存储在 Account-Session 中，如果后面有其他跟随token移动的session 在进行额外存储
        SaSession accountSession = StpUtil.getSessionByLoginId(user.getId());
        accountSession.set(CommonConst.USER_KEY, UserSessionBO.from(user, userOauthBind));
    }

    public UserSessionBO getUserSession() {
        SaSession accountSession = StpUtil.getSessionByLoginId(StpUtil.getLoginIdAsString());
        return accountSession.getModel(CommonConst.USER_KEY, UserSessionBO.class);
    }

    public UserInfoVO info() {
        UserSessionBO userSession = getUserSession();
        return UserInfoVO.from(userSession, fileComponent);

    }

    /**
     * 刷新当前用户session
     */
    private void flushUserSession() {
        UserSessionBO userSession = getUserSession();
        UserOauthBind userOauthBind = userSession.getUserOauthBind();
        User user = userMapper.selectById(userSession.getId());
        if (userOauthBind != null) {
            if (userOauthBind.getPlatform() == UserOauthBind.Platform.WX_MP) { // 更新微信小程序信息
                userOauthBind = wxService.getUserOauthBind(user.getId(), UserOauthBind.Platform.WX_MP);
            }
            if (userOauthBind.getPlatform() == UserOauthBind.Platform.WX) {
                userOauthBind = wxService.getUserOauthBind(user.getId(), UserOauthBind.Platform.WX);
            }
        }
        putUserSession(user, userOauthBind);
    }


    public void update(UpdateUserDTO updateUserDTO, MultipartFile avatarImg) {
        LambdaUpdateWrapper<User> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(User::getId, updateUserDTO.getUserId());
        // 昵称
        updateWrapper.set(StrUtil.isNotBlank(updateUserDTO.getNickName()), User::getNickName, updateUserDTO.getNickName());

        // 提取头像
        if (avatarImg != null) {
            String originalFilename = avatarImg.getOriginalFilename();
            if (originalFilename == null) {
                throw new BusinessException(CodeEnum.ParameterError, "无法获取图片信息");
            }
            try (InputStream is = avatarImg.getInputStream()) {
                String objectName = fileComponent.genObjectName(FileDirConst.USER_AVATAR_DIR, originalFilename);
                fileComponent.upload(is, objectName);
                updateWrapper.set(User::getAvatarImg, objectName);
            } catch (IOException e) {
                throw new BusinessException(e);
            }
        }
        userMapper.update(updateWrapper);

        // 刷新 session
        flushUserSession();
    }

    /**
     * 微信网页扫码登录
     *
     * <a href="https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html">通过code获取access_token</a>
     *
     * @param wxWebSigninDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO wxWebSignin(WxWebSigninDTO wxWebSigninDTO) {
        // 校验邀请码,邀请码合法则返回对应的邀请码对应的用户
        User inviterUser = validateInviteCode(wxWebSigninDTO.getInviteCode());

        String appId = wxProperties.wa().appid();
        String secret = wxProperties.wa().secret();
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId + "&secret=" + secret + "&code=" + wxWebSigninDTO.getCode() + "&grant_type=authorization_code";
        String bodyStr = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                // 接口返回的content type  是 html/text restClient客户端无法反序列化为json格式,需要字符串接收然后在转换为json
                .body(String.class);

        if (StrUtil.isBlank(bodyStr)) {
            log.error("微信网页扫码登录失败, url: {}", url);
            throw new BusinessException(CodeEnum.ParameterError, "微信网页扫码登录失败");
        }

        JsonNode body = JsonUtil.readTree(bodyStr);

        // 通过code获取access_token
        String accessToken = body.path("access_token").asText();
        String openid = body.path("openid").asText();
        String unionid = body.path("unionid").asText();
        WxUserInfo wxUserInfo = new WxUserInfo(openid, unionid);
        if (!StrUtil.isAllNotBlank(accessToken, openid, unionid)) {
            log.error("微信网页扫码登录失败, url: {}; res: {}", url, body);
            throw new BusinessException(CodeEnum.ParameterError, "微信网页扫码登录失败");
        }

        // 判断用户是否存在
        User user;
        UserOauthBind userOauthBindExists = ChainWrappers.lambdaQueryChain(userOauthBindMapper)
                .eq(UserOauthBind::getPlatform, UserOauthBind.Platform.WX)
                .eq(UserOauthBind::getOpenid, openid)
                .one();

        if (userOauthBindExists != null) { // 存在直接登录
            user = userMapper.selectById(userOauthBindExists.getUserId());
        } else {// 不存在则注册
            // 获取用户微信信息
            String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken + "&openid=" + openid;
            String userInfoBodyStr = restClient.get()
                    .uri(URI.create(userInfoUrl))
                    .retrieve()
                    .body(String.class);
            if (StrUtil.isBlank(userInfoBodyStr)) {
                log.error("微信网页扫码登录失败, url: {}", userInfoUrl);
                throw new BusinessException(CodeEnum.ParameterError, "微信网页扫码登录失败");
            }

            JsonNode userInfoBody = JsonUtil.readTree(userInfoBodyStr);

            user = new User();
            // 头像
            String headImgUrl = userInfoBody.path("headimgurl").asText();
            if (StrUtil.isNotBlank(headImgUrl)) {
                ResponseEntity<byte[]> entity = restClient.get().uri(URI.create(headImgUrl)).retrieve().toEntity(byte[].class);
                if (entity.getBody() == null) {
                    log.error("微信网页扫码登录失败, url: {}", headImgUrl);
                    throw new BusinessException(CodeEnum.ParameterError, "微信网页扫码登录失败");
                }

                MediaType contentType = entity.getHeaders().getContentType();
                Objects.requireNonNull(contentType);

                String objectName = fileComponent.genObjectName(FileDirConst.USER_AVATAR_DIR, null);
                fileComponent.upload(new ByteArrayInputStream(entity.getBody()), objectName, contentType.toString());

                user.setAvatarImg(objectName);
            }
            user.setNickName(userInfoBody.path("nickname").asText());

            int sex = userInfoBody.path("sex").asInt(0);
            if (sex != 0) {
                user.setGender(sex - 1); // 腾讯：1-男 2-女； 我们：0-男 1-女  减1正好
            }
            user.setCreateTime(LocalDateTime.now());
//            user.setPointsBalance(0);
            userMapper.insert(user);

            // 创建绑定
            userOauthBindExists = wxService.updateUserOauthBind(user.getId(), wxUserInfo, UserOauthBind.Platform.WX);

            // 设置免费订阅计划
            setFreeSubscriptionPlan(user);

            // 邀请码事件
            inviteRegisterEvent(inviterUser, user, wxWebSigninDTO.getInviteCode());

            return loginByUser(user, userOauthBindExists);
        }
        // 登录成功
        StpUtil.login(user.getId());
        // 存储session
        putUserSession(user);
        // 返回用户信息
        UserInfoVO userInfoVO = UserInfoVO.from(user, fileComponent);
        userInfoVO.setToken(StpUtil.getTokenValue());
        return userInfoVO;
    }

    public void sendSmsCode(String mobile) {
        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(RedisConst.MOBILE_CODE_RATE.formatted(mobile), "", 1, TimeUnit.MINUTES))) {
            throw new BusinessException(CodeEnum.ParameterError, "发送过于频繁");
        }
        String code = RandomUtil.randomNumbers(4);
        Map<String, String> param = Map.of("code", code);
//        sendMsgVolc(mobile, "SPT_09a29a26", param);
        sendMsgAli(mobile, "SMS_293125070", param);
        redisTemplate.opsForValue().set(RedisConst.MOBILE_CODE.formatted(mobile), code, 5, TimeUnit.MINUTES);
    }

    /**
     * 发送短信 阿里
     *
     * @param mobile
     * @param templateId
     * @param param
     */
    private void sendMsgAli(String mobile, String templateId, Map<String, String> param) {
        log.debug("Ali send msg: mobild:{} templateId:{} param:{}", mobile, templateId, param);
        // Parameter settings for API request
        SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                .phoneNumbers(mobile)
                .signName("北京扩散未来科技")
                .templateCode(templateId)
                .templateParam(JsonUtil.toJson(param))
                .build();

        // Asynchronously get the return value of the API request
        CompletableFuture<SendSmsResponse> response = asyncClient.sendSms(sendSmsRequest);
        // Synchronously get the return value of the API request
        try {
            SendSmsResponse resp = response.get();
            log.debug("阿里短信发送结果:{}", JsonUtil.toJson(resp));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 手机号登陆，不存在则注册
     *
     * @param mobileSigninDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO mobileSignin(MobileSigninDTO mobileSigninDTO) {
        // 校验验证码
        verifySmsCode(mobileSigninDTO.getMobile(), mobileSigninDTO.getCode());

        // 校验邀请码,邀请码合法则返回对应的邀请码对应的用户
        User inviterUser = validateInviteCode(mobileSigninDTO.getInviteCode());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, mobileSigninDTO.getMobile());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            // 不存在则注册
            user = new User();
            user.setNickName(genNickName());
            user.setMobile(mobileSigninDTO.getMobile());
//            user.setPointsBalance(0);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);

            // 设置免费订阅计划
            setFreeSubscriptionPlan(user);
            // 邀请码事件
            inviteRegisterEvent(inviterUser, user, mobileSigninDTO.getInviteCode());
        }

        // 登录成功
        StpUtil.login(user.getId());

        // 存储session
        putUserSession(user);

        // 返回用户信息
        UserInfoVO userInfoVO = UserInfoVO.from(user, fileComponent);
        userInfoVO.setToken(StpUtil.getTokenValue());
        return userInfoVO;
    }

    private String genNickName() {
        return "User_" + RandomUtil.randomNumbers(4);
    }

    /**
     * 验证短信验证码
     *
     * @param mobile
     * @param verifyCode
     */
    private void verifySmsCode(String mobile, String verifyCode) {
        String smsCode = redisTemplate.opsForValue().get(RedisConst.MOBILE_CODE.formatted(mobile));

        if (!"prod".equals(profile) && "7063".equals(verifyCode)) {
            return;
        }

        if (smsCode != null && smsCode.equals(verifyCode)) {
            return;
        }

        throw new BusinessException(CodeEnum.ParameterError, "验证码错误");
    }

    private void setFreeSubscriptionPlan(User user) {
        // 获取免费订阅计划
        SubscriptionPlan freeSubscriptionPlan = subscriptionPlanService.getFreeSubscriptionPlan();
//        user.setSubscriptionPlanTierCode(freeSubscriptionPlan.getTierCode());
        billingService.grantPointsForRegister(user.getId(), freeSubscriptionPlan);
    }


    /**
     * 校验邀请码
     *
     * @throws BusinessException 校验不通过抛出异常
     */
    public @Nullable User validateInviteCode(@Nullable String inviteCode) {
        if (StrUtil.isBlank(inviteCode)) {
            return null;
        }
        // 获取邀请码所有人
        User inviterUser = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(User::getInviteCode, inviteCode)
                .one();
        if (inviterUser == null) {
            throw new BusinessException(CodeEnum.ParameterError, "邀请码错误");
        }
        return inviterUser;
    }

    /**
     * 邀请注册事件
     *
     * @param inviterUser 邀请人
     * @param inviteeUser 受邀人
     * @param inviteCode  邀请码
     */
    public void inviteRegisterEvent(@Nullable User inviterUser, User inviteeUser, @Nullable String inviteCode) {
        if (inviterUser == null || StrUtil.isBlank(inviteCode)) {
            return;
        }

//        // 校验邀请人积分是否已达上限
//        String key = RedisConst.INVITER_POINTS_LIMIT.formatted(inviterUser.getId());
//        String inviterPoint = redisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(inviterPoint) && Integer.parseInt(inviterPoint) >= InvitationRecords.MAX_INVITATION_POINTS) {
//            log.warn("邀请人积分已达上限, 忽略 {}", inviterUser);
//            return;
//        }

        // 记录积分发放信息 有效期为 7 天
        InviterGrantInfoBO inviterGrantInfoBO = new InviterGrantInfoBO();
        inviterGrantInfoBO.setInviterUserId(inviterUser.getId());
        inviterGrantInfoBO.setInviteeUserId(inviteeUser.getId());
        inviterGrantInfoBO.setInviteCode(inviteCode);
        String inviterGrantInfoKey = RedisConst.INVITER_GRANT_INFO.formatted(inviteeUser.getId());
        redisTemplate.opsForValue().set(inviterGrantInfoKey, JsonUtil.toJson(inviterGrantInfoBO), Duration.ofDays(7));


//        // 发放积分
//        billingService.grantPointsForInviter(inviterUser.getId(), inviteeUser.getId(), inviteCode);
//        redisTemplate.opsForValue().increment(key, InvitationRecords.INVITATION_POINTS);
//        // 设置有效期为今天最后时刻
//        redisTemplate.expire(key, Duration.between(LocalDateTime.now(), LocalDate.now().atTime(LocalTime.MAX)));
    }

    /**
     * 创建api key
     *
     * @return
     */
    public String createApiKey() {
        // 获取原始用户信息
        User user = userMapper.selectById(StpUtil.getLoginIdAsString());
        if (StrUtil.isNotBlank(user.getApiKey())) {
            // 如果原来存在api key 则先注销
            SaApiKeyUtil.deleteApiKey(user.getApiKey());
        }

        String apiKey = SaApiKeyUtil.createApiKeyModel().getApiKey();

        ChainWrappers.lambdaUpdateChain(userMapper)
                .eq(User::getId, user.getId())
                .set(User::getApiKey, apiKey)
                .update();

        // 刷新
        flushUserSession();

        return apiKey;
    }

    /**
     * 获取当前用户的邀请码
     *
     * @return
     */
    public String getInviteCode() {
        User user = userMapper.selectById(StpUtil.getLoginIdAsString());
        if (StrUtil.isBlank(user.getInviteCode())) {
            // 没有邀请码则更新
            for (int i = 0; i < 5; i++) {
                String inviteCode = RandomUtil.randomString(6);

                Long count = ChainWrappers.lambdaQueryChain(userMapper)
                        .eq(User::getInviteCode, inviteCode)
                        .count();
                if (count > 0) {
                    continue;
                }
                ChainWrappers.lambdaUpdateChain(userMapper)
                        .eq(User::getId, user.getId())
                        .set(User::getInviteCode, inviteCode)
                        .update();
                return inviteCode;
            }

            // 超过上限还没有邀请码则 抛出异常
            throw new BusinessException(CodeEnum.Unknow, "生成邀请码失败");
        }

        return user.getInviteCode();
    }
}
