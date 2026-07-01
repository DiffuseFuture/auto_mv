package com.ohyesai.next.component.ai.chat_memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohyesai.next.component.ai.chat_memory.entity.MemoryMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryMessageMapper extends BaseMapper<MemoryMessage> {
}
