package com.zhuxiang.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request for updating lock status from a nearby BLE scan.
 */
public record BleStatusRequest(
        @NotNull(message = "battery 不能为空")
        @Min(value = 0, message = "battery 不能小于 0")
        @Max(value = 100, message = "battery 不能大于 100")
        Integer battery,

        @NotNull(message = "rssi 不能为空")
        Integer rssi
) {
}
