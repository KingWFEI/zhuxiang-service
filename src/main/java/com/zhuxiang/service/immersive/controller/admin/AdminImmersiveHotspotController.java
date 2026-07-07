package com.zhuxiang.service.immersive.controller.admin;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.immersive.dto.request.CreateHotspotRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateHotspotRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveHotspotResponse;
import com.zhuxiang.service.immersive.service.ImmersiveHotspotService;
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
@Tag(name = "沉浸式看房-热点管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminImmersiveHotspotController {

    private final ImmersiveHotspotService hotspotService;

    public AdminImmersiveHotspotController(ImmersiveHotspotService hotspotService) { this.hotspotService = hotspotService; }

    @PostMapping("/immersive-images/{imageId}/hotspots")
    @Operation(summary = "创建热点")
    public ApiResponse<ImmersiveHotspotResponse> create(@PathVariable String imageId,
            @Valid @RequestBody CreateHotspotRequest request, HttpServletRequest req) {
        return ApiResponse.success(hotspotService.create(imageId, request, CurrentUser.id(req)));
    }

    @GetMapping("/immersive-images/{imageId}/hotspots")
    @Operation(summary = "查询图片热点列表")
    public ApiResponse<List<ImmersiveHotspotResponse>> listByImage(@PathVariable String imageId) {
        return ApiResponse.success(hotspotService.listByImage(imageId));
    }

    @PutMapping("/immersive-hotspots/{hotspotId}")
    @Operation(summary = "修改热点")
    public ApiResponse<Void> update(@PathVariable String hotspotId,
            @Valid @RequestBody UpdateHotspotRequest request, HttpServletRequest req) {
        hotspotService.update(hotspotId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/immersive-hotspots/{hotspotId}")
    @Operation(summary = "删除热点")
    public ApiResponse<Void> delete(@PathVariable String hotspotId, HttpServletRequest req) {
        hotspotService.delete(hotspotId, CurrentUser.id(req));
        return ApiResponse.success(null);
    }
}
