package com.zhuxiang.service.immersive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SceneType {
    ENTRANCE("ENTRANCE", "入户"),
    LIVING_ROOM("LIVING_ROOM", "客厅"),
    MASTER_BEDROOM("MASTER_BEDROOM", "主卧"),
    SECOND_BEDROOM("SECOND_BEDROOM", "次卧"),
    BEDROOM("BEDROOM", "卧室"),
    KITCHEN("KITCHEN", "厨房"),
    BATHROOM("BATHROOM", "卫生间"),
    BALCONY("BALCONY", "阳台"),
    DINING_ROOM("DINING_ROOM", "餐厅"),
    STUDY("STUDY", "书房"),
    CORRIDOR("CORRIDOR", "过道"),
    OTHER("OTHER", "其他");

    @EnumValue @JsonValue
    private final String value;
    private final String label;

    SceneType(String value, String label) { this.value = value; this.label = label; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public static SceneType fromValue(String value) {
        for (SceneType t : values()) if (t.value.equals(value)) return t;
        throw new IllegalArgumentException("未知房间类型: " + value);
    }
}
