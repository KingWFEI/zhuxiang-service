package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request for updating lock status from a nearby BLE scan.
 */
@Schema(description = "门锁蓝牙扫描状态更新请求")
public record BleStatusRequest(
        @NotNull(message = "battery 不能为空")
        @Min(value = 0, message = "battery 不能小于 0")
        @Max(value = 100, message = "battery 不能大于 100")
        @Schema(description = "门锁电量百分比，范围 0-100", example = "86") Integer battery,

        @NotNull(message = "rssi 不能为空")
        @Schema(description = "蓝牙信号强度 RSSI，单位 dBm", example = "-58") Integer rssi
) {
}
