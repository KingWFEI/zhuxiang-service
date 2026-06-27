package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminHouseAttributeDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseFacility;
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseFacilityService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.HouseTagRelationService;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminHouseAttributeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminHouseAttributeServiceTests {

    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final HouseFacilityService facilityService = mock(HouseFacilityService.class);
    private final HouseFacilityRelationService facilityRelationService = mock(HouseFacilityRelationService.class);
    private final HouseTagService tagService = mock(HouseTagService.class);
    private final HouseTagRelationService tagRelationService = mock(HouseTagRelationService.class);
    private AdminHouseAttributeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminHouseAttributeServiceImpl(
                userService,
                houseService,
                facilityService,
                facilityRelationService,
                tagService,
                tagRelationService
        );
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
    }

    @Test
    void returnsCompleteFacilityAndTagDictionaries() {
        HouseFacility facility = new HouseFacility();
        facility.setId("wifi");
        facility.setName("Wi-Fi");
        facility.setIconKey("wifi");
        facility.setSortOrder(1);
        facility.setEnabled(1);

        HouseTag tag = new HouseTag();
        tag.setId("tag-metro");
        tag.setName("近地铁");
        tag.setTagType("traffic");
        tag.setSortOrder(1);
        tag.setEnabled(0);

        when(facilityService.list(any(Wrapper.class))).thenReturn(List.of(facility));
        when(tagService.list(any(Wrapper.class))).thenReturn(List.of(tag));

        List<AdminHouseAttributeDtos.FacilityItem> facilities = service.getFacilityDictionary("admin-1");
        List<AdminHouseAttributeDtos.TagItem> tags = service.getTagDictionary("admin-1");

        assertThat(facilities).containsExactly(
                new AdminHouseAttributeDtos.FacilityItem("wifi", "Wi-Fi", "wifi", 1, true)
        );
        assertThat(tags).containsExactly(
                new AdminHouseAttributeDtos.TagItem("tag-metro", "近地铁", "traffic", 1, false)
        );
    }

    @Test
    void rejectsUnknownOrDisabledFacilityBeforeReplacingRelations() {
        House house = new House();
        house.setId("house-1");
        when(houseService.getById("house-1")).thenReturn(house);
        when(facilityService.list(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.replaceFacilities(
                "house-1",
                List.of("disabled-facility"),
                "admin-1"
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("disabled-facility");
                });

        verify(facilityRelationService, never()).remove(any(Wrapper.class));
    }

    @Test
    void createsFacilityWithDefaults() {
        when(facilityService.count(any(Wrapper.class))).thenReturn(0L);
        when(facilityService.save(any(HouseFacility.class))).thenReturn(true);

        AdminHouseAttributeDtos.FacilityItem result = service.createFacility(
                new AdminHouseAttributeDtos.CreateFacilityRequest(" Wi-Fi ", " wifi ", null, null),
                "admin-1"
        );

        assertThat(result.id()).isNotBlank();
        assertThat(result.name()).isEqualTo("Wi-Fi");
        assertThat(result.iconKey()).isEqualTo("wifi");
        assertThat(result.sortOrder()).isZero();
        assertThat(result.enabled()).isTrue();
        verify(facilityService).save(any(HouseFacility.class));
    }

    @Test
    void updatesTagDictionaryItem() {
        HouseTag tag = new HouseTag();
        tag.setId("tag-metro");
        tag.setName("近地铁");
        tag.setTagType("traffic");
        tag.setSortOrder(1);
        tag.setEnabled(1);
        when(tagService.getById(tag.getId())).thenReturn(tag);
        when(tagService.count(any(Wrapper.class))).thenReturn(0L);
        when(tagService.updateById(tag)).thenReturn(true);

        AdminHouseAttributeDtos.TagItem result = service.updateTag(
                tag.getId(),
                new AdminHouseAttributeDtos.UpdateTagRequest("地铁房", "feature", 5, false),
                "admin-1"
        );

        assertThat(result).isEqualTo(
                new AdminHouseAttributeDtos.TagItem("tag-metro", "地铁房", "feature", 5, false)
        );
        verify(tagService).updateById(tag);
    }

    @Test
    void refusesToDeleteReferencedTag() {
        HouseTag tag = new HouseTag();
        tag.setId("tag-metro");
        when(tagService.getById(tag.getId())).thenReturn(tag);
        when(tagRelationService.count(any(Wrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteTag(tag.getId(), "admin-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(409);
                    assertThat(exception.getMessage()).contains("仍被房源引用");
                });

        verify(tagService, never()).removeById(tag.getId());
    }
}
