package com.ohyesai.next.common.enums;

/**
 * 模型枚举
 */
public enum ModelEnum {

    /**
     * VIDU
     */
    VIDUQ2("viduq2"),

    /**
     * seedance 2.0
     */
    SEEDANCE_2("doubao-seedance-2-0-260128"),

    /**
     * SEEDANCE_2_FAST
     */
    SEEDANCE_2_FAST("doubao-seedance-2-0-fast-260128"),

    /**
     * KLING_V3_OMNI
     */
    KLING_V3_OMNI("kling-v3-omni"),


    NONE("None");

    /**
     * 模型调用时的名称
     */
    public final String modelName;

    ModelEnum(String modelName) {
        this.modelName = modelName;
    }
}
