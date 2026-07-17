package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseAttributeDtos;
import com.zhuxiang.service.service.AdminHouseAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 房东端设施和标签字典查询接口。
 */
@RestController
@RequireAuth
@Tag(name = "房东设施与标签", description = "房东查询设施和标签字典")
@SecurityRequirement(name = "bearerAuth")
public class LandlordAttributeController {

    private final AdminHouseAttributeService attributeService;

    public LandlordAttributeController(AdminHouseAttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping("/landlord/house-facilities")
    @Operation(summary = "设施字典", description = "返回全部设施字典项")
    public ApiResponse<List<AdminHouseAttributeDtos.FacilityItem>> getFacilityDictionary(
            HttpServletRequest request
    ) {
        return ApiResponse.success(attributeService.getFacilityDictionary(CurrentUser.id(request)));
    }

    @GetMapping("/landlord/house-tags")
    @Operation(summary = "标签字典", description = "返回全部房源标签字典项")
    public ApiResponse<List<AdminHouseAttributeDtos.TagItem>> getTagDictionary(
            HttpServletRequest request
    ) {
        return ApiResponse.success(attributeService.getTagDictionary(CurrentUser.id(request)));
    }
}
