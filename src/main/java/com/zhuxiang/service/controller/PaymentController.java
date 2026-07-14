package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "支付", description = "租房订单支付回调")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final RentOrderService rentOrderService;
    private final AlipayService alipayService;
    private final PaymentRecordService paymentRecordService;

    public PaymentController(
            RentOrderService rentOrderService,
            AlipayService alipayService,
            PaymentRecordService paymentRecordService
    ) {
        this.rentOrderService = rentOrderService;
        this.alipayService = alipayService;
        this.paymentRecordService = paymentRecordService;
    }

    @RequireAuth
    @PostMapping("/payment-records/{recordId}/mock-callback")
    @Operation(summary = "模拟支付回调", description = "仅供开发和测试环境模拟支付网关回调，按支付记录 ID 确认付款。生产环境应由支付网关异步通知替代。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> mockCallback(
            HttpServletRequest request,
            @Parameter(description = "支付记录 ID", example = "payment_001") @PathVariable String recordId
    ) {
        rentOrderService.confirmPayment(recordId, null);
        return ApiResponse.<Void>success("支付回调确认成功", null);
    }

    @RequireAuth
    @PostMapping("/payments/alipay/{paymentNo}/confirm")
    @Operation(summary = "主动确认支付宝支付", description = "客户端支付完成后主动查询支付宝订单状态并确认支付。用于开发阶段支付宝异步通知无法到达本地时的兜底方案。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Boolean> confirmAlipayPayment(
            HttpServletRequest request,
            @Parameter(description = "支付编号 paymentNo", example = "ZF202607110001") @PathVariable String paymentNo
    ) {
        // 查找支付记录
        PaymentRecord record = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getPaymentNo, paymentNo)
                .one();
        if (record == null) {
            return new ApiResponse<>(400, "支付记录不存在", false);
        }

        // 已成功的直接返回
        if ("success".equals(record.getStatus())) {
            return ApiResponse.success("支付已完成", true);
        }

        // 主动查询支付宝订单状态
        AlipayService.AlipayNotifyResult result = alipayService.queryOrder(paymentNo);
        if (result == null) {
            return ApiResponse.success("暂未查询到支付结果，请稍后再试", false);
        }

        // 金额校验
        java.math.BigDecimal alipayAmount = new java.math.BigDecimal(result.totalAmount())
                .multiply(java.math.BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP);
        if (alipayAmount.intValue() != record.getAmount()) {
            log.error("支付宝金额不匹配 paymentNo={} alipay={} record={}",
                    paymentNo, alipayAmount.intValue(), record.getAmount());
            return new ApiResponse<>(400, "支付金额不匹配", false);
        }

        // 确认支付
        rentOrderService.confirmPayment(record.getId(), result.tradeNo());
        log.info("支付宝主动确认成功 paymentNo={} tradeNo={}", paymentNo, result.tradeNo());
        return ApiResponse.success("支付确认成功", true);
    }

}
