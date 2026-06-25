package com.zhuxiang.service.dto;

/**
 * 门锁初始化绑定结果。
 */
public record InitializeLockResponse(
        String id,
        String houseId,
        String roomId,
        Long lockId,
        Long keyId,
        String lockName,
        String lockMac,
        String status,
        String platformErrorCode,
        String platformErrorMessage
) {
}
