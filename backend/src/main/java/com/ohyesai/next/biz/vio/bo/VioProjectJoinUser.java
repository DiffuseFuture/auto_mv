package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.biz.vio.entity.VioProject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VioProjectJoinUser extends VioProject {

    private String nickName;

    private String avatarImg;

}
