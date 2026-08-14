package com.zhuxiang.service;

import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseRecommendationStats;
import com.zhuxiang.service.entity.RecommendationEvent;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HouseRecommendationStatsMapper;
import com.zhuxiang.service.service.RecommendationEventService;
import com.zhuxiang.service.service.impl.RecommendationRankingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationRankingServiceTests {

    @Test
    void ranksHouseMatchingRecentUserPreferenceFirst() {
        RecommendationEventService eventService = mock(RecommendationEventService.class);
        HouseRecommendationStatsMapper statsMapper = mock(HouseRecommendationStatsMapper.class);
        HouseMapper houseMapper = mock(HouseMapper.class);
        RecommendationRankingServiceImpl service = new RecommendationRankingServiceImpl(
                eventService, statsMapper, houseMapper
        );

        House historyHouse = house("history", "North", "community-history", "One bedroom", 2000);
        House matching = house("matching", "North", "community-a", "One bedroom", 2100);
        House different = house("different", "South", "community-b", "Three bedrooms", 6000);
        RecommendationEvent click = event("history", RecommendationEventType.EFFECTIVE_VIEW);

        when(eventService.recentUserEvents("user-1", 300)).thenReturn(List.of(click));
        when(houseMapper.selectBatchIds(any())).thenReturn(List.of(historyHouse));
        when(statsMapper.selectBatchIds(any())).thenReturn(List.of(
                neutralStats("matching"), neutralStats("different")
        ));

        List<House> ranked = service.rank(List.of(different, matching), "user-1");

        assertThat(ranked).extracting(House::getId).containsExactly("matching", "different");
    }

    @Test
    void repeatedExposureWithoutPositiveActionLowersHouse() {
        RecommendationEventService eventService = mock(RecommendationEventService.class);
        HouseRecommendationStatsMapper statsMapper = mock(HouseRecommendationStatsMapper.class);
        HouseMapper houseMapper = mock(HouseMapper.class);
        RecommendationRankingServiceImpl service = new RecommendationRankingServiceImpl(
                eventService, statsMapper, houseMapper
        );

        House repeated = house("repeated", "North", "community-a", "One bedroom", 2000);
        House fresh = house("fresh", "North", "community-b", "One bedroom", 2000);
        List<RecommendationEvent> exposures = List.of(
                event("repeated", RecommendationEventType.EXPOSURE),
                event("repeated", RecommendationEventType.EXPOSURE),
                event("repeated", RecommendationEventType.EXPOSURE)
        );
        when(eventService.recentUserEvents("user-1", 300)).thenReturn(exposures);
        when(statsMapper.selectBatchIds(any())).thenReturn(List.of(
                neutralStats("repeated"), neutralStats("fresh")
        ));

        List<House> ranked = service.rank(List.of(repeated, fresh), "user-1");

        assertThat(ranked.getFirst().getId()).isEqualTo("fresh");
    }

    private RecommendationEvent event(String houseId, RecommendationEventType type) {
        RecommendationEvent event = new RecommendationEvent();
        event.setHouseId(houseId);
        event.setEventType(type.name());
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private House house(String id, String location, String community, String roomType, int price) {
        House house = new House();
        house.setId(id);
        house.setLocation(location);
        house.setCommunityId(community);
        house.setLandlordId("landlord-" + id);
        house.setRoomType(roomType);
        house.setRentMode("WHOLE_RENT");
        house.setRentType("LONG_RENT");
        house.setPrice(price);
        house.setFavoriteCount(0);
        house.setCreatedAt(LocalDateTime.now().minusDays(2));
        return house;
    }

    private HouseRecommendationStats neutralStats(String houseId) {
        HouseRecommendationStats stats = new HouseRecommendationStats();
        stats.setHouseId(houseId);
        stats.setExposureCount(100L);
        stats.setDetailClickCount(8L);
        stats.setFavoriteCount(2L);
        stats.setAppointmentCount(1L);
        return stats;
    }
}
