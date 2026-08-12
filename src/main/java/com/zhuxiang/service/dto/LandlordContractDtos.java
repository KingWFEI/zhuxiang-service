package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LandlordContractDtos {

    private LandlordContractDtos() {
    }

    public record RejectRequest(
            @NotBlank(message = "拒签原因不能为空")
            @Size(max = 500, message = "拒签原因不能超过500个字符")
            String reason
    ) {}

    @Schema(description = "房东工作台合同列表项")
    public record ContractItem(
            String orderId,
            String contractId,
            String contractNo,
            String contractStatus,
            boolean tenantSigned,
            boolean lessorSigned,
            String signStage,
            String houseId,
            String houseName,
            String roomName,
            String address,
            String tenantName,
            String tenantPhone,
            LocalDate startDate,
            LocalDate endDate,
            Integer monthlyRent,
            Integer deposit,
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "房东工作台合同详情")
    public record ContractDetail(
            String orderId,
            String contractId,
            String contractStatus,
            boolean tenantSigned,
            boolean lessorSigned,
            String signStage,
            ContractPreviewResponse contract
    ) {
    }

    public record TerminationItem(
            String applicationId, String applicationNo, String status, String statusText,
            String contractId, String contractNo, String houseName, String roomName,
            String address, String tenantName, String tenantPhone,
            LocalDate expectedMoveOutDate, boolean tenantSigned, boolean lessorSigned,
            LocalDateTime updatedAt
    ) {}

    public record TerminationDetail(
            TerminationItem application, String reason, LocalDate actualMoveOutDate,
            Integer refundAmount, Integer deductionAmount
    ) {}

    public record RescissionAction(
            String action, String signFlowId, String signUrl, String shortUrl,
            String status, boolean currentUserSigned, boolean completed
    ) {}
}
