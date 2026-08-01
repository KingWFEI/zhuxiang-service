package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LandlordDtos;
import com.zhuxiang.service.service.LandlordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireAuth
@RequestMapping("/landlord/profile")
@Tag(name = "房东个人资料", description = "房东本人维护租客可查看的公开资料")
@SecurityRequirement(name = "bearerAuth")
public class LandlordProfileController {

    private final LandlordService landlordService;

    public LandlordProfileController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    @GetMapping
    @Operation(summary = "获取我的房东资料", description = "返回全部联系方式及其公开开关。")
    public ApiResponse<LandlordDtos.ProfileView> getMyProfile(HttpServletRequest request) {
        return ApiResponse.success(landlordService.getMyProfile(CurrentUser.id(request)));
    }

    @PutMapping
    @Operation(summary = "修改我的房东资料", description = "仅更新传入字段，认证、评分和出租统计不可自行修改。")
    public ApiResponse<LandlordDtos.ProfileView> updateMyProfile(
            HttpServletRequest servletRequest,
            @Valid @RequestBody LandlordDtos.UpdateLandlordProfileRequest request
    ) {
        return ApiResponse.success(
                "房东资料已更新",
                landlordService.updateMyProfile(CurrentUser.id(servletRequest), request)
        );
    }
}
