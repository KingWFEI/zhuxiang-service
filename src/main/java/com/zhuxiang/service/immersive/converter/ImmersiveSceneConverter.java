package com.zhuxiang.service.immersive.converter;

import com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse;
import com.zhuxiang.service.immersive.entity.ImmersiveSceneEntity;
import org.springframework.stereotype.Component;

@Component
public class ImmersiveSceneConverter {

    public ImmersiveSceneResponse toResponse(ImmersiveSceneEntity entity) {
        if (entity == null) return null;
        ImmersiveSceneResponse resp = new ImmersiveSceneResponse();
        resp.setSceneId(entity.getId());
        resp.setTourId(entity.getTourId());
        resp.setName(entity.getName());
        resp.setSceneType(entity.getSceneType());
        resp.setEntryImageId(entity.getEntryImageId());
        resp.setFloorPlanXRatio(entity.getFloorPlanXRatio());
        resp.setFloorPlanYRatio(entity.getFloorPlanYRatio());
        resp.setRenderMode(entity.getRenderMode());
        resp.setInitialYaw(entity.getInitialYaw());
        resp.setInitialPitch(entity.getInitialPitch());
        resp.setInitialHfov(entity.getInitialHfov());
        resp.setSortOrder(entity.getSortOrder());
        resp.setEnabled(entity.getEnabled());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
