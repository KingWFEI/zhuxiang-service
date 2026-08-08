package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.dto.HomeDtos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeDataContractTests {

    @Test
    void homeDataOnlyExposesTabsAndHouseGroups() {
        HomeDtos.HomeData data = new HomeDtos.HomeData(
                List.of(new HomeDtos.Tab("recommended", "推荐", 1, true)),
                Map.of()
        );

        var json = new ObjectMapper().valueToTree(data);

        assertThat(json.size()).isEqualTo(2);
        assertThat(json.has("tabs")).isTrue();
        assertThat(json.has("houseGroups")).isTrue();
    }
}
