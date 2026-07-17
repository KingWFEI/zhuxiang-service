package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.CommunityDtos;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.PoiRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Tag(name = "小区", description = "小区搜索与地图 POI 代理")
public class CommunityController {

    private final CommunityService communityService;
    private final PoiRateLimiter poiRateLimiter;

    public CommunityController(CommunityService communityService, PoiRateLimiter poiRateLimiter) {
        this.communityService = communityService;
        this.poiRateLimiter = poiRateLimiter;
    }

    @RequireAuth
    @GetMapping("/communities/search")
    @Operation(summary = "搜索内部小区", description = "按关键词搜索内部小区库，至少输入2个字")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CommunityDtos.SearchResponse> search(
            @Parameter(description = "小区名称关键词", example = "龙湖春森") @RequestParam String keyword,
            @Parameter(description = "高德行政区划代码（6位），用于限定城市", example = "500100") @RequestParam(required = false) String cityCode
    ) {
        return ApiResponse.success(communityService.searchCommunities(keyword, cityCode));
    }

    @RequireAuth
    @GetMapping("/communities/map-pois/search")
    @Operation(summary = "代理高德POI搜索", description = "无经纬度时调用高德文字搜索；有经纬度时调用高德周边搜索")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CommunityDtos.PoiSearchResponse> searchMapPois(
            HttpServletRequest request,
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "高德行政区划代码（6位）", example = "500100") @RequestParam(required = false) String cityCode,
            @Parameter(description = "经度（GCJ02），传此参数启动周边搜索") @RequestParam(required = false) BigDecimal longitude,
            @Parameter(description = "纬度（GCJ02）") @RequestParam(required = false) BigDecimal latitude,
            @Parameter(description = "搜索半径（米），默认3000") @RequestParam(defaultValue = "3000") @Min(100) @Max(50000) int radius
    ) {
        poiRateLimiter.check(CurrentUser.id(request));
        if (longitude != null && latitude != null) {
            return ApiResponse.success(communityService.searchMapPoisAround(keyword, longitude, latitude, radius));
        }
        String code = cityCode != null && !cityCode.isBlank() ? cityCode : "500000";
        return ApiResponse.success(communityService.searchMapPois(keyword, code));
    }

    @RequireAuth
    @PostMapping("/communities/from-map")
    @Operation(summary = "确认并导入小区", description = "仅传高德 POI ID，后端调高德详情接口获取可信数据并去重入库")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CommunityDtos.ImportResponse> fromMap(
            @Valid @RequestBody CommunityDtos.ImportRequest request
    ) {
        return ApiResponse.success(communityService.importFromMap(request));
    }
}
