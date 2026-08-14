package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
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

    /** 用户客服业务概览，不包含手机号和身份信息。 */
    public record ServiceOverview(
            String role,
            String realNameAuthStatus,
            String landlordAuthStatus,
            long activeLeaseCount,
            long pendingBillCount,
            long activeOrderCount,
            long activeAppointmentCount,
            long activeRepairCount
    ) {}

    public record HouseDetail(
            String houseId,
            String title,
            String location,
            String address,
            String communityName,
            String roomType,
            BigDecimal area,
            Integer monthlyRent,
            Integer deposit,
            String paymentMethod,
            String floor,
            String orientation,
            String decoration,
            LocalDate availableDate,
            String metro,
            String rentMode,
            String rentType,
            String status,
            boolean smartLockSupported,
            boolean selfViewingSupported
    ) {}

    public record RentOrderBrief(
            String orderId,
            String houseId,
            String houseName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer leaseMonths,
            String paymentMethod,
            Integer monthlyRent,
            Integer deposit,
            Integer serviceFee,
            Integer firstPaymentAmount,
            LocalDateTime realNameAt,
            LocalDateTime contractConfirmedAt,
            LocalDateTime paidAt,
            LocalDateTime signedAt,
            LocalDateTime paymentDeadlineAt,
            LocalDateTime cancelledAt,
            String cancelReason,
            LocalDateTime createdAt
    ) {}

    public record LeaseDetail(
            String leaseId,
            String contractId,
            String orderId,
            String houseId,
            String houseName,
            String houseAddress,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer leaseMonths,
            Integer monthlyRent,
            Integer deposit,
            Integer serviceFee,
            String paymentMethod,
            Integer paymentMonths
    ) {}

    public record ContractSummary(
            String contractId,
            String leaseId,
            String contractNo,
            String status,
            boolean tenantSigned,
            boolean landlordSigned,
            String houseName,
            LocalDate startDate,
            LocalDate endDate,
            Integer monthlyRent,
            Integer deposit,
            LocalDateTime signedAt
    ) {}

    public record BillDetail(
            String billId,
            String leaseId,
            Integer periodNo,
            Integer amountDue,
            Integer amountPaid,
            Integer overdueAmount,
            String status,
            LocalDate dueDate,
            LocalDateTime paidAt,
            LocalDateTime createdAt
    ) {}

    public record PaymentBrief(
            String paymentId,
            String orderId,
            String billId,
            String leaseId,
            String houseName,
            Integer amount,
            String paymentChannel,
            String status,
            String type,
            LocalDateTime paidAt,
            LocalDateTime createdAt
    ) {}

    public record TerminationStatus(
            String applicationId,
            String applicationNo,
            String leaseId,
            String houseId,
            String status,
            String reason,
            LocalDate expectedMoveOutDate,
            String rejectReason,
            String supplementReason,
            Integer totalDeduction,
            Integer refundAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record DepositSummary(
            String depositId,
            String leaseId,
            String houseId,
            Integer amount,
            Integer withheldAmount,
            Integer refundedAmount,
            String status,
            String settlementDetail,
            LocalDateTime terminatedAt,
            LocalDateTime refundedAt,
            LocalDateTime createdAt
    ) {}

    public record RepairDetail(
            String repairId,
            String orderNo,
            String houseId,
            String houseName,
            String roomName,
            String repairType,
            String description,
            LocalDateTime expectedVisitTime,
            String status,
            String assignee,
            String repairmanName,
            Integer rating,
            String reviewContent,
            String cancelReason,
            LocalDateTime completedTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record AppointmentDetail(
            String appointmentId,
            String houseId,
            String houseName,
            String status,
            String sourceType,
            String viewingMode,
            LocalDateTime appointmentStartAt,
            LocalDateTime appointmentEndAt,
            String meetingPoint,
            String viewingInstruction,
            String rejectReason,
            String cancelReason,
            LocalDateTime proposedStartAt,
            LocalDateTime proposedEndAt,
            String rescheduleReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record AuthStatus(
            String authStatus,
            String failureMessage,
            LocalDateTime verifiedAt,
            LocalDateTime updatedAt
    ) {}

    public record LandlordAuthStatus(
            String status,
            boolean landlord,
            boolean canSubmit,
            String rejectReason,
            LocalDateTime reviewedAt,
            LocalDateTime updatedAt
    ) {}

    // ========== 包装结果 ==========

    public record LeaseBriefList(List<LeaseBrief> items) {}

    public record BillBriefList(List<BillBrief> items) {}

    public record LockBriefList(List<LockBrief> items) {}

    public record AppointmentBriefList(List<AppointmentBrief> items) {}

    public record RepairBriefList(List<RepairBrief> items) {}
}
