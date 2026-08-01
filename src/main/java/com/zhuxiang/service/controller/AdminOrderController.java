package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminOrderDtos;
import com.zhuxiang.service.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequireAuth
@RequestMapping("/admin/orders")
@Tag(name = "管理端订单", description = "管理端查询真实租房订单、费用、支付和签约状态")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {
    private final AdminOrderService service;
    public AdminOrderController(AdminOrderService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "分页查询租房订单", description = "按订单状态或关键字分页查询租房订单，关键字支持订单号、租客、房东和房源信息。")
    public ApiResponse<PageData<AdminOrderDtos.OrderView>> list(HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(service.list(CurrentUser.id(request), status, keyword, page, pageSize));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "查询租房订单详情", description = "查询订单费用、租客、房东、房源、支付记录和合同签署状态。")
    public ApiResponse<AdminOrderDtos.OrderView> get(HttpServletRequest request,
                                                      @Parameter(description = "租房订单 ID") @PathVariable String orderId) {
        return ApiResponse.success(service.get(CurrentUser.id(request), orderId));
    }
}
