package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端区域字典接口 —— 供小区/房源等下拉选择。
 */
@RequireAuth
@RestController
@RequestMapping("/admin/regions")
@Tag(name = "管理端区域", description = "区域字典列表")
@SecurityRequirement(name = "bearerAuth")
public class AdminRegionController {

    private final RegionService regionService;

    public AdminRegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    @Operation(summary = "区域列表", description = "返回按排序的已启用区域")
    public ApiResponse<List<Map<String, Object>>> list() {
        var regions = regionService.list(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getEnabled, 1)
                        .orderByAsc(Region::getSortOrder));
        return ApiResponse.success(regions.stream().map(r -> Map.<String, Object>of(
                "id", r.getId() != null ? r.getId() : "",
                "name", r.getName() != null ? r.getName() : "",
                "code", r.getCode() != null ? r.getCode() : "",
                "level", r.getLevel() != null ? r.getLevel() : "",
                "parentId", r.getParentId() != null ? r.getParentId() : ""
        )).toList());
    }
}
