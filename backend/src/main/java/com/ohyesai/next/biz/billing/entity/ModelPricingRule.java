package com.ohyesai.next.biz.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import lombok.Data;

/**
 * 模型调用计费规则表
 */
@Data
public class ModelPricingRule {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务类型 (e.g., 音乐生成, 故事类MV-15秒)
     */
    private TaskType taskType;

    /**
     * 调用模型 (e.g., Mureka O2, viduq2)
     */
    private ModelEnum modelName;

    private Resolution resolution;

    /**
     * 消耗积分 (e.g., 5, 4, 14, 65)
     */
    private Integer pointsRequired;

    /**
     * 是否启用
     */
    private Boolean isActive;
}
