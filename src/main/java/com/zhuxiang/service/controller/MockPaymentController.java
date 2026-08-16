package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 仅在开发环境显式开启 alipay.mock-enabled 时注册的模拟支付端点。 */
@RestController
@ConditionalOnProperty(prefix = "alipay", name = "mock-enabled", havingValue = "true")
public class MockPaymentController {

    private final RentOrderService rentOrderService;
    private final PaymentRecordService paymentRecordService;

    public MockPaymentController(
            RentOrderService rentOrderService,
            PaymentRecordService paymentRecordService
    ) {
        this.rentOrderService = rentOrderService;
        this.paymentRecordService = paymentRecordService;
    }

    @RequireAuth
    @PostMapping("/payment-records/{recordId}/mock-callback")
    @Operation(summary = "模拟支付回调", description = "仅在开发环境显式开启模拟支付后可用。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> mockCallback(
            HttpServletRequest request,
            @Parameter(description = "支付记录 ID") @PathVariable String recordId
    ) {
        PaymentRecord record = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getId, recordId)
                .eq(PaymentRecord::getUserId, CurrentUser.id(request))
                .eq(PaymentRecord::getPaymentChannel, "mock")
                .one();
        if (record == null) {
            throw BusinessException.notFound("模拟支付记录不存在");
        }
        rentOrderService.confirmPayment(recordId, null);
        return ApiResponse.<Void>success("支付回调确认成功", null);
    }
}
