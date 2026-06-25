package com.zhuxiang.service.dto;

/**
 * Bluetooth unlock data for admin lock management.
 */
public record SmartLockUnlockDataResponse(
        String smartLockId,
        String lockName,
        String lockMac,
        String lockData,
        String roomName,
        String status
) {
}
