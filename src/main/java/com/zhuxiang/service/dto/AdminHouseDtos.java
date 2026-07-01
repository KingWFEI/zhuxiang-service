package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminHouseDtos {

    private AdminHouseDtos() {
    }

    @Schema(description = "管理端房源创建请求")
    public record CreateHouseRequest(
            @NotBlank(message = "房源标题不能为空")
            @Schema(description = "房源标题", example = "高新区精装一居室") String title,
            @NotBlank(message = "封面图不能为空")
            @Schema(description = "房源封面图 URL") String coverImage,
            @NotEmpty(message = "房源图片不能为空")
            @Size(max = 20, message = "房源图片不能超过20张")
            @Schema(description = "房源图片 URL 列表，URL 必须来自管理端房源图片上传接口")
            List<@NotBlank(message = "房源图片URL不能为空") String> imageUrls,
            @NotBlank(message = "位置不能为空")
            @Schema(description = "区域或商圈展示位置", example = "高新区金融城") String location,
            @NotBlank(message = "小区ID不能为空")
            @Schema(description = "小区 ID", example = "community_001") String communityId,
            @Schema(description = "详细地址", example = "天府大道中段 1 号") String address,
            @Schema(description = "楼栋", example = "2栋") String building,
            @Schema(description = "单元", example = "1单元") String unit,
            @Schema(description = "房号", example = "1801") String room,
            @NotNull(message = "月租金不能为空") @Min(0)
            @Schema(description = "月租金，单位元", example = "2800") Integer price,
            @Min(0) @Schema(description = "押金，单位元", example = "2800") Integer deposit,
            @Schema(description = "付款方式", example = "押一付三") String paymentMethod,
            @Schema(description = "户型", example = "1室1厅1卫") String roomType,
            @Schema(description = "建筑面积，单位平方米", example = "45.5") BigDecimal area,
            @Schema(description = "楼层描述", example = "18/32层") String floor,
            @Schema(description = "朝向", example = "南") String orientation,
            @Schema(description = "装修情况", example = "精装") String decoration,
            @Schema(description = "最早可入住日期", example = "2026-07-01") LocalDate availableDate,
            @Schema(description = "地铁信息", example = "距1号线金融城站500米") String metro,
            @Schema(description = "房源详细介绍") String description,
            @NotBlank(message = "租赁类型不能为空")
            @Schema(description = "租赁类型，如整租或合租", example = "整租") String rentType,
            @NotBlank(message = "房东ID不能为空")
            @Schema(description = "房东用户 ID", example = "user_001") String landlordId,
            @Schema(description = "是否支持智能门锁", example = "true") Boolean isSmartLockSupported,
            @Schema(description = "是否支持自助看房", example = "true") Boolean isSelfViewingSupported,
            @NotEmpty(message = "房源设施不能为空")
            @Size(max = 100, message = "房源设施不能超过100项")
            @Schema(description = "创建时绑定的启用设施 ID 列表")
            List<@NotBlank(message = "设施ID不能为空") String> facilityIds,
            @NotEmpty(message = "房源标签不能为空")
            @Size(max = 100, message = "房源标签不能超过100项")
            @Schema(description = "创建时绑定的启用标签 ID 列表")
            List<@NotBlank(message = "标签ID不能为空") String> tagIds
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
            List<String> imageUrls,
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

    @Schema(description = "管理端房源修改请求，所有字段可选，仅更新传入的非空字段")
    public record UpdateHouseRequest(
            @Schema(description = "房源标题", example = "高新区精装一居室") String title,
            @Schema(description = "房源封面图 URL") String coverImage,
            @Schema(description = "房源图片 URL 列表")
            List<String> imageUrls,
            @Schema(description = "区域或商圈展示位置", example = "高新区金融城") String location,
            @Schema(description = "小区 ID", example = "community_001") String communityId,
            @Schema(description = "详细地址", example = "天府大道中段 1 号") String address,
            @Schema(description = "楼栋", example = "2栋") String building,
            @Schema(description = "单元", example = "1单元") String unit,
            @Schema(description = "房号", example = "1801") String room,
            @Min(0) @Schema(description = "月租金，单位元", example = "2800") Integer price,
            @Min(0) @Schema(description = "押金，单位元", example = "2800") Integer deposit,
            @Schema(description = "付款方式", example = "押一付三") String paymentMethod,
            @Schema(description = "户型", example = "1室1厅1卫") String roomType,
            @Schema(description = "建筑面积，单位平方米", example = "45.5") BigDecimal area,
            @Schema(description = "楼层描述", example = "18/32层") String floor,
            @Schema(description = "朝向", example = "南") String orientation,
            @Schema(description = "装修情况", example = "精装") String decoration,
            @Schema(description = "最早可入住日期", example = "2026-07-01") LocalDate availableDate,
            @Schema(description = "地铁信息", example = "距1号线金融城站500米") String metro,
            @Schema(description = "房源详细介绍") String description,
            @Schema(description = "租赁类型，如整租或合租", example = "整租") String rentType,
            @Schema(description = "是否支持智能门锁", example = "true") Boolean isSmartLockSupported,
            @Schema(description = "是否支持自助看房", example = "true") Boolean isSelfViewingSupported,
            @Schema(description = "房东用户 ID", example = "user_001") String landlordId,
            @Size(max = 100, message = "房源设施不能超过100项")
            @Schema(description = "完整替换房源设施的启用设施 ID 列表；不传保持不变，空数组清空")
            List<@NotBlank(message = "设施ID不能为空") String> facilityIds,
            @Size(max = 100, message = "房源标签不能超过100项")
            @Schema(description = "完整替换房源标签的启用标签 ID 列表；不传保持不变，空数组清空")
            List<@NotBlank(message = "标签ID不能为空") String> tagIds
    ) {
    }
}
