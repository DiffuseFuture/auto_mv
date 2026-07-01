package com.ohyesai.next.biz.vio.bo.tool;

import dev.langchain4j.model.output.structured.Description;

public enum UpdateType {

    @Description("新增")
    ADD,
    @Description("更新/修改")
    UPDATE,
    @Description("删除")
    DELETE
}
