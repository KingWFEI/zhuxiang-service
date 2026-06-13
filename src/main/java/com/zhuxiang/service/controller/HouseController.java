package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserFavoriteHouseService;
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
    public ApiResponse<HouseDtos.FeedData> feed(
            @RequestParam String category,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
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
    public ApiResponse<PageData<HouseDtos.HouseView>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @Min(0) Integer minPrice,
            @RequestParam(required = false) @Min(0) Integer maxPrice,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) @Min(0) Integer minArea,
            @RequestParam(required = false) @Min(0) Integer maxArea,
            @RequestParam(required = false) String facilities,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
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
    public ApiResponse<HouseDtos.FilterOptions> filterOptions() {
        return ApiResponse.success(houseService.getFilterOptions());
    }

    /**
     * 获取指定房源详情。
     */
    @GetMapping("/{houseId}")
    public ApiResponse<HouseDtos.HouseDetail> detail(
            @PathVariable String houseId,
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
    public ApiResponse<HouseDtos.FavoriteResult> favorite(
            @PathVariable String houseId,
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
    public ApiResponse<HouseDtos.FavoriteResult> unfavorite(
            @PathVariable String houseId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "取消收藏成功",
                favoriteHouseService.unfavorite(CurrentUser.id(request), houseId)
        );
    }
}
