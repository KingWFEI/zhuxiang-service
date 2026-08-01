package com.zhuxiang.service.common;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum AppointmentStatus {
    PENDING_CONFIRMATION,
    RESCHEDULE_PROPOSED,
    CONFIRMED,
    READY,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    NO_SHOW;

    private static final Set<AppointmentStatus> TERMINAL = EnumSet.of(
            COMPLETED, REJECTED, CANCELLED, EXPIRED, NO_SHOW
    );

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public static AppointmentStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest("预约状态不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest("不支持的预约状态");
        }
    }
}
