package com.zhuxiang.service.dto;

/**
 * 按MAC查询门锁本地记录响应。
 */
public record SmartLockByMacResponse(
        String smartLockId,
        String lockName,
        String lockMac,
        String status,
        String houseId,
        String roomId,
        String houseName,
        String roomName
) {
}
