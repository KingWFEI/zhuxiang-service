package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "支付", description = "租房订单支付回调")
public class PaymentController {

    private final RentOrderService rentOrderService;

    public PaymentController(RentOrderService rentOrderService) {
        this.rentOrderService = rentOrderService;
    }

    /**
     * 模拟支付回调（开发/测试用）。
     * 生产环境接入微信/支付宝后，由支付网关的异步回调通知代替。
     */
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
}
