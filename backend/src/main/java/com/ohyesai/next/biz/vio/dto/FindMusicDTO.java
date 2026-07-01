package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询视频信息参数")
public class FindMusicDTO extends PageDTO {

    private String projectName;
}
