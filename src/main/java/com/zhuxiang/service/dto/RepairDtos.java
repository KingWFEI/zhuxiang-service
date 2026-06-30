package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "报修记录 DTO")
public final class RepairDtos {

    private RepairDtos() {
    }

    @Schema(description = "报修记录列表项")
    public record RepairItem(
            @Schema(description = "报修记录 ID") String id,
            @Schema(description = "报修编号", example = "BX202606300001") String orderNo,
            @Schema(description = "房源 ID") String houseId,
            @Schema(description = "房源名称", example = "3栋2单元1201") String houseName,
            @Schema(description = "房间名称", example = "3栋2单元1201") String roomName,
            @Schema(description = "报修类型", example = "plumbing") String repairType,
            @Schema(description = "问题描述") String description,
            @Schema(description = "图片URL列表") List<String> imageUrls,
            @Schema(description = "联系人姓名") String contactName,
            @Schema(description = "联系人手机号") String contactPhone,
            @Schema(description = "期望上门时间") LocalDateTime expectedVisitTime,
            @Schema(description = "状态", example = "submitted") String status,
            @Schema(description = "管家姓名") String housekeeperName,
            @Schema(description = "管家电话") String housekeeperPhone,
            @Schema(description = "维修人员姓名") String repairmanName,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "更新时间") LocalDateTime updatedAt,
            @Schema(description = "时间线") List<TimelineItem> timeline
    ) {
    }

    @Schema(description = "时间线项")
    public record TimelineItem(
            @Schema(description = "标题") String title,
            @Schema(description = "描述") String description,
            @Schema(description = "时间") LocalDateTime time,
            @Schema(description = "状态") String status
    ) {
    }

    @Schema(description = "管理端报修记录列表项")
    public record AdminRepairItem(
            @Schema(description = "报修记录 ID") String id,
            @Schema(description = "报修编号", example = "BX202606300001") String repairNo,
            @Schema(description = "房源 ID") String houseId,
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "房源地址") String houseAddress,
            @Schema(description = "房间名称") String roomName,
            @Schema(description = "租客 ID") String tenantId,
            @Schema(description = "租客姓名") String tenantName,
            @Schema(description = "租客手机号") String tenantPhone,
            @Schema(description = "报修类型", example = "plumbing") String repairType,
            @Schema(description = "问题描述") String description,
            @Schema(description = "状态", example = "submitted") String status,
            @Schema(description = "当前处理人") String assignee,
            @Schema(description = "维修人员姓名") String repairmanName,
            @Schema(description = "管家姓名") String housekeeperName,
            @Schema(description = "期望上门时间") LocalDateTime expectedVisitTime,
            @Schema(description = "完成时间") LocalDateTime completedAt,
            @Schema(description = "评分") Integer rating,
            @Schema(description = "评价内容") String reviewContent,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "创建报修请求")
    public record CreateRepairRequest(
            @Schema(description = "房源 ID") @NotBlank @Size(max = 36) String houseId,
            @Schema(description = "房源名称") @NotBlank @Size(max = 255) String houseName,
            @Schema(description = "房间名称") @Size(max = 255) String roomName,
            @Schema(description = "报修类型：plumbing/electrical/appliance/furniture/door_window/other")
            @NotBlank String repairType,
            @Schema(description = "问题描述") @NotBlank @Size(max = 1000) String description,
            @Schema(description = "图片URL列表") List<String> imageUrls,
            @Schema(description = "联系人姓名") @NotBlank @Size(max = 100) String contactName,
            @Schema(description = "联系人手机号") @NotBlank @Size(max = 20) String contactPhone,
            @Schema(description = "期望上门时间") LocalDateTime expectedVisitTime
    ) {
    }

    @Schema(description = "取消报修请求")
    public record CancelRepairRequest(
            @Schema(description = "取消原因") @Size(max = 500) String cancelReason
    ) {
    }

    @Schema(description = "评价报修请求")
    public record ReviewRepairRequest(
            @Schema(description = "评分 1-5") @Min(1) @Max(5) Integer rating,
            @Schema(description = "评价内容") @Size(max = 1000) String reviewContent
    ) {
    }
}
