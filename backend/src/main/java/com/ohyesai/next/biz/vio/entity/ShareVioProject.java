package com.ohyesai.next.biz.vio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ShareVioProject {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String vioProjectId;

}
