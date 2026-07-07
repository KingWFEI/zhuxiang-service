package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SortImmersiveScenesRequest {
    @NotEmpty private List<String> sceneIds;
    public List<String> getSceneIds() { return sceneIds; }
    public void setSceneIds(List<String> sceneIds) { this.sceneIds = sceneIds; }
}
