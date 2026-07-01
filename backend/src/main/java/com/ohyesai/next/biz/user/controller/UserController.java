package com.ohyesai.next.biz.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.user.dto.*;
import com.ohyesai.next.biz.user.entity.UserOauthBind;
import com.ohyesai.next.biz.user.service.UserService;
import com.ohyesai.next.biz.user.service.WxService;
import com.ohyesai.next.biz.user.vo.UserInfoVO;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    private final WxService wxService;

    // region ----- 登陆接口

    @Operation(summary = "小程序登录")
    @PostMapping("/mp-login")
    @SaIgnore
    public Result<UserInfoVO> mpLogin(@Validated @RequestBody MpLoginDTO mpLoginDTO) {
        UserInfoVO userInfoVO = userService.mpLogin(mpLoginDTO);
        return Result.success(userInfoVO);
    }

    @Operation(summary = "小程序手机号登录")
    @PostMapping("/mp-login-by-phone")
    @SaIgnore
    public Result<UserInfoVO> mpLoginByPhone(@Validated @RequestBody MpLoginPhoneDTO mpLoginDTO) {
        UserInfoVO userInfoVO = userService.mpLoginByPhone(mpLoginDTO);
        return Result.success(userInfoVO);
    }

    @Operation(summary = "微信web扫码登录/注册")
    @PostMapping("/wx-web-signin")
    @SaIgnore
    public Result<UserInfoVO> wxWebSignin(@RequestBody @Validated WxWebSigninDTO wxWebSigninDTO) {
        UserInfoVO userInfo = userService.wxWebSignin(wxWebSigninDTO);
        return Result.success(userInfo);
    }

    @Operation(summary = "手机号登陆/注册")
    @PostMapping("/mobile-signin")
    @SaIgnore
    public Result<UserInfoVO> mobileSignin(@RequestBody @Validated MobileSigninDTO mobileSigninDTO) {
        UserInfoVO userInfo = userService.mobileSignin(mobileSigninDTO);
        return Result.success(userInfo);
    }

    // endregion

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(userService.info());
    }

    @Operation(summary = "获取邀请码")
    @GetMapping("/invite-code")
    public Result<String> getInviteCode() {
        String inviteCode = userService.getInviteCode();
        return Result.success(inviteCode);
    }

    @Operation(summary = "修改用户信息")
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody UpdateUserDTO updateUserDTO) {
        // 设置为当前用户id
        updateUserDTO.setUserId(StpUtil.getLoginIdAsString());

        userService.update(updateUserDTO, null);
        return Result.success();
    }

    @Operation(summary = "修改用户头像")
    @PostMapping("/update-avatar-img")
    public Result<Void> updateAvatarImg(@Validated @NonNull @RequestParam("avatarImg") MultipartFile avatarImg) {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setUserId(StpUtil.getLoginIdAsString());
        userService.update(updateUserDTO, avatarImg);
        return Result.success();
    }

//    @Operation(summary = "使用userId登录 内部接口测试使用")
//    @PostMapping("/login-by-id")
//    @SaIgnore
//    public Result<UserInfoVO> loginById(@RequestParam String userId) {
//        UserOauthBind userOauthBind = wxService.getUserOauthBind(userId);
//        UserInfoVO userInfoVO = userService.loginByUserId(userId, userOauthBind);
//        return Result.success(userInfoVO);
//    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms-code")
    @SaIgnore
    public Result<Void> sendSmsCode(@RequestBody SendSmsCodeDTO sendSmsCodeDTO) {
        if (StrUtil.isBlank(sendSmsCodeDTO.getMobile()) && !StpUtil.isLogin()) {
            // 不传手机号时 必须登录
            throw new BusinessException(CodeEnum.ParameterError, "手机号不能为空");
        }
        if (StrUtil.isBlank(sendSmsCodeDTO.getMobile())) {
            sendSmsCodeDTO.setMobile(userService.info().getMobile());
        }
        userService.sendSmsCode(sendSmsCodeDTO.getMobile());
        return Result.success();
    }

    @Operation(summary = "创建 api key")
    @PostMapping("/create-api-key")
    public Result<String> createApiKey() {
        String apiKey = userService.createApiKey();
        return Result.success(apiKey);
    }
}
