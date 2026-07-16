package com.zhuxiang.service.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 实名认证状态枚举
 */
public enum RealNameAuthStatus {

    UNVERIFIED("UNVERIFIED", "未认证"),
    VERIFYING("VERIFYING", "认证中"),
    VERIFIED("VERIFIED", "已认证"),
    FAILED("FAILED", "认证失败"),
    EXPIRED("EXPIRED", "已过期"),
    CANCELED("CANCELED", "已取消");

    @EnumValue
    @JsonValue
    private final String value;
    private final String label;

    RealNameAuthStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 安全地从字符串解析状态枚举。未知值返回 null，不抛异常。
     */
    public static RealNameAuthStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (RealNameAuthStatus s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 判断给定的字符串是否是已知的认证状态。
     */
    public static boolean isKnown(String value) {
        return fromValue(value) != null;
    }
}
