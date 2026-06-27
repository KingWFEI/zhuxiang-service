package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserFavoriteHouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房源查询与收藏接口。
 */
@Validated
@RestController
@RequestMapping("/houses")
@Tag(name = "房源", description = "房源流、条件搜索、详情和收藏")
public class HouseController {

    private final HouseService houseService;
    private final UserFavoriteHouseService favoriteHouseService;

    public HouseController(
            HouseService houseService,
            UserFavoriteHouseService favoriteHouseService
    ) {
        this.houseService = houseService;
        this.favoriteHouseService = favoriteHouseService;
    }

    /**
     * 分页获取首页房源流。
     */
    @GetMapping("/feed")
    @Operation(summary = "获取房源流", description = "按首页分类分页获取房源和插入广告；携带令牌时返回收藏状态。")
    public ApiResponse<HouseDtos.FeedData> feed(
            @Parameter(description = "首页分类键，以首页 tabs 返回值为准", example = "recommend") @RequestParam String category,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(houseService.getFeed(
                category, page, pageSize, CurrentUser.optionalId(request)
        ));
    }

    /**
     * 按条件分页搜索房源。
     */
    @GetMapping
    @Operation(summary = "搜索房源", description = "按关键字、分类、区域、价格、户型、面积和设施组合筛选房源，并支持排序和分页。")
    public ApiResponse<PageData<HouseDtos.HouseView>> search(
            @Parameter(description = "标题、位置或小区关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "房源分类键") @RequestParam(required = false) String category,
            @Parameter(description = "区域名称") @RequestParam(required = false) String region,
            @Parameter(description = "最低月租金，单位元", example = "1000") @RequestParam(required = false) @Min(0) Integer minPrice,
            @Parameter(description = "最高月租金，单位元", example = "5000") @RequestParam(required = false) @Min(0) Integer maxPrice,
            @Parameter(description = "户型") @RequestParam(required = false) String roomType,
            @Parameter(description = "最小面积，单位平方米", example = "20") @RequestParam(required = false) @Min(0) Integer minArea,
            @Parameter(description = "最大面积，单位平方米", example = "120") @RequestParam(required = false) @Min(0) Integer maxArea,
            @Parameter(description = "设施值，多个值使用英文逗号分隔", example = "wifi,air_conditioner") @RequestParam(required = false) String facilities,
            @Parameter(description = "排序值，以筛选选项接口返回值为准", example = "default") @RequestParam(defaultValue = "default") String sort,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(houseService.searchHouses(
                keyword, category, region, minPrice, maxPrice, roomType,
                minArea, maxArea, facilities, sort, page, pageSize,
                CurrentUser.optionalId(request)
        ));
    }

    /**
     * 获取房源筛选选项。
     */
    @GetMapping("/filter-options")
    @Operation(summary = "获取筛选选项", description = "返回区域、价格区间、户型、设施和排序选项。")
    public ApiResponse<HouseDtos.FilterOptions> filterOptions() {
        return ApiResponse.success(houseService.getFilterOptions());
    }

    /**
     * 获取指定房源详情。
     */
    @GetMapping("/{houseId}")
    @Operation(summary = "获取房源详情", description = "返回房源图片、价格、地址、配套、房东资料和出租状态；携带令牌时返回收藏状态。")
    public ApiResponse<HouseDtos.HouseDetail> detail(
            @Parameter(description = "房源 ID", example = "house_001") @PathVariable String houseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                houseService.getHouseDetail(houseId, CurrentUser.optionalId(request))
        );
    }

    /**
     * 收藏指定房源。
     */
    @RequireAuth
    @PostMapping("/{houseId}/favorite")
    @Operation(summary = "收藏房源", description = "将指定房源加入当前用户收藏。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<HouseDtos.FavoriteResult> favorite(
            @Parameter(description = "房源 ID", example = "house_001") @PathVariable String houseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "收藏成功",
                favoriteHouseService.favorite(CurrentUser.id(request), houseId)
        );
    }

    /**
     * 取消收藏指定房源。
     */
    @RequireAuth
    @DeleteMapping("/{houseId}/favorite")
    @Operation(summary = "取消收藏房源", description = "从当前用户收藏中移除指定房源。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<HouseDtos.FavoriteResult> unfavorite(
            @Parameter(description = "房源 ID", example = "house_001") @PathVariable String houseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "取消收藏成功",
                favoriteHouseService.unfavorite(CurrentUser.id(request), houseId)
        );
    }
}
