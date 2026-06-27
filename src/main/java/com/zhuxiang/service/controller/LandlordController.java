package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.service.LandlordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房东资料查询接口。
 */
@RestController
@RequestMapping("/landlords")
@Tag(name = "房东", description = "公开房东资料查询")
public class LandlordController {

    private final LandlordService landlordService;

    public LandlordController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    /**
     * 获取指定房东资料。
     */
    @GetMapping("/{landlordId}")
    @Operation(summary = "获取房东资料", description = "返回指定房东的公开资料、认证状态、评分和出租统计。")
    public ApiResponse<HouseDtos.LandlordView> detail(
            @Parameter(description = "房东用户 ID", example = "user_001") @PathVariable String landlordId
    ) {
        return ApiResponse.success(landlordService.getLandlordDetail(landlordId));
    }
}
