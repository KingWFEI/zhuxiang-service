package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.service.LocationService;
import com.zhuxiang.service.service.LocationService.ReverseGeoResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 位置服务接口。
 */
@RestController
@RequestMapping("/location")
@Tag(name = "位置服务", description = "逆地理编码等位置相关接口")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 根据经纬度反查城市和区县。
     */
    @GetMapping("/reverse-geocode")
    @Operation(summary = "逆地理编码（到区级）", description = "根据经纬度坐标反查所在城市和区县名称")
    public ApiResponse<Map<String, String>> reverseGeocode(
            @Parameter(description = "纬度", example = "29.53") @RequestParam double lat,
            @Parameter(description = "经度", example = "106.60") @RequestParam double lng
    ) {
        ReverseGeoResult result = locationService.reverseGeocode(lat, lng);
        return ApiResponse.success(Map.of("city", result.city(), "district", result.district()));
    }
}
