package com.zhuxiang.service;

import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.dto.RecommendationDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RecommendationEvent;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HouseRecommendationStatsMapper;
import com.zhuxiang.service.mapper.RecommendationEventMapper;
import com.zhuxiang.service.service.impl.RecommendationEventServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationEventServiceTests {

    @Test
    void recordsValidEventAndUpdatesAggregateStats() {
        RecommendationEventMapper eventMapper = mock(RecommendationEventMapper.class);
        HouseRecommendationStatsMapper statsMapper = mock(HouseRecommendationStatsMapper.class);
        HouseMapper houseMapper = mock(HouseMapper.class);
        RecommendationEventServiceImpl service = new RecommendationEventServiceImpl(
                eventMapper, statsMapper, houseMapper
        );
        House house = new House();
        house.setId("house-1");
        when(houseMapper.selectById("house-1")).thenReturn(house);

        RecommendationDtos.BatchEventResult result = service.recordBatch(
                null,
                new RecommendationDtos.BatchEventRequest(
                        "guest-1", "session-1", null,
                        List.of(new RecommendationDtos.EventRequest(
                                "house-1", RecommendationEventType.EXPOSURE,
                                0, 1000L, "home"
                        ))
                )
        );

        assertThat(result.accepted()).isEqualTo(1);
        verify(eventMapper).insert(any(RecommendationEvent.class));
        verify(statsMapper).increment("house-1", "EXPOSURE");
    }
}
