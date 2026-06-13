package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.HomeDtos;
import com.zhuxiang.service.service.HomeService;
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
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * 获取首页首次加载所需的全部数据。
     */
    @GetMapping("/data")
    public ApiResponse<HomeDtos.HomeData> getHomeData(
            @RequestParam(required = false) String cityCode,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
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
