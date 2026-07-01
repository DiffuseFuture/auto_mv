package com.ohyesai.next.biz.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ohyesai.next.common.enums.Platform;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserTracking {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 来源；从那个网站跳过来的
     */
    private String referer;

    /**
     * 目标，访问的是哪个功能点
     */
    private Target target;

    private String fromIp;

    private Platform platform;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public enum Target {
        /**
         * 首页
         */
        HOME_PAGE,

        /**
         * 登录
         */
        LOGIN,

        /**
         * 首页-SHARE-查看创作过程
         */
        HOME_SHARE_VIEW_PROCESS,

        /**
         * 首页-SHARE-分享
         */
        HOME_SHARE_SHARE,

        /**
         * 创作-音乐-下载mp3
         */
        CREATE_MUSIC_DOWNLOAD_AUDIO,

        /**
         * 创作-mv-下载视频
         */
        CREATE_MV_DOWNLOAD_VIDEO,

        /**
         * 资产-音乐-分享
         */
        PROJECT_MUSIC_SHARE,

        /**
         * 资产-mv-分享
         */
        PROJECT_MV_SHARE,

        /**
         * 创作-音乐-分享
         */
        CREATE_MUSIC_SHARE,

        /**
         * 创作-mv-分享
         */
        CREATE_MV_SHARE,

        /**
         * 资产-音乐-下载
         */
        PROJECT_MUSIC_DOWNLOAD,

        /**
         * 资产-mv-下载
         */
        PROJECT_MV_DOWNLOAD,

        /**
         * 移动端-首页
         */
//        MOBILE_HOME_PAGE,

        /**
         * 移动端-登录
         */
//        MOBILE_LOGIN,

        /**
         * 网页端首页-查看二维码
         */
//        WEB_HOME_VIEW_QR_CODE,
        HOME_VIEW_QR_CODE,

        /**
         * 网页端首页-查看邀请规则
         */
//        WEB_HOME_VIEW_INVITE_RULES,
        HOME_VIEW_INVITE_RULES,

        /**
         * 网页端首页-点击升级
         */
//        WEB_HOME_CLICK_UPGRADE,
        HOME_CLICK_UPGRADE,

        /**
         * 网页端订阅页-点击订阅
         */
//        WEB_SUBSCRIPTION_CLICK_SUBSCRIBE,
        SUBSCRIPTION_CLICK_SUBSCRIBE,

        /**
         * 网页端-进入创作对话页
         */
//        WEB_ENTER_CREATE_CHAT,
        ENTER_CREATE_CHAT,

        /**
         * 网页端-创作页-创建新项目
         */
//        WEB_CREATE_NEW_PROJECT,
        CREATE_NEW_PROJECT,

        /**
         * mv 视频编辑埋点
         */
        MV_VIDEO_EDIT,

    }
}
