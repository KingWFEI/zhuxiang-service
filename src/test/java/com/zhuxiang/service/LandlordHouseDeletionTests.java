package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.PlatformLandlordProperties;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseFacilityService;
import com.zhuxiang.service.service.HouseImageService;
import com.zhuxiang.service.service.HousePropertyCertificateService;
import com.zhuxiang.service.service.HouseTagRelationService;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.HouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LandlordHouseDeletionTests {

    private final HouseMapper houseMapper = mock(HouseMapper.class);
    private final UserService userService = mock(UserService.class);
    private HouseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HouseServiceImpl(
                mock(CommunityService.class),
                mock(HouseImageService.class),
                mock(HouseTagService.class),
                mock(HouseTagRelationService.class),
                mock(HouseFacilityService.class),
                mock(HouseFacilityRelationService.class),
                mock(LandlordService.class),
                mock(AdvertisementService.class),
                mock(RegionService.class),
                mock(SmartLockMapper.class),
                mock(UserFavoriteHouseMapper.class),
                mock(RentOrderMapper.class),
                userService,
                mock(FileRecordService.class),
                mock(HousePropertyCertificateService.class),
                new PlatformLandlordProperties()
        );
        ReflectionTestUtils.setField(service, "baseMapper", houseMapper);
        when(houseMapper.updateById(any(House.class))).thenReturn(1);

        User landlord = new User();
        landlord.setId("landlord-1");
        landlord.setRole("LANDLORD");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
    }

    @Test
    void landlordCanSoftDeleteOwnOfflineHouse() {
        House house = house("offline", "landlord-1");
        when(houseMapper.selectById("house-1")).thenReturn(house);

        boolean deleted = service.deleteLandlordHouse("house-1", "landlord-1");

        assertThat(deleted).isTrue();
        assertThat(house.getStatus()).isEqualTo("deleted");
        assertThat(house.getUpdatedAt()).isNotNull();
        verify(houseMapper).updateById(house);
    }

    @Test
    void landlordCannotDeleteHouseBeforeItIsOffline() {
        when(houseMapper.selectById("house-1"))
                .thenReturn(house("available", "landlord-1"));

        assertThatThrownBy(() ->
                service.deleteLandlordHouse("house-1", "landlord-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("下架后才能删除");
                });
    }

    @Test
    void landlordCannotDeleteAnotherLandlordsHouse() {
        when(houseMapper.selectById("house-1"))
                .thenReturn(house("offline", "landlord-2"));

        assertThatThrownBy(() ->
                service.deleteLandlordHouse("house-1", "landlord-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(403));
    }

    private House house(String status, String landlordId) {
        House house = new House();
        house.setId("house-1");
        house.setStatus(status);
        house.setLandlordId(landlordId);
        return house;
    }
}
