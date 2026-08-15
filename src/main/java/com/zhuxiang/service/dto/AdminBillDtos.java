package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminBillDtos {

    private AdminBillDtos() {
    }

    @Schema(description = "管理端账单信息")
    public record BillView(
            @Schema(description = "账单 ID")
            String billId,
            @Schema(description = "租约 ID")
            String leaseId,
            @Schema(description = "账单期次")
            Integer periodNo,
            @Schema(description = "应付金额，单位分")
            Integer amountDue,
            @Schema(description = "已付金额，单位分")
            Integer amountPaid,
            @Schema(description = "逾期金额，单位分")
            Integer overdueAmount,
            @Schema(description = "待付金额，单位分")
            Integer outstandingAmount,
            @Schema(description = "账单到期日")
            LocalDate dueDate,
            @Schema(description = "付款完成时间")
            LocalDateTime paidAt,
            @Schema(description = "账单状态")
            String status,
            @Schema(description = "租客用户 ID")
            String tenantId,
            @Schema(description = "租客姓名")
            String tenantName,
            @Schema(description = "租客联系电话")
            String tenantPhone,
            @Schema(description = "房源 ID")
            String houseId,
            @Schema(description = "房源名称")
            String houseName,
            @Schema(description = "房源详细地址")
            String houseAddress,
            @Schema(description = "租约状态")
            String leaseStatus,
            @Schema(description = "最近支付单号")
            String paymentNo,
            @Schema(description = "最近支付渠道")
            String paymentChannel,
            @Schema(description = "最近支付状态")
            String paymentStatus,
            @Schema(description = "支付渠道交易号")
            String channelTradeNo,
            @Schema(description = "创建时间")
            LocalDateTime createdAt,
            @Schema(description = "最后更新时间")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "管理端账单汇总")
    public record BillSummary(
            @Schema(description = "账单总数")
            long totalBillCount,
            @Schema(description = "待生效账单数")
            long scheduledCount,
            @Schema(description = "待支付账单数")
            long pendingCount,
            @Schema(description = "已支付账单数")
            long paidCount,
            @Schema(description = "逾期账单数")
            long overdueCount,
            @Schema(description = "已取消账单数")
            long cancelledCount,
            @Schema(description = "应收总额，单位分")
            long receivableAmount,
            @Schema(description = "实收总额，单位分")
            long receivedAmount,
            @Schema(description = "待收总额，单位分")
            long outstandingAmount,
            @Schema(description = "逾期待收总额，单位分")
            long overdueOutstandingAmount
    ) {
        public static BillSummary empty() {
            return new BillSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
