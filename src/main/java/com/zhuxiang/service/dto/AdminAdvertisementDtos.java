package com.zhuxiang.service.dto;

import java.time.LocalDateTime;

public final class AdminAdvertisementDtos {

    private AdminAdvertisementDtos() {
    }

    public record SaveRequest(
            String title,
            String description,
            String imageUrl,
            String imageFileId,
            String targetType,
            String targetValue,
            String position,
            Boolean enabled,
            Integer sortOrder,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }

    public record EnableRequest(Boolean enabled) {
    }

    public record HouseOption(
            String id,
            String title,
            String coverImage,
            String community,
            String location,
            Integer price
    ) {
    }

    public record AdvertisementView(
            String id,
            String title,
            String description,
            String imageUrl,
            String targetType,
            String targetValue,
            String position,
            boolean enabled,
            int sortOrder,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String displayStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
