package com.zhuxiang.service.common;

import java.time.LocalDateTime;

/**
 * 预约自助看房的门锁授权时间规则。
 */
public final class AppointmentAccessWindow {

    public static final int MINUTES_BEFORE_START = 10;
    public static final int MINUTES_AFTER_END = 10;

    private AppointmentAccessWindow() {
    }

    public static LocalDateTime validFrom(LocalDateTime appointmentStartAt) {
        return appointmentStartAt.minusMinutes(MINUTES_BEFORE_START);
    }

    public static LocalDateTime validTo(LocalDateTime appointmentEndAt) {
        return appointmentEndAt.plusMinutes(MINUTES_AFTER_END);
    }

    /**
     * TTLock V4 期限密码的开始时间只能精确到小时。
     * 预约开始时间由服务端时段控制，必须本身就是整点，避免提前放大密码权限。
     */
    public static LocalDateTime passcodeValidFrom(LocalDateTime appointmentStartAt) {
        LocalDateTime wholeHour = appointmentStartAt
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        if (!appointmentStartAt.equals(wholeHour)) {
            throw new IllegalArgumentException("TTLock 期限密码开始时间必须为整点");
        }
        return wholeHour;
    }

    /**
     * 密码结束时间向上取整到小时，确保配置为非整小时的看房时长仍被完整覆盖。
     */
    public static LocalDateTime passcodeValidTo(LocalDateTime appointmentEndAt) {
        LocalDateTime wholeHour = appointmentEndAt
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return appointmentEndAt.equals(wholeHour)
                ? wholeHour
                : wholeHour.plusHours(1);
    }
}
