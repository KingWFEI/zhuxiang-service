package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairItem;
import com.zhuxiang.service.service.RepairRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/repairs")
@Tag(name = "管理端报修", description = "管理端报修记录管理接口")
@SecurityRequirement(name = "bearerAuth")
public class RepairAdminController {

    private final RepairRecordService repairRecordService;

    public RepairAdminController(RepairRecordService repairRecordService) {
        this.repairRecordService = repairRecordService;
    }

    @GetMapping
    @Operation(summary = "报修记录列表", description = "分页查询全平台报修记录，支持关键字搜索和状态筛选")
    public ApiResponse<PageData<AdminRepairItem>> listRepairs(
            @Parameter(description = "关键字搜索（工单号、租客姓名、手机号、房源名称、房源地址、报修内容）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.success(repairRecordService.listAdminRepairs(keyword, status, page, pageSize));
    }
}
