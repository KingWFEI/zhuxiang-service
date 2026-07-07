package com.zhuxiang.service.immersive.controller.app;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.immersive.dto.response.AdminImmersiveTourDetailResponse;
import com.zhuxiang.service.immersive.dto.response.AvailabilityResponse;
import com.zhuxiang.service.immersive.service.ImmersiveTourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/houses")
@Tag(name = "沉浸式看房-用户端", description = "房源沉浸式看房用户端接口")
public class ImmersiveTourAppController {

    private final ImmersiveTourService tourService;

    public ImmersiveTourAppController(ImmersiveTourService tourService) { this.tourService = tourService; }

    @GetMapping("/{houseId}/immersive-tour/availability")
    @Operation(summary = "查询沉浸式看房可用状态")
    public ApiResponse<AvailabilityResponse> getAvailability(@PathVariable String houseId) {
        return ApiResponse.success(tourService.getAvailability(houseId));
    }

    @GetMapping("/{houseId}/immersive-tour")
    @Operation(summary = "获取沉浸式看房完整数据")
    public ApiResponse<AdminImmersiveTourDetailResponse> getTourData(@PathVariable String houseId) {
        return ApiResponse.success(tourService.getUserTourData(houseId));
    }
}
