package com.zhuxiang.service.immersive.converter;

import com.zhuxiang.service.immersive.dto.response.ImmersiveImageResponse;
import com.zhuxiang.service.immersive.entity.ImmersiveSceneImageEntity;
import org.springframework.stereotype.Component;

@Component
public class ImmersiveImageConverter {

    public ImmersiveImageResponse toResponse(ImmersiveSceneImageEntity entity) {
        if (entity == null) return null;
        ImmersiveImageResponse resp = new ImmersiveImageResponse();
        resp.setImageId(entity.getId());
        resp.setSceneId(entity.getSceneId());
        resp.setName(entity.getName());
        resp.setImageUrl(entity.getImageUrl());
        resp.setThumbnailUrl(entity.getThumbnailUrl());
        resp.setWidth(entity.getWidth());
        resp.setHeight(entity.getHeight());
        resp.setProjectionType(entity.getProjectionType());
        resp.setImageWidth(entity.getImageWidth());
        resp.setImageHeight(entity.getImageHeight());
        resp.setSortOrder(entity.getSortOrder());
        resp.setEntry(entity.getIsEntry() != null && entity.getIsEntry() == 1);
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }
}
