package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.service.impl.RentOrderServiceImpl;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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
    @Operation(summary = "支付租房订单", description = "按支付方式和支付渠道发起付款；mock 渠道自动确认，alipay 渠道返回 H5 支付页面 URL。")
    public ApiResponse<PayResponse> pay(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId,
            @Valid @RequestBody PayRequest body
    ) {
        return ApiResponse.success(
                "支付请求已提交",
                rentOrderService.pay(CurrentUser.id(request), orderId, body)
        );
    }

    @PostMapping("/rent-orders/{orderId}/sign")
    @Operation(summary = "发起电子签署", description = "支付成功后发起 e签宝电子签署，返回当前用户的签署网页链接。")
    public ApiResponse<EsignSignResponse> sign(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success("请打开签署链接完成签名",
                rentOrderService.sign(CurrentUser.id(request), orderId));
    }

    @PostMapping("/rent-orders/{orderId}/contract-refresh")
    @Operation(summary = "刷新电子合同签署状态", description = "查询 e签宝最新签署状态，双方签完后自动创建租约完成订单。")
    public ApiResponse<EsignSignStatusResponse> contractRefresh(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.contractRefresh(CurrentUser.id(request), orderId)
        );
    }

    @GetMapping("/rent-orders/{orderId}/contract-download-url")
    @Operation(summary = "获取已签合同下载链接", description = "合同签署完成后获取临时下载地址和签署证书地址。")
    public ApiResponse<ContractDownloadUrlResponse> contractDownloadUrl(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID", example = "order_001") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                rentOrderService.contractDownloadUrl(CurrentUser.id(request), orderId)
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
