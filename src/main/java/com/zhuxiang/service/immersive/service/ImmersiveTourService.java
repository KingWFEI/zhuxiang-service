package com.zhuxiang.service.immersive.service;

import com.zhuxiang.service.immersive.dto.request.CreateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.response.AdminImmersiveTourDetailResponse;
import com.zhuxiang.service.immersive.dto.response.AvailabilityResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveTourSummaryResponse;

public interface ImmersiveTourService {
    ImmersiveTourSummaryResponse create(String houseId, CreateImmersiveTourRequest request, String userId);
    ImmersiveTourSummaryResponse getByHouseId(String houseId);
    AdminImmersiveTourDetailResponse getDetail(String tourId);
    void update(String tourId, UpdateImmersiveTourRequest request, String userId);
    void delete(String tourId, String userId);
    ImmersiveTourSummaryResponse publish(String tourId);
    ImmersiveTourSummaryResponse offline(String tourId);
    AvailabilityResponse getAvailability(String houseId);
    AdminImmersiveTourDetailResponse getUserTourData(String houseId);
}
