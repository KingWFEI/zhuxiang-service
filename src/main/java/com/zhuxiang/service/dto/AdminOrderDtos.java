package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminOrderDtos {
    private AdminOrderDtos() {}

    public record OrderView(
            String id, String status, String userId, String lessorUserId,
            String tenantName, String tenantPhone, String landlordName, String landlordPhone,
            String houseId, String houseName, String roomName, String houseAddress, String houseStatus,
            LocalDate startDate, LocalDate endDate, Integer leaseMonths,
            String paymentMethod, Integer paymentMonths, Integer tenantCount,
            Integer monthlyRent, Integer deposit, Integer serviceFee,
            Integer firstPaymentAmount, Integer totalAmount,
            String contractId, String contractNo, String contractStatus,
            String paymentNo, String paymentStatus, String paymentChannel,
            LocalDateTime realNameAt, LocalDateTime contractConfirmedAt,
            LocalDateTime paidAt, LocalDateTime signedAt, LocalDateTime cancelledAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}
}
