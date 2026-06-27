package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.BindRoomRequest;
import com.zhuxiang.service.dto.BleStatusRequest;
import com.zhuxiang.service.dto.InitializeLockResponse;
import com.zhuxiang.service.dto.LocalInitializedLockRequest;
import com.zhuxiang.service.dto.LocalInitializedLockResponse;
import com.zhuxiang.service.dto.SmartLockByMacResponse;
import com.zhuxiang.service.dto.SmartLockDetailResponse;
import com.zhuxiang.service.dto.SmartLockUnlockDataResponse;
import com.zhuxiang.service.service.AdminLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端门锁接口。
 */
@RequireAuth
@RestController
@RequestMapping("/admin/locks")
@Tag(name = "管理端门锁", description = "智能门锁录入、绑定、平台同步、状态查询和蓝牙开锁数据")
@SecurityRequirement(name = "bearerAuth")
public class AdminLockController {

    private final AdminLockService adminLockService;

    public AdminLockController(AdminLockService adminLockService) {
        this.adminLockService = adminLockService;
    }

    /**
     * 保存App端SDK初始化成功后的门锁数据。
     */
    @PostMapping("/local-initialized")
    @Operation(summary = "保存本地初始化门锁", description = "保存 App 通过蓝牙 SDK 初始化成功后返回的门锁数据，暂不绑定房源。")
    public ApiResponse<LocalInitializedLockResponse> saveLocalInitializedLock(
            HttpServletRequest servletRequest,
            @Valid @RequestBody LocalInitializedLockRequest request
    ) {
        return ApiResponse.success(
                "门锁初始化数据已保存",
                adminLockService.saveLocalInitializedLock(request, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 将已录入的门锁绑定到指定房源或房间。
     */
    @PostMapping("/{smartLockId}/bind-room")
    @Operation(summary = "绑定门锁到房源", description = "把已录入的智能门锁绑定到指定房源；roomId 可选。")
    public ApiResponse<InitializeLockResponse> bindRoom(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId,
            @Valid @RequestBody BindRoomRequest request
    ) {
        return ApiResponse.success(
                "门锁绑定房间成功",
                adminLockService.bindRoom(smartLockId, request, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 将已保存的门锁初始化数据同步到通通锁开放平台。
     */
    @PostMapping("/{smartLockId}/sync-platform")
    @Operation(summary = "同步门锁到开放平台", description = "将本地保存的初始化数据同步到通通锁开放平台，并保存平台门锁 ID 和钥匙 ID。")
    public ApiResponse<InitializeLockResponse> syncPlatform(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId
    ) {
        return ApiResponse.success(
                "门锁同步开放平台成功",
                adminLockService.syncPlatform(smartLockId, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 删除门锁和房源之间的绑定关系。
     * 仅用于云平台调用链路失败的时候回退调用
     */
    @DeleteMapping("/{smartLockId}/bind-room")
    @Operation(summary = "解除门锁房源绑定", description = "删除门锁与房源或房间的本地绑定关系，主要用于平台同步失败后的回退。")
    public ApiResponse<InitializeLockResponse> deleteRoomBinding(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId
    ) {
        return ApiResponse.success(
                "门锁绑定记录已删除",
                adminLockService.deleteRoomBinding(smartLockId, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 根据MAC查询门锁本地记录。
     */
    @GetMapping("/by-mac")
    @Operation(summary = "按 MAC 查询门锁", description = "根据蓝牙门锁 MAC 地址查询本地门锁及房源绑定摘要。")
    public ApiResponse<SmartLockByMacResponse> getByLockMac(
            HttpServletRequest servletRequest,
            @Parameter(description = "门锁 MAC 地址", example = "AA:BB:CC:DD:EE:FF") @RequestParam String lockMac
    ) {
        return ApiResponse.success(
                "查询成功",
                adminLockService.getByLockMac(lockMac, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 查询门锁管理详情。
     */
    @GetMapping("/{smartLockId}/detail")
    @Operation(summary = "查询门锁详情", description = "查询门锁平台信息、房源绑定、电量、信号和最近同步时间。")
    public ApiResponse<SmartLockDetailResponse> getDetail(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId
    ) {
        return ApiResponse.success(
                "查询成功",
                adminLockService.getDetail(smartLockId, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 根据扫描结果更新门锁信息
     */
    @PostMapping("/{smartLockId}/ble-status")
    @Operation(summary = "更新门锁蓝牙状态", description = "使用 App 蓝牙扫描结果更新门锁电量、RSSI 和最后同步时间。")
    public ApiResponse<SmartLockDetailResponse> updateBleStatus(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId,
            @Valid @RequestBody BleStatusRequest request
    ) {
        return ApiResponse.success(
                "门锁蓝牙状态已刷新",
                adminLockService.updateBleStatus(smartLockId, request, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 查询蓝牙开锁所需数据。
     */
    @GetMapping("/{smartLockId}/unlock-data")
    @Operation(summary = "获取蓝牙开锁数据", description = "返回管理端通过蓝牙执行开锁所需的门锁初始化数据；结果包含敏感字段，应避免记录日志。")
    public ApiResponse<SmartLockUnlockDataResponse> getUnlockData(
            HttpServletRequest servletRequest,
            @Parameter(description = "智能门锁本地记录 ID", example = "lock_001") @PathVariable String smartLockId
    ) {
        return ApiResponse.success(
                "查询成功",
                adminLockService.getUnlockData(smartLockId, CurrentUser.id(servletRequest))
        );
    }
}
