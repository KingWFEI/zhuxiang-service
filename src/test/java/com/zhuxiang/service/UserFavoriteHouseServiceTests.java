package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.UserFavoriteHouse;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.RecommendationEventService;
import com.zhuxiang.service.service.impl.UserFavoriteHouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFavoriteHouseServiceTests {

    private final HouseService houseService = mock(HouseService.class);
    private final RecommendationEventService recommendationEventService =
            mock(RecommendationEventService.class);
    private final UserFavoriteHouseMapper favoriteHouseMapper =
            mock(UserFavoriteHouseMapper.class);

    private UserFavoriteHouseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserFavoriteHouseServiceImpl(houseService, recommendationEventService);
        ReflectionTestUtils.setField(service, "baseMapper", favoriteHouseMapper);
    }

    @Test
    void unfavoriteAllowsRemovingAnOfflineHouse() {
        House offlineHouse = new House();
        offlineHouse.setId("house-offline");
        offlineHouse.setStatus("OFFLINE");
        offlineHouse.setFavoriteCount(3);

        when(houseService.requireAvailableHouse("house-offline"))
                .thenThrow(new AssertionError("unfavorite must not require an available house"));
        when(houseService.getById("house-offline")).thenReturn(offlineHouse);
        when(favoriteHouseMapper.delete(any(Wrapper.class))).thenReturn(1);

        HouseDtos.FavoriteResult result = service.unfavorite("user-1", "house-offline");

        assertThat(result.isFavorite()).isFalse();
        assertThat(offlineHouse.getFavoriteCount()).isEqualTo(2);
        verify(houseService).updateById(offlineHouse);
        verify(recommendationEventService).recordSystemEvent(
                "user-1",
                "house-offline",
                RecommendationEventType.UNFAVORITE
        );
    }
}
