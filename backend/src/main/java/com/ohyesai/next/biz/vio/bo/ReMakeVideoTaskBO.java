package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.common.enums.StateEnum;
import lombok.Data;

import java.util.List;

@Data
public class ReMakeVideoTaskBO {

    private Integer state;

    private SseMsgBO<List<SseMsgBO.Video>> data;

    private String message;

    public ReMakeVideoTaskBO() {
    }

    public ReMakeVideoTaskBO(StateEnum state) {
        this.state = state.code;
    }

    public ReMakeVideoTaskBO(StateEnum state, SseMsgBO<List<SseMsgBO.Video>> data) {
        this.state = state.code;
        this.data = data;
    }

    public ReMakeVideoTaskBO(StateEnum state, String message) {
        this.state = state.code;
        this.message = message;
    }
}
