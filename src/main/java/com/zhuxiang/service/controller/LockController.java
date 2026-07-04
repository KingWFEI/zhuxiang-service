package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.UnlockRecordDtos;
import com.zhuxiang.service.service.UnlockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@Tag(name = "门锁", description = "当前用户门锁相关操作")
@SecurityRequirement(name = "bearerAuth")
public class LockController {

    private final UnlockRecordService unlockRecordService;

    public LockController(UnlockRecordService unlockRecordService) {
        this.unlockRecordService = unlockRecordService;
    }

    /**
     * 查询当前用户所有开门记录（手动蓝牙 + 无感自动），按时间降序。
     */
    @GetMapping("/locks/unlock-records/my")
    @Operation(
            summary = "查询我的开门记录",
            description = "返回当前用户所有租约关联的开锁记录，包含房源、门锁、开锁方式等信息，按时间降序排列，不分页。"
    )
    public ApiResponse<UnlockRecordDtos.UnlockRecordListResponse> listMyUnlockRecords(
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                unlockRecordService.listMyRecords(CurrentUser.id(request))
        );
    }

    /**
     * 查询当前用户主门锁权限状态，用于开门记录页顶部卡片展示。
     */
    @GetMapping("/locks/my-permissions")
    @Operation(
            summary = "查询我的门锁权限",
            description = "返回当前用户主门锁的权限状态、房源门锁名称、最近开锁时间和支持的开锁方式。"
    )
    public ApiResponse<UnlockRecordDtos.LockPermissionResponse> getMyPermission(
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                unlockRecordService.getMyPermission(CurrentUser.id(request))
        );
    }
}
