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
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.entity.HouseTagRelation;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.entity.LockDevice;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.UserFavoriteHouse;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.HouseFacilityRelationService;
import com.zhuxiang.service.service.HouseFacilityService;
import com.zhuxiang.service.service.HouseImageService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.HouseTagRelationService;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.LockDeviceService;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.mapper.HouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    private final CommunityService communityService;
    private final HouseImageService imageService;
    private final HouseTagService tagService;
    private final HouseTagRelationService tagRelationService;
    private final HouseFacilityService facilityService;
    private final HouseFacilityRelationService facilityRelationService;
    private final LandlordService landlordService;
    private final AdvertisementService advertisementService;
    private final RegionService regionService;
    private final LockDeviceService lockDeviceService;
    private final UserFavoriteHouseMapper favoriteHouseMapper;
    private final RentOrderMapper rentOrderMapper;

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
            LockDeviceService lockDeviceService,
            UserFavoriteHouseMapper favoriteHouseMapper,
            RentOrderMapper rentOrderMapper
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
        this.lockDeviceService = lockDeviceService;
        this.favoriteHouseMapper = favoriteHouseMapper;
        this.rentOrderMapper = rentOrderMapper;
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
                null, category, null, null, null, null, null, null, null,
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
            String sort,
            long page,
            long pageSize,
            String userId
    ) {
        IPage<House> result = queryHouses(
                keyword, category, region, minPrice, maxPrice, roomType,
                minArea, maxArea, facilities, sort, page, pageSize
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
        return new HouseDtos.HouseDetail(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                images.isEmpty() ? List.of(house.getCoverImage()) : images,
                house.getLocation(),
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
                rent.activeOrderBelongsToMe()
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
                                .eq(House::getStatus, "available")
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
                        .in(RentOrder::getStatus, "pendingRealName", "pendingContract", "pendingPayment", "pendingSign")
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
        return new HouseDtos.HouseView(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                house.getLocation(),
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
                .eq(House::getStatus, "available")
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
    public AdminHouseDtos.AdminHouseView createHouse(AdminHouseDtos.CreateHouseRequest request) {
        LocalDateTime now = LocalDateTime.now();
        House house = new House();
        house.setId(UUID.randomUUID().toString());
        house.setTitle(request.title());
        house.setCoverImage(request.coverImage());
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
        return toAdminHouseView(house, null);
    }

    /**
     * 获取所有房源（含智能锁绑定信息）。
     */
    @Override
    public List<AdminHouseDtos.AdminHouseView> getAllHousesWithLockInfo() {
        List<House> houses = list(Wrappers.<House>lambdaQuery().orderByDesc(House::getCreatedAt));
        List<String> houseIds = houses.stream().map(House::getId).toList();
        Map<String, LockDevice> lockDeviceMap = houseIds.isEmpty()
                ? Map.of()
                : lockDeviceService.list(
                        Wrappers.<LockDevice>lambdaQuery().in(LockDevice::getHouseId, houseIds)
                ).stream().collect(Collectors.toMap(LockDevice::getHouseId, d -> d, (a, b) -> a));
        return houses.stream()
                .map(house -> toAdminHouseView(house, lockDeviceMap.get(house.getId())))
                .toList();
    }

    /**
     * 将房源实体及门锁信息转换为管理端视图。
     */
    private AdminHouseDtos.AdminHouseView toAdminHouseView(House house, LockDevice lockDevice) {
        AdminHouseDtos.LockDeviceView lockDeviceView = null;
        boolean smartLockBound = StringUtils.hasText(house.getSmartLockId())
                || (StringUtils.hasText(house.getLockBindStatus())
                && !"UNBOUND".equals(house.getLockBindStatus()));
        if (lockDevice != null) {
            smartLockBound = true;
            lockDeviceView = new AdminHouseDtos.LockDeviceView(
                    lockDevice.getId(),
                    lockDevice.getLockName(),
                    lockDevice.getLockBrand(),
                    lockDevice.getLockSn(),
                    lockDevice.getStatus(),
                    lockDevice.getBatteryLevel()
            );
        }
        return new AdminHouseDtos.AdminHouseView(
                house.getId(),
                house.getTitle(),
                house.getCoverImage(),
                house.getLocation(),
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
                house.getUpdatedAt()
        );
    }

    /**
     * 将面积数值转换为整数。
     */
    private Integer areaAsInteger(BigDecimal area) {
        return area == null ? 0 : area.intValue();
    }
}




