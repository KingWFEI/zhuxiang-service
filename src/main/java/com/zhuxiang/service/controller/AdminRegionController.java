package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;

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
    private final UserService userService;

    public AdminRegionController(RegionService regionService, UserService userService) {
        this.regionService = regionService;
        this.userService = userService;
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
                "parentId", r.getParentId() != null ? r.getParentId() : "",
                "sortOrder", r.getSortOrder() == null ? 0 : r.getSortOrder(),
                "enabled", Integer.valueOf(1).equals(r.getEnabled())
        )).toList());
    }

    public record SaveRequest(String name, String code, String level, String parentId,
                              Integer sortOrder, Boolean enabled) {}

    public record ImportRequest(String name, String code, String level, String parentCode,
                                Integer sortOrder, Boolean enabled) {}

    @PostMapping
    @Operation(summary = "新增城市或区域")
    public ApiResponse<Map<String, Object>> create(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody SaveRequest body) {
        requireOperator(request);
        validate(body, null);
        Region region = new Region();
        region.setId(UUID.randomUUID().toString());
        apply(region, body);
        LocalDateTime now = LocalDateTime.now();
        region.setCreatedAt(now);
        region.setUpdatedAt(now);
        regionService.save(region);
        return ApiResponse.success(toMap(region));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑城市或区域")
    public ApiResponse<Map<String, Object>> update(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String id,
            @RequestBody SaveRequest body) {
        requireOperator(request);
        Region region = regionService.getById(id);
        if (region == null) throw BusinessException.notFound("区域不存在");
        validate(body, id);
        apply(region, body);
        region.setUpdatedAt(LocalDateTime.now());
        regionService.updateById(region);
        return ApiResponse.success(toMap(region));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "停用城市或区域")
    public ApiResponse<Boolean> delete(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String id) {
        requireOperator(request);
        Region region = regionService.getById(id);
        if (region == null) throw BusinessException.notFound("区域不存在");
        if (regionService.count(Wrappers.<Region>lambdaQuery().eq(Region::getParentId, id).eq(Region::getEnabled, 1)) > 0) {
            throw BusinessException.conflict("该区域仍有启用的下级区域，请先停用下级区域");
        }
        region.setEnabled(0);
        region.setUpdatedAt(LocalDateTime.now());
        regionService.updateById(region);
        return ApiResponse.success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "JSON批量导入区域")
    @Transactional
    public ApiResponse<Map<String, Object>> importRegions(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody List<ImportRequest> requests) {
        requireOperator(request);
        if (requests == null || requests.isEmpty()) {
            throw BusinessException.badRequest("导入数据不能为空");
        }
        Map<String, String> codeToId = new HashMap<>();
        regionService.list().forEach(item -> {
            if (item.getCode() != null && !item.getCode().isBlank()) codeToId.put(item.getCode(), item.getId());
        });
        // 先处理城市，再处理区县/商圈，保证 parentCode 可以解析。
        List<ImportRequest> ordered = new ArrayList<>(requests);
        ordered.sort((a, b) -> Integer.compare(levelOrder(a.level()), levelOrder(b.level())));
        int created = 0, updated = 0;
        for (ImportRequest item : ordered) {
            if (item == null || item.name() == null || item.name().isBlank()
                    || item.level() == null || !Set.of("city", "district", "business_area").contains(item.level())) {
                throw BusinessException.badRequest("导入数据包含无效区域：" + item);
            }
            String parentId = "city".equals(item.level()) ? null : codeToId.get(item.parentCode());
            if (!"city".equals(item.level()) && (parentId == null || parentId.isBlank())) {
                throw BusinessException.badRequest("无法根据 parentCode 找到上级区域：" + item.name());
            }
            Region region = findByCodeOrName(item.code(), item.name(), item.level());
            if (region == null) {
                region = new Region();
                region.setId(UUID.randomUUID().toString());
                region.setCreatedAt(LocalDateTime.now());
                created++;
            } else {
                updated++;
            }
            region.setName(item.name().trim());
            region.setCode(item.code() == null ? "" : item.code().trim());
            region.setLevel(item.level());
            region.setParentId(parentId);
            region.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
            region.setEnabled(Boolean.FALSE.equals(item.enabled()) ? 0 : 1);
            region.setUpdatedAt(LocalDateTime.now());
            regionService.saveOrUpdate(region);
            if (!region.getCode().isBlank()) codeToId.put(region.getCode(), region.getId());
        }
        return ApiResponse.success(Map.of("created", created, "updated", updated, "total", requests.size()));
    }

    private int levelOrder(String level) {
        return "city".equals(level) ? 0 : "district".equals(level) ? 1 : 2;
    }

    private Region findByCodeOrName(String code, String name, String level) {
        if (code != null && !code.isBlank()) {
            Region item = regionService.getOne(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getCode, code.trim()).eq(Region::getLevel, level).last("LIMIT 1"), false);
            if (item != null) return item;
        }
        return regionService.getOne(Wrappers.<Region>lambdaQuery()
                .eq(Region::getName, name.trim()).eq(Region::getLevel, level).last("LIMIT 1"), false);
    }

    private void requireOperator(jakarta.servlet.http.HttpServletRequest request) {
        var user = userService.requireActiveUser(CurrentUser.id(request));
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        if (!Set.of("ADMIN", "HOUSEKEEPER").contains(role)) {
            throw BusinessException.forbidden("当前账号无权配置行政区域");
        }
    }

    private void validate(SaveRequest body, String excludeId) {
        if (body == null || body.name() == null || body.name().isBlank())
            throw BusinessException.badRequest("区域名称不能为空");
        if (!Set.of("city", "district", "business_area").contains(body.level()))
            throw BusinessException.badRequest("区域层级不合法");
        if ("city".equals(body.level()) && body.parentId() != null && !body.parentId().isBlank())
            throw BusinessException.badRequest("城市不能设置上级区域");
        if (!"city".equals(body.level()) && (body.parentId() == null || body.parentId().isBlank()))
            throw BusinessException.badRequest("区县或商圈必须设置上级区域");
        if (regionService.count(Wrappers.<Region>lambdaQuery()
                .eq(Region::getName, body.name().trim())
                .eq(Region::getLevel, body.level())
                .ne(excludeId != null, Region::getId, excludeId)) > 0)
            throw BusinessException.conflict("同层级下已存在同名区域");
        if (body.parentId() != null && !body.parentId().isBlank() && regionService.getById(body.parentId()) == null)
            throw BusinessException.badRequest("上级区域不存在");
    }

    private void apply(Region region, SaveRequest body) {
        region.setName(body.name().trim());
        region.setCode(body.code() == null ? "" : body.code().trim());
        region.setLevel(body.level());
        region.setParentId("city".equals(body.level()) ? null : body.parentId());
        region.setSortOrder(body.sortOrder() == null ? 0 : body.sortOrder());
        region.setEnabled(Boolean.FALSE.equals(body.enabled()) ? 0 : 1);
    }

    private Map<String, Object> toMap(Region r) {
        return Map.of("id", r.getId(), "name", r.getName(), "code", r.getCode() == null ? "" : r.getCode(),
                "level", r.getLevel(), "parentId", r.getParentId() == null ? "" : r.getParentId(),
                "sortOrder", r.getSortOrder() == null ? 0 : r.getSortOrder(),
                "enabled", Integer.valueOf(1).equals(r.getEnabled()));
    }
}
