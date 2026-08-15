package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminBillDtos;
import com.zhuxiang.service.service.AdminBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/bills")
@Tag(name = "管理端账单", description = "管理端查询租金账单、关联租约和支付信息")
@SecurityRequirement(name = "bearerAuth")
public class AdminBillController {

    private final AdminBillService adminBillService;

    public AdminBillController(AdminBillService adminBillService) {
        this.adminBillService = adminBillService;
    }

    @GetMapping
    @Operation(summary = "分页查询账单", description = "按账单状态、到期日和账单、租约、租客或房源关键词筛选。")
    public ApiResponse<PageData<AdminBillDtos.BillView>> getBills(
            @Parameter(description = "账单状态：scheduled、pending、paid、overdue 或 cancelled")
            @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词，匹配账单、租约、租客和房源信息")
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "到期日开始")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateStart,
            @Parameter(description = "到期日结束")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateEnd,
            @Parameter(description = "当前页码，从 1 开始")
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，最大 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminBillService.getBills(
                CurrentUser.id(request), status, keyword, dueDateStart, dueDateEnd, page, pageSize
        ));
    }

    @GetMapping("/summary")
    @Operation(summary = "查询账单汇总", description = "根据 rent_bill 表实时汇总应收、实收、待收和各状态账单数量。")
    public ApiResponse<AdminBillDtos.BillSummary> getSummary(HttpServletRequest request) {
        return ApiResponse.success(adminBillService.getSummary(CurrentUser.id(request)));
    }

    @GetMapping("/{billId}")
    @Operation(summary = "查询账单详情", description = "查询账单及其租客、房源、租约和最近支付记录。")
    public ApiResponse<AdminBillDtos.BillView> getBill(
            @Parameter(description = "账单 ID")
            @PathVariable String billId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminBillService.getBill(CurrentUser.id(request), billId));
    }
}
