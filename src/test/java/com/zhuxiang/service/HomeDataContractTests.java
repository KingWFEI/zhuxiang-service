package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.dto.HomeDtos;
import com.zhuxiang.service.dto.HouseDtos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeDataContractTests {

    @Test
    void homeDataExposesBannersTabsAndHouseGroups() {
        HomeDtos.HomeData data = new HomeDtos.HomeData(
                List.of(new HomeDtos.Tab("recommended", "推荐", 1, true)),
                List.of(new HouseDtos.AdvertisementView(
                        "banner-1", "首页活动", "活动描述", "品质精选", "/banner.jpg", "none", null
                )),
                Map.of()
        );

        var json = new ObjectMapper().valueToTree(data);

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.has("tabs")).isTrue();
        assertThat(json.path("homeBanners").get(0).path("id").asText()).isEqualTo("banner-1");
        assertThat(json.path("homeBanners").get(0).path("tag").asText()).isEqualTo("品质精选");
        assertThat(json.has("houseGroups")).isTrue();
    }
}
