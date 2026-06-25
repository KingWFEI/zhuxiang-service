package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * App端SDK初始化成功后的门锁数据保存请求。
 */
public record LocalInitializedLockRequest(
        String lockName,
        @NotBlank(message = "lockMac 不能为空") String lockMac,
        @NotBlank(message = "lockData 不能为空") String lockData,
        Integer rssi,
        Integer battery
) {
}
