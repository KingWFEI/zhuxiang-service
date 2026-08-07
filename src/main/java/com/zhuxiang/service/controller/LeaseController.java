package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.dto.UnlockRecordDtos;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.UnlockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@Tag(name = "租约", description = "当前用户租约及关联门锁查询")
@SecurityRequirement(name = "bearerAuth")
public class LeaseController {

    private final LeaseService leaseService;
    private final UnlockRecordService unlockRecordService;

    public LeaseController(
            LeaseService leaseService,
            UnlockRecordService unlockRecordService
    ) {
        this.leaseService = leaseService;
        this.unlockRecordService = unlockRecordService;
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
     * 根据租约 ID 获取当前租客自己的租约详情。
     */
    @GetMapping("/leases/{leaseId}")
    @Operation(summary = "查询租约详情", description = "根据租约 ID 返回当前登录租客自己的合同、房源、账单和门锁权限信息。")
    public ApiResponse<LeaseDtos.LeaseDetail> getLeaseDetail(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(leaseService.getLeaseDetail(leaseId, CurrentUser.id(request)));
    }

    @GetMapping("/leases/{leaseId}/contract")
    @Operation(summary = "查询租约电子合同", description = "返回合同摘要、正文条款以及 e签宝已签合同文件地址。")
    public ApiResponse<LeaseDtos.LeaseContractDocument> getLeaseContract(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                leaseService.getLeaseContract(leaseId, CurrentUser.id(request))
        );
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
    @Operation(
            summary = "获取租约门锁开锁摘要",
            description = "先校验租约归属和有效状态；失效租约仅返回 leaseValid=false，不返回 lockData 等开锁数据。"
    )
    public ApiResponse<LeaseDtos.UnlockDataResponse> getUnlockData(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(leaseService.getUnlockData(leaseId, CurrentUser.id(request)));
    }

    /**
     * 记录租客开锁结果（手动蓝牙 + 无感）。服务端会重新校验租约、门锁和 eKey 权限。
     */
    @PostMapping("/leases/{leaseId}/lock/unlock-records")
    @Operation(
            summary = "记录开锁结果",
            description = "仅接收脱敏审计字段；不接收 lockData、密码或 Token。门锁身份必须与当前有效租约一致。triggerType 支持 MANUAL_BLUETOOTH 或 AUTO_NEARBY。"
    )
    public ApiResponse<UnlockRecordDtos.UnlockRecordResponse> recordUnlock(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            @Valid @RequestBody UnlockRecordDtos.UnlockRecordRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "开锁记录成功",
                unlockRecordService.record(leaseId, CurrentUser.id(request), body)
        );
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

    /**
     * 当前租客在密码生成异常时主动重试。
     */
    @PostMapping("/leases/{leaseId}/lock/passcode/retry")
    @Operation(
            summary = "重新获取租约期限密码",
            description = "仅限租约本人操作。FAILED 状态会重新调用 TTLock，ACTIVE 状态直接复用已有密码。"
    )
    public ApiResponse<LeaseLockPasscodeResponse> retryPasscode(
            @Parameter(description = "租约 ID") @PathVariable String leaseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "开锁密码获取成功",
                leaseService.retryLockPasscode(leaseId, CurrentUser.id(request))
        );
    }
}
