package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 开锁日志接口模型（手动蓝牙 + 无感），不接收任何 lockData、密码或 Token。 */
public final class UnlockRecordDtos {

    private UnlockRecordDtos() {
    }

    @Schema(description = "开锁结果上报")
    public record UnlockRecordRequest(
            @NotBlank @Size(max = 36) String smartLockId,
            @NotNull @Positive Long ttlockLockId,
            @NotBlank @Pattern(regexp = "MANUAL_BLUETOOTH|AUTO_NEARBY") String triggerType,
            @Min(-127) @Max(20) Integer rssi,
            @Min(0) @Max(60000) Integer stableMillis,
            @NotBlank @Pattern(regexp = "SUCCESS|FAILED") String result,
            @Size(max = 64)
            @Pattern(regexp = "[A-Za-z0-9_-]*", message = "failureReason 格式不正确")
            String failureReason,
            @NotBlank @Size(max = 255) String deviceInfo,
            @NotBlank @Size(max = 64) String appVersion
    ) {
    }

    @Schema(description = "开锁日志写入结果")
    public record UnlockRecordResponse(
            String id,
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "开锁记录列表项")
    public record UnlockRecordItem(
            @Schema(description = "记录唯一标识") String id,
            @Schema(description = "房源 ID") String houseId,
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "门锁 ID") String lockId,
            @Schema(description = "门锁名称") String lockName,
            @Schema(description = "开锁方式") String unlockMethod,
            @Schema(description = "开锁结果") String unlockResult,
            @Schema(description = "开锁时间") LocalDateTime unlockTime,
            @Schema(description = "操作人姓名") String operatorName,
            @Schema(description = "操作人类型") String operatorType,
            @Schema(description = "失败原因") String failureReason,
            @Schema(description = "操作设备名") String deviceName,
            @Schema(description = "备注") String remark
    ) {
    }

    @Schema(description = "开锁记录列表")
    public record UnlockRecordListResponse(
            long total,
            List<UnlockRecordItem> items
    ) {
    }

    @Schema(description = "当前用户主门锁权限状态")
    public record LockPermissionResponse(
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "门锁名称") String lockName,
            @Schema(description = "权限状态：active/pending/expired") String permissionStatus,
            @Schema(description = "最近一次开锁时间") LocalDateTime lastUnlockTime,
            @Schema(description = "支持的开锁方式") List<String> supportedMethods
    ) {
    }
}
