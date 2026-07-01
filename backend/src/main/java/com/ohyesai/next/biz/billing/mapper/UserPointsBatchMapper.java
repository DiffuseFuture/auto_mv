package com.ohyesai.next.biz.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.biz.billing.entity.UserPointsBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserPointsBatchMapper extends BaseMapper<UserPointsBatch> {

    /**
     * 查询用户剩余积分
     * 查询不到会返回 0
     */
    @Select("""
            SELECT IFNULL(SUM(remaining_amount),0) AS available_balance
            FROM user_points_batch
            WHERE user_id = #{userId}
              AND remaining_amount > 0
              AND expire_time > #{now}
            """)
    int selectUserPoints(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户可用积分明细
     * @param userId
     * @param now
     * @return
     */
    @Select("""
            SELECT *
            FROM user_points_batch
            WHERE user_id = #{userId}
              AND remaining_amount > 0
              AND expire_time > #{now}
            """)
    List<UserPointsBatch> selectUserPointsBatch(@Param("userId") String userId, @Param("now") LocalDateTime now);

}
