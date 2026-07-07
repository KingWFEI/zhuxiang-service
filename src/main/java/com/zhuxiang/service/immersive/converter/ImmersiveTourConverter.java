package com.zhuxiang.service.immersive.converter;

import com.zhuxiang.service.immersive.dto.response.AdminImmersiveTourDetailResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveTourSummaryResponse;
import com.zhuxiang.service.immersive.entity.ImmersiveTourEntity;
import org.springframework.stereotype.Component;

@Component
public class ImmersiveTourConverter {

    public ImmersiveTourSummaryResponse toSummary(ImmersiveTourEntity entity) {
        if (entity == null) return null;
        ImmersiveTourSummaryResponse resp = new ImmersiveTourSummaryResponse();
        resp.setTourId(entity.getId());
        resp.setHouseId(entity.getHouseId());
        resp.setTitle(entity.getTitle());
        resp.setCoverImageUrl(entity.getCoverImageUrl());
        resp.setFloorPlanUrl(entity.getFloorPlanUrl());
        resp.setEntrySceneId(entity.getEntrySceneId());
        resp.setStatus(entity.getStatus());
        resp.setPublishedAt(entity.getPublishedAt());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

    public AdminImmersiveTourDetailResponse toAdminDetail(ImmersiveTourEntity entity,
            java.util.List<com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse> scenes) {
        if (entity == null) return null;
        AdminImmersiveTourDetailResponse resp = new AdminImmersiveTourDetailResponse();
        resp.setTourId(entity.getId());
        resp.setHouseId(entity.getHouseId());
        resp.setTitle(entity.getTitle());
        resp.setCoverImageUrl(entity.getCoverImageUrl());
        resp.setFloorPlanUrl(entity.getFloorPlanUrl());
        resp.setEntrySceneId(entity.getEntrySceneId());
        resp.setStatus(entity.getStatus());
        resp.setPublishedAt(entity.getPublishedAt());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        resp.setScenes(scenes);
        return resp;
    }
}
