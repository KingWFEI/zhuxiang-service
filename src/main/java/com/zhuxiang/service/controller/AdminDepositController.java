package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.DepositDtos;
import com.zhuxiang.service.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/deposits")
@Tag(name = "管理端押金", description = "管理端分页查看和筛选押金记录")
@SecurityRequirement(name = "bearerAuth")
public class AdminDepositController {

    private final DepositService depositService;

    public AdminDepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping
    @Operation(summary = "分页查询押金记录", description = "按状态、关键词（押金编号/租客姓名/手机号/房源）筛选押金记录。")
    public ApiResponse<PageData<DepositDtos.AdminDepositItem>> getDeposits(
            @Parameter(description = "押金状态：held/deducted/refunding/refunded") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(depositService.getDeposits(
                CurrentUser.id(request), status, keyword, page, pageSize
        ));
    }
}
