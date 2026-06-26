package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.service.RentOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
    public ApiResponse<Void> mockCallback(
            HttpServletRequest request,
            @PathVariable String recordId
    ) {
        rentOrderService.confirmPayment(recordId, null);
        return ApiResponse.<Void>success("支付回调确认成功", null);
    }
}
