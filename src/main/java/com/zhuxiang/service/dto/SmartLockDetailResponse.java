package com.zhuxiang.service.dto;

import java.time.LocalDateTime;

/**
 * 门锁管理详情响应。
 */
public record SmartLockDetailResponse(
        String smartLockId,
        String lockName,
        String lockMac,
        String status,
        Long lockId,
        Long keyId,
        String houseId,
        String roomId,
        String houseName,
        String roomName,
        Integer battery,
        Integer rssi,
        String batterySource,
        LocalDateTime lastBleSyncTime,
        LocalDateTime lastPlatformSyncTime,
        String platformErrorMessage
) {
}
