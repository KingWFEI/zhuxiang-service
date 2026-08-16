package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.BillDtos;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.service.BillService;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.impl.BillServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@Tag(name = "账单", description = "租金账单查询与支付")
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;
    private final BillServiceImpl billServiceImpl;
    private final PaymentRecordService paymentRecordService;
    private final AlipayService alipayService;

    public BillController(
            BillService billService,
            BillServiceImpl billServiceImpl,
            PaymentRecordService paymentRecordService,
            AlipayService alipayService
    ) {
        this.billService = billService;
        this.billServiceImpl = billServiceImpl;
        this.paymentRecordService = paymentRecordService;
        this.alipayService = alipayService;
    }

    @GetMapping("/bills/my")
    @Operation(summary = "我的账单", description = "获取当前用户所有待付和已付账单，按状态分组")
    public ApiResponse<BillDtos.BillGroupedResponse> getMyBills(HttpServletRequest request) {
        return ApiResponse.success(billService.getMyBills(CurrentUser.id(request)));
    }

    @GetMapping("/bills/{billId}")
    @Operation(summary = "账单详情", description = "查看单条账单详情，校验归属")
    public ApiResponse<BillDtos.BillItem> getBillDetail(
            HttpServletRequest request,
            @Parameter(description = "账单 ID") @PathVariable String billId
    ) {
        return ApiResponse.success(billService.getBillDetail(CurrentUser.id(request), billId));
    }

    @PostMapping("/bills/{billId}/pay")
    @Operation(summary = "支付账单", description = "发起支付宝账单支付，按环境返回 H5 URL 或 APP SDK 订单串")
    public ApiResponse<BillDtos.BillPayResponse> payBill(
            HttpServletRequest request,
            @Parameter(description = "账单 ID") @PathVariable String billId,
            @Valid @RequestBody BillDtos.BillPayRequest body
    ) {
        return ApiResponse.success(billService.payBill(CurrentUser.id(request), billId, body));
    }

    @PostMapping("/bills/{paymentNo}/confirm")
    @Operation(summary = "主动确认账单支付", description = "客户端支付后查询支付宝订单，验金额后确认账单支付")
    public ApiResponse<Boolean> confirmBillPayment(
            HttpServletRequest request,
            @Parameter(description = "支付编号") @PathVariable String paymentNo
    ) {
        PaymentRecord record = paymentRecordService.getOne(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getPaymentNo, paymentNo)
                        .eq(PaymentRecord::getUserId, CurrentUser.id(request))
                        .eq(PaymentRecord::getPaymentChannel, "alipay"),
                false
        );
        if (record == null) {
            return new ApiResponse<>(400, "支付记录不存在", false);
        }
        if ("success".equals(record.getStatus())) {
            return ApiResponse.success("支付已完成", true);
        }
        AlipayService.AlipayNotifyResult result = alipayService.queryOrder(paymentNo);
        if (result == null) {
            return ApiResponse.success("暂未查询到支付结果，请稍后再试", false);
        }
        int paidAmount = new java.math.BigDecimal(result.totalAmount())
                .multiply(java.math.BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();
        if (paidAmount != record.getAmount()) {
            return new ApiResponse<>(400, "支付金额不匹配", false);
        }
        billServiceImpl.confirmBillPayment(record.getId(), result.tradeNo());
        return ApiResponse.success("支付确认成功", true);
    }
}
