package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentOrderResponse(
        String id,
        String userId,
        String houseId,
        String status,
        String houseStatus,
        LocalDate startDate,
        LocalDate endDate,
        Integer leaseMonths,
        String paymentMethod,
        Integer paymentMonths,
        Integer tenantCount,
        Integer monthlyRent,
        Integer deposit,
        Integer serviceFee,
        Integer firstPaymentAmount,
        Integer totalAmount,
        String tenantName,
        String tenantPhone,
        String tenantIdCard,
        LocalDateTime realNameAt,
        LocalDateTime contractConfirmedAt,
        LocalDateTime paidAt,
        LocalDateTime signedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String houseName,
        String roomName,
        String address
) {
}
