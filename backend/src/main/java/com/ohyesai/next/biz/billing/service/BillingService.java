package com.ohyesai.next.biz.billing.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.billing.bo.ChangeUserPointsRespBO;
import com.ohyesai.next.biz.billing.entity.*;
import com.ohyesai.next.biz.billing.enums.InvitationPointsRule;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import com.ohyesai.next.biz.billing.mapper.*;
import com.ohyesai.next.biz.billing.vo.PointsLogVO;
import com.ohyesai.next.biz.user.bo.InviterGrantInfoBO;
import com.ohyesai.next.biz.user.entity.InvitationRecords;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.mapper.InvitationRecordsMapper;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.consts.PointsConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.dto.PageDTO;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.vo.PageResult;
import com.ohyesai.next.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class BillingService {

    /**
     * 最低积分限制
     */
    public static final int MIN_POINTS = 10;

    private final UserMapper userMapper;

    private final ModelPricingRuleMapper modelPricingRuleMapper;

    private final PointsTransactionLogMapper pointsTransactionLogMapper;

    private final PointsGrantScheduleMapper pointsGrantScheduleMapper;

    private final RedissonClient redissonClient;

    private final SessionPointsTransactionMapMapper sessionPointsTransactionMapMapper;

    private final InvitationRecordsMapper invitationRecordsMapper;

    private final StringRedisTemplate redisTemplate;

    private final UserPointsBatchMapper userPointsBatchMapper;

    /**
     * 任务消费积分
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangeUserPointsRespBO consumePoints(String userId, ModelEnum modelName, TaskType taskType, int points) {
        if (points < 1) {
            log.error("积分数小于1，忽略本次操作");
            throw new BusinessException(CodeEnum.ParameterError, "消费积分数不能小于1");
        }
        String desc = taskType.taskName + "任务消费积分";
        if (modelName != ModelEnum.NONE) {
            desc += "，模型：" + modelName.modelName;
        }

        // 检查邀请发放积分；邀请机制：当受邀人消耗积分时才会发放积分
//        inviteRegisterEvent(userId);


        // 积分改为负数 减去积分
        return changeUserPoints(userId, -points, null, desc, null, PointsOperation.CONSUME);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeUserPointsRespBO consumePoints(String userId, ModelEnum modelName, Resolution resolution, TaskType taskType, int points) {
        if (points < 1) {
            log.error("积分数小于1，忽略本次操作");
            throw new BusinessException(CodeEnum.ParameterError, "消费积分数不能小于1");
        }
        String desc = taskType.taskName + "任务消费积分";
        if (modelName != ModelEnum.NONE) {
            desc += "，模型：" + modelName.modelName + ", 分辨率：" + resolution;
        }

        // 检查邀请发放积分；邀请机制：当受邀人消耗积分时才会发放积分
//        inviteRegisterEvent(userId);


        // 积分改为负数 减去积分
        return changeUserPoints(userId, -points, null, desc, null, PointsOperation.CONSUME);
    }

    /**
     * 受邀人发放积分
     * <p> 受邀人在触发事件时 执行该方法
     *
     * @param inviteeUserId 受邀人，
     */
    public void inviteRegisterEvent(String inviteeUserId, InvitationPointsRule invitationPointsRule) {
        String inviterGrantInfoKey = RedisConst.INVITER_GRANT_INFO.formatted(inviteeUserId);
        String inviterGrantInfoStr = redisTemplate.opsForValue().get(inviterGrantInfoKey);
        if (inviterGrantInfoStr == null) { // 不存在则返回
            return;
        }

        InviterGrantInfoBO inviterGrantInfoBO = JsonUtil.toObject(inviterGrantInfoStr, InviterGrantInfoBO.class);
        // 邀请人的积分上限
        String inviterPointsKey = RedisConst.INVITER_POINTS_LIMIT.formatted(inviterGrantInfoBO.getInviterUserId());
        // 受邀人已经执行过的 发放积分事件
        String inviteeGrantEventKey = RedisConst.INVITEE_GRANTED_EVENT.formatted(inviterGrantInfoBO.getInviteeUserId());

        // 受邀人是否已经执行过 本事件
        Long addSize = redisTemplate.opsForSet().add(inviteeGrantEventKey, invitationPointsRule.name());
        if (Objects.requireNonNull(addSize) == 0) {
            log.warn("受邀人已执行过发放事件 {}", invitationPointsRule);
            return;
        }

        if (invitationPointsRule == InvitationPointsRule.MAKE_MV) {
            // 校验邀请人积分是否已达上限 目前只有 LEVEL 1 需要校验 https://e.gitee.com/diffusefuture/projects/840838/requirements/table?issue=IJLRXK
            String inviterPoint = redisTemplate.opsForValue().get(inviterPointsKey);
            if (StrUtil.isNotBlank(inviterPoint) && Integer.parseInt(inviterPoint) >= invitationPointsRule.limit) {
                log.warn("邀请人积分已达上限 {}", inviterGrantInfoBO.getInviterUserId());
                return;
            }
        }

        // 发放积分
        grantPointsForInviter(inviterGrantInfoBO.getInviterUserId(), inviterGrantInfoBO.getInviteeUserId(), inviterGrantInfoBO.getInviteCode(), invitationPointsRule);

        // 记录邀请人累计积分 有效期为今天最后时刻
        redisTemplate.opsForValue().increment(inviterPointsKey, invitationPointsRule.points);
        redisTemplate.expire(inviterPointsKey, Duration.between(LocalDateTime.now(), LocalDate.now().atTime(LocalTime.MAX)));

        // 清理 key
        Object[] roles = Arrays.stream(InvitationPointsRule.values()).map(InvitationPointsRule::name).toArray(); // 获取所有事件
        long count = Objects.requireNonNull(redisTemplate.opsForSet().isMember(inviteeGrantEventKey, roles)).entrySet().stream().filter(Map.Entry::getValue).count(); // 统计已经执行过的事件
        if (count == roles.length) { // 当所有事件都执行过以后 清理受邀人相关 key
            redisTemplate.delete(inviterGrantInfoKey);
            redisTemplate.delete(inviteeGrantEventKey);
        }
    }


    /**
     * 充值积分
     * <p>
     * 积分发放逻辑
     * 月付： 直接给积分到账户
     * 年付： 付款后立即发放第一个月积分到账户， 之后以付款日期开始计算，每隔30天 凌晨 发放 下个月积分
     * 积分包： 直接给积分到账户
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangeUserPointsRespBO rechargeUserPoints(String userId,
                                                     Integer periodIndex,
                                                     LocalDateTime pointsExpireTime,
                                                     SubscriptionPlan subscriptionPlan,
                                                     PointsOperation pointsOperation) {
        String desc = switch (subscriptionPlan.getSubscriptionType()) {
            case FREE, YEAR, MONTH ->
                    subscriptionPlan.getTierName() + "【" + subscriptionPlan.getSubscriptionType().description + "】套餐充值, 第 " + periodIndex + " 期";
            case POINT_PKG -> "积分包充值【" + subscriptionPlan.getTierName() + "】";
        };

        return changeUserPoints(userId, subscriptionPlan.getGrantedPoints(), null, desc, pointsExpireTime, pointsOperation);
    }

    /**
     * 主要给升级充值使用
     * 可以精确控制充值积分与可用的积分
     *
     * @param userId
     * @param periodIndex
     * @param totalAmount
     * @param pointsExpireTime
     * @param subscriptionPlan
     * @param pointsOperation
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangeUserPointsRespBO upgradeUserPoints(String userId,
                                                    Integer periodIndex,
                                                    int totalAmount,
                                                    int remainingAmount,
                                                    LocalDateTime pointsExpireTime,
                                                    SubscriptionPlan subscriptionPlan,
                                                    PointsOperation pointsOperation) {

        String desc = switch (subscriptionPlan.getSubscriptionType()) {
            case FREE, YEAR, MONTH ->
                    subscriptionPlan.getTierName() + "【" + subscriptionPlan.getSubscriptionType().description + "】套餐充值, 第 " + periodIndex + " 期";
            case POINT_PKG -> "积分包充值【" + subscriptionPlan.getTierName() + "】";
        };

        return changeUserPoints(userId, totalAmount, remainingAmount, desc, pointsExpireTime, pointsOperation);
    }

    /**
     * 注册赠送积分
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangeUserPointsRespBO grantPointsForRegister(String userId, SubscriptionPlan subscriptionPlan) {
        // 更新免费用户订阅计划
        ChainWrappers.lambdaUpdateChain(userMapper)
                .eq(User::getId, userId)
                .set(User::getSubscriptionPlanTierCode, subscriptionPlan.getTierCode())
                .update();

        // 赠送积分 1个自然月
        return changeUserPoints(userId, subscriptionPlan.getGrantedPoints(), null, "新用户赠送积分", LocalDateTime.now().plusMonths(1), PointsOperation.REGISTER);
    }

    /**
     * 邀请人增加积分
     * 该方法不会对 邀请逻辑进行相关校验 只负责增加积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantPointsForInviter(String inviterUserId, String inviteeUserId, String inviteCode, InvitationPointsRule invitationPointsRule) {
        InvitationRecords invitationRecords = new InvitationRecords();
        invitationRecords.setInviterId(inviterUserId);
        invitationRecords.setInviteeId(inviteeUserId);
        invitationRecords.setInviteCode(inviteCode);
        invitationRecords.setCreateTime(LocalDateTime.now());
        invitationRecordsMapper.insert(invitationRecords);

        // 赠送积分
        changeUserPoints(inviterUserId, invitationPointsRule.points, null, "邀请新用户赠送积分", PointsConst.NEVER_EXPIRE, PointsOperation.INVITE);

    }


    /**
     * 发放 年度订阅 每个月的积分
     * 用在定时任务的积分发放上
     *
     * @param pointsGrantSchedule
     * @param subscriptionPlan
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantPointsForYearSubscription(PointsGrantSchedule pointsGrantSchedule, SubscriptionPlan subscriptionPlan) {
        if (pointsGrantSchedule.getStatus() == PointsGrantSchedule.Status.GRANTED) {
            // 积分已经发放
            log.warn("积分已经发放, 忽略 {}", pointsGrantSchedule);
            return;
        }

        if (subscriptionPlan.getSubscriptionType() != SubscriptionPlan.SubscriptionType.YEAR) {
            log.error("积分发放任务失败: 订阅计划不是年付 {}", pointsGrantSchedule);
            return;
        }
        // 发放积分；有效期为一个月
        rechargeUserPoints(pointsGrantSchedule.getUserId(),
                pointsGrantSchedule.getPeriodIndex(),
                pointsGrantSchedule.getExpectedGrantDate().plusMonths(PointsConst.POINTS_GRANT_INTERVAL),
                subscriptionPlan,
                PointsOperation.RECHARGE_SUBSCRIPTION
        );

        // 更新发放计划 ; 与发放积分动作在同一个事务中
        pointsGrantSchedule.setStatus(PointsGrantSchedule.Status.GRANTED);
        pointsGrantSchedule.setActualGrantTime(LocalDateTime.now());
        pointsGrantScheduleMapper.updateById(pointsGrantSchedule);
    }


//    @Transactional(rollbackFor = Exception.class)
//    private ChangeUserPointsRespBO changeUserPoints(String userId,
//                                                    int points,
//                                                    String description,
//                                                    @Nullable LocalDateTime pointsExpireTime,
//                                                    PointsOperation pointsType) {
//
//    }

    /**
     * 修改积分
     * 所有积分变动都应该使用这个方法
     *
     * @param userId
     * @param totalAmount      正数：增加积分   负数：扣减积分
     * @param remainingAmount  可用积分，通常与 totalAmount 相等; null 则与 totalAmount 相等
     * @param pointsExpireTime 积分有效期 永久积分使用  CommonConst.NEVER_EXPIRE; 消费积分时传null
     * @param pointsType       积分操作类型
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    private ChangeUserPointsRespBO changeUserPoints(String userId,
                                                    int totalAmount,
                                                    @Nullable Integer remainingAmount,
                                                    String description,
                                                    @Nullable LocalDateTime pointsExpireTime,
                                                    PointsOperation pointsType) {

        RLock lock = redissonClient.getLock(RedisConst.BILLING_LOCK_USER.formatted(userId));

        try {
            lock.lock();
            // 获取用户明细
            List<UserPointsBatch> userPointsBatches = userPointsBatchMapper.selectUserPointsBatch(userId, LocalDateTime.now());
            if (totalAmount < 0) { // 扣减积分
                deductUserPointsBatch(userPointsBatches, Math.abs(totalAmount));
            } else if (totalAmount > 0) { // 增加积分
                if (remainingAmount == null) {
                    remainingAmount = totalAmount;
                }
                addUserPointsBatch(userId, totalAmount, remainingAmount, pointsExpireTime, pointsType);
            } else {
                throw new BusinessException(CodeEnum.Unknow, "扣除积分不能为 0");
            }

            // 获取积分总和
            Integer newPoints = userPointsBatchMapper.selectUserPoints(userId, LocalDateTime.now());

            User user = userMapper.selectById(userId);
//            int newPoints = overwritePoints ? points : user.getPointsBalance() + points;
//            ChainWrappers.lambdaUpdateChain(userMapper)
//                    .eq(User::getId, userId)
//                    .set(User::getPointsBalance, newPoints)
//                    .update();

            // 更新结构体数据 方便后续使用
//            user.setPointsBalance(newPoints);

            PointsTransactionLog pointsTransactionLog = new PointsTransactionLog();
            pointsTransactionLog.setUserId(userId);
            pointsTransactionLog.setTransactionType(pointsType);
            pointsTransactionLog.setAmount(totalAmount);
            pointsTransactionLog.setBalanceAfter(newPoints);
            pointsTransactionLog.setDescription(description);
            pointsTransactionLog.setCreateTime(LocalDateTime.now());
            pointsTransactionLogMapper.insert(pointsTransactionLog);
            return new ChangeUserPointsRespBO(user, pointsTransactionLog);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 增加积分批次
     *
     * @param userId
     * @param totalAmount      总积分
     * @param remainingAmount  可用积分 通常来说他与总积分相同
     * @param pointsExpireTime
     * @param pointsType
     */
    private void addUserPointsBatch(String userId, int totalAmount, int remainingAmount, LocalDateTime pointsExpireTime, PointsOperation pointsType) {
        LocalDateTime now = LocalDateTime.now();
        if (pointsExpireTime == null || pointsExpireTime.isBefore(now)) {
            throw new BusinessException(CodeEnum.Unknow, "过期时间不能小于当前时间");
        }
        UserPointsBatch userPointsBatch = new UserPointsBatch();
        userPointsBatch.setUserId(userId);
        userPointsBatch.setPointsType(pointsType);
        userPointsBatch.setTotalAmount(totalAmount);
        userPointsBatch.setRemainingAmount(remainingAmount);
        userPointsBatch.setExpireTime(pointsExpireTime);
        userPointsBatch.setCreateTime(now);
        userPointsBatch.setUpdateTime(now);
        userPointsBatchMapper.insert(userPointsBatch);
    }

    /**
     * 扣减积分批次
     * UserPointsBatch 扣减
     * 扣减后会直接更新到数据库
     *
     * @param deductedPoints 要扣减的积分数；确保传入列表可扣减积分 > 0 并且均在有效期内
     */
    public void deductUserPointsBatch(List<UserPointsBatch> userPointsBatches, int deductedPoints) {
        // 判断总积分是否充足
        int totalRemainingAmount = userPointsBatches.stream().mapToInt(UserPointsBatch::getRemainingAmount).sum();
        if (totalRemainingAmount < deductedPoints) {
            throw new BusinessException(CodeEnum.PointNotEnough, "积分剩余积分不足");
        }

        // 按照扣减顺序排序  如果相同则先消费要到期的积分
        userPointsBatches = userPointsBatches.stream()
                .sorted(Comparator.comparing(UserPointsBatch::getPointsType).thenComparing(UserPointsBatch::getExpireTime))
                .toList();

        // 开始扣分
        List<UserPointsBatch> updatedUserPointsBatches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserPointsBatch userPointsBatch : userPointsBatches) {
            if (deductedPoints <= 0) {
                break; // 已经扣完了，直接退出
            }

            // 关键计算：本次能够且需要从当前批次扣除的额度
            // 它是“当前批次剩余积分”和“还需要扣减的积分”之间的最小值
            int actualDeduct = Math.min(userPointsBatch.getRemainingAmount(), deductedPoints);

            userPointsBatch.setRemainingAmount(userPointsBatch.getRemainingAmount() - actualDeduct);
            userPointsBatch.setUpdateTime(now);
            updatedUserPointsBatches.add(userPointsBatch);
            deductedPoints -= actualDeduct;

        }

        userPointsBatchMapper.updateById(updatedUserPointsBatches);

    }


    /**
     * 校验用户积分是否充足
     *
     * @param userId
     * @param amount
     * @return true 充足
     */
    public boolean checkUserBalance(String userId, int amount) {
        return compareBalance(userId, amount) >= 0;
    }

    /**
     * 检查用户 积分与目标积分的差距
     *
     * @param userId
     * @param amount
     * @return > 0 积分充足， < 0 积分不足   = 0 积分正好足够
     */
    public int compareBalance(String userId, int amount) {
        int userPoints = userPointsBatchMapper.selectUserPoints(userId, LocalDateTime.now());
        return userPoints - amount;
    }

    // 根据 modelName taskType 获取计费规则
    public ModelPricingRule getBillingRule(ModelEnum model, Resolution resolution, TaskType taskType) {
        ModelPricingRule modelPricingRule = ChainWrappers.lambdaQueryChain(modelPricingRuleMapper)
                .eq(ModelPricingRule::getModelName, model)
                .eq(Objects.nonNull(resolution), ModelPricingRule::getResolution, resolution)
                .eq(ModelPricingRule::getTaskType, taskType)
                .one();
        if (modelPricingRule == null) {
            throw new BusinessException(CodeEnum.ParameterError, "获取计费规则失败: " + model + "-" + taskType);
        }
        return modelPricingRule;
    }

    // 根据 modelName taskType 获取 积分数量
    public int getPointsRequired(ModelEnum model, Resolution resolution, TaskType taskType) {
        return getBillingRule(model, resolution, taskType).getPointsRequired();
    }

    public int getPointsRequired(ModelEnum model, TaskType taskType) {
        if (taskType == TaskType.MAKE_MV) { // mv 任务需要传入分辨率
            throw new BusinessException(CodeEnum.ParameterError, "获取计费规则失败,taskType 不能为 MAKE_MV");
        }
        return getBillingRule(model, null, taskType).getPointsRequired();
    }

    /**
     * 查询积分消耗日志
     *
     * @param pageDTO
     * @return
     */
    public PageResult<PointsLogVO> getPointsLog(PageDTO pageDTO) {

        Page<PointsTransactionLog> page = Page.of(pageDTO.getPage(), pageDTO.getSize());

        Page<PointsTransactionLog> pointsTransactionLogPage = ChainWrappers.lambdaQueryChain(pointsTransactionLogMapper)
                .eq(PointsTransactionLog::getUserId, StpUtil.getLoginIdAsString())
                .orderByDesc(PointsTransactionLog::getCreateTime)
                .page(page);

        List<PointsLogVO> pointsLogVOS = pointsTransactionLogPage.getRecords()
                .stream()
                .map(PointsLogVO::from)
                .toList();

        return PageResult.success(pointsTransactionLogPage.getTotal(), pointsLogVOS);
    }

    public List<PointsLogVO> getSessionPoints(String sessionId) {
        List<PointsTransactionLog> pointsTransactionLogs = pointsTransactionLogMapper.selectPointsTransactionLogBySessionId(sessionId);
        return pointsTransactionLogs.stream().map(PointsLogVO::from).toList();
    }

    /**
     * 映射 session 与 订单流水
     */
    public void mapSessionWithPointsTransactionLog(String sessionId, Integer pointsTransactionLogId) {
        SessionPointsTransactionMap sessionPointsTransactionMap = new SessionPointsTransactionMap();
        sessionPointsTransactionMap.setChatSessionId(sessionId);
        sessionPointsTransactionMap.setPointsTransactionLogId(pointsTransactionLogId);
        sessionPointsTransactionMapMapper.insert(sessionPointsTransactionMap);
    }


}
