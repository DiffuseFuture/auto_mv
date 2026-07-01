package com.ohyesai.next.biz.user.bo;

import cn.hutool.core.bean.BeanUtil;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.entity.UserOauthBind;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后段存储用户信息的实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionBO extends User {

    private UserOauthBind userOauthBind;

    public static UserSessionBO from(User user, UserOauthBind userOauthBind) {
        UserSessionBO userSessionBO = new UserSessionBO();
        BeanUtil.copyProperties(user, userSessionBO);
        // 绑定三方信息
        userSessionBO.setUserOauthBind(userOauthBind);

        return userSessionBO;
    }
}
