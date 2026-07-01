package com.ohyesai.next.biz.user.vo;

import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.component.FileComponent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息")
public class UserInfoVO {

    private String id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "性别 0-男 1-女")
    private Integer gender;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "生日")
    private LocalDateTime birthDate;

    @Schema(description = "头像")
    private String avatarImg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "api key")
    private String apiKey;

    private String token;

    // 根据user对象创建UserInfoVO对象
    public static UserInfoVO from(User user, FileComponent fileComponent) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setAccount(user.getAccount());
        userInfoVO.setMobile(user.getMobile());
        userInfoVO.setNickName(user.getNickName());
        userInfoVO.setGender(user.getGender());
        userInfoVO.setBirthDate(user.getBirthDate());
        if (StrUtil.isNotBlank(user.getAvatarImg())) {
            userInfoVO.setAvatarImg(fileComponent.shareUrl(user.getAvatarImg()));
        }
        userInfoVO.setCreateTime(user.getCreateTime());
        userInfoVO.setApiKey(user.getApiKey());
        return userInfoVO;
    }
}
