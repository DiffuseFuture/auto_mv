package com.ohyesai.next.biz.billing.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PointsTransactionLogMapper extends BaseMapper<PointsTransactionLog> {

    @Select("""
            select ptl.* from points_transaction_log ptl
            inner join session_points_transaction_map sptm on ptl.id = sptm.points_transaction_log_id
            where sptm.chat_session_id = #{sessionId}
            order by ptl.create_time desc
            """)
    List<PointsTransactionLog> selectPointsTransactionLogBySessionId(@Param("sessionId") String sessionId);
}
