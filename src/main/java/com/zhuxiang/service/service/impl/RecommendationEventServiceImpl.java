package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.dto.RecommendationDtos;
import com.zhuxiang.service.entity.RecommendationEvent;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HouseRecommendationStatsMapper;
import com.zhuxiang.service.mapper.RecommendationEventMapper;
import com.zhuxiang.service.service.RecommendationEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationEventServiceImpl implements RecommendationEventService {
    private final RecommendationEventMapper eventMapper;
    private final HouseRecommendationStatsMapper statsMapper;
    private final HouseMapper houseMapper;

    public RecommendationEventServiceImpl(
            RecommendationEventMapper eventMapper,
            HouseRecommendationStatsMapper statsMapper,
            HouseMapper houseMapper
    ) {
        this.eventMapper = eventMapper;
        this.statsMapper = statsMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    @Transactional
    public RecommendationDtos.BatchEventResult recordBatch(
            String userId,
            RecommendationDtos.BatchEventRequest request
    ) {
        if (!StringUtils.hasText(userId) && !StringUtils.hasText(request.anonymousId())) {
            throw BusinessException.badRequest("userId and anonymousId cannot both be empty");
        }
        int accepted = 0;
        for (RecommendationDtos.EventRequest event : request.events()) {
            if (houseMapper.selectById(event.houseId()) == null) {
                continue;
            }
            insertEvent(
                    userId,
                    request.anonymousId(),
                    request.sessionId(),
                    request.requestId(),
                    event.houseId(),
                    event.eventType(),
                    event.position(),
                    event.durationMs(),
                    event.sourcePage()
            );
            accepted++;
        }
        return new RecommendationDtos.BatchEventResult(accepted);
    }

    @Override
    @Transactional
    public void recordSystemEvent(
            String userId,
            String houseId,
            RecommendationEventType eventType
    ) {
        insertEvent(userId, null, null, null, houseId, eventType, null, null, "system");
    }

    @Override
    public List<RecommendationEvent> recentUserEvents(String userId, int limit) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return eventMapper.selectList(
                Wrappers.<RecommendationEvent>lambdaQuery()
                        .eq(RecommendationEvent::getUserId, userId)
                        .ge(RecommendationEvent::getCreatedAt, LocalDateTime.now().minusDays(90))
                        .orderByDesc(RecommendationEvent::getCreatedAt)
                        .last("LIMIT " + safeLimit)
        );
    }

    private void insertEvent(
            String userId,
            String anonymousId,
            String sessionId,
            String requestId,
            String houseId,
            RecommendationEventType eventType,
            Integer position,
            Long durationMs,
            String sourcePage
    ) {
        RecommendationEvent event = new RecommendationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setAnonymousId(anonymousId);
        event.setSessionId(sessionId);
        event.setRequestId(requestId);
        event.setHouseId(houseId);
        event.setEventType(eventType.name());
        event.setPositionIndex(position);
        event.setDurationMs(durationMs);
        event.setSourcePage(sourcePage);
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
        statsMapper.increment(houseId, eventType.name());
    }
}
