package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.HouseSourceType;
import com.zhuxiang.service.config.PlatformLandlordProperties;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseFacility;
import com.zhuxiang.service.entity.HouseFacilityRelation;
import com.zhuxiang.service.entity.HouseImage;
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.entity.HouseTagRelation;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseRoomTypeService;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminHouseImageCreationTests {

    private final CommunityService communityService = mock(CommunityService.class);
    private final HouseImageService imageService = mock(HouseImageService.class);
    private final HouseTagService tagService = mock(HouseTagService.class);
    private final HouseTagRelationService tagRelationService = mock(HouseTagRelationService.class);
    private final HouseFacilityService facilityService = mock(HouseFacilityService.class);
    private final HouseFacilityRelationService facilityRelationService = mock(HouseFacilityRelationService.class);
    private final HouseRoomTypeService roomTypeService = mock(HouseRoomTypeService.class);
    private final LandlordService landlordService = mock(LandlordService.class);
    private final AdvertisementService advertisementService = mock(AdvertisementService.class);
    private final RegionService regionService = mock(RegionService.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final UserFavoriteHouseMapper favoriteHouseMapper = mock(UserFavoriteHouseMapper.class);
    private final RentOrderMapper rentOrderMapper = mock(RentOrderMapper.class);
    private final UserService userService = mock(UserService.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final HousePropertyCertificateService propertyCertificateService =
            mock(HousePropertyCertificateService.class);
    private final HouseMapper houseMapper = mock(HouseMapper.class);
    private final PlatformLandlordProperties platformLandlordProperties =
            new PlatformLandlordProperties();
    private HouseServiceImpl service;

    @BeforeEach
    void setUp() {
        platformLandlordProperties.setId("platform-landlord-1");
        service = new HouseServiceImpl(
                communityService, imageService, tagService, tagRelationService,
                facilityService, facilityRelationService, roomTypeService, landlordService,
                advertisementService, regionService, smartLockMapper,
                favoriteHouseMapper, rentOrderMapper, userService, fileRecordService,
                propertyCertificateService, platformLandlordProperties
        );
        ReflectionTestUtils.setField(service, "baseMapper", houseMapper);
        when(houseMapper.insert(any(House.class))).thenReturn(1);
        when(imageService.saveBatch(any(Collection.class))).thenReturn(true);
        when(facilityRelationService.saveBatch(any(Collection.class))).thenReturn(true);
        when(tagRelationService.saveBatch(any(Collection.class))).thenReturn(true);
        when(roomTypeService.count(any(Wrapper.class))).thenReturn(1L);
    }

    @Test
    void createsHouseAndPersistsOwnedCoverAndImagesInOneFlow() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        HouseImage cover = image("https://cdn.example.com/cover.jpg");
        HouseImage bedroom = image("https://cdn.example.com/bedroom.jpg");
        when(imageService.list(any(Wrapper.class))).thenReturn(List.of(cover, bedroom));
        when(facilityService.list(any(Wrapper.class))).thenReturn(List.of(facility("wifi"), facility("air_conditioner")));
        when(tagService.list(any(Wrapper.class))).thenReturn(List.of(tag("near_metro"), tag("direct_rent")));

        AdminHouseDtos.AdminHouseView response = service.createHouse(request(), "admin-1");

        verify(fileRecordService).validateFileOwnership(
                "admin-1", "https://cdn.example.com/cover.jpg", "house_image"
        );
        verify(fileRecordService).validateFileOwnership(
                "admin-1", "https://cdn.example.com/bedroom.jpg", "house_image"
        );
        ArgumentCaptor<Collection<HouseImage>> imagesCaptor = collectionCaptor();
        verify(imageService).saveBatch(imagesCaptor.capture());
        List<HouseImage> saved = imagesCaptor.getValue().stream().toList();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getImageType()).isEqualTo("cover");
        assertThat(saved.get(0).getSortOrder()).isZero();
        assertThat(saved.get(1).getImageType()).isEqualTo("normal");
        assertThat(response.coverImage()).isEqualTo("https://cdn.example.com/cover.jpg");
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/cover.jpg",
                "https://cdn.example.com/bedroom.jpg"
        );
        assertThat(response.deposit()).isEqualTo(2800);
        assertThat(response.sourceType()).isEqualTo(HouseSourceType.PLATFORM.name());

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).insert(houseCaptor.capture());
        assertThat(houseCaptor.getValue().getSourceType())
                .isEqualTo(HouseSourceType.PLATFORM.name());
        assertThat(houseCaptor.getValue().getLandlordId()).isEqualTo("platform-landlord-1");
        assertThat(houseCaptor.getValue().getCreatedBy()).isEqualTo("admin-1");
        assertThat(houseCaptor.getValue().getRentMode()).isEqualTo("WHOLE_RENT");
        assertThat(houseCaptor.getValue().getRentType()).isEqualTo("LONG_RENT");

        ArgumentCaptor<Collection<HouseFacilityRelation>> facilitiesCaptor = collectionCaptor();
        verify(facilityRelationService).saveBatch(facilitiesCaptor.capture());
        assertThat(facilitiesCaptor.getValue())
                .extracting(HouseFacilityRelation::getFacilityId)
                .containsExactly("wifi", "air_conditioner");

        ArgumentCaptor<Collection<HouseTagRelation>> tagsCaptor = collectionCaptor();
        verify(tagRelationService).saveBatch(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(HouseTagRelation::getTagId)
                .containsExactly("near_metro", "direct_rent");
    }

    @Test
    void tenantCannotCreateHouseOrBindUploadedImages() {
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setRole("TENANT");
        when(userService.requireActiveUser("tenant-1")).thenReturn(tenant);

        assertThatThrownBy(() -> service.createHouse(request(), "tenant-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(403);
        verify(fileRecordService, never()).validateFileOwnership(
                any(String.class), any(String.class), any(String.class)
        );
        verify(houseMapper, never()).insert(any(House.class));
    }

    @Test
    void landlordCreatesHouseWithRequestLandlordIdWithoutAdminRole() {
        User landlord = new User();
        landlord.setId("landlord-1");
        landlord.setRole("LANDLORD");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        when(imageService.list(any(Wrapper.class))).thenReturn(List.of(
                image("https://cdn.example.com/cover.jpg"),
                image("https://cdn.example.com/bedroom.jpg")
        ));
        when(facilityService.list(any(Wrapper.class))).thenReturn(
                List.of(facility("wifi"), facility("air_conditioner")));
        when(tagService.list(any(Wrapper.class))).thenReturn(
                List.of(tag("near_metro"), tag("direct_rent")));

        AdminHouseDtos.AdminHouseView response =
                service.createLandlordHouse(request(), "landlord-1");

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).insert(houseCaptor.capture());
        assertThat(houseCaptor.getValue().getLandlordId()).isEqualTo("landlord-1");
        assertThat(houseCaptor.getValue().getStatus()).isEqualTo("draft");
        assertThat(houseCaptor.getValue().getSourceType())
                .isEqualTo(HouseSourceType.LANDLORD.name());
        assertThat(houseCaptor.getValue().getCreatedBy()).isEqualTo("landlord-1");
        assertThat(response.landlordId()).isEqualTo("landlord-1");
        assertThat(response.sourceType()).isEqualTo(HouseSourceType.LANDLORD.name());
    }

    @Test
    void publicListAndDetailDtosReturnServerControlledSourceType() {
        House house = new House();
        house.setId("house-source-1");
        house.setTitle("房东直租房源");
        house.setCoverImage("https://cdn.example.com/cover.jpg");
        house.setCommunityId("community-1");
        house.setLocation("渝北区");
        house.setStatus("reserved");
        house.setLandlordId("landlord-1");
        house.setSourceType(HouseSourceType.LANDLORD.name());
        house.setPrice(280000);
        house.setViewCount(0);

        when(houseMapper.selectById("house-source-1")).thenReturn(house);

        HouseDtos.HouseView listItem = service.toHouseView(house, null);
        HouseDtos.HouseDetail detail = service.getHouseDetail("house-source-1", null);

        assertThat(listItem.sourceType()).isEqualTo(HouseSourceType.LANDLORD.name());
        assertThat(detail.sourceType()).isEqualTo(HouseSourceType.LANDLORD.name());
    }

    @Test
    void publicHouseDetailReturnsConfiguredFacilityIcons() {
        House house = new House();
        house.setId("house-facilities-1");
        house.setTitle("设施图标测试房源");
        house.setCoverImage("https://cdn.example.com/cover.jpg");
        house.setStatus("reserved");
        house.setViewCount(0);

        HouseFacilityRelation relation = new HouseFacilityRelation();
        relation.setHouseId(house.getId());
        relation.setFacilityId("facility-wifi");

        HouseFacility facility = facility("facility-wifi");
        facility.setName("Wi-Fi");
        facility.setIconKey("wifi");
        facility.setSortOrder(10);

        when(houseMapper.selectById(house.getId())).thenReturn(house);
        when(facilityRelationService.list(any(Wrapper.class))).thenReturn(List.of(relation));
        when(facilityService.listByIds(anyList())).thenReturn(List.of(facility));

        HouseDtos.HouseDetail detail = service.getHouseDetail(house.getId(), null);

        assertThat(detail.facilities()).containsExactly("Wi-Fi");
        assertThat(detail.facilityItems()).containsExactly(
                new HouseDtos.FacilityView("facility-wifi", "Wi-Fi", "wifi")
        );
    }

    @Test
    void createAndUpdateRequestsCannotAcceptSourceAuditFields() {
        assertThat(Arrays.stream(AdminHouseDtos.CreateHouseRequest.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("landlordId", "sourceType", "createdBy");
        assertThat(Arrays.stream(AdminHouseDtos.UpdateHouseRequest.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("landlordId", "sourceType", "createdBy");
    }

    @Test
    void landlordIdentityComesFromAuthenticatedUser() {
        User landlord = new User();
        landlord.setId("other-landlord");
        landlord.setRole("LANDLORD");
        when(userService.requireActiveUser("other-landlord")).thenReturn(landlord);
        when(imageService.list(any(Wrapper.class))).thenReturn(List.of(
                image("https://cdn.example.com/cover.jpg"),
                image("https://cdn.example.com/bedroom.jpg")
        ));
        when(facilityService.list(any(Wrapper.class))).thenReturn(
                List.of(facility("wifi"), facility("air_conditioner")));
        when(tagService.list(any(Wrapper.class))).thenReturn(
                List.of(tag("near_metro"), tag("direct_rent")));

        service.createLandlordHouse(request(), "other-landlord");

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).insert(houseCaptor.capture());
        assertThat(houseCaptor.getValue().getLandlordId()).isEqualTo("other-landlord");
        verify(fileRecordService).validateFileOwnership(
                "other-landlord", "https://cdn.example.com/cover.jpg", "house_image"
        );
    }

    @Test
    void invalidFacilityPreventsHouseFromBeingCreated() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(facilityService.list(any(Wrapper.class))).thenReturn(List.of(facility("wifi")));

        assertThatThrownBy(() -> service.createHouse(request(), "admin-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("air_conditioner");
                });

        verify(houseMapper, never()).insert(any(House.class));
        verify(imageService, never()).saveBatch(any(Collection.class));
        verify(facilityRelationService, never()).saveBatch(any(Collection.class));
        verify(tagRelationService, never()).saveBatch(any(Collection.class));
    }

    @Test
    void updatesAttributesAndAppendsImagesWithoutDeletingExistingRecords() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        House house = new House();
        house.setId("house-1");
        house.setCoverImage("https://cdn.example.com/old-cover.jpg");
        HouseImage oldImage = image("https://cdn.example.com/old-cover.jpg");
        oldImage.setSortOrder(0);
        HouseImage newCover = image("https://cdn.example.com/new-cover.jpg");
        newCover.setSortOrder(1);
        HouseImage newRoom = image("https://cdn.example.com/new-room.jpg");
        newRoom.setSortOrder(2);

        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(houseMapper.updateById(any(House.class))).thenReturn(1);
        when(facilityService.list(any(Wrapper.class))).thenReturn(List.of(facility("wifi")));
        when(tagService.list(any(Wrapper.class))).thenReturn(List.of(tag("near_metro")));
        when(imageService.list(any(Wrapper.class)))
                .thenReturn(List.of(oldImage))
                .thenReturn(List.of(oldImage, newCover, newRoom));

        AdminHouseDtos.AdminHouseView response = service.updateHouse(
                "house-1",
                updateRequest(
                        "https://cdn.example.com/new-cover.jpg",
                        List.of("https://cdn.example.com/new-cover.jpg", "https://cdn.example.com/new-room.jpg"),
                        List.of("wifi"),
                        List.of("near_metro")
                ),
                "admin-1"
        );

        verify(imageService, never()).remove(any(Wrapper.class));
        ArgumentCaptor<Collection<HouseImage>> imagesCaptor = collectionCaptor();
        verify(imageService).saveBatch(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue())
                .extracting(HouseImage::getImageUrl)
                .containsExactly("https://cdn.example.com/new-cover.jpg", "https://cdn.example.com/new-room.jpg");
        assertThat(response.coverImage()).isEqualTo("https://cdn.example.com/new-cover.jpg");
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/old-cover.jpg",
                "https://cdn.example.com/new-cover.jpg",
                "https://cdn.example.com/new-room.jpg"
        );

        verify(facilityRelationService).remove(any(Wrapper.class));
        verify(tagRelationService).remove(any(Wrapper.class));
        ArgumentCaptor<Collection<HouseFacilityRelation>> facilitiesCaptor = collectionCaptor();
        verify(facilityRelationService).saveBatch(facilitiesCaptor.capture());
        assertThat(facilitiesCaptor.getValue())
                .extracting(HouseFacilityRelation::getFacilityId)
                .containsExactly("wifi");
        ArgumentCaptor<Collection<HouseTagRelation>> tagsCaptor = collectionCaptor();
        verify(tagRelationService).saveBatch(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(HouseTagRelation::getTagId)
                .containsExactly("near_metro");
    }

    @Test
    void emptyAttributeListsClearRelationsWithoutSavingEmptyBatches() {
        User admin = new User();
        admin.setRole("ADMIN");
        House house = new House();
        house.setId("house-1");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(houseMapper.selectById("house-1")).thenReturn(house);

        service.updateHouse(
                "house-1",
                updateRequest(null, null, List.of(), List.of()),
                "admin-1"
        );

        verify(facilityRelationService).remove(any(Wrapper.class));
        verify(tagRelationService).remove(any(Wrapper.class));
        verify(facilityRelationService, never()).saveBatch(any(Collection.class));
        verify(tagRelationService, never()).saveBatch(any(Collection.class));
    }

    @Test
    void returnsSingleAdminHouseWithImages() {
        House house = new House();
        house.setId("house-1");
        house.setTitle("高新区精装一居室");
        house.setSourceType(HouseSourceType.PLATFORM.name());
        house.setCreatedBy("admin-1");
        HouseImage cover = image("https://cdn.example.com/cover.jpg");
        cover.setSortOrder(0);
        HouseImage room = image("https://cdn.example.com/room.jpg");
        room.setSortOrder(1);
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(imageService.list(any(Wrapper.class))).thenReturn(List.of(cover, room));

        AdminHouseDtos.AdminHouseView response = service.getAdminHouseById("house-1");

        assertThat(response.id()).isEqualTo("house-1");
        assertThat(response.title()).isEqualTo("高新区精装一居室");
        assertThat(response.sourceType()).isEqualTo("PLATFORM");
        assertThat(response.createdBy()).isEqualTo("admin-1");
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/cover.jpg",
                "https://cdn.example.com/room.jpg"
        );
    }

    @Test
    void adminHouseListReturnsCreatorAndSourceMarkers() {
        House platformHouse = new House();
        platformHouse.setId("platform-house");
        platformHouse.setSourceType(HouseSourceType.PLATFORM.name());
        platformHouse.setCreatedBy("admin-1");
        House landlordHouse = new House();
        landlordHouse.setId("landlord-house");
        landlordHouse.setSourceType(HouseSourceType.LANDLORD.name());
        landlordHouse.setCreatedBy("landlord-1");
        when(houseMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(platformHouse, landlordHouse));
        when(smartLockMapper.selectLatestByHouseIds(anyList())).thenReturn(List.of());

        List<AdminHouseDtos.AdminHouseView> response =
                service.getAllHousesWithLockInfo();

        assertThat(response)
                .extracting(AdminHouseDtos.AdminHouseView::sourceType)
                .containsExactly("PLATFORM", "LANDLORD");
        assertThat(response)
                .extracting(AdminHouseDtos.AdminHouseView::createdBy)
                .containsExactly("admin-1", "landlord-1");
    }

    @Test
    void returnsFacilityAndTagIdsInLandlordHouseDetail() {
        User landlord = new User();
        landlord.setId("landlord-1");
        landlord.setRole("LANDLORD");
        House house = new House();
        house.setId("house-1");
        house.setLandlordId("landlord-1");
        HouseFacilityRelation facilityRelation = new HouseFacilityRelation();
        facilityRelation.setHouseId("house-1");
        facilityRelation.setFacilityId("facility_001");
        HouseTagRelation tagRelation = new HouseTagRelation();
        tagRelation.setHouseId("house-1");
        tagRelation.setTagId("tag_001");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(facilityRelationService.list(any(Wrapper.class))).thenReturn(List.of(facilityRelation));
        when(tagRelationService.list(any(Wrapper.class))).thenReturn(List.of(tagRelation));

        AdminHouseDtos.AdminHouseView response =
                service.getLandlordHouseById("house-1", "landlord-1");

        assertThat(response.facilityIds()).containsExactly("facility_001");
        assertThat(response.tagIds()).containsExactly("tag_001");
    }

    @Test
    void rejectsMissingAdminHouse() {
        when(houseMapper.selectById("missing-house")).thenReturn(null);

        assertThatThrownBy(() -> service.getAdminHouseById("missing-house"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(404));
    }

    @Test
    void bringsOfflineHouseBackOnline() {
        House house = new House();
        house.setId("house-1");
        house.setStatus("offline");
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(houseMapper.updateById(any(House.class))).thenReturn(1);
        when(imageService.list(any(Wrapper.class))).thenReturn(List.of());

        AdminHouseDtos.AdminHouseView response = service.onlineHouse("house-1");

        assertThat(response.status()).isEqualTo("available");
        assertThat(house.getStatus()).isEqualTo("available");
        verify(houseMapper).updateById(house);
    }

    @Test
    void rejectsOnlineRequestForHouseThatIsNotOffline() {
        House house = new House();
        house.setId("house-1");
        house.setStatus("rented");
        when(houseMapper.selectById("house-1")).thenReturn(house);

        assertThatThrownBy(() -> service.onlineHouse("house-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("只有已下架房源");
                });

        verify(houseMapper, never()).updateById(any(House.class));
    }

    private AdminHouseDtos.CreateHouseRequest request() {
        return new AdminHouseDtos.CreateHouseRequest(
                "高新区精装一居室",
                "https://cdn.example.com/cover.jpg",
                List.of(
                        "https://cdn.example.com/cover.jpg",
                        "https://cdn.example.com/bedroom.jpg"
                ),
                "高新区金融城", "community-1", "天府大道1号",
                "2栋", "1单元", "1801", 2800, 999,
                "押一付三", "1室1厅1卫", new BigDecimal("45.5"),
                "18/32层", "南", "精装", LocalDate.of(2026, 7, 1),
                "距地铁500米", "房源介绍", "WHOLE_RENT", "LONG_RENT",
                true, true,
                List.of("wifi", "air_conditioner"),
                List.of("near_metro", "direct_rent"),
                null, null, null, null, null, null, null
        );
    }

    private AdminHouseDtos.UpdateHouseRequest updateRequest(
            String coverImage,
            List<String> imageUrls,
            List<String> facilityIds,
            List<String> tagIds
    ) {
        return new AdminHouseDtos.UpdateHouseRequest(
                null, coverImage, imageUrls, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, facilityIds, tagIds,
                null, null, null, null, null, null, null
        );
    }

    private HouseFacility facility(String id) {
        HouseFacility facility = new HouseFacility();
        facility.setId(id);
        facility.setEnabled(1);
        return facility;
    }

    private HouseTag tag(String id) {
        HouseTag tag = new HouseTag();
        tag.setId(id);
        tag.setEnabled(1);
        return tag;
    }

    private HouseImage image(String url) {
        HouseImage image = new HouseImage();
        image.setImageUrl(url);
        return image;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Collection<T>> collectionCaptor() {
        return ArgumentCaptor.forClass((Class) Collection.class);
    }
}
