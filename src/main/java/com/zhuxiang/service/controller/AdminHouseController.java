package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端房源管理接口。
 */
@RestController
@RequireAuth
@RequestMapping("/admin/houses")
@Tag(name = "管理端房源", description = "管理端创建和查询房源")
@SecurityRequirement(name = "bearerAuth")
public class AdminHouseController {

    private final HouseService houseService;

    public AdminHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 新增房源。
     */
    @PostMapping
    @Operation(summary = "新增房源", description = "创建房源并关联管理端已上传的封面和房源图片。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            HttpServletRequest servletRequest,
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest request
    ) {
        return ApiResponse.success(
                "房源创建成功",
                houseService.createHouse(request, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 获取所有房源详情（含智能锁绑定信息）。
     */
    @GetMapping
    @Operation(summary = "查询全部房源", description = "返回所有房源详情以及每套房源当前绑定的智能锁信息。")
    public ApiResponse<List<AdminHouseDtos.AdminHouseView>> getAllHouses() {
        return ApiResponse.success(houseService.getAllHousesWithLockInfo());
    }
}
