package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.entity.Community;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端小区接口 —— CRUD + 搜索。
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/communities")
@Tag(name = "管理端小区", description = "管理端小区CRUD与搜索")
@SecurityRequirement(name = "bearerAuth")
public class AdminCommunityController {

    private final CommunityService communityService;
    private final RegionService regionService;

    public AdminCommunityController(CommunityService communityService, RegionService regionService) {
        this.communityService = communityService;
        this.regionService = regionService;
    }

    // ── DTO ──

    public record CreateCommunityRequest(
            @NotBlank String name,
            String address,
            String regionId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record UpdateCommunityRequest(
            String name,
            String address,
            String regionId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    // ── CRUD ──

    /**
     * 新增小区。
     */
    @PostMapping
    @Operation(summary = "新增小区")
    public ApiResponse<Map<String, Object>> create(@RequestBody @Valid CreateCommunityRequest body) {
        Community c = new Community();
        c.setId(UUID.randomUUID().toString());
        c.setName(body.name());
        c.setAddress(body.address());
        c.setRegionId(body.regionId());
        c.setLatitude(body.latitude());
        c.setLongitude(body.longitude());
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        communityService.save(c);
        return ApiResponse.success(toMap(c));
    }

    /**
     * 更新小区信息。
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑小区")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody UpdateCommunityRequest body) {
        Community c = communityService.getById(id);
        if (c == null) throw BusinessException.notFound("小区不存在");
        if (body.name() != null) c.setName(body.name());
        if (body.address() != null) c.setAddress(body.address());
        if (body.regionId() != null) c.setRegionId(body.regionId());
        if (body.latitude() != null) c.setLatitude(body.latitude());
        if (body.longitude() != null) c.setLongitude(body.longitude());
        c.setUpdatedAt(LocalDateTime.now());
        communityService.updateById(c);
        return ApiResponse.success(toMap(c));
    }

    /**
     * 删除小区。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除小区")
    public ApiResponse<Void> delete(@PathVariable String id) {
        communityService.removeById(id);
        return ApiResponse.success(null);
    }

    /**
     * 获取单个小区详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "小区详情")
    public ApiResponse<Map<String, Object>> getById(@PathVariable String id) {
        Community c = communityService.getById(id);
        if (c == null) throw BusinessException.notFound("小区不存在");
        return ApiResponse.success(toMap(c));
    }

    /**
     * 模糊搜索 / 分页列表。
     */
    @GetMapping
    @Operation(summary = "搜索/列表小区")
    public ApiResponse<List<Map<String, Object>>> search(
            @Parameter(description = "关键词") @RequestParam(defaultValue = "") String keyword
    ) {
        var q = Wrappers.<Community>lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            q.like(Community::getName, keyword.trim());
        }
        q.last("LIMIT 200");
        return ApiResponse.success(communityService.list(q).stream().map(this::toMap).toList());
    }

    /**
     * 合并重复小区。
     */
    @PostMapping("/merge")
    @Operation(summary = "合并小区", description = "将 sourceId 的所有房源迁移到 targetId，source 标记为 merged")
    public ApiResponse<Void> merge(@RequestBody MergeRequest body) {
        communityService.mergeCommunities(body.sourceId(), body.targetId());
        return ApiResponse.success("合并完成", null);
    }

    public record MergeRequest(
            @NotBlank String sourceId,
            @NotBlank String targetId
    ) {}

    private Map<String, Object> toMap(Community c) {
        String regionName = "";
        if (c.getRegionId() != null) {
            Region r = regionService.getById(c.getRegionId());
            regionName = r != null ? r.getName() : "";
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", c.getId() != null ? c.getId() : "");
        map.put("name", c.getName() != null ? c.getName() : "");
        map.put("address", c.getAddress() != null ? c.getAddress() : "");
        map.put("regionId", c.getRegionId() != null ? c.getRegionId() : "");
        map.put("regionName", regionName);
        map.put("province", c.getProvince() != null ? c.getProvince() : "");
        map.put("city", c.getCity() != null ? c.getCity() : "");
        map.put("district", c.getDistrict() != null ? c.getDistrict() : "");
        map.put("latitude", c.getLatitude() != null ? c.getLatitude() : BigDecimal.ZERO);
        map.put("longitude", c.getLongitude() != null ? c.getLongitude() : BigDecimal.ZERO);
        map.put("mapProvider", c.getMapProvider() != null ? c.getMapProvider() : "");
        map.put("externalPoiId", c.getExternalPoiId() != null ? c.getExternalPoiId() : "");
        map.put("status", c.getStatus() != null ? c.getStatus() : "approved");
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        return map;
    }
}
