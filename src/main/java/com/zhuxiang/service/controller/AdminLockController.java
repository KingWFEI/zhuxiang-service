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
public class AdminLockController {

    private final AdminLockService adminLockService;

    public AdminLockController(AdminLockService adminLockService) {
        this.adminLockService = adminLockService;
    }

    /**
     * 保存App端SDK初始化成功后的门锁数据。
     */
    @PostMapping("/local-initialized")
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
    public ApiResponse<InitializeLockResponse> bindRoom(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId,
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
    public ApiResponse<InitializeLockResponse> syncPlatform(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId
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
    public ApiResponse<InitializeLockResponse> deleteRoomBinding(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId
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
    public ApiResponse<SmartLockByMacResponse> getByLockMac(
            HttpServletRequest servletRequest,
            @RequestParam String lockMac
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
    public ApiResponse<SmartLockDetailResponse> getDetail(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId
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
    public ApiResponse<SmartLockDetailResponse> updateBleStatus(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId,
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
    public ApiResponse<SmartLockUnlockDataResponse> getUnlockData(
            HttpServletRequest servletRequest,
            @PathVariable String smartLockId
    ) {
        return ApiResponse.success(
                "查询成功",
                adminLockService.getUnlockData(smartLockId, CurrentUser.id(servletRequest))
        );
    }
}
