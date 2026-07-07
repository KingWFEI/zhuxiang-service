package com.zhuxiang.service.immersive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TourStatus {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    OFFLINE("OFFLINE", "已下线");

    @EnumValue @JsonValue
    private final String value;
    private final String label;

    TourStatus(String value, String label) { this.value = value; this.label = label; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public static TourStatus fromValue(String value) {
        for (TourStatus s : values()) if (s.value.equals(value)) return s;
        throw new IllegalArgumentException("未知项目状态: " + value);
    }
}
