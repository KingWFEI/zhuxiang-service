package com.zhuxiang.service.service;

import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.dto.RecommendationDtos;
import com.zhuxiang.service.entity.RecommendationEvent;

import java.util.List;

public interface RecommendationEventService {
    RecommendationDtos.BatchEventResult recordBatch(
            String userId,
            RecommendationDtos.BatchEventRequest request
    );

    void recordSystemEvent(String userId, String houseId, RecommendationEventType eventType);

    List<RecommendationEvent> recentUserEvents(String userId, int limit);
}
