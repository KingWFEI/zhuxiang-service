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
            List<Tab> tabs,
            Map<String, HouseDtos.FeedData> houseGroups
    ) {
    }

    public record Tab(
            String key,
            String title,
            int sort,
            boolean enabled
    ) {
    }

}
