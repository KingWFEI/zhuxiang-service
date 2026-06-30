package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "支付记录 DTO")
public final class PaymentDtos {

    private PaymentDtos() {
    }

    @Schema(description = "支付记录列表项")
    public record PaymentItem(
            @Schema(description = "支付记录 ID", example = "pay_xxx") String id,
            @Schema(description = "支付编号", example = "ZF202606300001") String paymentNo,
            @Schema(description = "关联账单 ID") String billId,
            @Schema(description = "关联租约 ID") String leaseId,
            @Schema(description = "房源名称", example = "3栋2单元1201") String houseName,
            @Schema(description = "支付类型", example = "rent") String type,
            @Schema(description = "支付类型文本", example = "租金") String typeText,
            @Schema(description = "支付金额(分)", example = "268000") Integer amount,
            @Schema(description = "状态", example = "paid") String status,
            @Schema(description = "状态文本", example = "支付成功") String statusText,
            @Schema(description = "支付方式", example = "wechat") String paymentMethod,
            @Schema(description = "支付方式文本", example = "微信支付") String paymentMethodText,
            @Schema(description = "支付时间") LocalDateTime paidAt,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "支付记录详情")
    public record PaymentDetail(
            @Schema(description = "支付记录 ID", example = "pay_xxx") String id,
            @Schema(description = "支付编号", example = "ZF202606300001") String paymentNo,
            @Schema(description = "关联账单 ID") String billId,
            @Schema(description = "关联租约 ID") String leaseId,
            @Schema(description = "房源名称", example = "3栋2单元1201") String houseName,
            @Schema(description = "支付类型", example = "rent") String type,
            @Schema(description = "支付类型文本", example = "租金") String typeText,
            @Schema(description = "支付金额(分)", example = "268000") Integer amount,
            @Schema(description = "状态", example = "paid") String status,
            @Schema(description = "状态文本", example = "支付成功") String statusText,
            @Schema(description = "支付方式", example = "wechat") String paymentMethod,
            @Schema(description = "支付方式文本", example = "微信支付") String paymentMethodText,
            @Schema(description = "渠道交易号", example = "wx_202606300001") String transactionNo,
            @Schema(description = "支付时间") LocalDateTime paidAt,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "备注", example = "2026年6月租金") String remark
    ) {
    }
}
