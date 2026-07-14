package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "账单 DTO")
public final class BillDtos {

    private BillDtos() {}

    @Schema(description = "按租约分组的账单列表")
    public record BillGroupedResponse(
            @Schema(description = "未到期账单（预生成，不可支付）") List<BillItem> scheduledBills,
            @Schema(description = "待付/逾期账单") List<BillItem> pendingBills,
            @Schema(description = "已付账单") List<BillItem> paidBills
    ) {}

    @Schema(description = "账单列表项")
    public record BillItem(
            @Schema(description = "账单 ID") String id,
            @Schema(description = "租约 ID") String leaseId,
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "房源图片") String houseImageUrl,
            @Schema(description = "第几期") Integer periodNo,
            @Schema(description = "应缴金额，单位分") Integer amountDue,
            @Schema(description = "已缴金额，单位分") Integer amountPaid,
            @Schema(description = "滞纳金，单位分") Integer overdueAmount,
            @Schema(description = "应缴日期") LocalDate dueDate,
            @Schema(description = "支付时间") LocalDateTime paidAt,
            @Schema(description = "状态：pending/paid/overdue/cancelled") String status
    ) {}

    @Schema(description = "账单支付请求")
    public record BillPayRequest(
            @Schema(description = "支付渠道：alipay/wechat/mock") String paymentChannel
    ) {}

    @Schema(description = "账单支付响应")
    public record BillPayResponse(
            @Schema(description = "账单 ID") String billId,
            @Schema(description = "支付记录 ID") String paymentRecordId,
            @Schema(description = "支付编号") String paymentNo,
            @Schema(description = "支付类型") String payType,
            @Schema(description = "支付页面 URL") String paymentUrl,
            @Schema(description = "支付金额，单位分") Integer amount
    ) {}
}
