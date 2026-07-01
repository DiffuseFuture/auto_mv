package com.ohyesai.next.component;

import com.ohyesai.next.biz.billing.service.SubscriptionPlanService;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.consts.RedisConst;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Component
@AllArgsConstructor
public class TaskScheduleComponent {

    private final SubscriptionPlanService subscriptionPlanService;

    private final RedissonClient redissonClient;

    /**
     * 定时清理 本机缓存文件
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredCache() {
        File targetDir = new File(FileDirConst.LOCAL_CACHE_DIR);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            log.warn("缓存目录不存在或不是目录: {}", targetDir.getAbsolutePath());
            return;
        }

        log.info("开始清理本地缓存目录: {}", targetDir.getAbsolutePath());

        long expireTime = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        AtomicInteger deletedCount = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(targetDir.toPath(), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < expireTime;
                        } catch (IOException e) {
                            log.error("获取文件修改时间失败: {}", path, e);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            LocalDateTime modifiedTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis()),
                                    ZoneId.systemDefault()
                            );
                            Files.deleteIfExists(path);
                            log.info("删除过期文件: {}, 修改时间: {}", path, modifiedTime);
                            deletedCount.incrementAndGet();
                        } catch (IOException e) {
                            log.error("删除文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("清理缓存失败", e);
        }

        log.info("清理完成，共删除 {} 个文件", deletedCount.get());
    }

    /**
     * 每天凌晨1点 发放用户积分
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void grantUserPoints() {
        log.info("开始发放用户积分");
        RLock lock = redissonClient.getLock(RedisConst.POINTS_GRANT_LOCK);
        if (lock.tryLock()) {
            try {
                log.info("发放用户积分开始执行");
                subscriptionPlanService.grantPointsTask();
                log.info("发放用户积分结束执行");
            } finally {
                lock.unlock();
                log.info("发放用户积分锁释放成功");
            }
        } else {
            log.warn("发放用户积分锁被占用");
        }
    }

    /**
     * 每小时执行一次  用户积分到期
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void userPointsExpire() {
        log.info("开始处理用户积分到期");
        RLock lock = redissonClient.getLock(RedisConst.POINTS_EXPIRE_LOCK);
        if (lock.tryLock()) {
            try {
                log.info("处理用户积分到期开始执行");
                int updateCount = subscriptionPlanService.userPointsExpireTask();
                log.info("处理用户积分到期结束执行,处理了 {} 条记录",updateCount);
            }finally {
                lock.unlock();
            }
        }


    }
}
