package com.zhuxiang.service.dto;

import java.time.LocalDate;

public record ContractPreviewResponse(
        String contractNo,
        String status,
        String tenantName,
        String tenantPhone,
        String tenantIdCard,
        String houseName,
        String roomName,
        String houseAddress,
        LocalDate startDate,
        LocalDate endDate,
        Integer leaseMonths,
        Integer monthlyRent,
        Integer deposit,
        Integer serviceFee,
        Integer firstPaymentAmount
) {
}
