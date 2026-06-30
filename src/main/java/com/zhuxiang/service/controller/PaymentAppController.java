package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.PaymentDtos.PaymentDetail;
import com.zhuxiang.service.dto.PaymentDtos.PaymentItem;
import com.zhuxiang.service.service.PaymentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/payments")
@Tag(name = "App 支付记录", description = "App 端支付记录查询接口")
@SecurityRequirement(name = "bearerAuth")
public class PaymentAppController {

    private final PaymentRecordService paymentRecordService;

    public PaymentAppController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @GetMapping("/my")
    @Operation(summary = "我的支付记录", description = "分页查询当前登录用户的支付记录，支持按状态和类型筛选")
    public ApiResponse<PageData<PaymentItem>> listMyPayments(
            @Parameter(description = "支付状态：pending/success/failed/refunded")
            @RequestParam(required = false) String status,
            @Parameter(description = "支付类型：rent/deposit/service_fee/refund")
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        return ApiResponse.success(paymentRecordService.listMyPayments(userId, status, type, page, pageSize));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "支付记录详情", description = "查看单条支付记录的详细信息，必须校验当前用户权限")
    public ApiResponse<PaymentDetail> getPaymentDetail(
            @Parameter(description = "支付记录 ID") @PathVariable String paymentId,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        return ApiResponse.success(paymentRecordService.getPaymentDetail(userId, paymentId));
    }
}
