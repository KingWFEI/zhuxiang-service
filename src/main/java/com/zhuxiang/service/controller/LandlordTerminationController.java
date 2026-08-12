package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordContractDtos;
import com.zhuxiang.service.service.LandlordTerminationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequireAuth
@RequestMapping("/landlord/termination-applications")
public class LandlordTerminationController {
    private final LandlordTerminationService service;

    public LandlordTerminationController(LandlordTerminationService service) {
        this.service = service;
    }

    @GetMapping("/pending-sign")
    public ApiResponse<PageData<LandlordContractDtos.TerminationItem>> pendingSign(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.listPendingSign(CurrentUser.id(request), page, pageSize));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<LandlordContractDtos.TerminationDetail> detail(
            HttpServletRequest request, @PathVariable String applicationId) {
        return ApiResponse.success(service.getDetail(CurrentUser.id(request), applicationId));
    }

    @PostMapping("/{applicationId}/rescission-sign-url")
    public ApiResponse<LandlordContractDtos.RescissionAction> signUrl(
            HttpServletRequest request, @PathVariable String applicationId) {
        return ApiResponse.success(service.getSignUrl(CurrentUser.id(request), applicationId));
    }

    @PostMapping("/{applicationId}/refresh")
    public ApiResponse<LandlordContractDtos.RescissionAction> refresh(
            HttpServletRequest request, @PathVariable String applicationId) {
        return ApiResponse.success(service.refresh(CurrentUser.id(request), applicationId));
    }
}
