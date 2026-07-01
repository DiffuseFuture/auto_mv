package com.ohyesai.next.common.consts;

import java.time.LocalDateTime;

public interface PointsConst {

    /**
     * 永不过期的时间
     * 永不过期通常使用一个不可能到达的未来日期，在索引和查询上这么做通常会很方便
     */
    LocalDateTime NEVER_EXPIRE = LocalDateTime.of(2126, 5, 25, 0, 0, 0);

    /**
     * 积分发放间隔 1个自然月
     */
    int POINTS_GRANT_INTERVAL = 1;

    /**
     * 积分包有效期 12 个自然月
     */
    int POINT_PKG_VALIDITY = 12;

}
