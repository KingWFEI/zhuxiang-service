package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SetEntrySceneRequest {
    @NotBlank private String sceneId;
    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }
}
