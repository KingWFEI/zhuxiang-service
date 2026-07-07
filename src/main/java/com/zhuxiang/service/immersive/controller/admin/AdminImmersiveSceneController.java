package com.zhuxiang.service.immersive.controller.admin;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.immersive.dto.request.*;
import com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse;
import com.zhuxiang.service.immersive.service.ImmersiveSceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequireAuth
@RestController
@RequestMapping("/admin")
@Tag(name = "沉浸式看房-场景管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminImmersiveSceneController {

    private final ImmersiveSceneService sceneService;

    public AdminImmersiveSceneController(ImmersiveSceneService sceneService) { this.sceneService = sceneService; }

    @PostMapping("/immersive-tours/{tourId}/scenes")
    @Operation(summary = "创建房间")
    public ApiResponse<ImmersiveSceneResponse> create(@PathVariable String tourId,
            @Valid @RequestBody CreateImmersiveSceneRequest request, HttpServletRequest req) {
        return ApiResponse.success(sceneService.create(tourId, request, CurrentUser.id(req)));
    }

    @GetMapping("/immersive-tours/{tourId}/scenes")
    @Operation(summary = "查询房间列表")
    public ApiResponse<List<ImmersiveSceneResponse>> list(@PathVariable String tourId) {
        return ApiResponse.success(sceneService.listByTourId(tourId));
    }

    @PutMapping("/immersive-scenes/{sceneId}")
    @Operation(summary = "修改房间")
    public ApiResponse<Void> update(@PathVariable String sceneId,
            @Valid @RequestBody UpdateImmersiveSceneRequest request, HttpServletRequest req) {
        sceneService.update(sceneId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/immersive-scenes/{sceneId}")
    @Operation(summary = "删除房间")
    public ApiResponse<Void> delete(@PathVariable String sceneId,
            @RequestParam(value = "cascade", defaultValue = "false") boolean cascade, HttpServletRequest req) {
        sceneService.delete(sceneId, cascade, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PutMapping("/immersive-tours/{tourId}/scenes/sort")
    @Operation(summary = "房间排序")
    public ApiResponse<Void> sort(@PathVariable String tourId,
            @Valid @RequestBody SortImmersiveScenesRequest request, HttpServletRequest req) {
        sceneService.sort(tourId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PutMapping("/immersive-tours/{tourId}/entry-scene")
    @Operation(summary = "设置入口房间")
    public ApiResponse<Void> setEntryScene(@PathVariable String tourId,
            @Valid @RequestBody SetEntrySceneRequest request, HttpServletRequest req) {
        sceneService.setEntryScene(tourId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PutMapping("/immersive-scenes/{sceneId}/floor-plan-position")
    @Operation(summary = "设置户型图位置")
    public ApiResponse<Void> setFloorPlanPosition(@PathVariable String sceneId,
            @Valid @RequestBody SetSceneFloorPlanPositionRequest request, HttpServletRequest req) {
        sceneService.setFloorPlanPosition(sceneId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }
}
