package com.zhuxiang.service.dto;

import java.util.List;
import java.util.Map;

/**
 * 首页聚合接口数据对象。
 */
public final class HomeDtos {

    private HomeDtos() {
    }

    public record HomeData(
            Header header,
            long unreadMessageCount,
            List<ServiceEntry> serviceEntries,
            List<Tab> tabs,
            Map<String, HouseDtos.FeedData> houseGroups,
            List<Advertisement> advertisements
    ) {
    }

    public record Header(
            String cityName,
            String greeting,
            String subtitle,
            String searchPlaceholder,
            String backgroundImageUrl
    ) {
    }

    public record ServiceEntry(
            String key,
            String title,
            String iconKey,
            String targetType,
            String targetValue,
            boolean requiresLogin,
            boolean enabled
    ) {
    }

    public record Tab(
            String key,
            String title,
            int sort,
            boolean enabled
    ) {
    }

    public record Advertisement(
            String id,
            String title,
            String description,
            String imageUrl,
            String position,
            String targetType,
            String targetValue
    ) {
    }
}
