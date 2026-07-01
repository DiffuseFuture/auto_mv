package com.ohyesai.next.common.dto;

import cn.hutool.crypto.digest.DigestUtil;
import com.ohyesai.next.common.interfaces.GenCacheKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页条件通用参数
 * 如果有额外条件 继承此类
 */
@Data
@Schema(description = "分页参数")
public class PageDTO implements GenCacheKey {

    @Schema(description = "页码", example = "1")
    @NotNull(message = "页码不能为空")
    private Integer page = 1;

    @Schema(description = "页大小", example = "10")
    @NotNull(message = "页大小不能为空")
    private Integer size = 10;

}
