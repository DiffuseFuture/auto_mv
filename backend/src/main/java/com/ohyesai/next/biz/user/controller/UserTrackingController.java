package com.ohyesai.next.biz.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ohyesai.next.biz.user.dto.SaveUserTrackingDTO;
import com.ohyesai.next.biz.user.entity.UserTracking;
import com.ohyesai.next.biz.user.service.UserTrackingService;
import com.ohyesai.next.common.vo.Result;
import com.ohyesai.next.util.MiscUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-tracking")
@Tag(name = "用户行为追踪")
@AllArgsConstructor
public class UserTrackingController {

    private final UserTrackingService userTrackingService;

    @Operation(summary = "保存用户行为追踪")
    @PostMapping("/save")
    @SaIgnore
    public Result<Void> save(@RequestBody SaveUserTrackingDTO saveUserTrackingDTO, HttpServletRequest request) {
        String clientIp = MiscUtil.getClientIp(request);
        userTrackingService.save(saveUserTrackingDTO, clientIp);
        return Result.success();
    }
}
