package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminHouseAttributeDtos;
import com.zhuxiang.service.entity.HouseFacility;
import com.zhuxiang.service.entity.HouseFacilityRelation;
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.entity.HouseTagRelation;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdminHouseAttributeService;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseFacilityService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.HouseTagRelationService;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminHouseAttributeServiceImpl implements AdminHouseAttributeService {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");

    private final UserService userService;
    private final HouseService houseService;
    private final HouseFacilityService facilityService;
    private final HouseFacilityRelationService facilityRelationService;
    private final HouseTagService tagService;
    private final HouseTagRelationService tagRelationService;

    public AdminHouseAttributeServiceImpl(
            UserService userService,
            HouseService houseService,
            HouseFacilityService facilityService,
            HouseFacilityRelationService facilityRelationService,
            HouseTagService tagService,
            HouseTagRelationService tagRelationService
    ) {
        this.userService = userService;
        this.houseService = houseService;
        this.facilityService = facilityService;
        this.facilityRelationService = facilityRelationService;
        this.tagService = tagService;
        this.tagRelationService = tagRelationService;
    }

    @Override
    public List<AdminHouseAttributeDtos.FacilityItem> getFacilityDictionary(String operatorId) {
        requireAdminRole(operatorId);
        return facilityService.list(
                        Wrappers.<HouseFacility>lambdaQuery()
                                .orderByAsc(HouseFacility::getSortOrder)
                                .orderByAsc(HouseFacility::getId)
                ).stream()
                .map(this::toFacilityItem)
                .toList();
    }

    @Override
    public AdminHouseAttributeDtos.FacilityItem createFacility(
            AdminHouseAttributeDtos.CreateFacilityRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        String name = request.name().trim();
        ensureFacilityNameAvailable(name, null);

        HouseFacility facility = new HouseFacility();
        facility.setId(UUID.randomUUID().toString());
        facility.setName(name);
        facility.setIconKey(trimToNull(request.iconKey()));
        facility.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        facility.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
        facility.setCreatedAt(LocalDateTime.now());
        if (!facilityService.save(facility)) {
            throw BusinessException.badRequest("设施创建失败");
        }
        return toFacilityItem(facility);
    }

    @Override
    public AdminHouseAttributeDtos.FacilityItem updateFacility(
            String facilityId,
            AdminHouseAttributeDtos.UpdateFacilityRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        HouseFacility facility = requireFacility(facilityId);
        String name = request.name().trim();
        ensureFacilityNameAvailable(name, facility.getId());

        facility.setName(name);
        facility.setIconKey(trimToNull(request.iconKey()));
        facility.setSortOrder(request.sortOrder());
        facility.setEnabled(request.enabled() ? 1 : 0);
        if (!facilityService.updateById(facility)) {
            throw BusinessException.badRequest("设施更新失败");
        }
        return toFacilityItem(facility);
    }

    @Override
    @Transactional
    public void deleteFacility(String facilityId, String operatorId) {
        requireAdminRole(operatorId);
        HouseFacility facility = requireFacility(facilityId);
        long referenceCount = facilityRelationService.count(
                Wrappers.<HouseFacilityRelation>lambdaQuery()
                        .eq(HouseFacilityRelation::getFacilityId, facility.getId())
        );
        if (referenceCount > 0) {
            throw BusinessException.conflict("该设施仍被房源引用，请先解除关联或停用");
        }
        if (!facilityService.removeById(facility.getId())) {
            throw BusinessException.badRequest("设施删除失败");
        }
    }

    @Override
    public List<AdminHouseAttributeDtos.TagItem> getTagDictionary(String operatorId) {
        requireAdminRole(operatorId);
        return tagService.list(
                        Wrappers.<HouseTag>lambdaQuery()
                                .orderByAsc(HouseTag::getSortOrder)
                                .orderByAsc(HouseTag::getId)
                ).stream()
                .map(this::toTagItem)
                .toList();
    }

    @Override
    public AdminHouseAttributeDtos.TagItem createTag(
            AdminHouseAttributeDtos.CreateTagRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        String name = request.name().trim();
        ensureTagNameAvailable(name, null);

        HouseTag tag = new HouseTag();
        tag.setId(UUID.randomUUID().toString());
        tag.setName(name);
        tag.setTagType(request.tagType().trim());
        tag.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        tag.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
        tag.setCreatedAt(LocalDateTime.now());
        if (!tagService.save(tag)) {
            throw BusinessException.badRequest("标签创建失败");
        }
        return toTagItem(tag);
    }

    @Override
    public AdminHouseAttributeDtos.TagItem updateTag(
            String tagId,
            AdminHouseAttributeDtos.UpdateTagRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        HouseTag tag = requireTag(tagId);
        String name = request.name().trim();
        ensureTagNameAvailable(name, tag.getId());

        tag.setName(name);
        tag.setTagType(request.tagType().trim());
        tag.setSortOrder(request.sortOrder());
        tag.setEnabled(request.enabled() ? 1 : 0);
        if (!tagService.updateById(tag)) {
            throw BusinessException.badRequest("标签更新失败");
        }
        return toTagItem(tag);
    }

    @Override
    @Transactional
    public void deleteTag(String tagId, String operatorId) {
        requireAdminRole(operatorId);
        HouseTag tag = requireTag(tagId);
        long referenceCount = tagRelationService.count(
                Wrappers.<HouseTagRelation>lambdaQuery()
                        .eq(HouseTagRelation::getTagId, tag.getId())
        );
        if (referenceCount > 0) {
            throw BusinessException.conflict("该标签仍被房源引用，请先解除关联或停用");
        }
        if (!tagService.removeById(tag.getId())) {
            throw BusinessException.badRequest("标签删除失败");
        }
    }

    @Override
    public AdminHouseAttributeDtos.HouseAttributes getHouseAttributes(String houseId, String operatorId) {
        requireAdminRole(operatorId);
        return loadHouseAttributes(houseId);
    }

    private AdminHouseAttributeDtos.HouseAttributes loadHouseAttributes(String houseId) {
        requireHouse(houseId);

        List<String> facilityIds = facilityRelationService.list(
                        Wrappers.<HouseFacilityRelation>lambdaQuery()
                                .eq(HouseFacilityRelation::getHouseId, houseId)
                ).stream()
                .map(HouseFacilityRelation::getFacilityId)
                .toList();
        List<AdminHouseAttributeDtos.FacilityItem> facilities = facilityIds.isEmpty()
                ? List.of()
                : facilityService.listByIds(facilityIds).stream()
                .sorted(facilityComparator())
                .map(this::toFacilityItem)
                .toList();

        List<String> tagIds = tagRelationService.list(
                        Wrappers.<HouseTagRelation>lambdaQuery()
                                .eq(HouseTagRelation::getHouseId, houseId)
                ).stream()
                .map(HouseTagRelation::getTagId)
                .toList();
        List<AdminHouseAttributeDtos.TagItem> tags = tagIds.isEmpty()
                ? List.of()
                : tagService.listByIds(tagIds).stream()
                .sorted(tagComparator())
                .map(this::toTagItem)
                .toList();

        return new AdminHouseAttributeDtos.HouseAttributes(houseId, facilities, tags);
    }

    @Override
    @Transactional
    public AdminHouseAttributeDtos.HouseAttributes replaceFacilities(
            String houseId,
            List<String> facilityIds,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        requireHouse(houseId);
        List<String> normalizedIds = normalizeIds(facilityIds);
        validateFacilities(normalizedIds);

        facilityRelationService.remove(
                Wrappers.<HouseFacilityRelation>lambdaQuery()
                        .eq(HouseFacilityRelation::getHouseId, houseId)
        );
        if (!normalizedIds.isEmpty()) {
            List<HouseFacilityRelation> relations = normalizedIds.stream()
                    .map(facilityId -> newFacilityRelation(houseId, facilityId))
                    .toList();
            facilityRelationService.saveBatch(relations);
        }
        return loadHouseAttributes(houseId);
    }

    @Override
    @Transactional
    public AdminHouseAttributeDtos.HouseAttributes replaceTags(
            String houseId,
            List<String> tagIds,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        requireHouse(houseId);
        List<String> normalizedIds = normalizeIds(tagIds);
        validateTags(normalizedIds);

        tagRelationService.remove(
                Wrappers.<HouseTagRelation>lambdaQuery()
                        .eq(HouseTagRelation::getHouseId, houseId)
        );
        if (!normalizedIds.isEmpty()) {
            List<HouseTagRelation> relations = normalizedIds.stream()
                    .map(tagId -> newTagRelation(houseId, tagId))
                    .toList();
            tagRelationService.saveBatch(relations);
        }
        return loadHouseAttributes(houseId);
    }

    private void requireAdminRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        if (!ADMIN_ROLES.contains(user.getRole())) {
            throw BusinessException.forbidden("当前账号无权配置房源设施和标签");
        }
    }

    private void requireHouse(String houseId) {
        if (houseService.getById(houseId) == null) {
            throw BusinessException.notFound("房源不存在");
        }
    }

    private HouseFacility requireFacility(String facilityId) {
        HouseFacility facility = facilityService.getById(facilityId);
        if (facility == null) {
            throw BusinessException.notFound("设施字典项不存在");
        }
        return facility;
    }

    private HouseTag requireTag(String tagId) {
        HouseTag tag = tagService.getById(tagId);
        if (tag == null) {
            throw BusinessException.notFound("标签字典项不存在");
        }
        return tag;
    }

    private void ensureFacilityNameAvailable(String name, String excludeId) {
        long count = facilityService.count(
                Wrappers.<HouseFacility>lambdaQuery()
                        .eq(HouseFacility::getName, name)
                        .ne(excludeId != null, HouseFacility::getId, excludeId)
        );
        if (count > 0) {
            throw BusinessException.conflict("设施名称已存在");
        }
    }

    private void ensureTagNameAvailable(String name, String excludeId) {
        long count = tagService.count(
                Wrappers.<HouseTag>lambdaQuery()
                        .eq(HouseTag::getName, name)
                        .ne(excludeId != null, HouseTag::getId, excludeId)
        );
        if (count > 0) {
            throw BusinessException.conflict("标签名称已存在");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateFacilities(List<String> facilityIds) {
        if (facilityIds.isEmpty()) {
            return;
        }
        Set<String> validIds = facilityService.list(
                        Wrappers.<HouseFacility>lambdaQuery()
                                .in(HouseFacility::getId, facilityIds)
                                .eq(HouseFacility::getEnabled, 1)
                ).stream()
                .map(HouseFacility::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<String> invalidIds = facilityIds.stream().filter(id -> !validIds.contains(id)).toList();
        if (!invalidIds.isEmpty()) {
            throw BusinessException.badRequest("设施不存在或已停用: " + String.join(", ", invalidIds));
        }
    }

    private void validateTags(List<String> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        Set<String> validIds = tagService.list(
                        Wrappers.<HouseTag>lambdaQuery()
                                .in(HouseTag::getId, tagIds)
                                .eq(HouseTag::getEnabled, 1)
                ).stream()
                .map(HouseTag::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<String> invalidIds = tagIds.stream().filter(id -> !validIds.contains(id)).toList();
        if (!invalidIds.isEmpty()) {
            throw BusinessException.badRequest("标签不存在或已停用: " + String.join(", ", invalidIds));
        }
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            throw BusinessException.badRequest("ID 列表不能为空");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw BusinessException.badRequest("ID 不能为空");
            }
            normalized.add(id.trim());
        }
        return List.copyOf(normalized);
    }

    private HouseFacilityRelation newFacilityRelation(String houseId, String facilityId) {
        HouseFacilityRelation relation = new HouseFacilityRelation();
        relation.setId(UUID.randomUUID().toString());
        relation.setHouseId(houseId);
        relation.setFacilityId(facilityId);
        return relation;
    }

    private HouseTagRelation newTagRelation(String houseId, String tagId) {
        HouseTagRelation relation = new HouseTagRelation();
        relation.setId(UUID.randomUUID().toString());
        relation.setHouseId(houseId);
        relation.setTagId(tagId);
        return relation;
    }

    private AdminHouseAttributeDtos.FacilityItem toFacilityItem(HouseFacility facility) {
        return new AdminHouseAttributeDtos.FacilityItem(
                facility.getId(),
                facility.getName(),
                facility.getIconKey(),
                facility.getSortOrder(),
                Integer.valueOf(1).equals(facility.getEnabled())
        );
    }

    private AdminHouseAttributeDtos.TagItem toTagItem(HouseTag tag) {
        return new AdminHouseAttributeDtos.TagItem(
                tag.getId(),
                tag.getName(),
                tag.getTagType(),
                tag.getSortOrder(),
                Integer.valueOf(1).equals(tag.getEnabled())
        );
    }

    private Comparator<HouseFacility> facilityComparator() {
        return Comparator.comparing(
                        HouseFacility::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                )
                .thenComparing(HouseFacility::getId, Comparator.nullsLast(String::compareTo));
    }

    private Comparator<HouseTag> tagComparator() {
        return Comparator.comparing(
                        HouseTag::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                )
                .thenComparing(HouseTag::getId, Comparator.nullsLast(String::compareTo));
    }
}
