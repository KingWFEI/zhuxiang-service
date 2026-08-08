package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminAdvertisementDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.Advertisement;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequireAuth
@RestController
@RequestMapping("/admin/advertisements")
@Tag(name = "管理端广告管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminAdvertisementController {

    private static final Set<String> OPERATOR_ROLES = Set.of("ADMIN", "HOUSEKEEPER");
    private static final Set<String> POSITIONS = Set.of("home_banner", "home_feed");
    private static final Set<String> TARGET_TYPES = Set.of("none", "house", "url");

    private final AdvertisementService advertisementService;
    private final UserService userService;
    private final HouseService houseService;
    private final FileRecordService fileRecordService;

    public AdminAdvertisementController(
            AdvertisementService advertisementService,
            UserService userService,
            HouseService houseService,
            FileRecordService fileRecordService
    ) {
        this.advertisementService = advertisementService;
        this.userService = userService;
        this.houseService = houseService;
        this.fileRecordService = fileRecordService;
    }

    @GetMapping
    public ApiResponse<PageData<AdminAdvertisementDtos.AdvertisementView>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        requireOperator(request);
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Advertisement> query = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(Advertisement::getTitle, value)
                    .or().like(Advertisement::getDescription, value));
        }
        if (StringUtils.hasText(position)) {
            query.eq(Advertisement::getPosition, normalize(position));
        }
        applyStatusFilter(query, status, now);
        query.orderByAsc(Advertisement::getSortOrder)
                .orderByDesc(Advertisement::getUpdatedAt);
        Page<Advertisement> result = advertisementService.page(
                new Page<>(safePage, safePageSize), query
        );
        return ApiResponse.success(PageData.of(
                result.getRecords().stream().map(this::toView).toList(),
                safePage, safePageSize, result.getTotal()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminAdvertisementDtos.AdvertisementView> detail(
            HttpServletRequest request, @PathVariable String id
    ) {
        requireOperator(request);
        return ApiResponse.success(toView(requireAdvertisement(id)));
    }

    @GetMapping("/house-options/search")
    public ApiResponse<List<AdminAdvertisementDtos.HouseOption>> searchHouseOptions(
            HttpServletRequest request,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") long limit
    ) {
        requireOperator(request);
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.success(List.of());
        }
        long safeLimit = Math.min(20, Math.max(1, limit));
        PageData<HouseDtos.HouseView> result = houseService.searchHouses(
                keyword.trim(), null, null, null, null, null, null, null,
                null, null, null, null, null, null, "default", 1, safeLimit, null
        );
        return ApiResponse.success(result.items().stream()
                .map(item -> new AdminAdvertisementDtos.HouseOption(
                        item.id(), item.title(), item.coverImage(), item.community(),
                        item.location(), item.price()
                ))
                .toList());
    }

    @PostMapping
    @Transactional
    public ApiResponse<AdminAdvertisementDtos.AdvertisementView> create(
            HttpServletRequest request,
            @RequestBody AdminAdvertisementDtos.SaveRequest body
    ) {
        String operatorId = requireOperator(request);
        validate(body, null, operatorId);
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID().toString());
        apply(advertisement, body);
        LocalDateTime now = LocalDateTime.now();
        advertisement.setCreatedAt(now);
        advertisement.setUpdatedAt(now);
        advertisementService.save(advertisement);
        return ApiResponse.success("广告创建成功", toView(advertisement));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<AdminAdvertisementDtos.AdvertisementView> update(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody AdminAdvertisementDtos.SaveRequest body
    ) {
        String operatorId = requireOperator(request);
        Advertisement advertisement = requireAdvertisement(id);
        validate(body, advertisement, operatorId);
        apply(advertisement, body);
        advertisement.setUpdatedAt(LocalDateTime.now());
        advertisementService.updateById(advertisement);
        return ApiResponse.success("广告更新成功", toView(advertisement));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<AdminAdvertisementDtos.AdvertisementView> setEnabled(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody AdminAdvertisementDtos.EnableRequest body
    ) {
        requireOperator(request);
        if (body == null || body.enabled() == null) {
            throw BusinessException.badRequest("启用状态不能为空");
        }
        Advertisement advertisement = requireAdvertisement(id);
        advertisement.setEnabled(body.enabled() ? 1 : 0);
        advertisement.setUpdatedAt(LocalDateTime.now());
        advertisementService.updateById(advertisement);
        return ApiResponse.success(body.enabled() ? "广告已启用" : "广告已停用", toView(advertisement));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(HttpServletRequest request, @PathVariable String id) {
        requireOperator(request);
        requireAdvertisement(id);
        advertisementService.removeById(id);
        return ApiResponse.success("广告已删除", true);
    }

    private String requireOperator(HttpServletRequest request) {
        String userId = CurrentUser.id(request);
        var operator = userService.requireActiveUser(userId);
        String role = operator.getRole() == null ? "" : operator.getRole().toUpperCase(Locale.ROOT);
        if (!OPERATOR_ROLES.contains(role)) {
            throw BusinessException.forbidden("当前账号无权管理广告");
        }
        return userId;
    }

    private Advertisement requireAdvertisement(String id) {
        Advertisement advertisement = advertisementService.getById(id);
        if (advertisement == null) {
            throw BusinessException.notFound("广告不存在");
        }
        return advertisement;
    }

    private void validate(
            AdminAdvertisementDtos.SaveRequest body,
            Advertisement existing,
            String operatorId
    ) {
        if (body == null || !StringUtils.hasText(body.title())) {
            throw BusinessException.badRequest("广告标题不能为空");
        }
        if (body.title().trim().length() > 100) {
            throw BusinessException.badRequest("广告标题不能超过100个字符");
        }
        if (StringUtils.hasText(body.description()) && body.description().trim().length() > 500) {
            throw BusinessException.badRequest("广告描述不能超过500个字符");
        }
        if (!StringUtils.hasText(body.imageUrl()) || body.imageUrl().trim().length() > 500) {
            throw BusinessException.badRequest("请上传有效的广告图片");
        }
        String position = normalize(body.position());
        if (!POSITIONS.contains(position)) {
            throw BusinessException.badRequest("不支持的广告位置");
        }
        String targetType = normalize(body.targetType());
        if (!TARGET_TYPES.contains(targetType)) {
            throw BusinessException.badRequest("不支持的跳转类型");
        }
        if (!"none".equals(targetType) && !StringUtils.hasText(body.targetValue())) {
            throw BusinessException.badRequest("该跳转类型必须填写跳转目标");
        }
        if (StringUtils.hasText(body.targetValue()) && body.targetValue().trim().length() > 500) {
            throw BusinessException.badRequest("跳转目标不能超过500个字符");
        }
        if ("house".equals(targetType) && houseService.getById(body.targetValue().trim()) == null) {
            throw BusinessException.badRequest("跳转房源不存在");
        }
        if ("url".equals(targetType)) {
            validateExternalUrl(body.targetValue());
        }
        if (body.startTime() != null && body.endTime() != null
                && !body.endTime().isAfter(body.startTime())) {
            throw BusinessException.badRequest("结束时间必须晚于开始时间");
        }
        if (body.sortOrder() != null && (body.sortOrder() < 0 || body.sortOrder() > 9999)) {
            throw BusinessException.badRequest("排序值必须在0到9999之间");
        }
        boolean imageChanged = existing == null || !body.imageUrl().trim().equals(existing.getImageUrl());
        if (imageChanged) {
            if (!StringUtils.hasText(body.imageFileId())) {
                throw BusinessException.badRequest("广告图片缺少上传凭证，请重新上传");
            }
            fileRecordService.validateFileOwnership(
                    operatorId, body.imageFileId().trim(), body.imageUrl().trim(), "advertisement_image"
            );
        }
    }

    private void validateExternalUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw BusinessException.badRequest("外部链接必须是有效的 HTTP 或 HTTPS 地址");
        }
    }

    private void apply(Advertisement advertisement, AdminAdvertisementDtos.SaveRequest body) {
        advertisement.setTitle(body.title().trim());
        advertisement.setDescription(trimToNull(body.description()));
        advertisement.setImageUrl(body.imageUrl().trim());
        advertisement.setTargetType(normalize(body.targetType()));
        advertisement.setTargetValue("none".equals(normalize(body.targetType()))
                ? null : trimToNull(body.targetValue()));
        advertisement.setPosition(normalize(body.position()));
        advertisement.setEnabled(Boolean.FALSE.equals(body.enabled()) ? 0 : 1);
        advertisement.setSortOrder(body.sortOrder() == null ? 0 : body.sortOrder());
        advertisement.setStartTime(body.startTime());
        advertisement.setEndTime(body.endTime());
    }

    private void applyStatusFilter(
            LambdaQueryWrapper<Advertisement> query, String status, LocalDateTime now
    ) {
        if (!StringUtils.hasText(status)) return;
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "DISABLED" -> query.eq(Advertisement::getEnabled, 0);
            case "SCHEDULED" -> query.eq(Advertisement::getEnabled, 1)
                    .gt(Advertisement::getStartTime, now);
            case "EXPIRED" -> query.eq(Advertisement::getEnabled, 1)
                    .isNotNull(Advertisement::getEndTime)
                    .lt(Advertisement::getEndTime, now);
            case "ACTIVE" -> query.eq(Advertisement::getEnabled, 1)
                    .and(item -> item.isNull(Advertisement::getStartTime)
                            .or().le(Advertisement::getStartTime, now))
                    .and(item -> item.isNull(Advertisement::getEndTime)
                            .or().ge(Advertisement::getEndTime, now));
            default -> throw BusinessException.badRequest("不支持的广告状态");
        }
    }

    private AdminAdvertisementDtos.AdvertisementView toView(Advertisement item) {
        return new AdminAdvertisementDtos.AdvertisementView(
                item.getId(), item.getTitle(), item.getDescription(), item.getImageUrl(),
                item.getTargetType(), item.getTargetValue(), item.getPosition(),
                Integer.valueOf(1).equals(item.getEnabled()),
                item.getSortOrder() == null ? 0 : item.getSortOrder(),
                item.getStartTime(), item.getEndTime(), displayStatus(item),
                item.getCreatedAt(), item.getUpdatedAt()
        );
    }

    private String displayStatus(Advertisement item) {
        if (!Integer.valueOf(1).equals(item.getEnabled())) return "DISABLED";
        LocalDateTime now = LocalDateTime.now();
        if (item.getStartTime() != null && item.getStartTime().isAfter(now)) return "SCHEDULED";
        if (item.getEndTime() != null && item.getEndTime().isBefore(now)) return "EXPIRED";
        return "ACTIVE";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
