package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@Tag(name = "租房订单", description = "租房订单创建、实名、合同确认、支付和签约流程")
@SecurityRequirement(name = "bearerAuth")
public class RentOrderController {

    private final RentOrderService rentOrderService;

    public RentOrderController(RentOrderService rentOrderService) {
        this.rentOrderService = rentOrderService;
    }

    @PostMapping("/rent-orders")
    @Operation(summary = "创建租房订单", description = "按房源、起租日期、租期、付款方式和入住人数创建订单并锁定初始费用。")
    public ApiResponse<RentOrderResponse> createOrder(
            HttpServletRequest request,
            @Valid @RequestBody CreateRentOrderRequest body
    ) {
        return ApiResponse.success(
                "订单创建成功",
                rentOrderService.createOrder(CurrentUser.id(request), body)
        );
    }

    @GetMapping("/rent-orders/{orderId}")
    @Operation(summary = "获取订单详情", description = "查询当前用户的租房订单、费用、实名、合同和支付状态。")
    public ApiResponse<RentOrderResponse> getOrderDetail(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getOrderDetail(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/real-name")
    @Operation(summary = "提交实名认证", description = "为订单提交租客姓名、手机号、身份证号以及身份证正反面图片地址。")
    public ApiResponse<RentOrderResponse> submitRealName(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId,
            @Valid @RequestBody RealNameRequest body
    ) {
        return ApiResponse.success(
                "实名认证提交成功",
                rentOrderService.submitRealName(CurrentUser.id(request), orderId, body)
        );
    }

    @GetMapping("/rent-orders/{orderId}/contract-preview")
    @Operation(summary = "预览租赁合同", description = "根据订单和实名认证信息生成合同预览数据。")
    public ApiResponse<ContractPreviewResponse> getContractPreview(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getContractPreview(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/confirm-contract")
    @Operation(summary = "确认租赁合同", description = "确认当前订单的合同内容，确认后进入待支付阶段。")
    public ApiResponse<RentOrderResponse> confirmContract(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                "合同已确认",
                rentOrderService.confirmContract(CurrentUser.id(request), orderId)
        );
    }

    @GetMapping("/rent-orders/{orderId}/payment-info")
    @Operation(summary = "获取支付信息", description = "返回订单首期应付金额、租金、押金、服务费和付款周期。")
    public ApiResponse<PaymentInfoResponse> getPaymentInfo(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.getPaymentInfo(CurrentUser.id(request), orderId)
        );
    }

    @PostMapping("/rent-orders/{orderId}/pay")
    @Operation(summary = "支付租房订单", description = "按支付方式和支付渠道发起付款；当前实现可使用 mock 渠道。")
    public ApiResponse<RentOrderResponse> pay(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId,
            @Valid @RequestBody PayRequest body
    ) {
        return ApiResponse.success(
                "支付成功",
                rentOrderService.pay(CurrentUser.id(request), orderId, body)
        );
    }

    @PostMapping("/rent-orders/{orderId}/sign")
    @Operation(summary = "完成订单签约", description = "在合同确认并支付后完成签约，生成后续租约数据。")
    public ApiResponse<RentOrderResponse> sign(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                "签约完成",
                rentOrderService.sign(CurrentUser.id(request), orderId)
        );
    }
}
