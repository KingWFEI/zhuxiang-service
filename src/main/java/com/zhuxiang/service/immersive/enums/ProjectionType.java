package com.zhuxiang.service.immersive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectionType {
    FLAT("FLAT", "平面"),
    EQUIRECTANGULAR("EQUIRECTANGULAR", "等距柱状");

    @EnumValue @JsonValue
    private final String value;
    private final String label;

    ProjectionType(String value, String label) { this.value = value; this.label = label; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public static ProjectionType fromValue(String value) {
        for (ProjectionType t : values()) if (t.value.equals(value)) return t;
        throw new IllegalArgumentException("未知投影类型: " + value);
    }
}
