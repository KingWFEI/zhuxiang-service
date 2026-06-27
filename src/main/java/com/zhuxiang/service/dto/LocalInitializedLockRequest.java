package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * App端SDK初始化成功后的门锁数据保存请求。
 */
@Schema(description = "App 蓝牙 SDK 初始化门锁后的数据")
public record LocalInitializedLockRequest(
        @Schema(description = "门锁名称", example = "客厅门锁") String lockName,
        @NotBlank(message = "lockMac 不能为空")
        @Schema(description = "门锁 MAC 地址", example = "AA:BB:CC:DD:EE:FF") String lockMac,
        @NotBlank(message = "lockData 不能为空")
        @Schema(description = "SDK 返回的门锁初始化数据，属于敏感数据") String lockData,
        @Schema(description = "初始化时蓝牙信号强度 RSSI，单位 dBm", example = "-58") Integer rssi,
        @Schema(description = "初始化时门锁电量百分比", example = "86") Integer battery
) {
}
