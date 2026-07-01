package com.ohyesai.next.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ohyesai.next.common.consts.RedisConst;
import com.ohyesai.next.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {

    /**
     * cache manager 由spring boot自动配置
     * 声明 xxCustomizer bean 可以让自定义部分配置
     *
     * @return
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer myRedisCacheManagerBuilderCustomizer() {
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        genericJackson2JsonRedisSerializer.configure(objectMapper ->
                // 开启java8 时间格式
                objectMapper.registerModule(new JavaTimeModule())
        );
        // json 序列化方式配置
        return (builder) -> builder
                .cacheDefaults(
                        RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                                )
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(genericJackson2JsonRedisSerializer)
                                )
                                .prefixCacheNameWith(RedisConst.REDIS_PREFIX + ":")
                                // 全局默认超时1小时
                                .entryTtl(Duration.ofHours(1))
                );
    }
}
