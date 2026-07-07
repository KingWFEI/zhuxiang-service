package com.zhuxiang.service.immersive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RenderMode {
    PHOTO("PHOTO", "普通图片"),
    PANORAMA("PANORAMA", "全景");

    @EnumValue @JsonValue
    private final String value;
    private final String label;

    RenderMode(String value, String label) { this.value = value; this.label = label; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public static RenderMode fromValue(String value) {
        for (RenderMode m : values()) if (m.value.equals(value)) return m;
        throw new IllegalArgumentException("未知渲染模式: " + value);
    }
}
