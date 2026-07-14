package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.BillDtos;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.service.BillService;
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

    public BillController(
            BillService billService,
            BillServiceImpl billServiceImpl,
            PaymentRecordService paymentRecordService
    ) {
        this.billService = billService;
        this.billServiceImpl = billServiceImpl;
        this.paymentRecordService = paymentRecordService;
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
    @Operation(summary = "支付账单", description = "发起账单支付，返回支付页面 URL 或 mock 自动确认")
    public ApiResponse<BillDtos.BillPayResponse> payBill(
            HttpServletRequest request,
            @Parameter(description = "账单 ID") @PathVariable String billId,
            @Valid @RequestBody BillDtos.BillPayRequest body
    ) {
        return ApiResponse.success(billService.payBill(CurrentUser.id(request), billId, body));
    }

    @PostMapping("/bills/{paymentNo}/confirm")
    @Operation(summary = "主动确认账单支付", description = "支付完成后主动确认账单支付，开发阶段兜底")
    public ApiResponse<Boolean> confirmBillPayment(
            HttpServletRequest request,
            @Parameter(description = "支付编号") @PathVariable String paymentNo
    ) {
        PaymentRecord record = paymentRecordService.getOne(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getPaymentNo, paymentNo),
                false
        );
        if (record == null) {
            return new ApiResponse<>(400, "支付记录不存在", false);
        }
        if ("success".equals(record.getStatus())) {
            return ApiResponse.success("支付已完成", true);
        }
        billServiceImpl.confirmBillPayment(record.getId(), "alipay_client_" + paymentNo);
        return ApiResponse.success("支付确认成功", true);
    }
}
