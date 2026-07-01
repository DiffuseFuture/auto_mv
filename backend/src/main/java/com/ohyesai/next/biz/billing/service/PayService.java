package com.ohyesai.next.biz.billing.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.billing.bo.TransactionBO;
import com.ohyesai.next.biz.billing.bo.UpdateSubscriptionPlanResp;
import com.ohyesai.next.biz.billing.dto.TransactionsDTO;
import com.ohyesai.next.biz.billing.entity.PayOrder;
import com.ohyesai.next.biz.billing.entity.PointsGrantSchedule;
import com.ohyesai.next.biz.billing.entity.SubscriptionPlan;
import com.ohyesai.next.biz.billing.entity.UserPointsBatch;
import com.ohyesai.next.biz.billing.enums.InvitationPointsRule;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import com.ohyesai.next.biz.billing.mapper.PayOrderMapper;
import com.ohyesai.next.biz.billing.mapper.PointsGrantScheduleMapper;
import com.ohyesai.next.biz.billing.mapper.UserPointsBatchMapper;
import com.ohyesai.next.biz.billing.vo.TransactionsVO;
import com.ohyesai.next.biz.user.bo.UserSessionBO;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import com.ohyesai.next.biz.user.service.UserService;
import com.ohyesai.next.common.consts.PointsConst;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.StateEnum;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.PayProperties;
import com.ohyesai.next.component.FeiShuNotifyComponent;
import com.ohyesai.next.trace.ProxyThread;
import com.ohyesai.next.util.JsonUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PayService {

    /**
     * 订单超时时间  20 分钟
     */
    private final static int ORDER_TIME_OUT = 20;

    private final NativePayService nativePayService;

    private final AlipayClient alipayClient;

    private final PayProperties payProperties;

    private final StringRedisTemplate redisTemplate;

    private final PayOrderMapper payOrderMapper;

    private final UserService userService;

    private final JsapiServiceExtension jsapiServiceExtension;

    private final BillingService billingService;

    private final SubscriptionPlanService subscriptionPlanService;

    private final UserMapper userMapper;

    private final PointsGrantScheduleMapper pointsGrantScheduleMapper;

    private final FeiShuNotifyComponent feiShuNotifyComponent;

    private final UserPointsBatchMapper userPointsBatchMapper;

    public void payNotifyEvent(TransactionBO transactionBO) {
        ProxyThread.startVirtualThread(() -> {
            // 获取用户信息
            User user = userMapper.selectById(transactionBO.getUserId());
            SubscriptionPlan subscriptionPlan = transactionBO.getSubscriptionPlan();
            feiShuNotifyComponent.sendNotify("喜报", "用户【%s】刚刚购买了【%s-%s】套餐".formatted(
                            user.getNickName(),
                            subscriptionPlan.getSubscriptionType().description,
                            subscriptionPlan.getTierName()
                    )
            );
        });
    }

    /**
     * 支付回调
     */
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(String outTradeNo) {
        String json = redisTemplate.opsForValue().get(RedisConst.PAY_ORDER.formatted(outTradeNo));
        if (StrUtil.isBlank(json)) {
            log.error("payCallback 订单不存在: {}", outTradeNo);
            return;
        }
        TransactionBO transactionBO = JsonUtil.toObject(json, TransactionBO.class);

        if (transactionBO.getStatus() == StateEnum.SUCCESS) {
            log.warn("payCallback 订单已处理,本次操作忽略, outTradeNo {}", outTradeNo);
            return;
        }

        if (transactionBO.isUpgrade()) {// 升级订阅
            upgradeSubscription(outTradeNo, transactionBO);
        } else {// 直接订阅
            // 充值积分 积分包与订阅为两套充值逻辑
            switch (transactionBO.getSubscriptionPlan().getSubscriptionType()) {
                // 订阅购买
                case YEAR, MONTH -> rechargeSubscription(outTradeNo, transactionBO);
                // 积分包购买
                case POINT_PKG -> rechargePointPackage(outTradeNo, transactionBO);
            }
        }

        // 更新状态为成功
        transactionBO.setStatus(StateEnum.SUCCESS);
        // 更新持久化状态
        ChainWrappers.lambdaUpdateChain(payOrderMapper)
                .eq(PayOrder::getTradeNo, outTradeNo)
                .set(PayOrder::getState, StateEnum.SUCCESS)
                .update();

        redisTemplate.opsForValue().set(RedisConst.PAY_ORDER.formatted(outTradeNo), JsonUtil.toJson(transactionBO), Duration.ofMinutes(30));

        // 发放积分事件
        billingService.inviteRegisterEvent(transactionBO.getUserId(), InvitationPointsRule.RECHARGE);

        // 发送通知
        payNotifyEvent(transactionBO);
    }

    /**
     * 升级订阅
     * 更新积分发放策略
     * - 历史发放策略所有待发放改为废弃
     * - 生成新的积分发放策略（仅生成未发放的批次）
     * 清空本月已有的订阅积分批次
     * 充值积分
     *
     * @param outTradeNo
     * @param transactionBO
     */
    private void upgradeSubscription(String outTradeNo, TransactionBO transactionBO) {
        LocalDateTime now = LocalDateTime.now();
        // 清除 有效的订阅积分批次
        LambdaQueryWrapper<UserPointsBatch> deleteQueryWrapper = new LambdaQueryWrapper<>();
        deleteQueryWrapper.eq(UserPointsBatch::getUserId, transactionBO.getUserId())
                .eq(UserPointsBatch::getPointsType, PointsOperation.RECHARGE_SUBSCRIPTION) // 订阅积分
                .ge(UserPointsBatch::getExpireTime, now); // 还在有效期内
        userPointsBatchMapper.delete(deleteQueryWrapper);

        // 充值当月积分
        SubscriptionPlan subscriptionPlan = transactionBO.getSubscriptionPlan();
        int currentMonthPoints = subscriptionPlan.getGrantedPoints() - transactionBO.getUsedPoints(); // 本月积分 减去使用积分
        User user = billingService.upgradeUserPoints(
                transactionBO.getUserId(),
                1,
                subscriptionPlan.getGrantedPoints(),
                currentMonthPoints,
                now.plusMonths(PointsConst.POINTS_GRANT_INTERVAL),
                subscriptionPlan,
                PointsOperation.RECHARGE_SUBSCRIPTION
        ).user();
        user.setSubscriptionPlanTierCode(subscriptionPlan.getTierCode());
        userMapper.updateById(user);

        // 年度订阅要生成预发放记录
        if (subscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.YEAR) {
            // 查询历史批次
            List<PointsGrantSchedule> pointsGrantSchedules = subscriptionPlanService.findPointsGrantSchedulesByUser(transactionBO.getUserId());

            // 积分发放批次的时间应该与原始批次的年月相同 日期和时间为当前时刻
            int nowDay = now.getDayOfMonth();
            LocalTime nowTime = now.toLocalTime();
            List<PointsGrantSchedule> schedules = new ArrayList<>();
            for (int i = 0; i < pointsGrantSchedules.size(); i++) {
                PointsGrantSchedule oldPointsGrantSchedule = pointsGrantSchedules.get(i);

                PointsGrantSchedule schedule = new PointsGrantSchedule();
                schedule.setUserId(transactionBO.getUserId());
                schedule.setTradeNo(outTradeNo);
                schedule.setTierCode(subscriptionPlan.getTierCode());
                schedule.setPeriodIndex(oldPointsGrantSchedule.getPeriodIndex()); // 批次要与旧批次相同

                // 积分发放日期应该与历史年月相同  天和时间为当前时刻
                LocalDate oldLocalDate = oldPointsGrantSchedule.getExpectedGrantDate().toLocalDate();
                int maxDay = oldLocalDate.lengthOfMonth();
                int validDay = Math.min(nowDay, maxDay); // 防止出现2月29这种情况
                LocalDateTime expectedGrantDate = oldLocalDate.withDayOfMonth(validDay).atTime(nowTime);

                schedule.setExpectedGrantDate(expectedGrantDate);
                if (i == 0) { // 第一批按已发放处理
                    schedule.setStatus(PointsGrantSchedule.Status.GRANTED);
                    schedule.setActualGrantTime(schedule.getExpectedGrantDate());
                    schedule.setPointsToGrant(currentMonthPoints); // 升级的本月积分要减去已用积分
                } else {
                    schedule.setStatus(PointsGrantSchedule.Status.PENDING);
                    schedule.setActualGrantTime(null);
                    schedule.setPointsToGrant(subscriptionPlan.getGrantedPoints());
                }

                schedule.setCreatedAt(now);
                schedules.add(schedule);

                // 计算下一次发放时间
//                expectedGrantDate = expectedGrantDate.plusMonths(PointsConst.POINTS_GRANT_INTERVAL);

                // 旧批次改为废弃
                oldPointsGrantSchedule.setStatus(PointsGrantSchedule.Status.ABANDONED);
            }
            // 更新旧批次为废弃
            pointsGrantScheduleMapper.updateById(pointsGrantSchedules);
            // 插入新批次
            pointsGrantScheduleMapper.insert(schedules);

        }

    }

    /**
     * 积分包充值
     */
    private void rechargePointPackage(String _outTradeNo, TransactionBO transactionBO) {
        SubscriptionPlan subscriptionPlan = transactionBO.getSubscriptionPlan();
        if (subscriptionPlan.getSubscriptionType() != SubscriptionPlan.SubscriptionType.POINT_PKG) {
            throw new BusinessException(CodeEnum.ParameterError, "传入了不支持的积分包");
        }
        // 充值积分 设置有效期
        billingService.rechargeUserPoints(transactionBO.getUserId(),
                1,
                LocalDateTime.now().plusMonths(PointsConst.POINT_PKG_VALIDITY),
                subscriptionPlan,
                PointsOperation.RECHARGE_POINT_PACKAGE
        );
    }

    /**
     * 订阅充值
     *
     * @param outTradeNo
     * @param transactionBO
     */
    private void rechargeSubscription(String outTradeNo, TransactionBO transactionBO) {
        LocalDateTime now = LocalDateTime.now();
        // 充值当月积分
        SubscriptionPlan subscriptionPlan = transactionBO.getSubscriptionPlan();
        User user = billingService.rechargeUserPoints(transactionBO.getUserId(),
                        1,
                        now.plusMonths(PointsConst.POINTS_GRANT_INTERVAL),
                        subscriptionPlan,
                        PointsOperation.RECHARGE_SUBSCRIPTION
                )
                .user();
        // 设置用户套餐及过期时间
        LocalDateTime subscriptionEndTime = switch (subscriptionPlan.getSubscriptionType()) {
            case MONTH -> now.plusMonths(1);
            case FREE -> {
                log.error("传入了不支持的订阅计划");
                throw new BusinessException(CodeEnum.ParameterError, "传入了不支持的订阅计划 free");
            }
            case YEAR -> now.plusYears(1);
            default -> throw new BusinessException(CodeEnum.ParameterError, "传入了不支持的订阅计划");
        };
        user.setSubscriptionPlanTierCode(subscriptionPlan.getTierCode());
        user.setSubscriptionEndTime(subscriptionEndTime);
        userMapper.updateById(user);
        // 年度订阅要生成预发放记录
        if (subscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.YEAR) {
            LocalDateTime expectedGrantDate = now; // 下一次预发放时间
            List<PointsGrantSchedule> schedules = new ArrayList<>();
            for (int i = 1; i <= 12; i++) { // 12个月
                PointsGrantSchedule schedule = new PointsGrantSchedule();
                schedule.setUserId(transactionBO.getUserId());
                schedule.setTradeNo(outTradeNo);
                schedule.setTierCode(subscriptionPlan.getTierCode());
                schedule.setPeriodIndex(i);
                schedule.setPointsToGrant(subscriptionPlan.getGrantedPoints());
                schedule.setExpectedGrantDate(expectedGrantDate);
                if (i == 1) { // 第一批按已发放处理
                    schedule.setStatus(PointsGrantSchedule.Status.GRANTED);
                    schedule.setActualGrantTime(schedule.getExpectedGrantDate());
                } else {
                    schedule.setStatus(PointsGrantSchedule.Status.PENDING);
                    schedule.setActualGrantTime(null);
                }

                schedule.setCreatedAt(now);
                schedules.add(schedule);

                // 计算下一次发放时间
                expectedGrantDate = expectedGrantDate.plusMonths(PointsConst.POINTS_GRANT_INTERVAL);
            }
            pointsGrantScheduleMapper.insert(schedules);
        }
    }

    public TransactionsVO transactions(TransactionsDTO transactionsDTO) {
        // 使用tier code 换取订阅信息
        SubscriptionPlan newSubscriptionPlan = subscriptionPlanService.getSubscriptionPlan(transactionsDTO.getTierCode());
        if (newSubscriptionPlan == null || newSubscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.FREE) {
            throw new BusinessException(CodeEnum.ParameterError, "订单异常，不支持的订阅类型");
        }

        // 判断当前用户是否在有效订阅期间内
        User user = userMapper.selectById(StpUtil.getLoginIdAsString());
        // 是处在有效期内
        boolean isInSubscription = user.getSubscriptionEndTime() != null && user.getSubscriptionEndTime().isAfter(LocalDateTime.now());

        // 积分包 必须在有效期内
        // 校验订阅类型与当前订阅状态的合法性
        boolean isPointPkg = newSubscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.POINT_PKG;

        if (isPointPkg && !isInSubscription) { // 积分包 需在有效期
            throw new BusinessException(CodeEnum.ParameterError, "购买积分包需要先订阅会员");
        }

        int amountF; // 订阅所需金额
        int usedPoints;
        boolean upgrade;
        // 订阅并且再有效期内 视为升级操作
        if (!isPointPkg && isInSubscription) {
            // 获取用户原始订阅套餐
            SubscriptionPlan oldSubscriptionPlan = subscriptionPlanService.getSubscriptionPlan(user.getSubscriptionPlanTierCode());
            UpdateSubscriptionPlanResp updateSubscriptionPlanResp = updateSubscriptionPlan(user, oldSubscriptionPlan, newSubscriptionPlan);

            amountF = updateSubscriptionPlanResp.diffAmountF();
            usedPoints = updateSubscriptionPlanResp.usedPoints();
            upgrade = true;
        } else { // 常规购买
            amountF = newSubscriptionPlan.getPrice();
            usedPoints = 0;
            upgrade = false;
        }

        // 获取价格；白名单为1分钱
        int payAmountF; // 实际支付金额
        if (payProperties.payWhiteList().contains(user.getMobile())) {
            payAmountF = 1;
        } else {
            payAmountF = amountF;
        }

        String desc = newSubscriptionPlan.getTierName();

        // 统一下单
        TransactionsVO transactionsVO = switch (transactionsDTO.getPayKind()) {
            case WX_PC -> wxNativeTransactions(payAmountF, desc);
            case ALI_PC -> aliPcTransactions(payAmountF, desc);
            case WX_MP -> wxJsapiTransactions(payAmountF, desc);
        };

        // 缓存订单信息
        cacheTransaction(transactionsVO.getTradeNo(), usedPoints, upgrade, newSubscriptionPlan);

        transactionsVO.setAmountF(amountF);

        return transactionsVO;
    }

    /**
     * 升级订阅套餐 升级公式遵循下面示例
     * <p>
     * # 月付升级方案
     * 基础版: 78
     * 标准版：160
     * <p>
     * 用户在使用了1000积分后升级：（基础 -> 标准）
     * 补差价：160-78 = 82
     * 当月赠送积分：7800-1000 = 6800
     *
     *
     * <p>
     * # 年付升级方案
     * 基础版：65x12=780
     * 标准版：130x12=1560
     * 用户第三个月在使用了1000积分后进行升级：（基础 -> 标准）
     * 补差价：130*(12-2) - 65*(12-2) = 650
     * 当月赠送积分：18000-1000 = 17000
     *
     * @param oldSubscriptionPlan 当前所处订阅
     * @param newSubscriptionPlan 新的订阅
     * @return
     */
    private UpdateSubscriptionPlanResp updateSubscriptionPlan(User user, SubscriptionPlan oldSubscriptionPlan, SubscriptionPlan newSubscriptionPlan) {
        // 判断是否为向上升级  新的订阅级别小于老订阅 则不允许
        if (newSubscriptionPlan.getTierLevel() <= oldSubscriptionPlan.getTierLevel()) {
            throw new BusinessException(CodeEnum.ParameterError, "仅支持升级订阅");
        }
        // 只能同订阅类型升级
        if (newSubscriptionPlan.getSubscriptionType() != oldSubscriptionPlan.getSubscriptionType()) {
            throw new BusinessException(CodeEnum.ParameterError, "仅支持同订阅类型升级");
        }
        // 仅支持年付和月付进行升级
        if (newSubscriptionPlan.getSubscriptionType() != SubscriptionPlan.SubscriptionType.YEAR && newSubscriptionPlan.getSubscriptionType() != SubscriptionPlan.SubscriptionType.MONTH) {
            throw new BusinessException(CodeEnum.ParameterError, "仅支持年付和月付进行升级");
        }

        // 获取当前月积分使用情况
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(); // 本月起始时刻
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        List<UserPointsBatch> userPointsBatchs = ChainWrappers.lambdaQueryChain(userPointsBatchMapper)
                .eq(UserPointsBatch::getUserId, user.getId())
                .eq(UserPointsBatch::getPointsType, PointsOperation.RECHARGE_SUBSCRIPTION) // 订阅获得的积分 一个月只会有一条
                .ge(UserPointsBatch::getCreateTime, monthStart)
                .lt(UserPointsBatch::getCreateTime, monthEnd)
                .list();

        if (userPointsBatchs.size() > 1) {
            log.error("用户积分批次异常,同一个月份出现了多条订阅积分批 UserPointsBatchs:{}", userPointsBatchs);
            throw new BusinessException(CodeEnum.Unknow);
        }


        int usedPoints = 0; // 已用掉积分
        if (!userPointsBatchs.isEmpty()) {
            UserPointsBatch userPointsBatch = userPointsBatchs.getFirst();
            usedPoints = userPointsBatch.getTotalAmount() - userPointsBatch.getRemainingAmount();
        }


        int monthPriceDiff;
        // 计算月升级差价
        if (newSubscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.MONTH) {
            monthPriceDiff = newSubscriptionPlan.getPrice() - oldSubscriptionPlan.getPrice();
        } else if (newSubscriptionPlan.getSubscriptionType() == SubscriptionPlan.SubscriptionType.YEAR) {
            // 原订阅 月单价
            int oldMonthPrice = oldSubscriptionPlan.getPrice() / 12;
            // 新订阅 月单价
            int newMonthPrice = newSubscriptionPlan.getPrice() / 12;

            // 查询本月及以后的积分发放计划
            int billingMonth = Math.toIntExact(subscriptionPlanService.findPointsGrantScheduleCountByUser(user.getId()));
            // 原来订阅还剩几个月
//            YearMonth startMonth = YearMonth.from(LocalDateTime.now());
//            YearMonth endMonth = YearMonth.from(user.getSubscriptionEndTime());
//            long billingMonth = ChronoUnit.MONTHS.between(startMonth, endMonth) + 1;// 获取剩余月数 +1 是包含当前月
//            int billingMonth = pointsGrantSchedules.size();

            // 计算需要补的差价
            monthPriceDiff = newMonthPrice * billingMonth - oldMonthPrice * billingMonth;
        } else {
            throw new BusinessException(CodeEnum.ParameterError, "仅支持年付和月付进行升级");
        }

        return new UpdateSubscriptionPlanResp(usedPoints, monthPriceDiff);


    }

    /**
     * 微信 navtive 支付下单
     *
     * @return 二维码
     */
    private TransactionsVO wxNativeTransactions(int amountF, String description) {

        String tradeNo = IdUtil.fastSimpleUUID();

        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        // 人民币单位：分
        // 1元10积分
        amount.setTotal(amountF);
        request.setAmount(amount);
        request.setAppid(payProperties.wx().appId());
        request.setMchid(payProperties.wx().merchantId());
        request.setDescription(description);
        request.setNotifyUrl(payProperties.wx().notifyUrl());
        // 内部订单号
        request.setOutTradeNo(tradeNo);

        // 20分钟后过期
        String ttl = ZonedDateTime.now()
                .plusMinutes(ORDER_TIME_OUT)
                // 截断秒部分  它不需要
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        request.setTimeExpire(ttl);

        // 调用下单方法，得到应答
        PrepayResponse response = nativePayService.prepay(request);
        // 使用微信扫描 code_url 对应的二维码，即可体验Native支付
        String codeUrl = response.getCodeUrl();

        // 缓存订单信息
//        cacheTransaction(tradeNo, amountF, transactionsDTO);

        return TransactionsVO.from(tradeNo, codeUrl);
    }

    /**
     *
     * @return 2
     */
    private int calcWxAmount(int point) {
//        Set<String> whitelist = Set.of("18200585623");
////        UserSessionBO userSession = userService.getUserSession();
////        if (whitelist.contains(userSession.getMobile())) {
////            return 1;
////        }
        return point * 10;
    }

    /**
     * 微信 jsapi 支付下单
     */
    private TransactionsVO wxJsapiTransactions(int amountF, String description) {
        String tradeNo = IdUtil.fastSimpleUUID();

//        int amountF = calcWxAmount(transactionsDTO.getPoint());

        UserSessionBO userSessionBO = userService.getUserSession();

        com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request = new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
        com.wechat.pay.java.service.payments.jsapi.model.Amount amount = new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        // 单位是分 一个积分一毛钱
        amount.setTotal(amountF);
        request.setAmount(amount);
        request.setAppid(payProperties.wx().appId());
        request.setMchid(payProperties.wx().merchantId());
        request.setDescription(description);
        request.setNotifyUrl(payProperties.wx().notifyUrl());
        // 内部订单号
        request.setOutTradeNo(tradeNo);
        // 20分钟后过期
        String ttl = ZonedDateTime.now()
                .plusMinutes(ORDER_TIME_OUT)
                // 截断秒部分  它不需要
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        request.setTimeExpire(ttl);

        Payer payer = new Payer();
        payer.setOpenid(userSessionBO.getUserOauthBind().getOpenid());
        request.setPayer(payer);

        PrepayWithRequestPaymentResponse response = jsapiServiceExtension.prepayWithRequestPayment(request);
        // 缓存订单信息
//        cacheTransaction(tradeNo, amountF, transactionsDTO);

        return TransactionsVO.fromMp(tradeNo, response);
    }

    /**
     * 缓存订单信息，等待回调使用
     */
    private void cacheTransaction(String tradeNo, Integer deductedPoints, boolean upgrade, SubscriptionPlan subscriptionPlan) {
        // 缓存订单信息
        TransactionBO transactionBO = new TransactionBO();
        transactionBO.setUserId(StpUtil.getLoginIdAsString());
        transactionBO.setTradeNo(tradeNo);
//        transactionBO.setPoint(subscriptionPlan.getGrantedPoints());
//        transactionBO.setAmountF(amountF);
        transactionBO.setStatus(StateEnum.WAIT);
        transactionBO.setSubscriptionPlan(subscriptionPlan);
        transactionBO.setUsedPoints(deductedPoints);
        transactionBO.setUpgrade(upgrade);

        redisTemplate.opsForValue().set(RedisConst.PAY_ORDER.formatted(tradeNo), JsonUtil.toJson(transactionBO), Duration.ofMinutes(20));

        // 持久化订单信息
        PayOrder payOrder = new PayOrder();
        payOrder.setUserId(transactionBO.getUserId());
        payOrder.setTierCode(subscriptionPlan.getTierCode());
        payOrder.setTradeNo(transactionBO.getTradeNo());
        payOrder.setState(StateEnum.WAIT);
        payOrder.setAmount(subscriptionPlan.getPrice());
        payOrder.setCreateTime(LocalDateTime.now());
        payOrderMapper.insert(payOrder);
    }

    /**
     * <a href="https://opendocs.alipay.com/open/59da99d0_alipay.trade.page.pay?pathHash=e26b497f&scene=22&ref=api">doc</a>
     */
    private TransactionsVO aliPcTransactions(int amountF, String description) {
        String tradeNo = IdUtil.fastSimpleUUID();

        // 构造请求参数以调用接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(payProperties.ali().notifyUrl());

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();

        // 设置商户订单号
        model.setOutTradeNo(tradeNo);

        // 设置订单总金额(元)
        BigDecimal dj = new BigDecimal("0.01");
        BigDecimal count = new BigDecimal(amountF);
        String amountY = dj.multiply(count).toString(); // 元

        model.setTotalAmount(amountY);

        // 设置订单标题
        model.setSubject(description);

        // 设置产品码
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 设置PC扫码支付的方式
        model.setQrPayMode("4");
        model.setQrcodeWidth(105L);

        // 设置商户自定义二维码宽度
//        model.setQrcodeWidth(100L);

        // 设置订单绝对超时时间 20 分钟
        ZonedDateTime localDateTime = ZonedDateTime.now().plusMinutes(ORDER_TIME_OUT);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        model.setTimeExpire(localDateTime.format(dateTimeFormatter));

        request.setBizModel(model);
        // 第三方代调用模式下请设置app_auth_token
        // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            log.info(response.getBody());
            // 缓存订单信息

//            cacheTransaction(tradeNo, amountF, transactionsDTO);
            return TransactionsVO.from(tradeNo, response.getBody());
        } catch (AlipayApiException e) {
            throw new BusinessException(e);
        }
    }

    public Integer transactionsState(String tradeNo) {
        String json = redisTemplate.opsForValue().get(RedisConst.PAY_ORDER.formatted(tradeNo));
        if (StrUtil.isBlank(json)) {
            log.error("订单不存在:{}", tradeNo);
            return StateEnum.FAIL.code;
        }
        TransactionBO transactionBO = JsonUtil.toObject(json, TransactionBO.class);
        return transactionBO.getStatus().code;
    }
}
