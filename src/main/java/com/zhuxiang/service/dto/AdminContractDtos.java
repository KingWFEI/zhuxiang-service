package com.zhuxiang.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminContractDtos {
    private AdminContractDtos() {}

    public record ContractSummary(
            String id, String contractNo, String contractNum, String orderId,
            String houseId, String houseName, String houseAddress,
            String tenantName, String tenantPhone, String landlordName, String landlordPhone,
            String status, boolean lessorSigned, boolean tenantSigned,
            LocalDate startDate, LocalDate endDate, Integer leaseMonths,
            Integer monthlyRent, Integer deposit, Integer templateVersion,
            boolean hasContractFile, LocalDateTime signedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    public record ContractDetail(
            String id, String contractNo, String contractNum, String orderId,
            String userId, String houseId, String houseName, String roomName, String houseAddress,
            String tenantName, String tenantPhone, String landlordName, String landlordPhone,
            String status, boolean lessorSigned, boolean tenantSigned,
            LocalDate startDate, LocalDate endDate, Integer leaseMonths,
            Integer monthlyRent, Integer deposit, Integer serviceFee,
            Integer paymentMonths, Integer firstPaymentAmount,
            String docTemplateId, String templateConfigId, Integer templateVersion,
            String contractFileId, String signFlowId, boolean hasContractFile,
            String failureCode, String failureMessage,
            LocalDateTime signedAt, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    public record DownloadUrl(String fileName, String url, LocalDateTime expiresAt) {}
}
