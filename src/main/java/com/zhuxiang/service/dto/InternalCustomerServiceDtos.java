package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内部白名单接口DTO —— 仅供 Python Agent 调用
 */
public final class InternalCustomerServiceDtos {

    private InternalCustomerServiceDtos() {}

    /** 租约简要信息 */
    public record LeaseBrief(
            String leaseId,
            String houseId,
            String houseName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer monthlyRent,
            Integer deposit,
            String paymentMethod,
            LocalDateTime createdAt
    ) {}

    /** 账单简要信息 */
    public record BillBrief(
            String billId,
            String leaseId,
            Integer periodNo,
            Integer amountDue,
            Integer amountPaid,
            String status,
            LocalDate dueDate,
            LocalDateTime paidAt
    ) {}

    /** 门锁简要信息 */
    public record LockBrief(
            String lockId,
            String houseId,
            String houseName,
            String lockName,
            String lockStatus,
            Integer batteryLevel,
            String permissionStatus,
            LocalDateTime permissionStartTime,
            LocalDateTime permissionEndTime
    ) {}

    /** 预约简要信息 */
    public record AppointmentBrief(
            String appointmentId,
            String houseId,
            LocalDate appointmentDate,
            String timeSlot,
            String status,
            LocalDateTime createdAt
    ) {}

    /** 报修简要信息 */
    public record RepairBrief(
            String repairId,
            String orderNo,
            String houseId,
            String houseName,
            String repairType,
            String description,
            String status,
            String assignee,
            Integer rating,
            LocalDateTime createdAt,
            LocalDateTime completedTime
    ) {}

    /** 房源简要信息 */
    public record HouseBrief(
            String houseId,
            String title,
            String address,
            String roomType,
            Integer monthlyRent,
            String communityName
    ) {}

    // ========== 包装结果 ==========

    public record LeaseBriefList(List<LeaseBrief> items) {}

    public record BillBriefList(List<BillBrief> items) {}

    public record LockBriefList(List<LockBrief> items) {}

    public record AppointmentBriefList(List<AppointmentBrief> items) {}

    public record RepairBriefList(List<RepairBrief> items) {}
}
