package com.ohyesai.next.common.interfaces;

public interface GenCacheKey {

    /**
     * 默认实现依赖 hashCode 必须基于字段重写该方法
     * 推荐使用 lombok 注解
     * @return
     */
    default String cacheKey() {
        return String.valueOf(hashCode());
    }
}
