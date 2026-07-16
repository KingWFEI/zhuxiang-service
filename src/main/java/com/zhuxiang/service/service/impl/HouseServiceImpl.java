package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.Advertisement;
import com.zhuxiang.service.entity.Community;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseFacility;
import com.zhuxiang.service.entity.HouseFacilityRelation;
import com.zhuxiang.service.entity.HouseImage;
import com.zhuxiang.service.entity.HouseLocation;
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.entity.HouseTagRelation;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.UserFavoriteHouse;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseLocationMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseFacilityService;
import com.zhuxiang.service.service.HouseImageService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.HouseTagRelationService;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.mapper.HouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author king-wang
* @description 针对表【house(房源主表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:05
*/
@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House>
    implements HouseService{

    private static final Set<String> CATEGORIES =
            Set.of("recommended", "short_rent", "homestay", "long_rent");
    private static final Set<String> SORTS =
            Set.of("default", "price_asc", "price_desc", "latest", "distance");
    private static final Set<String> ADMIN_ROLES =
            Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");

    private final CommunityService communityService;
    private final HouseImageService imageService;
    private final HouseTagService tagService;
    private final HouseTagRelationService tagRelationService;
    private final HouseFacilityService facilityService;
    private final HouseFacilityRelationService facilityRelationService;
    private final LandlordService landlordService;
    private final AdvertisementService advertisementService;
    private final RegionService regionService;
    private final SmartLockMapper smartLockMapper;
    private final UserFavoriteHouseMapper favoriteHouseMapper;
    private final RentOrderMapper rentOrderMapper;
    private final UserService userService;
    private final FileRecordService fileRecordService;
    private final HouseLocationMapper houseLocationMapper;

    public HouseServiceImpl(
            CommunityService communityService,
            HouseImageService imageService,
            HouseTagService tagService,
            HouseTagRelationService tagRelationService,
            HouseFacilityService facilityService,
            HouseFacilityRelationService facilityRelationService,
            LandlordService landlordService,
            AdvertisementService advertisementService,
            RegionService regionService,
            SmartLockMapper smartLockMapper,
            UserFavoriteHouseMapper favoriteHouseMapper,
            RentOrderMapper rentOrderMapper,
            UserService userService,
            FileRecordService fileRecordService,
            HouseLocationMapper houseLocationMapper
    ) {
        this.communityService = communityService;
        this.imageService = imageService;
        this.tagService = tagService;
        this.tagRelationService = tagRelationService;
        this.facilityService = facilityService;
        this.facilityRelationService = facilityRelationService;
        this.landlordService = landlordService;
        this.advertisementService = advertisementService;
        this.regionService = regionService;
        this.smartLockMapper = smartLockMapper;
        this.favoriteHouseMapper = favoriteHouseMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.userService = userService;
        this.fileRecordService = fileRecordService;
        this.houseLocationMapper = houseLocationMapper;
    }

    /**
     * 查询首页房源并按规则插入广告。
     */
    @Override
    public HouseDtos.FeedData getFeed(
            String category,
            long page,
            long pageSize,
            String userId
    ) {
        validateCategory(category);
        IPage<House> result = queryHouses(
                null, category, null, null, null, null, null, null, null, null,
                "default", page, pageSize
        );
        List<HouseDtos.FeedItem> items = result.getRecords().stream()
                .map(house -> HouseDtos.FeedItem.house(toHouseView(house, userId)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (page == 1 && !items.isEmpty()) {
            Advertisement advertisement = findActiveAdvertisement();
            if (advertisement != null) {
                items.add(Math.min(1, items.size()), HouseDtos.FeedItem.advertisement(
                        new HouseDtos.AdvertisementView(
                                advertisement.getId(),
                                advertisement.getTitle(),
                                advertisement.getDescription(),
                                advertisement.getImageUrl(),
                                advertisement.getTargetType(),
                                advertisement.getTargetValue()
                        )
                ));
            }
        }
        return new HouseDtos.FeedData(items, page, pageSize, result.getCurrent() < result.getPages());
    }

    /**
     * 按筛选条件分页查询房源。
     */
    @Override
    public PageData<HouseDtos.HouseView> searchHouses(
            String keyword,
            String category,
            String region,
            Integer minPrice,
            Integer maxPrice,
            String roomType,
            Integer minArea,
            Integer maxArea,
            String facilities,
            List<String> tags,
            String sort,
            long page,
            long pageSize,
            String userId
    ) {
        IPage<House> result = queryHouses(
                keyword, category, region, minPrice, maxPrice, roomType,
                minArea, maxArea, facilities, tags, sort, page, pageSize
        );
        return PageData.of(
                result.getRecords().stream()
                        .map(house -> toHouseView(house, userId))
                        .toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    /**
     * 查询房源详情并增加浏览次数。
     */
    @Override
    @Transactional
    public HouseDtos.HouseDetail getHouseDetail(String houseId, String userId) {
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        if ("rented".equals(house.getStatus())) {
            throw BusinessException.conflict("该房源已出租");
        }
        if (!"available".equals(house.getStatus()) && !"reserved".equals(house.getStatus())) {
            throw BusinessException.notFound("房源不存在或已下架");
        }
        if ("available".equals(house.getStatus())) {
            house.setViewCount((house.getViewCount() == null ? 0 : house.getViewCount()) + 1);
            updateById(house);
        }
        Community community = communityService.getById(house.getCommunityId());
        Landlord landlord = landlordService.getById(house.getLandlordId());
        List<String> images = imageService.list(
                        Wrappers.<HouseImage>lambdaQuery()
                                .eq(HouseImage::getHouseId, houseId)
                                .orderByAsc(HouseImage::getSortOrder)
                ).stream()
                .map(HouseImage::getImageUrl)
                .toList();
        RentAvailabilityData rent = loadRentAvailability(house, userId);
        HouseLocation houseLocation = findHouseLocation(houseId);
        return new HouseDtos.HouseDetail(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                images.isEmpty() ? List.of(house.getCoverImage()) : images,
                buildLocationDisplay(houseLocation, house),
                community == null ? "" : community.getName(),
                house.getAddress(),
                house.getPrice(),
                house.getDeposit(),
                house.getPaymentMethod(),
                house.getRoomType(),
                areaAsInteger(house.getArea()),
                house.getFloor(),
                house.getOrientation(),
                getTags(houseId),
                getFacilities(houseId),
                house.getDescription(),
                integerBoolean(house.getIsSmartLockSupported()),
                isFavorite(userId, houseId),
                house.getMetro(),
                house.getDecoration(),
                house.getAvailableDate(),
                landlord == null ? null : landlord.getId(),
                landlord == null ? "" : landlord.getName(),
                landlord == null ? "" : landlord.getAvatarUrl(),
                landlord != null && integerBoolean(landlord.getIsVerified()),
                landlord == null ? BigDecimal.ZERO : landlord.getRating(),
                landlord == null ? 0 : landlord.getRentedCount(),
                landlord == null ? "" : landlord.getResponseDescription(),
                house.getStatus(),
                "rented".equals(house.getStatus()),
                rent.rentAvailability(),
                rent.activeOrderId(),
                rent.activeOrderBelongsToMe(),
                houseLocation == null ? null : houseLocation.getLongitude(),
                houseLocation == null ? null : houseLocation.getLatitude()
        );
    }

    /**
     * 汇总可用的房源筛选选项。
     */
    @Override
    public HouseDtos.FilterOptions getFilterOptions() {
        List<HouseDtos.Option> regions = regionService.list(
                        Wrappers.<Region>lambdaQuery()
                                .eq(Region::getEnabled, 1)
                                .eq(Region::getLevel, "district")
                                .orderByAsc(Region::getSortOrder)
                ).stream()
                .map(region -> new HouseDtos.Option(region.getName(), region.getCode()))
                .toList();
        List<HouseDtos.Option> facilities = facilityService.list(
                        Wrappers.<HouseFacility>lambdaQuery()
                                .eq(HouseFacility::getEnabled, 1)
                                .orderByAsc(HouseFacility::getSortOrder)
                ).stream()
                .map(facility -> new HouseDtos.Option(facility.getName(), facility.getId()))
                .toList();
        List<HouseDtos.Option> roomTypes = list(
                        Wrappers.<House>lambdaQuery()
                                .select(House::getRoomType)
                                .in(House::getStatus, "available", "reserved")
                                .groupBy(House::getRoomType)
                ).stream()
                .map(House::getRoomType)
                .filter(StringUtils::hasText)
                .distinct()
                .map(value -> new HouseDtos.Option(value, value))
                .toList();
        return new HouseDtos.FilterOptions(
                regions,
                List.of(
                        new HouseDtos.PriceRange("1000以下", 0, 100000),
                        new HouseDtos.PriceRange("1000-2000", 100000, 200000),
                        new HouseDtos.PriceRange("2000-3000", 200000, 300000),
                        new HouseDtos.PriceRange("3000以上", 300000, 2_000_000_000)
                ),
                roomTypes,
                facilities,
                List.of(
                        new HouseDtos.Option("默认排序", "default"),
                        new HouseDtos.Option("价格升序", "price_asc"),
                        new HouseDtos.Option("价格降序", "price_desc"),
                        new HouseDtos.Option("最新发布", "latest"),
                        new HouseDtos.Option("距离优先", "distance")
                )
        );
    }

    /**
     * 查询并校验处于可租状态的房源。
     */
    @Override
    public House requireAvailableHouse(String houseId) {
        House house = getById(houseId);
        if (house == null || !"available".equals(house.getStatus())) {
            throw BusinessException.notFound("房源不存在或已下架");
        }
        return house;
    }

    /**
     * 查询房源的出租可用状态。available/rented 不额外查库，reserved 查订单表。
     */
    private RentAvailabilityData loadRentAvailability(House house, String userId) {
        if ("rented".equals(house.getStatus())) {
            return new RentAvailabilityData("rented", null, false);
        }
        if (!"reserved".equals(house.getStatus())) {
            return new RentAvailabilityData("available", null, false);
        }
        RentOrder activeOrder = rentOrderMapper.selectOne(
                Wrappers.<RentOrder>lambdaQuery()
                        .eq(RentOrder::getHouseId, house.getId())
                        .in(RentOrder::getStatus, "created", "pendingRealName", "pendingContract", "pendingPayment", "pendingSign")
                        .last("LIMIT 1")
        );
        if (activeOrder != null) {
            boolean belongsToMe = userId != null && userId.equals(activeOrder.getUserId());
            return new RentAvailabilityData(
                    "locked",
                    belongsToMe ? activeOrder.getId() : null,
                    belongsToMe
            );
        }
        return new RentAvailabilityData("available", null, false);
    }

    private record RentAvailabilityData(String rentAvailability, String activeOrderId, boolean activeOrderBelongsToMe) {
    }

    /**
     * 将房源实体转换为列表展示数据。
     */
    @Override
    public HouseDtos.HouseView toHouseView(House house, String userId) {
        Community community = communityService.getById(house.getCommunityId());
        RentAvailabilityData rent = loadRentAvailability(house, userId);
        String location = buildLocationDisplay(findHouseLocation(house.getId()), house);
        return new HouseDtos.HouseView(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                location,
                community == null ? "" : community.getName(),
                house.getPrice(),
                house.getRoomType(),
                areaAsInteger(house.getArea()),
                house.getFloor(),
                house.getOrientation(),
                getTags(house.getId()),
                getFacilities(house.getId()),
                house.getDescription(),
                integerBoolean(house.getIsSmartLockSupported()),
                isFavorite(userId, house.getId()),
                house.getMetro(),
                house.getDecoration(),
                house.getAvailableDate(),
                house.getStatus(),
                "rented".equals(house.getStatus()),
                rent.rentAvailability(),
                rent.activeOrderId(),
                rent.activeOrderBelongsToMe()
        );
    }

    /**
     * 构建房源查询条件并执行分页查询。
     */
    private IPage<House> queryHouses(
            String keyword,
            String category,
            String region,
            Integer minPrice,
            Integer maxPrice,
            String roomType,
            Integer minArea,
            Integer maxArea,
            String facilityNames,
            List<String> tags,
            String sort,
            long page,
            long pageSize
    ) {
        if (StringUtils.hasText(category)) {
            validateCategory(category);
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw BusinessException.badRequest("最低价格不能高于最高价格");
        }
        if (minArea != null && maxArea != null && minArea > maxArea) {
            throw BusinessException.badRequest("最小面积不能大于最大面积");
        }
        String actualSort = StringUtils.hasText(sort) ? sort : "default";
        if (!SORTS.contains(actualSort)) {
            throw BusinessException.badRequest("排序方式不支持");
        }

        LambdaQueryWrapper<House> wrapper = Wrappers.<House>lambdaQuery()
                .in(House::getStatus, "available", "reserved")
                .apply("NOT EXISTS (SELECT 1 FROM lease WHERE lease.house_id = house.id AND lease.status = 'active')")
                .eq(StringUtils.hasText(category), House::getRentType, category)
                .ge(minPrice != null, House::getPrice, minPrice)
                .le(maxPrice != null, House::getPrice, maxPrice)
                .eq(StringUtils.hasText(roomType), House::getRoomType, roomType)
                .ge(minArea != null, House::getArea, minArea)
                .le(maxArea != null, House::getArea, maxArea);

        if (StringUtils.hasText(region)) {
            Region matchedRegion = regionService.getOne(
                    Wrappers.<Region>lambdaQuery()
                            .and(query -> query.eq(Region::getCode, region)
                                    .or().eq(Region::getName, region))
                            .last("LIMIT 1"),
                    false
            );
            if (matchedRegion == null) {
                wrapper.like(House::getLocation, region);
            } else {
                List<String> communityIds = communityService.list(
                                Wrappers.<Community>lambdaQuery()
                                        .eq(Community::getRegionId, matchedRegion.getId())
                        ).stream()
                        .map(Community::getId)
                        .toList();
                if (communityIds.isEmpty()) {
                    return new Page<>(page, pageSize, 0);
                }
                wrapper.in(House::getCommunityId, communityIds);
            }
        }

        if (StringUtils.hasText(keyword)) {
            List<String> communityIds = communityService.list(
                            Wrappers.<Community>lambdaQuery().like(Community::getName, keyword)
                    ).stream()
                    .map(Community::getId)
                    .toList();
            wrapper.and(query -> {
                query.like(House::getTitle, keyword)
                        .or().like(House::getLocation, keyword)
                        .or().like(House::getAddress, keyword);
                if (!communityIds.isEmpty()) {
                    query.or().in(House::getCommunityId, communityIds);
                }
            });
        }

        Set<String> facilityHouseIds = resolveFacilityHouseIds(facilityNames);
        if (facilityHouseIds != null) {
            if (facilityHouseIds.isEmpty()) {
                return new Page<>(page, pageSize, 0);
            }
            wrapper.in(House::getId, facilityHouseIds);
        }

        Set<String> tagHouseIds = resolveTagHouseIds(tags);
        if (tagHouseIds != null) {
            if (tagHouseIds.isEmpty()) {
                return new Page<>(page, pageSize, 0);
            }
            wrapper.in(House::getId, tagHouseIds);
        }

        switch (actualSort) {
            case "price_asc" -> wrapper.orderByAsc(House::getPrice);
            case "price_desc" -> wrapper.orderByDesc(House::getPrice);
            case "latest" -> wrapper.orderByDesc(House::getCreatedAt);
            default -> wrapper.orderByDesc(House::getFavoriteCount)
                    .orderByDesc(House::getCreatedAt);
        }
        return page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 解析设施条件并返回同时匹配的房源编号。
     */
    private Set<String> resolveFacilityHouseIds(String facilityNames) {
        if (!StringUtils.hasText(facilityNames)) {
            return null;
        }
        List<String> values = Arrays.stream(facilityNames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<HouseFacility> matched = facilityService.list(
                Wrappers.<HouseFacility>lambdaQuery()
                        .and(query -> query.in(HouseFacility::getId, values)
                                .or().in(HouseFacility::getName, values))
        );
        if (matched.size() < values.size()) {
            return Set.of();
        }
        Set<String> facilityIds = matched.stream()
                .map(HouseFacility::getId)
                .collect(Collectors.toSet());
        return facilityRelationService.list(
                        Wrappers.<HouseFacilityRelation>lambdaQuery()
                                .in(HouseFacilityRelation::getFacilityId, facilityIds)
                ).stream()
                .collect(Collectors.groupingBy(
                        HouseFacilityRelation::getHouseId,
                        Collectors.mapping(
                                HouseFacilityRelation::getFacilityId,
                                Collectors.toSet()
                        )
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(facilityIds))
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析标签条件并返回同时匹配的房源编号。
     */
    private Set<String> resolveTagHouseIds(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> values = tags.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        List<HouseTag> matched = tagService.list(
                Wrappers.<HouseTag>lambdaQuery()
                        .and(query -> query.in(HouseTag::getId, values)
                                .or().in(HouseTag::getName, values))
        );
        if (matched.size() < values.size()) {
            return Set.of();
        }
        Set<String> tagIds = matched.stream()
                .map(HouseTag::getId)
                .collect(Collectors.toSet());
        return tagRelationService.list(
                        Wrappers.<HouseTagRelation>lambdaQuery()
                                .in(HouseTagRelation::getTagId, tagIds)
                ).stream()
                .collect(Collectors.groupingBy(
                        HouseTagRelation::getHouseId,
                        Collectors.mapping(
                                HouseTagRelation::getTagId,
                                Collectors.toSet()
                        )
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(tagIds))
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 查询指定房源的标签名称。
     */
    private List<String> getTags(String houseId) {
        List<String> ids = tagRelationService.list(
                        Wrappers.<HouseTagRelation>lambdaQuery()
                                .eq(HouseTagRelation::getHouseId, houseId)
                ).stream()
                .map(HouseTagRelation::getTagId)
                .toList();
        return ids.isEmpty()
                ? List.of()
                : tagService.listByIds(ids).stream().map(HouseTag::getName).toList();
    }

    /**
     * 查询指定房源的设施名称。
     */
    private List<String> getFacilities(String houseId) {
        List<String> ids = facilityRelationService.list(
                        Wrappers.<HouseFacilityRelation>lambdaQuery()
                                .eq(HouseFacilityRelation::getHouseId, houseId)
                ).stream()
                .map(HouseFacilityRelation::getFacilityId)
                .toList();
        return ids.isEmpty()
                ? List.of()
                : facilityService.listByIds(ids).stream()
                .map(HouseFacility::getName)
                .toList();
    }

    /**
     * 判断用户是否已收藏指定房源。
     */
    private boolean isFavorite(String userId, String houseId) {
        return StringUtils.hasText(userId) && favoriteHouseMapper.selectCount(
                Wrappers.<UserFavoriteHouse>lambdaQuery()
                        .eq(UserFavoriteHouse::getUserId, userId)
                        .eq(UserFavoriteHouse::getHouseId, houseId)
        ) > 0;
    }

    /**
     * 查询当前有效的首页信息流广告。
     */
    private Advertisement findActiveAdvertisement() {
        LocalDateTime now = LocalDateTime.now();
        return advertisementService.getOne(
                Wrappers.<Advertisement>lambdaQuery()
                        .eq(Advertisement::getPosition, "home_feed")
                        .eq(Advertisement::getEnabled, 1)
                        .and(query -> query.isNull(Advertisement::getStartTime)
                                .or().le(Advertisement::getStartTime, now))
                        .and(query -> query.isNull(Advertisement::getEndTime)
                                .or().ge(Advertisement::getEndTime, now))
                        .orderByAsc(Advertisement::getSortOrder)
                        .last("LIMIT 1"),
                false
        );
    }

    /**
     * 校验房源分类是否受支持。
     */
    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw BusinessException.badRequest("房源分类不支持");
        }
    }

    /**
     * 将整型标志转换为布尔值。
     */
    private boolean integerBoolean(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    /**
     * 创建新房源，返回管理端视图。
     */
    @Override
    @Transactional
    public AdminHouseDtos.AdminHouseView createHouse(
            AdminHouseDtos.CreateHouseRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        List<String> facilityIds = normalizeAttributeIds(request.facilityIds(), "设施");
        List<String> tagIds = normalizeAttributeIds(request.tagIds(), "标签");
        validateEnabledFacilities(facilityIds);
        validateEnabledTags(tagIds);
        List<String> imageUrls = normalizeAndValidateHouseImages(request, operatorId);
        String coverImage = imageUrls.getFirst();
        LocalDateTime now = LocalDateTime.now();
        House house = new House();
        house.setId(UUID.randomUUID().toString());
        house.setTitle(request.title());
        house.setCoverImage(coverImage);
        house.setLocation(request.location());
        house.setCommunityId(request.communityId());
        house.setAddress(request.address());
        house.setBuilding(request.building());
        house.setUnit(request.unit());
        house.setRoom(request.room());
        house.setPrice(request.price());
        house.setDeposit(request.deposit() != null ? request.deposit() : 0);
        house.setPaymentMethod(request.paymentMethod());
        house.setRoomType(request.roomType());
        house.setArea(request.area());
        house.setFloor(request.floor());
        house.setOrientation(request.orientation());
        house.setDecoration(request.decoration());
        house.setAvailableDate(request.availableDate());
        house.setMetro(request.metro());
        house.setDescription(request.description());
        house.setRentType(request.rentType());
        house.setStatus("draft");
        house.setIsSmartLockSupported(request.isSmartLockSupported() != null
                && request.isSmartLockSupported() ? 1 : 0);
        house.setIsSelfViewingSupported(request.isSelfViewingSupported() != null
                && request.isSelfViewingSupported() ? 1 : 0);
        house.setLandlordId(request.landlordId());
        house.setViewCount(0);
        house.setFavoriteCount(0);
        house.setCreatedAt(now);
        house.setUpdatedAt(now);
        save(house);
        saveHouseImages(house.getId(), coverImage, imageUrls, now);
        saveHouseFacilityRelations(house.getId(), facilityIds);
        saveHouseTagRelations(house.getId(), tagIds);
        saveHouseLocation(house.getId(), request.longitude(), request.latitude(),
                request.province(), request.city(), request.district(),
                request.township(), request.neighborhood(), request.address(), now);
        return toAdminHouseView(house, null, findHouseLocation(house.getId()));
    }

    /** 校验管理端角色。 */
    private void requireAdminRole(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (operator.getRole() == null
                || !ADMIN_ROLES.contains(operator.getRole().toUpperCase(Locale.ROOT))) {
            throw BusinessException.forbidden("当前账号无权创建房源");
        }
    }

    /** 合并封面和图片列表，并校验均属于当前操作者的房源图片上传记录。 */
    private List<String> normalizeAndValidateHouseImages(
            AdminHouseDtos.CreateHouseRequest request,
            String operatorId
    ) {
        if (!StringUtils.hasText(request.coverImage())) {
            throw BusinessException.badRequest("封面图不能为空");
        }
        if (request.imageUrls() == null || request.imageUrls().isEmpty()) {
            throw BusinessException.badRequest("房源图片不能为空");
        }
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        urls.add(request.coverImage().trim());
        request.imageUrls().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(urls::add);
        if (urls.size() > 20) {
            throw BusinessException.badRequest("房源图片不能超过20张");
        }
        urls.forEach(url -> fileRecordService.validateFileOwnership(
                operatorId, url, "house_image"
        ));
        return List.copyOf(urls);
    }

    /** 在房源创建事务中保存封面和普通图片记录。 */
    private void saveHouseImages(
            String houseId,
            String coverImage,
            List<String> imageUrls,
            LocalDateTime createdAt
    ) {
        List<HouseImage> images = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            String imageUrl = imageUrls.get(index);
            HouseImage image = new HouseImage();
            image.setId(UUID.randomUUID().toString());
            image.setHouseId(houseId);
            image.setImageUrl(imageUrl);
            image.setImageType(imageUrl.equals(coverImage) ? "cover" : "normal");
            image.setSortOrder(index);
            image.setCreatedAt(createdAt);
            images.add(image);
        }
        if (!imageService.saveBatch(images)) {
            throw new IllegalStateException("房源图片保存失败");
        }
    }

    /** 规范化创建请求中的设施或标签 ID，并自动去重。 */
    private List<String> normalizeAttributeIds(List<String> ids, String label) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("房源" + label + "不能为空");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw BusinessException.badRequest(label + "ID不能为空");
            }
            normalized.add(id.trim());
        }
        return List.copyOf(normalized);
    }

    /** 创建房源前校验全部设施存在且已启用。 */
    private void validateEnabledFacilities(List<String> facilityIds) {
        if (facilityIds.isEmpty()) {
            return;
        }
        Set<String> validIds = facilityService.list(
                        Wrappers.<HouseFacility>lambdaQuery()
                                .in(HouseFacility::getId, facilityIds)
                                .eq(HouseFacility::getEnabled, 1)
                ).stream()
                .map(HouseFacility::getId)
                .collect(Collectors.toSet());
        List<String> invalidIds = facilityIds.stream()
                .filter(id -> !validIds.contains(id))
                .toList();
        if (!invalidIds.isEmpty()) {
            throw BusinessException.badRequest("设施不存在或已停用: " + String.join(", ", invalidIds));
        }
    }

    /** 创建房源前校验全部标签存在且已启用。 */
    private void validateEnabledTags(List<String> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        Set<String> validIds = tagService.list(
                        Wrappers.<HouseTag>lambdaQuery()
                                .in(HouseTag::getId, tagIds)
                                .eq(HouseTag::getEnabled, 1)
                ).stream()
                .map(HouseTag::getId)
                .collect(Collectors.toSet());
        List<String> invalidIds = tagIds.stream()
                .filter(id -> !validIds.contains(id))
                .toList();
        if (!invalidIds.isEmpty()) {
            throw BusinessException.badRequest("标签不存在或已停用: " + String.join(", ", invalidIds));
        }
    }

    /** 在房源创建事务中保存设施关联。 */
    private void saveHouseFacilityRelations(String houseId, List<String> facilityIds) {
        List<HouseFacilityRelation> relations = facilityIds.stream().map(facilityId -> {
            HouseFacilityRelation relation = new HouseFacilityRelation();
            relation.setId(UUID.randomUUID().toString());
            relation.setHouseId(houseId);
            relation.setFacilityId(facilityId);
            return relation;
        }).toList();
        if (!facilityRelationService.saveBatch(relations)) {
            throw new IllegalStateException("房源设施关联保存失败");
        }
    }

    /** 若请求传入了坐标，则在事务中保存前端传入的完整位置信息。 */
    private void saveHouseLocation(
            String houseId,
            BigDecimal longitude,
            BigDecimal latitude,
            String province,
            String city,
            String district,
            String township,
            String neighborhood,
            String address,
            LocalDateTime createdAt
    ) {
        if (longitude == null || latitude == null) {
            return;
        }
        HouseLocation location = new HouseLocation();
        location.setId(UUID.randomUUID().toString());
        location.setHouseId(houseId);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setProvince(province == null ? "" : province);
        location.setCity(city == null ? "" : city);
        location.setDistrict(district == null ? "" : district);
        location.setTownship(township == null ? "" : township);
        location.setNeighborhood(neighborhood == null ? "" : neighborhood);
        location.setAddress(address == null ? "" : address);
        location.setCreatedAt(createdAt);
        location.setUpdatedAt(createdAt);
        houseLocationMapper.insert(location);
    }

    /** 查询房源位置信息，无记录时返回 null。 */
    private HouseLocation findHouseLocation(String houseId) {
        return houseLocationMapper.selectOne(
                Wrappers.<HouseLocation>lambdaQuery().eq(HouseLocation::getHouseId, houseId),
                false
        );
    }

    /** 从 house_location 表拼接五级街道信息。无记录或为空时回退到 house_location.address → house.location。 */
    private String buildLocationDisplay(HouseLocation loc, House house) {
        String result = "";
        if (loc != null) {
            List<String> parts = new ArrayList<>();
            addNotEmpty(parts, loc.getProvince());
            addNotEmpty(parts, loc.getCity());
            addNotEmpty(parts, loc.getDistrict());
            addNotEmpty(parts, loc.getTownship());
            addNotEmpty(parts, loc.getNeighborhood());
            result = parts.isEmpty() ? "" : String.join("", parts);
        }
        if (result.isEmpty() && loc != null && loc.getAddress() != null && !loc.getAddress().isBlank()) {
            result = loc.getAddress();
        }
        if (result.isEmpty() && house != null && house.getLocation() != null && !house.getLocation().isBlank()) {
            result = house.getLocation();
        }
        return result;
    }

    private void addNotEmpty(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    /** 更新房源位置信息：删除旧记录后写入前端传入的完整位置信息。 */
    private void updateHouseLocation(
            String houseId,
            BigDecimal longitude,
            BigDecimal latitude,
            String province,
            String city,
            String district,
            String township,
            String neighborhood,
            String address,
            LocalDateTime now
    ) {
        houseLocationMapper.delete(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<HouseLocation>lambdaQuery()
                        .eq(HouseLocation::getHouseId, houseId)
        );
        saveHouseLocation(houseId, longitude, latitude, province, city, district,
                township, neighborhood, address, now);
    }

    /** 在房源创建事务中保存标签关联。 */
    private void saveHouseTagRelations(String houseId, List<String> tagIds) {
        List<HouseTagRelation> relations = tagIds.stream().map(tagId -> {
            HouseTagRelation relation = new HouseTagRelation();
            relation.setId(UUID.randomUUID().toString());
            relation.setHouseId(houseId);
            relation.setTagId(tagId);
            return relation;
        }).toList();
        if (!tagRelationService.saveBatch(relations)) {
            throw new IllegalStateException("房源标签关联保存失败");
        }
    }

    /**
     * 发布房源（草稿 → 可租）。
     */
    @Override
    public AdminHouseDtos.AdminHouseView publishHouse(String houseId) {
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        if (!"draft".equals(house.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的房源才能发布，当前状态：" + house.getStatus());
        }
        house.setStatus("available");
        house.setUpdatedAt(LocalDateTime.now());
        updateById(house);
        return toAdminHouseView(house, null, findHouseLocation(house.getId()));
    }

    /**
     * 下架房源（可租/草稿 → 下架）。
     */
    @Override
    public AdminHouseDtos.AdminHouseView offlineHouse(String houseId) {
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        if ("offline".equals(house.getStatus())) {
            throw BusinessException.badRequest("房源已处于下架状态");
        }
        if ("rented".equals(house.getStatus()) || "reserved".equals(house.getStatus())) {
            throw BusinessException.badRequest("已出租或已被预定的房源不能下架，当前状态：" + house.getStatus());
        }
        house.setStatus("offline");
        house.setUpdatedAt(LocalDateTime.now());
        updateById(house);
        return toAdminHouseView(house, null, findHouseLocation(house.getId()));
    }

    /**
     * 重新上架房源（下架 → 可租）。
     */
    @Override
    public AdminHouseDtos.AdminHouseView onlineHouse(String houseId) {
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        if (!"offline".equals(house.getStatus())) {
            throw BusinessException.badRequest("只有已下架房源才能重新上架，当前状态：" + house.getStatus());
        }
        house.setStatus("available");
        house.setUpdatedAt(LocalDateTime.now());
        updateById(house);
        return toAdminHouseView(house, smartLockMapper.selectLatestByHouseId(houseId), findHouseLocation(houseId));
    }

    /**
     * 修改房源信息。
     */
    @Override
    @Transactional
    public AdminHouseDtos.AdminHouseView updateHouse(
            String houseId,
            AdminHouseDtos.UpdateHouseRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.title() != null) {
            house.setTitle(request.title());
        }
        if (request.location() != null) {
            house.setLocation(request.location());
        }
        if (request.communityId() != null) {
            house.setCommunityId(request.communityId());
        }
        if (request.address() != null) {
            house.setAddress(request.address());
        }
        if (request.building() != null) {
            house.setBuilding(request.building());
        }
        if (request.unit() != null) {
            house.setUnit(request.unit());
        }
        if (request.room() != null) {
            house.setRoom(request.room());
        }
        if (request.price() != null) {
            house.setPrice(request.price());
        }
        if (request.deposit() != null) {
            house.setDeposit(request.deposit());
        }
        if (request.paymentMethod() != null) {
            house.setPaymentMethod(request.paymentMethod());
        }
        if (request.roomType() != null) {
            house.setRoomType(request.roomType());
        }
        if (request.area() != null) {
            house.setArea(request.area());
        }
        if (request.floor() != null) {
            house.setFloor(request.floor());
        }
        if (request.orientation() != null) {
            house.setOrientation(request.orientation());
        }
        if (request.decoration() != null) {
            house.setDecoration(request.decoration());
        }
        if (request.availableDate() != null) {
            house.setAvailableDate(request.availableDate());
        }
        if (request.metro() != null) {
            house.setMetro(request.metro());
        }
        if (request.description() != null) {
            house.setDescription(request.description());
        }
        if (request.rentType() != null) {
            house.setRentType(request.rentType());
        }
        if (request.isSmartLockSupported() != null) {
            house.setIsSmartLockSupported(request.isSmartLockSupported() ? 1 : 0);
        }
        if (request.isSelfViewingSupported() != null) {
            house.setIsSelfViewingSupported(request.isSelfViewingSupported() ? 1 : 0);
        }
        if (request.landlordId() != null) {
            house.setLandlordId(request.landlordId());
        }
        if (request.facilityIds() != null) {
            List<String> facilityIds = normalizeOptionalAttributeIds(request.facilityIds(), "设施");
            validateEnabledFacilities(facilityIds);
            replaceHouseFacilityRelations(houseId, facilityIds);
        }
        if (request.tagIds() != null) {
            List<String> tagIds = normalizeOptionalAttributeIds(request.tagIds(), "标签");
            validateEnabledTags(tagIds);
            replaceHouseTagRelations(houseId, tagIds);
        }
        if (request.coverImage() != null || request.imageUrls() != null) {
            String coverImage = request.coverImage() == null
                    ? house.getCoverImage()
                    : requireImageUrl(request.coverImage(), "封面图不能为空");
            LinkedHashSet<String> requestedUrls = new LinkedHashSet<>();
            if (request.coverImage() != null) {
                requestedUrls.add(coverImage);
            }
            if (request.imageUrls() != null) {
                for (String imageUrl : request.imageUrls()) {
                    requestedUrls.add(requireImageUrl(imageUrl, "房源图片URL不能为空"));
                }
            }
            if (requestedUrls.size() > 20) {
                throw BusinessException.badRequest("单次新增房源图片不能超过20张");
            }
            // 全量替换：删除旧图片，写入新图片
            replaceHouseImages(houseId, coverImage, requestedUrls, now);
            house.setCoverImage(coverImage);
        }
        if (request.longitude() != null && request.latitude() != null) {
            updateHouseLocation(houseId, request.longitude(), request.latitude(),
                    request.province(), request.city(), request.district(),
                    request.township(), request.neighborhood(), request.address(), now);
        }
        house.setUpdatedAt(now);
        updateById(house);
        return toAdminHouseView(house, null, findHouseLocation(house.getId()));
    }

    /** 规范化修改请求中的设施或标签 ID；空数组表示清空关联。 */
    private List<String> normalizeOptionalAttributeIds(List<String> ids, String label) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw BusinessException.badRequest(label + "ID不能为空");
            }
            normalized.add(id.trim());
        }
        return List.copyOf(normalized);
    }

    /** 完整替换房源设施关联。 */
    private void replaceHouseFacilityRelations(String houseId, List<String> facilityIds) {
        facilityRelationService.remove(
                Wrappers.<HouseFacilityRelation>lambdaQuery().eq(HouseFacilityRelation::getHouseId, houseId)
        );
        if (!facilityIds.isEmpty()) {
            saveHouseFacilityRelations(houseId, facilityIds);
        }
    }

    /** 完整替换房源标签关联。 */
    private void replaceHouseTagRelations(String houseId, List<String> tagIds) {
        tagRelationService.remove(
                Wrappers.<HouseTagRelation>lambdaQuery().eq(HouseTagRelation::getHouseId, houseId)
        );
        if (!tagIds.isEmpty()) {
            saveHouseTagRelations(houseId, tagIds);
        }
    }

    /** 校验并规范化图片 URL。 */
    private String requireImageUrl(String imageUrl, String message) {
        if (!StringUtils.hasText(imageUrl)) {
            throw BusinessException.badRequest(message);
        }
        return imageUrl.trim();
    }

    /** 新图片增量入库，保留房源已有图片记录，并避免重复保存相同 URL。 */
    /** 全量替换房源图片：删除旧图片记录，写入新图片。 */
    private void replaceHouseImages(
            String houseId,
            String coverImage,
            Collection<String> requestedUrls,
            LocalDateTime createdAt
    ) {
        imageService.remove(Wrappers.<HouseImage>lambdaQuery().eq(HouseImage::getHouseId, houseId));
        saveHouseImages(houseId, coverImage, new ArrayList<>(requestedUrls), createdAt);
    }

    private void appendHouseImages(
            String houseId,
            String coverImage,
            Collection<String> requestedUrls,
            String operatorId,
            LocalDateTime createdAt
    ) {
        List<HouseImage> existingImages = imageService.list(
                Wrappers.<HouseImage>lambdaQuery()
                        .eq(HouseImage::getHouseId, houseId)
                        .orderByAsc(HouseImage::getSortOrder)
        );
        Set<String> existingUrls = existingImages.stream()
                .map(HouseImage::getImageUrl)
                .collect(Collectors.toSet());
        List<String> newUrls = requestedUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .toList();
        newUrls.forEach(url -> fileRecordService.validateFileOwnership(operatorId, url, "house_image"));
        if (newUrls.isEmpty()) {
            return;
        }
        int nextSortOrder = existingImages.stream()
                .map(HouseImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        List<HouseImage> newImages = new ArrayList<>();
        for (String imageUrl : newUrls) {
            HouseImage image = new HouseImage();
            image.setId(UUID.randomUUID().toString());
            image.setHouseId(houseId);
            image.setImageUrl(imageUrl);
            image.setImageType(imageUrl.equals(coverImage) ? "cover" : "normal");
            image.setSortOrder(nextSortOrder++);
            image.setCreatedAt(createdAt);
            newImages.add(image);
        }
        if (!imageService.saveBatch(newImages)) {
            throw new IllegalStateException("房源图片保存失败");
        }
    }

    /**
     * 获取所有房源（含智能锁绑定信息）。
     */
    @Override
    public List<AdminHouseDtos.AdminHouseView> getAllHousesWithLockInfo() {
        List<House> houses = list(Wrappers.<House>lambdaQuery().orderByDesc(House::getCreatedAt));
        List<String> houseIds = houses.stream().map(House::getId).toList();
        Map<String, SmartLock> smartLockMap = smartLockMapper.selectLatestByHouseIds(houseIds)
                .stream()
                .collect(Collectors.toMap(SmartLock::getHouseId, lock -> lock, (latest, ignored) -> latest));
        Map<String, HouseLocation> locationMap = houseLocationMapper.selectList(
                        Wrappers.<HouseLocation>lambdaQuery().in(HouseLocation::getHouseId, houseIds)
                ).stream()
                .collect(Collectors.toMap(HouseLocation::getHouseId, loc -> loc, (latest, ignored) -> latest));
        return houses.stream()
                .map(house -> toAdminHouseView(house,
                        smartLockMap.get(house.getId()),
                        locationMap.get(house.getId())))
                .toList();
    }

    /**
     * 根据房源 ID 获取管理端房源详情。
     */
    @Override
    public AdminHouseDtos.AdminHouseView getAdminHouseById(String houseId) {
        House house = getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        SmartLock smartLock = smartLockMapper.selectLatestByHouseId(houseId);
        return toAdminHouseView(house, smartLock, findHouseLocation(houseId));
    }

    /**
     * 将房源实体及门锁信息转换为管理端视图。
     */
    private AdminHouseDtos.AdminHouseView toAdminHouseView(House house, SmartLock smartLock,
                                                          HouseLocation houseLocation) {
        AdminHouseDtos.LockDeviceView lockDeviceView = null;
        boolean smartLockBound = StringUtils.hasText(house.getSmartLockId())
                || (StringUtils.hasText(house.getLockBindStatus())
                && !"UNBOUND".equals(house.getLockBindStatus()));
        if (smartLock != null) {
            smartLockBound = true;
            lockDeviceView = new AdminHouseDtos.LockDeviceView(
                    smartLock.getId(),
                    smartLock.getLockName(),
                    "TTLock",
                    smartLock.getLockMac(),
                    smartLock.getStatus(),
                    smartLock.getBattery()
            );
        }
        return new AdminHouseDtos.AdminHouseView(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                getHouseImageUrls(house.getId()),
                buildLocationDisplay(houseLocation, house),
                house.getCommunityId(),
                house.getAddress(),
                house.getBuilding(),
                house.getUnit(),
                house.getRoom(),
                house.getPrice(),
                house.getDeposit(),
                house.getPaymentMethod(),
                house.getRoomType(),
                house.getArea(),
                house.getFloor(),
                house.getOrientation(),
                house.getDecoration(),
                house.getAvailableDate(),
                house.getMetro(),
                house.getDescription(),
                house.getRentType(),
                house.getStatus(),
                integerBoolean(house.getIsSmartLockSupported()),
                integerBoolean(house.getIsSelfViewingSupported()),
                smartLockBound,
                lockDeviceView,
                house.getLandlordId(),
                house.getViewCount(),
                house.getFavoriteCount(),
                house.getCreatedAt(),
                house.getUpdatedAt(),
                houseLocation == null ? null : houseLocation.getLongitude(),
                houseLocation == null ? null : houseLocation.getLatitude()
        );
    }

    /** 按展示顺序查询房源图片 URL。 */
    private List<String> getHouseImageUrls(String houseId) {
        return imageService.list(
                        Wrappers.<HouseImage>lambdaQuery()
                                .eq(HouseImage::getHouseId, houseId)
                                .orderByAsc(HouseImage::getSortOrder)
                ).stream()
                .map(HouseImage::getImageUrl)
                .toList();
    }

    /**
     * 将面积数值转换为整数。
     */
    private Integer areaAsInteger(BigDecimal area) {
        return area == null ? 0 : area.intValue();
    }

    @Override
    public List<HouseDtos.HotCommunityItem> getHotCommunities(int limit) {
        List<Map<String, Object>> raw = baseMapper.selectMaps(
                Wrappers.<House>lambdaQuery()
                        .isNotNull(House::getCommunityId)
                        .ne(House::getCommunityId, "")
                        .apply("community_id IN (SELECT id FROM community)")
                        .groupBy(House::getCommunityId)
                        .last("ORDER BY COUNT(1) DESC LIMIT " + limit)
        );
        if (raw.isEmpty()) return List.of();

        List<String> topIds = raw.stream()
                .map(m -> m.get("community_id").toString())
                .toList();

        Map<String, String> names = communityService.listByIds(topIds).stream()
                .collect(Collectors.toMap(Community::getId, Community::getName, (a, b) -> a));

        Map<String, String> regionNames = communityService.listByIds(topIds).stream()
                .map(Community::getRegionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()).stream()
                .flatMap(rid -> regionService.listByIds(List.of(rid)).stream())
                .collect(Collectors.toMap(Region::getId, Region::getName, (a, b) -> a));

        List<HouseDtos.HotCommunityItem> items = new ArrayList<>();
        for (String cid : topIds) {
            String name = names.getOrDefault(cid, "");
            Community comm = communityService.getById(cid);
            String district = comm != null && comm.getRegionId() != null
                    ? regionNames.getOrDefault(comm.getRegionId(), "")
                    : "";

            List<House> houses = list(Wrappers.<House>lambdaQuery()
                    .select(House::getPrice)
                    .eq(House::getCommunityId, cid)
                    .orderByAsc(House::getPrice)
                    .last("LIMIT 1"));
            int minPrice = houses.isEmpty() || houses.get(0).getPrice() == null
                    ? 0 : houses.get(0).getPrice() / 100;

            int colorValue = 0xFF000000 | (Math.abs(cid.hashCode()) & 0x00FFFFFF);

            items.add(new HouseDtos.HotCommunityItem(name, district, minPrice, colorValue));
        }
        return items;
    }
}




