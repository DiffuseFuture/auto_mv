package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 会员套餐规则表
 */
@Data
public class SubscriptionPlan {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 套餐编码
     */
    private String tierCode;

    /**
     * 套餐名称 (e.g., 基础版, 标准版)
     */
    private String tierName;

    /**
     * 套餐等级；主要用于判断升级策略
     */
    private Integer tierLevel;

    /**
     * 订阅价格/分
     */
    private Integer price;

    /**
     * 订阅类型
     */
    private SubscriptionType subscriptionType;

    /**
     * 赠送/包含积分数 (e.g., 780, 1800)
     */
    private Integer grantedPoints;

    /**
     * 订阅描述
     * 使用 Description 类进行序列化
     */
    private String description;

    public enum SubscriptionType {


        FREE("免费"),

        /**
         * 年付：YEAR
         */
        YEAR("年付"),

        /**
         * 月付：MONTH
         */
        MONTH("月付"),

        /**
         * 积分包: POINT_PKG
         */
        POINT_PKG("积分包");

        public final String description;

        SubscriptionType(String description) {
            this.description = description;
        }
    }

    public record Description(
            /**
             * 歌曲数量（首）
             */
            String songQuantity,

            /**
             * 歌词字幕视频
             */
            String lyricSubtitleVideo,

            /**
             * 叙事类mv
             */
            String narrativeMv,

            /**
             * 是否可商用
             */
            Boolean commercialUsable,

            /**
             * 是否去水印
             */
            Boolean watermarkRemoved
    ) {

    }
}
