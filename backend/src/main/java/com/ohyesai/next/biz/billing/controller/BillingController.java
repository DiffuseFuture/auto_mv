package com.ohyesai.next.biz.billing.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ohyesai.next.biz.billing.dto.GetPointsPriceDTO;
import com.ohyesai.next.biz.billing.service.BillingService;
import com.ohyesai.next.biz.billing.service.SubscriptionPlanService;
import com.ohyesai.next.biz.billing.vo.PointsLogVO;
import com.ohyesai.next.biz.billing.vo.PointsPackageVO;
import com.ohyesai.next.biz.billing.vo.SubscriptionPlanVO;
import com.ohyesai.next.biz.billing.vo.UserPlanVO;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.dto.PageDTO;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "计费模块")
@RestController
@RequestMapping("/billing")
@Slf4j
@AllArgsConstructor
public class BillingController {

    private final BillingService billingService;

    private final SubscriptionPlanService subscriptionPlanService;

    @Operation(summary = "获取预消耗积分")
    @GetMapping("/get-points-price")
    public Result<Integer> getPointsPrice(@ParameterObject @Validated GetPointsPriceDTO getPointsPriceDTO) {
        if (getPointsPriceDTO.getTaskType() == TaskType.MAKE_MV && getPointsPriceDTO.getDuration() < 1) {
            throw new BusinessException(CodeEnum.ParameterError, "获取视频预消耗积分时，必须传入时长");
        }

        if (getPointsPriceDTO.getTaskType() == TaskType.LIP_SYNC && getPointsPriceDTO.getDuration() < 1) {
            throw new BusinessException(CodeEnum.ParameterError, "获取视频预消耗积分时，必须传入时长");
        }

        if (getPointsPriceDTO.getTaskType() == TaskType.MAKE_MV && getPointsPriceDTO.getResolution() == null) {
            throw new BusinessException(CodeEnum.ParameterError, "获取视频预消耗积分时，分辨率不能为空");
        }

        // 获取单价
        int pointsRequired = billingService.getPointsRequired(getPointsPriceDTO.getModelName(), getPointsPriceDTO.getResolution(), getPointsPriceDTO.getTaskType());
        if (getPointsPriceDTO.getTaskType() == TaskType.MAKE_MV || getPointsPriceDTO.getTaskType() == TaskType.LIP_SYNC) {
            pointsRequired *= getPointsPriceDTO.getDuration();
        }
        return Result.success(pointsRequired);
    }

    @Operation(summary = "积分交易日志")
    @GetMapping("/points-log")
    public PageResult<PointsLogVO> getPointsLog(@ParameterObject @Validated PageDTO pageDTO) {
        return billingService.getPointsLog(pageDTO);
    }

    @Operation(summary = "获取对话session下的积分交易日志")
    @GetMapping("/session-points")
    public Result<List<PointsLogVO>> getSessionPoints(@Validated @NotBlank(message = "sessionId不能为空") String sessionId) {
        List<PointsLogVO> result = billingService.getSessionPoints(sessionId);
        return Result.success(result);
    }

    @Operation(summary = "获取订阅计划")
    @GetMapping("/subscription-plan")
    @SaIgnore
    public Result<SubscriptionPlanVO> getSubscriptionPlan() {
        SubscriptionPlanVO subscriptionPlanVO = subscriptionPlanService.getSubscriptionPlan();
        return Result.success(subscriptionPlanVO);
    }

    @Operation(summary = "获取积分包列表")
    @GetMapping("/points-package")
    @SaIgnore
    public Result<List<PointsPackageVO>> getPointsPackage() {
        List<PointsPackageVO> pointsPackageList = subscriptionPlanService.getPointsPackage();
        return Result.success(pointsPackageList);
    }

    @Operation(summary = "获取当前用户订阅计划信息")
    @GetMapping("/user-plan")
    public Result<UserPlanVO> getUserPlan() {
        UserPlanVO userPlan = subscriptionPlanService.getUserPlan();
        return Result.success(userPlan);
    }
}
