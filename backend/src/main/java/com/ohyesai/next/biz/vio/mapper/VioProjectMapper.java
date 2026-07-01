package com.ohyesai.next.biz.vio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ohyesai.next.biz.vio.bo.VioProjectJoinUser;
import com.ohyesai.next.biz.vio.entity.VioProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VioProjectMapper extends BaseMapper<VioProject> {

    IPage<VioProjectJoinUser> selectPageJoinUser(IPage<VioProject> page);

}
