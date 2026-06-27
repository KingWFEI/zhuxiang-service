package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.service.RentOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
public class RentOrderController {

    private final RentOrderService rentOrderService;

    public RentOrderController(RentOrderService rentOrderService) {
        this.rentOrderService = rentOrderService;
    }

    @PostMapping("/rent-orders")
    public ApiResponse<RentOrderResponse> createOrder(
            HttpServletRequest request,
            @Valid @RequestBody CreateRentOrderRequest body
    ) {
        return ApiResponse.success(
                "订单创建成功",
                rentOrderService.createOrder(CurrentUser.id(request), body)
        );
    }

    @GetMapping("/rent-orders/my")
    public ApiResponse<PageData<RentOrderResponse>> listMyOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.success(
                rentOrderService.listMyOrders(CurrentUser.id(request), page, pageSize)
        );
    }

    @GetMapping("/rent-orders/{orderId}")
    public ApiResponse<RentOrderResponse> getOrderDetail(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getOrderDetail(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/real-name")
    public ApiResponse<RentOrderResponse> submitRealName(
            HttpServletRequest request,
            @PathVariable String orderId,
            @Valid @RequestBody RealNameRequest body
    ) {
        return ApiResponse.success(
                "实名认证提交成功",
                rentOrderService.submitRealName(CurrentUser.id(request), orderId, body)
        );
    }

    @GetMapping("/rent-orders/{orderId}/contract-preview")
    public ApiResponse<ContractPreviewResponse> getContractPreview(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getContractPreview(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/confirm-contract")
    public ApiResponse<RentOrderResponse> confirmContract(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                "合同已确认",
                rentOrderService.confirmContract(CurrentUser.id(request), orderId)
        );
    }

    @GetMapping("/rent-orders/{orderId}/payment-info")
    public ApiResponse<PaymentInfoResponse> getPaymentInfo(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getPaymentInfo(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/pay")
    public ApiResponse<RentOrderResponse> pay(
            HttpServletRequest request,
            @PathVariable String orderId,
            @Valid @RequestBody PayRequest body
    ) {
        return ApiResponse.success(
                "支付成功",
                rentOrderService.pay(CurrentUser.id(request), orderId, body)
        );
    }

    @PostMapping("/rent-orders/{orderId}/sign")
    public ApiResponse<RentOrderResponse> sign(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                "签约完成",
                rentOrderService.sign(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/cancel")
    public ApiResponse<RentOrderResponse> cancelOrder(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        return ApiResponse.success(
                "订单已取消",
                rentOrderService.cancelOrder(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/hide")
    public ApiResponse<Object> hideOrder(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        rentOrderService.hideOrder(CurrentUser.id(request), orderId);
        return ApiResponse.success("删除成功", null);
    }
}
