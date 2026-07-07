package com.zhuxiang.service.immersive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TargetType {
    SCENE("SCENE", "跳转到场景入口图"),
    IMAGE("IMAGE", "跳转到指定图片");

    @EnumValue @JsonValue
    private final String value;
    private final String label;

    TargetType(String value, String label) { this.value = value; this.label = label; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public static TargetType fromValue(String value) {
        if (value == null || value.isEmpty()) return SCENE;
        for (TargetType t : values()) if (t.value.equals(value)) return t;
        throw new IllegalArgumentException("未知跳转类型: " + value);
    }
}
