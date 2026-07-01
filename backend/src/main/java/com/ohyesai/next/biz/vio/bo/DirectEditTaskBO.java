package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.common.enums.StateEnum;
import lombok.Data;

@Data
public class DirectEditTaskBO {

    private Integer state;

    private SseMsgBO<?> data;

    private String message;

    public DirectEditTaskBO() {
    }

    public DirectEditTaskBO(StateEnum state) {
        this.state = state.code;
    }

    public DirectEditTaskBO(StateEnum state, SseMsgBO<?> data) {
        this.state = state.code;
        this.data = data;
    }

    public DirectEditTaskBO(StateEnum state, String message) {
        this.state = state.code;
        this.message = message;
    }
}
