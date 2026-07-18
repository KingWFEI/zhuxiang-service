package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房东端房源管理接口。
 */
@RestController
@RequireAuth
@RequestMapping("/landlord/houses")
@Tag(name = "房东房源", description = "房东对自己的房源进行增删改查和上下架")
@SecurityRequirement(name = "bearerAuth")
public class LandlordHouseController {

    private final HouseService houseService;

    public LandlordHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @GetMapping
    @Operation(summary = "房源列表", description = "返回当前房东的房源，支持 ?status=draft,available,offline 筛选")
    public ApiResponse<List<AdminHouseDtos.AdminHouseView>> listHouses(
            HttpServletRequest request,
            @Parameter(description = "房源状态") @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(houseService.getLandlordHouses(CurrentUser.id(request), status));
    }

    @GetMapping("/{houseId}")
    @Operation(summary = "房源详情", description = "返回房源详情，校验归属权")
    public ApiResponse<AdminHouseDtos.AdminHouseView> getHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                houseService.getLandlordHouseById(houseId, CurrentUser.id(request))
        );
    }

    @PostMapping
    @Operation(summary = "发布房源", description = "创建新房源，landlordId 从 Token 自动获取。创建后状态为草稿，需调用上架接口发布。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            HttpServletRequest request,
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest body
    ) {
        return ApiResponse.success(
                "房源创建成功",
                houseService.createLandlordHouse(body, CurrentUser.id(request))
        );
    }

    @PutMapping("/{houseId}")
    @Operation(summary = "修改房源", description = "仅更新传入的字段，校验房源归属当前房东。设施和标签采用完整替换语义。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> updateHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId,
            @Valid @RequestBody AdminHouseDtos.UpdateHouseRequest body
    ) {
        return ApiResponse.success(
                "房源修改成功",
                houseService.updateLandlordHouse(houseId, body, CurrentUser.id(request))
        );
    }

    @PutMapping("/{houseId}/publish")
    @Operation(summary = "上架房源", description = "将草稿状态的房源改为可租，对外公开可见。校验归属权。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> publishHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                "房源上架成功",
                houseService.publishLandlordHouse(houseId, CurrentUser.id(request))
        );
    }

    @PutMapping("/{houseId}/offline")
    @Operation(summary = "下架房源", description = "将可租状态的房源下架，不再对外展示。校验归属权。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> offlineHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                "房源下架成功",
                houseService.offlineLandlordHouse(houseId, CurrentUser.id(request))
        );
    }
}
