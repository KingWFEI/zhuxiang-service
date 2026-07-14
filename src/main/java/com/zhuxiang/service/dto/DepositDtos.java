package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "押金 DTO")
public final class DepositDtos {

    private DepositDtos() {}

    @Schema(description = "押金记录详情")
    public record DepositDetail(
            @Schema(description = "押金记录 ID") String id,
            @Schema(description = "租约 ID") String leaseId,
            @Schema(description = "押金总额，单位分") Integer amount,
            @Schema(description = "已扣款金额，单位分") Integer withheldAmount,
            @Schema(description = "已退款金额，单位分") Integer refundedAmount,
            @Schema(description = "状态：held托管中/deducted已扣款/refunding退款中/refunded已退款") String status,
            @Schema(description = "扣款明细列表") List<DeductionView> deductions,
            @Schema(description = "退款时间") LocalDateTime refundedAt,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}

    @Schema(description = "扣款明细视图")
    public record DeductionView(
            @Schema(description = "扣款 ID") String id,
            @Schema(description = "扣款类型：damage/cleaning/rent_arrears/bill_arrears/other") String deductionType,
            @Schema(description = "扣款金额，单位分") Integer amount,
            @Schema(description = "扣款说明") String description,
            @Schema(description = "凭证图片") List<String> evidenceUrls
    ) {}

    @Schema(description = "管理端押金列表项")
    public record AdminDepositItem(
            @Schema(description = "押金记录 ID") String id,
            @Schema(description = "租约 ID") String leaseId,
            @Schema(description = "租客昵称") String tenantName,
            @Schema(description = "租客手机号") String tenantPhone,
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "押金总额，单位分") Integer amount,
            @Schema(description = "已扣款金额，单位分") Integer withheldAmount,
            @Schema(description = "已退款金额，单位分") Integer refundedAmount,
            @Schema(description = "状态") String status,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}
}
