package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminLeaseDtos;
import com.zhuxiang.service.service.AdminLeaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/leases")
@Tag(name = "管理端租约", description = "管理端分页查看和筛选租约")
@SecurityRequirement(name = "bearerAuth")
public class AdminLeaseController {

    private final AdminLeaseService adminLeaseService;

    public AdminLeaseController(AdminLeaseService adminLeaseService) {
        this.adminLeaseService = adminLeaseService;
    }

    @GetMapping
    @Operation(summary = "分页查询租约", description = "按租约状态以及租约编号、租客姓名、手机号或房源信息筛选租约。")
    public ApiResponse<PageData<AdminLeaseDtos.AdminLeaseView>> getLeases(
            @Parameter(description = "租约状态：pending、active、expired 或 terminated") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词，匹配租约编号、租客姓名、手机号、房源名称和地址") @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminLeaseService.getLeases(
                CurrentUser.id(request), status, keyword, page, pageSize
        ));
    }
}
