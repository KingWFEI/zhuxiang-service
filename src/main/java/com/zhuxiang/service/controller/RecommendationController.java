package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.RecommendationDtos;
import com.zhuxiang.service.service.RecommendationEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation")
@Tag(name = "Recommendation", description = "Recommendation behavior collection")
public class RecommendationController {
    private final RecommendationEventService eventService;

    public RecommendationController(RecommendationEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events/batch")
    @Operation(summary = "Record recommendation behavior events")
    public ApiResponse<RecommendationDtos.BatchEventResult> recordEvents(
            @Valid @RequestBody RecommendationDtos.BatchEventRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(eventService.recordBatch(CurrentUser.optionalId(request), body));
    }
}
