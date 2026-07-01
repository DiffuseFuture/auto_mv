package com.ohyesai.next.biz.vio.dto;

import com.ohyesai.next.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "聊天列表参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatListDTO extends PageDTO {

    @Schema(description = "会话名称")
    private String sessionName;

}
