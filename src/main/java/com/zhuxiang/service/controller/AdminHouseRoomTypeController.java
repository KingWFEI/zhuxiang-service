package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.HouseRoomTypeDtos;
import com.zhuxiang.service.service.AdminHouseRoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireAuth
@RequestMapping("/admin/house-room-types")
@Tag(name = "管理端户型配置", description = "统一维护房源发布和筛选使用的户型字典")
@SecurityRequirement(name = "bearerAuth")
public class AdminHouseRoomTypeController {
    private final AdminHouseRoomTypeService service;

    public AdminHouseRoomTypeController(AdminHouseRoomTypeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询全部户型")
    public ApiResponse<List<HouseRoomTypeDtos.Item>> list(HttpServletRequest request) {
        return ApiResponse.success(service.list(CurrentUser.id(request)));
    }

    @PostMapping
    @Operation(summary = "新增户型")
    public ApiResponse<HouseRoomTypeDtos.Item> create(
            HttpServletRequest request,
            @Valid @RequestBody HouseRoomTypeDtos.CreateRequest body
    ) {
        return ApiResponse.success("户型创建成功", service.create(body, CurrentUser.id(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑户型")
    public ApiResponse<HouseRoomTypeDtos.Item> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody HouseRoomTypeDtos.UpdateRequest body
    ) {
        return ApiResponse.success("户型更新成功", service.update(id, body, CurrentUser.id(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除未使用户型")
    public ApiResponse<Boolean> delete(HttpServletRequest request, @PathVariable String id) {
        service.delete(id, CurrentUser.id(request));
        return ApiResponse.success("户型删除成功", true);
    }
}
