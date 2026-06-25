package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.service.LeaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    /**
     * 获取当前用户全部租约（当前生效 + 历史）。
     */
    @GetMapping("/leases/my")
    public ApiResponse<LeaseDtos.LeaseListResponse> getMyLeases(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getUserLeases(CurrentUser.id(request)));
    }

    /**
     * 获取当前用户租约对应的门锁展示信息。
     */
    @GetMapping("/leases/my/lock")
    public ApiResponse<ProfileDtos.LockInfo> getMyLockInfo(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getLockInfo(CurrentUser.id(request)));
    }
}
