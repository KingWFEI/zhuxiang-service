package com.zhuxiang.service.dto;

import com.zhuxiang.service.common.RecommendationEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class RecommendationDtos {
    private RecommendationDtos() {
    }

    public record BatchEventRequest(
            @Size(max = 64) String anonymousId,
            @Size(max = 64) String sessionId,
            @Size(max = 64) String requestId,
            @NotEmpty @Size(max = 50) List<@Valid EventRequest> events
    ) {
    }

    public record EventRequest(
            @NotBlank @Size(max = 36) String houseId,
            @NotNull RecommendationEventType eventType,
            @Min(0) @Max(1000) Integer position,
            @Min(0) @Max(86400000) Long durationMs,
            @NotBlank @Size(max = 32) String sourcePage
    ) {
    }

    public record BatchEventResult(int accepted) {
    }
}
