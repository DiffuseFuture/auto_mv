package com.ohyesai.next.biz.user.controller;

import com.ohyesai.next.biz.user.dto.AddFeedbackDTO;
import com.ohyesai.next.biz.user.service.FeedbackService;
import com.ohyesai.next.biz.user.vo.QueryFeedbackVO;
import com.ohyesai.next.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
@Tag(name = "反馈模块")
@AllArgsConstructor
public class FeedbackController {

    private FeedbackService feedbackService;

    @Operation(summary = "添加反馈")
    @PostMapping("/add")
    public Result<Void> addFeedback(@Validated @RequestBody AddFeedbackDTO feedback) {
        feedbackService.addFeedback(feedback);
        return Result.success();
    }

    @Operation(summary = "查询反馈")
    @GetMapping("/query")
    public Result<QueryFeedbackVO> queryFeedback(@Validated @NotBlank String messageId) {
        QueryFeedbackVO queryFeedback = feedbackService.queryFeedback(messageId);
        return Result.success(queryFeedback);
    }
}
