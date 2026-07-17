package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LandlordContractDtos {

    private LandlordContractDtos() {
    }

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
}
