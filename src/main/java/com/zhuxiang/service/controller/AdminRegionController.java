package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.Community;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.CommunityService;
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
    private final CommunityService communityService;
    private final UserService userService;

    public AdminRegionController(
            RegionService regionService,
            CommunityService communityService,
            UserService userService
    ) {
        this.regionService = regionService;
        this.communityService = communityService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "区域列表", description = "默认返回启用区域；管理配置可包含停用区域")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        var query = Wrappers.<Region>lambdaQuery().orderByAsc(Region::getSortOrder);
        if (!includeDisabled) query.eq(Region::getEnabled, 1);
        var regions = regionService.list(query);
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

    public record EnabledRequest(Boolean enabled) {}

    public record ImportRequest(String name, String code, String level, String parentCode,
                                Integer sortOrder, Boolean enabled) {}

    @PostMapping
    @Operation(summary = "新增城市或区域")
    public ApiResponse<Map<String, Object>> create(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody SaveRequest body) {
        requireOperator(request);
        validate(body, null);
        validateEnabledState(null, "province".equals(body.level()) ? null : body.parentId(),
                !Boolean.FALSE.equals(body.enabled()));
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
        validateEnabledState(id, "province".equals(body.level()) ? null : body.parentId(),
                !Boolean.FALSE.equals(body.enabled()));
        apply(region, body);
        region.setUpdatedAt(LocalDateTime.now());
        regionService.updateById(region);
        return ApiResponse.success(toMap(region));
    }

    @PutMapping("/{id}/enabled")
    @Operation(summary = "启用或停用城市或区域")
    public ApiResponse<Map<String, Object>> setEnabled(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String id,
            @RequestBody EnabledRequest body) {
        requireOperator(request);
        if (body == null || body.enabled() == null) {
            throw BusinessException.badRequest("请指定区域是否启用");
        }
        Region region = regionService.getById(id);
        if (region == null) throw BusinessException.notFound("区域不存在");
        validateEnabledState(id, region.getParentId(), body.enabled());
        region.setEnabled(body.enabled() ? 1 : 0);
        region.setUpdatedAt(LocalDateTime.now());
        regionService.updateById(region);
        return ApiResponse.success(toMap(region));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "永久删除城市或区域")
    @Transactional
    public ApiResponse<Boolean> delete(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String id) {
        requireOperator(request);
        Region region = regionService.getById(id);
        if (region == null) throw BusinessException.notFound("区域不存在");
        if (regionService.count(Wrappers.<Region>lambdaQuery().eq(Region::getParentId, id)) > 0) {
            throw BusinessException.conflict("该区域仍有下级区域，请先删除下级区域");
        }
        if (communityService.count(Wrappers.<Community>lambdaQuery().eq(Community::getRegionId, id)) > 0) {
            throw BusinessException.conflict("该区域已关联小区，请先调整小区所属区域");
        }
        return ApiResponse.success(regionService.removeById(id));
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
        Map<String, String> codeToLevel = new HashMap<>();
        regionService.list().forEach(item -> {
            if (item.getCode() != null && !item.getCode().isBlank()) {
                codeToId.put(item.getCode(), item.getId());
                codeToLevel.put(item.getCode(), item.getLevel());
            }
        });
        // 先处理省，再处理城市、区县和商圈，保证 parentCode 可以解析。
        List<ImportRequest> ordered = new ArrayList<>(requests);
        ordered.sort((a, b) -> Integer.compare(
                levelOrder(a == null ? null : a.level()),
                levelOrder(b == null ? null : b.level())));
        int created = 0, updated = 0;
        for (ImportRequest item : ordered) {
            if (item == null || item.name() == null || item.name().isBlank()
                    || item.level() == null || !Set.of("province", "city", "district", "business_area").contains(item.level())) {
                throw BusinessException.badRequest("导入数据包含无效区域：" + item);
            }
            String parentCode = normalizeCode(item.parentCode());
            if ("province".equals(item.level()) && parentCode != null) {
                throw BusinessException.badRequest("省级区域不能设置 parentCode：" + item.name());
            }
            String expectedParentLevel = switch (item.level()) {
                case "city" -> "province";
                case "district" -> "city";
                case "business_area" -> "district";
                default -> null;
            };
            boolean parentRequired = !"province".equals(item.level()) && !"city".equals(item.level());
            String parentId = parentCode == null ? null : codeToId.get(parentCode);
            if (parentRequired && parentId == null) {
                throw BusinessException.badRequest("无法根据 parentCode 找到上级区域：" + item.name());
            }
            if (parentCode != null && (parentId == null || !expectedParentLevel.equals(codeToLevel.get(parentCode)))) {
                throw BusinessException.badRequest("parentCode 对应的上级层级不正确：" + item.name());
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
            region.setCode(normalizeCode(item.code()));
            region.setLevel(item.level());
            region.setParentId(parentId);
            region.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
            region.setEnabled(Boolean.FALSE.equals(item.enabled()) ? 0 : 1);
            region.setUpdatedAt(LocalDateTime.now());
            regionService.saveOrUpdate(region);
            if (region.getCode() != null) {
                codeToId.put(region.getCode(), region.getId());
                codeToLevel.put(region.getCode(), region.getLevel());
            }
        }
        return ApiResponse.success(Map.of("created", created, "updated", updated, "total", requests.size()));
    }

    private int levelOrder(String level) {
        if (level == null) return 4;
        return switch (level) {
            case "province" -> 0;
            case "city" -> 1;
            case "district" -> 2;
            case "business_area" -> 3;
            default -> 4;
        };
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
        if (!Set.of("province", "city", "district", "business_area").contains(body.level()))
            throw BusinessException.badRequest("区域层级不合法");
        if ("province".equals(body.level()) && body.parentId() != null && !body.parentId().isBlank())
            throw BusinessException.badRequest("省级区域不能设置上级区域");
        if (Set.of("district", "business_area").contains(body.level())
                && (body.parentId() == null || body.parentId().isBlank()))
            throw BusinessException.badRequest("区县或商圈必须设置上级区域");
        if (regionService.count(Wrappers.<Region>lambdaQuery()
                .eq(Region::getName, body.name().trim())
                .eq(Region::getLevel, body.level())
                .ne(excludeId != null, Region::getId, excludeId)) > 0)
            throw BusinessException.conflict("同层级下已存在同名区域");
        String code = normalizeCode(body.code());
        if (code != null && regionService.count(Wrappers.<Region>lambdaQuery()
                .eq(Region::getCode, code)
                .ne(excludeId != null, Region::getId, excludeId)) > 0)
            throw BusinessException.conflict("行政编码已存在");
        if (body.parentId() != null && !body.parentId().isBlank()) {
            if (excludeId != null && excludeId.equals(body.parentId()))
                throw BusinessException.badRequest("区域不能将自身设置为上级区域");
            Region parent = regionService.getById(body.parentId());
            if (parent == null)
                throw BusinessException.badRequest("上级区域不存在");
            String expectedParentLevel = switch (body.level()) {
                case "city" -> "province";
                case "district" -> "city";
                case "business_area" -> "district";
                default -> throw BusinessException.badRequest("省级区域不能设置上级区域");
            };
            if (!expectedParentLevel.equals(parent.getLevel())) {
                String message = switch (body.level()) {
                    case "city" -> "城市的上级区域必须是省级区域";
                    case "district" -> "区县的上级区域必须是城市";
                    default -> "商圈的上级区域必须是区县";
                };
                throw BusinessException.badRequest(message);
            }
            if (!Boolean.FALSE.equals(body.enabled()) && !Integer.valueOf(1).equals(parent.getEnabled())) {
                throw BusinessException.conflict("上级区域已停用，不能启用下级区域");
            }
        }
    }

    private void validateEnabledState(String regionId, String parentId, boolean enabled) {
        if (enabled && parentId != null && !parentId.isBlank()) {
            Region parent = regionService.getById(parentId);
            if (parent == null || !Integer.valueOf(1).equals(parent.getEnabled())) {
                throw BusinessException.conflict("上级区域已停用，不能启用下级区域");
            }
        }
        if (!enabled && regionId != null
                && regionService.count(Wrappers.<Region>lambdaQuery()
                        .eq(Region::getParentId, regionId)
                        .eq(Region::getEnabled, 1)) > 0) {
            throw BusinessException.conflict("该区域仍有启用的下级区域，请先停用下级区域");
        }
    }

    private void apply(Region region, SaveRequest body) {
        region.setName(body.name().trim());
        region.setCode(normalizeCode(body.code()));
        region.setLevel(body.level());
        region.setParentId("province".equals(body.level()) ? null : body.parentId());
        region.setSortOrder(body.sortOrder() == null ? 0 : body.sortOrder());
        region.setEnabled(Boolean.FALSE.equals(body.enabled()) ? 0 : 1);
    }

    private String normalizeCode(String code) {
        if (code == null) return null;
        String normalized = code.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<String, Object> toMap(Region r) {
        return Map.of("id", r.getId(), "name", r.getName(), "code", r.getCode() == null ? "" : r.getCode(),
                "level", r.getLevel(), "parentId", r.getParentId() == null ? "" : r.getParentId(),
                "sortOrder", r.getSortOrder() == null ? 0 : r.getSortOrder(),
                "enabled", Integer.valueOf(1).equals(r.getEnabled()));
    }
}
