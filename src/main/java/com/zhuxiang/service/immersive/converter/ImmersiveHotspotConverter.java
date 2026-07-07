package com.zhuxiang.service.immersive.converter;

import com.zhuxiang.service.immersive.dto.response.ImmersiveHotspotResponse;
import com.zhuxiang.service.immersive.entity.ImmersiveImageHotspotEntity;
import com.zhuxiang.service.immersive.entity.ImmersiveSceneEntity;
import com.zhuxiang.service.immersive.entity.ImmersiveSceneImageEntity;
import com.zhuxiang.service.immersive.enums.TargetType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImmersiveHotspotConverter {

    public ImmersiveHotspotResponse toResponse(ImmersiveImageHotspotEntity entity,
            Map<String, String> entryImageIdByScene,
            Map<String, ImmersiveSceneEntity> sceneMap,
            Map<String, ImmersiveSceneImageEntity> imageMap) {
        if (entity == null) return null;
        ImmersiveHotspotResponse resp = new ImmersiveHotspotResponse();
        resp.setHotspotId(entity.getId());
        resp.setSourceImageId(entity.getSourceImageId());
        resp.setLabel(entity.getLabel());
        resp.setXRatio(entity.getXRatio());
        resp.setYRatio(entity.getYRatio());
        resp.setYaw(entity.getYaw());
        resp.setPitch(entity.getPitch());

        TargetType tt = entity.getTargetType() != null ? entity.getTargetType() : TargetType.SCENE;
        resp.setTargetType(tt.getValue());
        resp.setTargetSceneId(entity.getTargetSceneId());

        if (tt == TargetType.IMAGE && entity.getTargetImageId() != null) {
            resp.setTargetImageId(entity.getTargetImageId());
        } else {
            resp.setTargetImageId(entryImageIdByScene != null
                    ? entryImageIdByScene.get(entity.getTargetSceneId()) : null);
        }

        if (sceneMap != null && entity.getTargetSceneId() != null) {
            ImmersiveSceneEntity targetScene = sceneMap.get(entity.getTargetSceneId());
            if (targetScene != null) resp.setTargetSceneName(targetScene.getName());
        }

        String resolvedImageId = resp.getTargetImageId();
        if (imageMap != null && resolvedImageId != null) {
            ImmersiveSceneImageEntity targetImage = imageMap.get(resolvedImageId);
            if (targetImage != null) resp.setTargetImageUrl(targetImage.getImageUrl());
        }

        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
