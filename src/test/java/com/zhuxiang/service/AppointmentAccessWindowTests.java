package com.zhuxiang.service;

import com.zhuxiang.service.common.AppointmentAccessWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentAccessWindowTests {

    @Test
    void periodPasscodeUsesWholeHourAppointmentWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 3, 11, 0);

        assertThat(AppointmentAccessWindow.passcodeValidFrom(start)).isEqualTo(start);
        assertThat(AppointmentAccessWindow.passcodeValidTo(end)).isEqualTo(end);
    }

    @Test
    void periodPasscodeRoundsNonWholeEndUpToNextHour() {
        LocalDateTime end = LocalDateTime.of(2026, 8, 3, 11, 30, 20);

        assertThat(AppointmentAccessWindow.passcodeValidTo(end))
                .isEqualTo(LocalDateTime.of(2026, 8, 3, 12, 0));
    }

    @Test
    void periodPasscodeRejectsNonWholeStart() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 1);

        assertThatThrownBy(() -> AppointmentAccessWindow.passcodeValidFrom(start))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须为整点");
    }
}
