package com.ohyesai.next.biz.vio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.biz.vio.bo.ChatHistoryBO;
import com.ohyesai.next.biz.vio.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    ChatHistoryBO selectChatHistory(@Param("sessionId") String sessionId);

    /**
     * 当cover为null时，更新cover字段值
     *
     * @param sessionId
     * @param cover
     * @return
     */
    @Update("""
            update chat_session
            SET metadata = JSON_SET(COALESCE(metadata, '{}'), '$.cover', #{cover})
            where id = #{sessionId} and metadata->>'$.cover' = 'null'
            """)
    int updateCover4Null(@Param("sessionId") String sessionId, @Param("cover") String cover);
}
