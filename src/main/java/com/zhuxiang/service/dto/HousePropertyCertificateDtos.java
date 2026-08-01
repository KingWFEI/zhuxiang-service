package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class HousePropertyCertificateDtos {

    private HousePropertyCertificateDtos() {
    }

    @Schema(description = "房产证明材料视图")
    public record CertificateView(
            String id,
            String houseId,
            String originalName,
            String contentType,
            Long fileSize,
            String auditStatus,
            boolean current,
            String reviewRemark,
            String reviewerId,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "房源审核请求")
    public record ReviewRequest(
            @NotBlank(message = "审核动作不能为空")
            @Pattern(regexp = "APPROVE|REJECT", message = "审核动作只能是 APPROVE 或 REJECT")
            @Schema(description = "审核动作", example = "APPROVE")
            String action,

            @Size(max = 500, message = "审核意见不能超过500字")
            @Schema(description = "审核意见；驳回时必填", example = "房产证照片信息不完整")
            String remark
    ) {
    }
}

