package com.zhuxiang.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminHouseDtos {

    private AdminHouseDtos() {
    }

    public record CreateHouseRequest(
            @NotBlank(message = "房源标题不能为空") String title,
            @NotBlank(message = "封面图不能为空") String coverImage,
            @NotBlank(message = "位置不能为空") String location,
            @NotBlank(message = "小区ID不能为空") String communityId,
            String address,
            String building,
            String unit,
            String room,
            @NotNull(message = "月租金不能为空") @Min(0) Integer price,
            @Min(0) Integer deposit,
            String paymentMethod,
            String roomType,
            BigDecimal area,
            String floor,
            String orientation,
            String decoration,
            LocalDate availableDate,
            String metro,
            String description,
            @NotBlank(message = "租赁类型不能为空") String rentType,
            @NotBlank(message = "房东ID不能为空") String landlordId,
            Boolean isSmartLockSupported,
            Boolean isSelfViewingSupported
    ) {
    }

    public record LockDeviceView(
            String lockId,
            String lockName,
            String lockBrand,
            String lockSn,
            String lockStatus,
            Integer batteryLevel
    ) {
    }

    public record AdminHouseView(
            String id,
            String title,
            String coverImage,
            String location,
            String communityId,
            String address,
            String building,
            String unit,
            String room,
            Integer price,
            Integer deposit,
            String paymentMethod,
            String roomType,
            BigDecimal area,
            String floor,
            String orientation,
            String decoration,
            LocalDate availableDate,
            String metro,
            String description,
            String rentType,
            String status,
            boolean isSmartLockSupported,
            boolean isSelfViewingSupported,
            boolean smartLockBound,
            LockDeviceView lockDevice,
            String landlordId,
            Integer viewCount,
            Integer favoriteCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
