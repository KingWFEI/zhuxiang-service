package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractDtos;
import com.zhuxiang.service.service.AdminContractService;
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
@RequestMapping("/admin/contracts")
@Tag(name = "管理端合同", description = "管理端查询合同列表、合同详情和已签合同下载地址")
@SecurityRequirement(name = "bearerAuth")
public class AdminContractController {
    private final AdminContractService service;
    public AdminContractController(AdminContractService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "分页查询合同", description = "按合同状态或关键字分页查询平台合同，关键字支持合同编号、房源、租客和房东信息。")
    public ApiResponse<PageData<AdminContractDtos.ContractSummary>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(service.list(CurrentUser.id(request), status, keyword, page, pageSize));
    }

    @GetMapping("/{contractId}")
    @Operation(summary = "查询合同详情", description = "根据合同 ID 查询合同快照、签署状态、模板版本和 e签宝流程信息。")
    public ApiResponse<AdminContractDtos.ContractDetail> get(HttpServletRequest request,
                                                              @Parameter(description = "合同 ID") @PathVariable String contractId) {
        return ApiResponse.success(service.get(CurrentUser.id(request), contractId));
    }

    @GetMapping("/{contractId}/download-url")
    @Operation(summary = "获取合同下载地址", description = "获取合同文件的临时下载地址；已发起签署的合同优先返回 e签宝签署文件地址。")
    public ApiResponse<AdminContractDtos.DownloadUrl> download(HttpServletRequest request,
                                                                @Parameter(description = "合同 ID") @PathVariable String contractId) {
        return ApiResponse.success(service.downloadUrl(CurrentUser.id(request), contractId));
    }
}
