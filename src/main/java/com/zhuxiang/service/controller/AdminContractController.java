package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractDtos;
import com.zhuxiang.service.service.AdminContractService;
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
public class AdminContractController {
    private final AdminContractService service;
    public AdminContractController(AdminContractService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageData<AdminContractDtos.ContractSummary>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(service.list(CurrentUser.id(request), status, keyword, page, pageSize));
    }

    @GetMapping("/{contractId}")
    public ApiResponse<AdminContractDtos.ContractDetail> get(HttpServletRequest request,
                                                              @PathVariable String contractId) {
        return ApiResponse.success(service.get(CurrentUser.id(request), contractId));
    }

    @GetMapping("/{contractId}/download-url")
    public ApiResponse<AdminContractDtos.DownloadUrl> download(HttpServletRequest request,
                                                                @PathVariable String contractId) {
        return ApiResponse.success(service.downloadUrl(CurrentUser.id(request), contractId));
    }
}
