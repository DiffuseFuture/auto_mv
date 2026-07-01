package com.ohyesai.next.common.consts;

public interface CommonConst {
    /**
     * session key 存储用户信息
     */
    String USER_KEY = "user";

    String OMNI_BASE_URL = "https://api.cloubic.com";

//    String OMNI_BASE_URL = "http://18.136.201.255:3000";

    String GUOYAN_BASE_URL = "https://newapi.ohyesai.com";

    String ARGS_NULL_ERROR = "参数不能为空，按照工具参数要求传入正确数据";

    /**
     * 工具返回结果中，表示"需要用户先确认积分，再带 confirmed=true 重新调用"的业务标识
     */
    String TOOL_RESULT_NEED_CONFIRM = "NEED_CONFIRM";

    /**
     * 修改主体时，引导 agent 向用户确认积分消耗的提示模板。
     * 参数: %s - 本次修改所需积分
     */
    String CONFIRM_POINTS_PROMPT_TEMPLATE = """
            请严格执行以下步骤：
            1. 用自然语言告知用户：本次修改需要消耗 %s 积分，询问用户是否同意。
            2. 如果用户回答同意，请再次调用本工具，并务必设置参数 confirmed = true；如果用户拒绝，请终止操作并礼貌回复。
            """;
}
