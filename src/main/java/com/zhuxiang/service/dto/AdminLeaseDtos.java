package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminLeaseDtos {

    private AdminLeaseDtos() {
    }

    public record AdminLeaseView(
            String leaseId,
            String tenantId,
            String tenantName,
            String tenantPhone,
            String houseId,
            String houseName,
            String houseAddress,
            String leaseStatus,
            LocalDate startDate,
            LocalDate endDate,
            Integer leaseMonths,
            String paymentMethod,
            Integer paymentMonths,
            Integer monthlyRent,
            Integer deposit,
            Integer serviceFee,
            Integer firstPaymentAmount,
            String contractId,
            String contractNo,
            String contractStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
