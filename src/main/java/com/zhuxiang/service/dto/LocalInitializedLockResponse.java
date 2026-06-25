package com.zhuxiang.service.dto;

/**
 * 门锁本地初始化数据保存结果。
 */
public record LocalInitializedLockResponse(
        String smartLockId,
        String lockName,
        String lockMac,
        String status
) {
}
