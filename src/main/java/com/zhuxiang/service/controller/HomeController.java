package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.HomeDtos;
import com.zhuxiang.service.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页聚合数据接口。
 */
@Validated
@RestController
@RequestMapping("/home")
@Tag(name = "首页", description = "移动端首页聚合数据")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * 获取首页首次加载所需的全部数据。
     */
    @GetMapping("/data")
    @Operation(summary = "获取首页聚合数据", description = "一次返回城市标题、未读数、服务入口、房源标签页、房源流和广告；携带令牌时会返回用户相关状态。")
    public ApiResponse<HomeDtos.HomeData> getHomeData(
            @Parameter(description = "城市行政区划代码", example = "510100") @RequestParam(required = false) String cityCode,
            @Parameter(description = "区域名称", example = "高新区") @RequestParam(required = false) String region,
            @Parameter(description = "当前位置纬度，需与 longitude 同时传入", example = "30.5728") @RequestParam(required = false) Double latitude,
            @Parameter(description = "当前位置经度，需与 latitude 同时传入", example = "104.0668") @RequestParam(required = false) Double longitude,
            @Parameter(description = "每个房源分组返回数量，范围 1-100", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(homeService.getHomeData(
                cityCode,
                region,
                latitude,
                longitude,
                pageSize,
                CurrentUser.optionalId(request)
        ));
    }
}
