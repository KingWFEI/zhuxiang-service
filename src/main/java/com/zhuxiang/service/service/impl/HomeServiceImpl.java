package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HomeDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.Advertisement;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.HomeService;
import com.zhuxiang.service.service.HouseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页聚合数据服务实现。
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final List<HomeDtos.Tab> TABS = List.of(
            new HomeDtos.Tab("recommended", "推荐", 1, true),
            new HomeDtos.Tab("short_rent", "短租", 2, true),
            new HomeDtos.Tab("long_rent", "长租", 3, true)
    );

    private final HouseService houseService;
    private final AdvertisementService advertisementService;

    public HomeServiceImpl(
            HouseService houseService,
            AdvertisementService advertisementService
    ) {
        this.houseService = houseService;
        this.advertisementService = advertisementService;
    }

    /** 聚合首页标签与房源流；广告仅以信息流条目形式返回。 */
    @Override
    public HomeDtos.HomeData getHomeData(
            String cityCode,
            String region,
            Double latitude,
            Double longitude,
            long pageSize,
            String userId
    ) {
        Advertisement feedAdvertisement = getActiveFeedAdvertisement();
        return new HomeDtos.HomeData(
                TABS,
                buildHouseGroups(region, pageSize, userId, feedAdvertisement)
        );
    }

    /**
     * 查询并组装全部启用栏目的首批房源。
     */
    private Map<String, HouseDtos.FeedData> buildHouseGroups(
            String region,
            long pageSize,
            String userId,
            Advertisement feedAdvertisement
    ) {
        Map<String, HouseDtos.FeedData> groups = new LinkedHashMap<>();
        for (HomeDtos.Tab tab : TABS) {
            PageData<HouseDtos.HouseView> houses = houseService.searchHouses(
                    null, tab.key(), null, null, region, null, null, null,
                    null, null, null, null, null, null, "default", 1, pageSize, userId
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
    private Advertisement getActiveFeedAdvertisement() {
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

}
