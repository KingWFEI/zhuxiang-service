package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.service.HouseService;
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
@RequestMapping("/admin/houses")
public class AdminHouseController {

    private final HouseService houseService;

    public AdminHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 新增房源。
     */
    @PostMapping
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest request
    ) {
        return ApiResponse.success("房源创建成功", houseService.createHouse(request));
    }

    /**
     * 获取所有房源详情（含智能锁绑定信息）。
     */
    @GetMapping
    public ApiResponse<List<AdminHouseDtos.AdminHouseView>> getAllHouses() {
        return ApiResponse.success(houseService.getAllHousesWithLockInfo());
    }
}
