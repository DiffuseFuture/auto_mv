package com.ohyesai.next.biz.billing.dto;

import com.ohyesai.next.biz.vio.enums.TaskType;
import com.ohyesai.next.common.enums.ModelEnum;
import com.ohyesai.next.common.enums.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "获取积分价格")
@Data
public class GetPointsPriceDTO {

    @Schema(description = "模型名称")
    @NotNull(message = "模型名称不能为空")
    private ModelEnum modelName;

    @Schema(description = "任务类型")
    @NotNull(message = "任务类型不能为空")
    private TaskType taskType;

    @Schema(description = "分辨率; 仅当 taskType = MAKE_MV 时需要")
    private Resolution resolution;

    @Schema(description = "时长/秒；仅当 taskType = MAKE_MV、LIP_SYNC 时需要")
    private int duration;
}
