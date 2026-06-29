package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.service.LeaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@Tag(name = "租约", description = "当前用户租约及关联门锁查询")
@SecurityRequirement(name = "bearerAuth")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    /**
     * 获取当前用户全部租约（当前生效 + 历史）。
     */
    @GetMapping("/leases/my")
    @Operation(summary = "获取我的租约", description = "分别返回当前生效租约和历史租约，包含合同、账单、门锁权限和管家信息。")
    public ApiResponse<LeaseDtos.LeaseListResponse> getMyLeases(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getUserLeases(CurrentUser.id(request)));
    }

    /**
     * 获取当前用户租约对应的门锁展示信息。
     */
    @GetMapping("/leases/my/lock")
    @Operation(summary = "获取我的租约门锁", description = "返回当前有效租约关联的门锁及开锁权限摘要。")
    public ApiResponse<ProfileDtos.LockInfo> getMyLockInfo(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getLockInfo(CurrentUser.id(request)));
    }

    /**
     * 获取指定租约的门锁权限摘要。
     */
    @GetMapping("/leases/{leaseId}/lock/unlock-data")
    @Operation(summary = "获取租约门锁开锁摘要", description = "返回蓝牙和期限密码可用性，不返回管理员 lockData 或明文密码。")
    public ApiResponse<LeaseDtos.UnlockDataResponse> getUnlockData(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(leaseService.getUnlockData(leaseId, CurrentUser.id(request)));
    }

    /**
     * 获取当前租客的租约期限密码。
     */
    @GetMapping("/leases/{leaseId}/lock/passcode")
    @Operation(
            summary = "获取租约期限密码",
            description = "仅限租约本人在有效期内查看。请在密码生效后的24小时内至少使用一次，否则密码可能失效。"
    )
    public ApiResponse<LeaseLockPasscodeResponse> getPasscode(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(leaseService.getLockPasscode(leaseId, CurrentUser.id(request)));
    }
}
