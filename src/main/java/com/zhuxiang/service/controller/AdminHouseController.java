package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "管理端房源", description = "管理端创建和查询房源")
public class AdminHouseController {

    private final HouseService houseService;

    public AdminHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 新增房源。
     */
    @PostMapping
    @Operation(summary = "新增房源", description = "创建一条房源记录，包含租金、地址、租赁类型和智能锁能力等信息。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest request
    ) {
        return ApiResponse.success("房源创建成功", houseService.createHouse(request));
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
