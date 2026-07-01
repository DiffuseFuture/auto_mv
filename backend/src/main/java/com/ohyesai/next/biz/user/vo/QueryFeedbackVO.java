package com.ohyesai.next.biz.user.vo;

import com.ohyesai.next.biz.user.entity.UserFeedback;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询反馈")
public class QueryFeedbackVO {

//    @Schema(description = "反馈id")
//    private Integer id;

    @Schema(description = "反馈态度")
    private UserFeedback.AttitudeType attitudeType;

    @Schema(description = "反馈内容")
    private String content;
}
