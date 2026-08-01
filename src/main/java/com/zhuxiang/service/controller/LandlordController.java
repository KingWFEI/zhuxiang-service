package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LandlordDtos;
import com.zhuxiang.service.service.LandlordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/landlords")
@Tag(name = "房东", description = "供租客查看的房东公开资料")
public class LandlordController {

    private final LandlordService landlordService;

    public LandlordController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    @GetMapping("/{landlordUserId}")
    @Operation(
            summary = "获取房东公开资料",
            description = "按房东用户 ID 返回公开资料；联系方式受房东的公开开关控制。"
    )
    public ApiResponse<LandlordDtos.ProfileView> detail(
            @Parameter(description = "房东用户 ID", example = "user_001")
            @PathVariable String landlordUserId
    ) {
        return ApiResponse.success(landlordService.getPublicProfile(landlordUserId));
    }
}
