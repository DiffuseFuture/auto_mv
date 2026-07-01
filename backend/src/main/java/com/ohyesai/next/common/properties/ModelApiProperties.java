package com.ohyesai.next.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "model-api")
public record ModelApiProperties(

        /**
         * yunwu
         */
        KeyInfo yunwu,

        /**
         * omnimaas
         */
        KeyInfo omnimaas,

        /*
          阿里云百炼
         */
        KeyInfo dashscope,

        /**
         * 智谱大模型
         */
        KeyInfo bigmodel,

        /**
         * 智谱 coding
         */
        KeyInfo bigmodelCoding,

        /**
         * sunoapi
         */
        KeyInfo sunoapi,

        /**
         * 火山 方舟模型
         */
        KeyInfo volcFangzhou,

        /**
         * 国研 new api
         */
        KeyInfo guoyan,
        /**
         * vidu
         */
        KeyInfo vidu,
        /**
         * mureka
         */
        KeyInfo mureka,
        /**
         * replicate
         */
        KeyInfo replicate,
        /**
         * volc-mediakit
         */
        KeyInfo volcMediakit

) {

    public record KeyInfo(
            String appId,
            String ak,

            /**
             * AccessToken
             */
            String at,
            String sk
    ) {
    }
}
