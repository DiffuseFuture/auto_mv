package com.ohyesai.next.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum Platform {
    WEB,
    @Schema(description = "H5")
    H5,
    @Schema(description = "微信小程序")
    WX_MP,
}
