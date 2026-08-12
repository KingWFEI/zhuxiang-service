package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Fast contract tests for the public conflict codes used by concurrent rental flows. */
class RentalReservationConcurrencyTests {

    @Test
    void reservationConflictCarriesStableBusinessCodeAndHttp409() {
        BusinessException exception = BusinessException.conflict(
                "HOUSE_ALREADY_RESERVED", "该房源已被其他用户办理，请选择其他房源");

        assertThat(exception.getCode()).isEqualTo(409);
        assertThat(exception.getBusinessCode()).isEqualTo("HOUSE_ALREADY_RESERVED");
        assertThat(exception.getMessage()).isEqualTo("该房源已被其他用户办理，请选择其他房源");
    }

    @Test
    void lostReservationCarriesStableBusinessCodeAndHttp409() {
        BusinessException exception = BusinessException.conflict(
                "ORDER_RESERVATION_LOST", "房源占用已失效，请重新选择房源");

        assertThat(exception.getCode()).isEqualTo(409);
        assertThat(exception.getBusinessCode()).isEqualTo("ORDER_RESERVATION_LOST");
    }
}
