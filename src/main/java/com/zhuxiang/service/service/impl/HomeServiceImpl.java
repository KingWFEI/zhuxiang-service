package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HomeDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.Advertisement;
import com.zhuxiang.service.entity.AppUser;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.AppUserService;
import com.zhuxiang.service.service.HomeService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页聚合数据服务实现。
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final String DEFAULT_CITY_NAME = "重庆";
    private static final List<HomeDtos.ServiceEntry> SERVICE_ENTRIES = List.of(
            new HomeDtos.ServiceEntry(
                    "lease", "我的租约", "lease", "route", "lease", true, true
            ),
            new HomeDtos.ServiceEntry(
                    "unlock_record", "开门记录", "lock", "route", "unlock_records", true, true
            ),
            new HomeDtos.ServiceEntry(
                    "repair", "报修服务", "repair", "route", "repairs", true, true
            ),
            new HomeDtos.ServiceEntry(
                    "customer_service", "在线客服", "service", "route",
                    "customer_service", false, true
            )
    );
    private static final List<HomeDtos.Tab> TABS = List.of(
            new HomeDtos.Tab("recommended", "推荐", 1, true),
            new HomeDtos.Tab("short_rent", "短租", 2, true),
            new HomeDtos.Tab("homestay", "民宿", 3, true),
            new HomeDtos.Tab("long_rent", "长租", 4, true)
    );

    private final HouseService houseService;
    private final MessageService messageService;
    private final AppUserService appUserService;
    private final AdvertisementService advertisementService;
    private final RegionService regionService;

    public HomeServiceImpl(
            HouseService houseService,
            MessageService messageService,
            AppUserService appUserService,
            AdvertisementService advertisementService,
            RegionService regionService
    ) {
        this.houseService = houseService;
        this.messageService = messageService;
        this.appUserService = appUserService;
        this.advertisementService = advertisementService;
        this.regionService = regionService;
    }

    /**
     * 聚合首页头部、服务入口、栏目房源和广告数据。
     */
    @Override
    public HomeDtos.HomeData getHomeData(
            String cityCode,
            String region,
            Double latitude,
            Double longitude,
            long pageSize,
            String userId
    ) {
        AppUser user = StringUtils.hasText(userId) ? appUserService.requireActiveUser(userId) : null;
        List<Advertisement> activeAdvertisements = getActiveAdvertisements();
        return new HomeDtos.HomeData(
                buildHeader(cityCode, region, user),
                user == null ? 0 : messageService.getUnreadCounts(userId).total(),
                SERVICE_ENTRIES,
                TABS,
                buildHouseGroups(region, pageSize, userId, activeAdvertisements),
                toBannerViews(activeAdvertisements)
        );
    }

    /**
     * 组装首页头部信息。
     */
    private HomeDtos.Header buildHeader(String cityCode, String region, AppUser user) {
        return new HomeDtos.Header(
                resolveCityName(cityCode, region),
                buildGreeting(user),
                "找到属于你的安心居住空间",
                "搜索小区、地址或房源",
                ""
        );
    }

    /**
     * 根据城市或区域编码解析城市名称。
     */
    private String resolveCityName(String cityCode, String region) {
        Region matched = null;
        if (StringUtils.hasText(cityCode)) {
            matched = findRegion(cityCode);
        }
        if (matched == null && StringUtils.hasText(region)) {
            matched = findRegion(region);
        }
        if (matched == null) {
            return DEFAULT_CITY_NAME;
        }
        Region current = matched;
        while (current != null && !"city".equals(current.getLevel())
                && StringUtils.hasText(current.getParentId())) {
            current = regionService.getById(current.getParentId());
        }
        return current != null && "city".equals(current.getLevel())
                ? current.getName()
                : DEFAULT_CITY_NAME;
    }

    /**
     * 根据编码或名称查询启用的区域。
     */
    private Region findRegion(String value) {
        return regionService.getOne(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getEnabled, 1)
                        .and(query -> query.eq(Region::getCode, value)
                                .or().eq(Region::getName, value))
                        .last("LIMIT 1"),
                false
        );
    }

    /**
     * 根据当前时间和用户昵称生成问候语。
     */
    private String buildGreeting(AppUser user) {
        int hour = LocalTime.now().getHour();
        String greeting;
        if (hour < 6) {
            greeting = "夜深了";
        } else if (hour < 9) {
            greeting = "早安";
        } else if (hour < 12) {
            greeting = "上午好";
        } else if (hour < 18) {
            greeting = "下午好";
        } else {
            greeting = "晚上好";
        }
        return user == null || !StringUtils.hasText(user.getNickname())
                ? greeting
                : greeting + "，" + user.getNickname();
    }

    /**
     * 查询并组装全部启用栏目的首批房源。
     */
    private Map<String, HouseDtos.FeedData> buildHouseGroups(
            String region,
            long pageSize,
            String userId,
            List<Advertisement> activeAdvertisements
    ) {
        Map<String, HouseDtos.FeedData> groups = new LinkedHashMap<>();
        Advertisement feedAdvertisement = activeAdvertisements.stream()
                .filter(item -> "home_feed".equals(item.getPosition()))
                .findFirst()
                .orElse(null);
        for (HomeDtos.Tab tab : TABS) {
            PageData<HouseDtos.HouseView> houses = houseService.searchHouses(
                    null, tab.key(), region, null, null, null,
                    null, null, null, "default", 1, pageSize, userId
            );
            List<HouseDtos.FeedItem> items = new ArrayList<>(
                    houses.items().stream().map(HouseDtos.FeedItem::house).toList()
            );
            if ("recommended".equals(tab.key()) && feedAdvertisement != null && !items.isEmpty()) {
                items.add(Math.min(1, items.size()), HouseDtos.FeedItem.advertisement(
                        toFeedAdvertisement(feedAdvertisement)
                ));
            }
            groups.put(tab.key(), new HouseDtos.FeedData(
                    items, houses.page(), houses.pageSize(), houses.hasMore()
            ));
        }
        return groups;
    }

    /**
     * 查询当前时间有效的首页广告。
     */
    private List<Advertisement> getActiveAdvertisements() {
        LocalDateTime now = LocalDateTime.now();
        return advertisementService.list(
                Wrappers.<Advertisement>lambdaQuery()
                        .eq(Advertisement::getEnabled, 1)
                        .and(query -> query.isNull(Advertisement::getStartTime)
                                .or().le(Advertisement::getStartTime, now))
                        .and(query -> query.isNull(Advertisement::getEndTime)
                                .or().ge(Advertisement::getEndTime, now))
                        .orderByAsc(Advertisement::getSortOrder)
        );
    }

    /**
     * 将信息流广告转换为房源流广告视图。
     */
    private HouseDtos.AdvertisementView toFeedAdvertisement(Advertisement advertisement) {
        return new HouseDtos.AdvertisementView(
                advertisement.getId(),
                advertisement.getTitle(),
                advertisement.getDescription(),
                advertisement.getImageUrl(),
                advertisement.getTargetType(),
                advertisement.getTargetValue()
        );
    }

    /**
     * 将首页 Banner 广告转换为接口视图。
     */
    private List<HomeDtos.Advertisement> toBannerViews(List<Advertisement> advertisements) {
        return advertisements.stream()
                .filter(item -> "home_banner".equals(item.getPosition()))
                .map(item -> new HomeDtos.Advertisement(
                        item.getId(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getImageUrl(),
                        item.getPosition(),
                        item.getTargetType(),
                        item.getTargetValue()
                ))
                .toList();
    }
}
