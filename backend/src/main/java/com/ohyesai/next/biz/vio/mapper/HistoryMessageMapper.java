package com.ohyesai.next.biz.vio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.biz.vio.entity.HistoryMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HistoryMessageMapper extends BaseMapper<HistoryMessage> {

    @Select("""
            select max(sort) max_sort from history_message where chat_session_id = #{chatSessionId}
            """)
    Integer selectMaxSort(@Param("chatSessionId") String chatSessionId);
}
