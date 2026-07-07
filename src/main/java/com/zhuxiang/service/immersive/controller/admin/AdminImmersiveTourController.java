package com.zhuxiang.service.immersive.controller.admin;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.immersive.dto.request.CreateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.response.AdminImmersiveTourDetailResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveTourSummaryResponse;
import com.zhuxiang.service.immersive.service.ImmersiveImageService;
import com.zhuxiang.service.immersive.service.ImmersiveTourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequireAuth
@RestController
@RequestMapping("/admin")
@Tag(name = "沉浸式看房-管理端", description = "沉浸式看房项目管理接口")
@SecurityRequirement(name = "bearerAuth")
public class AdminImmersiveTourController {

    private final ImmersiveTourService tourService;
    private final ImmersiveImageService imageService;

    public AdminImmersiveTourController(ImmersiveTourService tourService, ImmersiveImageService imageService) {
        this.tourService = tourService; this.imageService = imageService;
    }

    @PostMapping("/houses/{houseId}/immersive-tour")
    @Operation(summary = "创建沉浸式项目")
    public ApiResponse<ImmersiveTourSummaryResponse> create(@PathVariable String houseId,
            @Valid @RequestBody CreateImmersiveTourRequest request, HttpServletRequest req) {
        return ApiResponse.success(tourService.create(houseId, request, CurrentUser.id(req)));
    }

    @GetMapping("/houses/{houseId}/immersive-tour")
    @Operation(summary = "根据房源查询沉浸式项目")
    public ApiResponse<ImmersiveTourSummaryResponse> getByHouseId(@PathVariable String houseId) {
        return ApiResponse.success(tourService.getByHouseId(houseId));
    }

    @GetMapping("/immersive-tours/{tourId}")
    @Operation(summary = "查询沉浸式项目详情")
    public ApiResponse<AdminImmersiveTourDetailResponse> getDetail(@PathVariable String tourId) {
        return ApiResponse.success(tourService.getDetail(tourId));
    }

    @PutMapping("/immersive-tours/{tourId}")
    @Operation(summary = "更新沉浸式项目")
    public ApiResponse<Void> update(@PathVariable String tourId,
            @Valid @RequestBody UpdateImmersiveTourRequest request, HttpServletRequest req) {
        tourService.update(tourId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/immersive-tours/{tourId}")
    @Operation(summary = "删除沉浸式项目")
    public ApiResponse<Void> delete(@PathVariable String tourId, HttpServletRequest req) {
        tourService.delete(tourId, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PostMapping("/immersive-tours/{tourId}/publish")
    @Operation(summary = "发布沉浸式项目")
    public ApiResponse<ImmersiveTourSummaryResponse> publish(@PathVariable String tourId) {
        return ApiResponse.success("项目发布成功", tourService.publish(tourId));
    }

    @PostMapping("/immersive-tours/{tourId}/offline")
    @Operation(summary = "下线沉浸式项目")
    public ApiResponse<ImmersiveTourSummaryResponse> offline(@PathVariable String tourId) {
        return ApiResponse.success("项目下线成功", tourService.offline(tourId));
    }

    @PostMapping(value = "/immersive-tours/{tourId}/floor-plan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传户型图")
    public ApiResponse<String> uploadFloorPlan(@PathVariable String tourId,
            @RequestParam("file") MultipartFile file, HttpServletRequest req) {
        return ApiResponse.success(imageService.uploadFloorPlan(tourId, file, CurrentUser.id(req)));
    }
}
