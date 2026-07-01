package com.ohyesai.next.biz.billing.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.billing.entity.PointsGrantSchedule;
import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.biz.billing.entity.UserPointsBatch;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import com.ohyesai.next.biz.billing.mapper.PointsGrantScheduleMapper;
import com.ohyesai.next.biz.billing.mapper.PointsTransactionLogMapper;
import com.ohyesai.next.biz.billing.mapper.SubscriptionPlanMapper;
import com.ohyesai.next.biz.billing.mapper.UserPointsBatchMapper;
import com.ohyesai.next.biz.billing.vo.PointsPackageVO;
import com.ohyesai.next.biz.billing.vo.SubscriptionPlanVO;
import com.ohyesai.next.biz.billing.vo.UserPlanVO;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanMapper subscriptionPlanMapper;

    private final PointsGrantScheduleMapper pointsGrantScheduleMapper;

    private final BillingService billingService;

    private final UserMapper userMapper;

    private final UserPointsBatchMapper userPointsBatchMapper;

    private final PointsTransactionLogMapper pointsTransactionLogMapper;

    // 根据 tier code 获取 订阅信息
    public SubscriptionPlan getSubscriptionPlan(String tierCode) {
        return ChainWrappers.lambdaQueryChain(subscriptionPlanMapper)
                .eq(SubscriptionPlan::getTierCode, tierCode)
                .one();
    }

    public SubscriptionPlan getSubscriptionPlanByUser(String userId) {
        User user = userMapper.selectById(userId);
        return getSubscriptionPlan(user.getSubscriptionPlanTierCode());
    }

    /**
     * 获取免费的订阅计划
     * 免费订阅计划有且仅有一个
     *
     * @return
     */
    public SubscriptionPlan getFreeSubscriptionPlan() {
        return ChainWrappers.lambdaQueryChain(subscriptionPlanMapper)
                .eq(SubscriptionPlan::getSubscriptionType, SubscriptionPlan.SubscriptionType.FREE)
                .one();
    }

    /**
     * 积分发放任务
     * 获取所有待发放积分 进行发放
     */
    public void grantPointsTask() {
        // 更新已过期的 订阅用户状态 更新用户订阅状态为免费
        SubscriptionPlan freeSubscriptionPlan = getFreeSubscriptionPlan();

        ChainWrappers.lambdaUpdateChain(userMapper)
                .ne(User::getSubscriptionPlanTierCode, freeSubscriptionPlan.getTierCode()) // 非免费计划
                .lt(User::getSubscriptionEndTime, LocalDateTime.now()) // 过期
                .set(User::getSubscriptionPlanTierCode, freeSubscriptionPlan.getTierCode()) // 设置为免费计划
                .update();

        // 循环分批发放积分
        LocalDateTime now = LocalDateTime.now();
        while (true) {
            List<PointsGrantSchedule> pointsGrantSchedules = ChainWrappers.lambdaQueryChain(pointsGrantScheduleMapper)
                    .eq(PointsGrantSchedule::getStatus, PointsGrantSchedule.Status.PENDING)
                    .le(PointsGrantSchedule::getExpectedGrantDate, now) // 本期需要发放的积分
                    .last("limit 100")
                    .list();

            if (CollUtil.isEmpty(pointsGrantSchedules)) {
                return;
            }

            for (PointsGrantSchedule pointsGrantSchedule : pointsGrantSchedules) {
                // 获取对应订阅计划
                SubscriptionPlan subscriptionPlan = getSubscriptionPlan(pointsGrantSchedule.getTierCode());
                billingService.grantPointsForYearSubscription(pointsGrantSchedule, subscriptionPlan);
            }
        }
    }


    /**
     * 获取订阅计划
     *
     * @return
     */
    public SubscriptionPlanVO getSubscriptionPlan() {
        List<SubscriptionPlan> subscriptionPlanList = ChainWrappers.lambdaQueryChain(subscriptionPlanMapper)
//                .eq(SubscriptionPlan::getIsActive, true)
                .in(SubscriptionPlan::getSubscriptionType, List.of(
                        SubscriptionPlan.SubscriptionType.YEAR,
                        SubscriptionPlan.SubscriptionType.MONTH,
                        SubscriptionPlan.SubscriptionType.FREE
                ))
                .orderByAsc(SubscriptionPlan::getId)
                .list();

        // 根据订阅类型分组
        Map<SubscriptionPlan.SubscriptionType, List<SubscriptionPlan>> subPlanGroups = subscriptionPlanList.stream()
                .collect(Collectors.groupingBy(SubscriptionPlan::getSubscriptionType));

        List<SubscriptionPlanVO.PLan> year = subPlanGroups.get(SubscriptionPlan.SubscriptionType.YEAR)
                .stream()
                .map(SubscriptionPlanVO.PLan::from)
                .toList();

        List<SubscriptionPlanVO.PLan> month = subPlanGroups.get(SubscriptionPlan.SubscriptionType.MONTH)
                .stream()
                .map(SubscriptionPlanVO.PLan::from)
                .toList();

        List<SubscriptionPlanVO.PLan> free = subPlanGroups.get(SubscriptionPlan.SubscriptionType.FREE)
                .stream()
                .map(SubscriptionPlanVO.PLan::from)
                .toList();

        return new SubscriptionPlanVO(free, month, year);
    }

    public UserPlanVO getUserPlan() {
        User user = userMapper.selectById(StpUtil.getLoginIdAsString());
        SubscriptionPlan subscriptionPlan = getSubscriptionPlan(user.getSubscriptionPlanTierCode());

        UserPlanVO userPlan = new UserPlanVO();
        userPlan.setPointsBalance(userPointsBatchMapper.selectUserPoints(user.getId(), LocalDateTime.now()));
        if (subscriptionPlan != null) {
            userPlan.setTierCode(subscriptionPlan.getTierCode());
            userPlan.setTierName(subscriptionPlan.getTierName());
        }
        if (user.getSubscriptionEndTime() != null) {
            userPlan.setExpireTime(ZonedDateTime.of(user.getSubscriptionEndTime(), ZoneId.systemDefault()));
        }
        return userPlan;
    }

    /**
     * 获取积分包
     *
     * @return
     */
    public List<PointsPackageVO> getPointsPackage() {
        List<SubscriptionPlan> subscriptionPlans = ChainWrappers.lambdaQueryChain(subscriptionPlanMapper)
                .eq(SubscriptionPlan::getSubscriptionType, SubscriptionPlan.SubscriptionType.POINT_PKG)
                .orderByAsc(SubscriptionPlan::getId)
                .list();

        return subscriptionPlans.stream()
                .map(PointsPackageVO::from)
                .toList();
    }

    /**
     * 用户积分过期任务
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public int userPointsExpireTask() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 获取用户积分大于0 并且过期时间小于当前日期
        List<UserPointsBatch> userPointsBatches = ChainWrappers.lambdaQueryChain(userPointsBatchMapper)
                .gt(UserPointsBatch::getRemainingAmount, 0)
                .le(UserPointsBatch::getExpireTime, now)
                .list();

        // 遍历修改可用积分为0，并插入积分流水表
        List<UserPointsBatch> updateUserPointsBatches = new ArrayList<>();
        List<PointsTransactionLog> insertPointsTransactionLogs = new ArrayList<>();
        for (UserPointsBatch userPointsBatch : userPointsBatches) {
            // 统计要更新的积分批次
            userPointsBatch.setRemainingAmount(0);
            userPointsBatch.setUpdateTime(now);
            updateUserPointsBatches.add(userPointsBatch);

            // 生成积分扣减策略
            PointsTransactionLog pointsTransactionLog = new PointsTransactionLog();
            pointsTransactionLog.setUserId(userPointsBatch.getUserId());
            pointsTransactionLog.setTransactionType(PointsOperation.EXPIRED);
            pointsTransactionLog.setAmount(-userPointsBatch.getRemainingAmount());

            // 获取有效的积分余额
            int userPoints = userPointsBatchMapper.selectUserPoints(userPointsBatch.getUserId(), now);
            pointsTransactionLog.setBalanceAfter(userPoints);
            pointsTransactionLog.setDescription("积分到期失效，到期时间: " + dateFormatter.format(userPointsBatch.getExpireTime()));
            pointsTransactionLog.setCreateTime(now);
            insertPointsTransactionLogs.add(pointsTransactionLog);
        }

        userPointsBatchMapper.updateById(updateUserPointsBatches, 1000);
        pointsTransactionLogMapper.insert(insertPointsTransactionLogs, 1000);
        return updateUserPointsBatches.size();
    }

    /**
     * 根据用户查询 当前月及以后的积分发放计划  并且 状态为 PENDING、GRANTED
     *
     * @param userId
     */
    public List<PointsGrantSchedule> findPointsGrantSchedulesByUser(String userId) {
        return ChainWrappers.lambdaQueryChain(pointsGrantScheduleMapper)
                .eq(PointsGrantSchedule::getUserId, userId)
                .ge(PointsGrantSchedule::getExpectedGrantDate, LocalDate.now().withDayOfMonth(1).atStartOfDay()) // 查找本期及以后
                .in(PointsGrantSchedule::getStatus, PointsGrantSchedule.Status.PENDING, PointsGrantSchedule.Status.GRANTED) // 已发放和待发放的积分
                .list();
    }

    public Long findPointsGrantScheduleCountByUser(String userId) {
        return ChainWrappers.lambdaQueryChain(pointsGrantScheduleMapper)
                .eq(PointsGrantSchedule::getUserId, userId)
                .ge(PointsGrantSchedule::getExpectedGrantDate, LocalDate.now().withDayOfMonth(1).atStartOfDay()) // 查找本期及以后
                .in(PointsGrantSchedule::getStatus, PointsGrantSchedule.Status.PENDING, PointsGrantSchedule.Status.GRANTED) // 已发放和待发放的积分
                .count();
    }
}
