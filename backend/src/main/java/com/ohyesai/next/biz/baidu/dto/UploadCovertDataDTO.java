package com.ohyesai.next.biz.baidu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "上传转换数据")
public class UploadCovertDataDTO {

    @Schema(description = "带有&bd_vid=xxx的落地页url地址，只有在百度搜索广告里点击进入的落地页url，才会带有&bd_vid")
    @NotBlank(message = "logidUrl不能为空")
    private String logidUrl;

    @Schema(description = "转化类型")
    @NotNull(message = "newType不能为空")
    private NewType newType;

    public enum NewType {

        /**
         * 注册
         */
        @Schema(description = "注册")
        REGISTER(25),

        /**
         * 服务购买成功
         */
        @Schema(description = "服务购买成功")
        SERVICE_SUCCESS(10),

        /**
         * 3	表单提交成功
         */
        @Schema(description = "表单提交成功")
        FORM_SUCCESS(3),

        ;

        public final int code;

        NewType(int code) {
            this.code = code;
        }
    }
}
