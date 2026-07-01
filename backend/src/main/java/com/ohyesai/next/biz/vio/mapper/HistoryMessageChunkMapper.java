package com.ohyesai.next.biz.vio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.biz.vio.entity.HistoryMessageChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HistoryMessageChunkMapper extends BaseMapper<HistoryMessageChunk> {

    /**
     * join 方式删除
     *
     * @param chatSessionId
     * @return
     */
    @Delete("""
                delete hmc
                from history_message_chunk hmc
                join history_message hm on hmc.history_message_id = hm.id
                where hm.chat_session_id = #{chatSessionId}
            """)
    int deleteByHistoryByChatSessionId(@Param("chatSessionId") String chatSessionId);

    /**
     * 根据主键查询增加userId校验
     * @param chunkId
     * @param userId
     * @return
     */
    @Select("""
                select
                hmc.*
                from history_message_chunk hmc
                inner join history_message hm on hm.id = hmc.history_message_id
                inner join chat_session cs on cs.id = hm.chat_session_id
                where hmc.id = #{chunkId} and cs.user_id = #{userId}
            """)
    HistoryMessageChunk selectByIdAndUser(@Param("chunkId") Integer chunkId, @Param("userId") String userId);
}
