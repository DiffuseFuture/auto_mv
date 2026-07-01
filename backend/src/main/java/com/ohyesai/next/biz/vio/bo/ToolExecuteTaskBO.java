package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.common.enums.StateEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ToolExecuteTaskBO {

    private StateEnum state;

    private Object data;

    public ToolExecuteTaskBO() {
    }

    public ToolExecuteTaskBO(StateEnum state) {
        this.state = state;
    }

    public ToolExecuteTaskBO(StateEnum state, Object data) {
        this.state = state;
        this.data = data;
    }
}
