package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.util.List;

public record ContractPreviewResponse(
        String orderId,
        String contractNo,
        String status,
        String contractStatus,
        String previewUrl,
        String tenantName,
        String tenantPhone,
        String tenantIdCard,
        String houseName,
        String roomName,
        String houseAddress,
        String landlordName,
        String landlordPhone,
        String landlordIdCard,
        LocalDate startDate,
        LocalDate endDate,
        Integer leaseMonths,
        Integer monthlyRent,
        Integer deposit,
        Integer serviceFee,
        String paymentMethod,
        Integer paymentMonths,
        List<String> clauses
) {
}
