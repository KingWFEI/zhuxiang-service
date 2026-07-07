package com.zhuxiang.service.immersive.dto.response;

import com.zhuxiang.service.immersive.enums.RenderMode;
import com.zhuxiang.service.immersive.enums.SceneType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ImmersiveSceneResponse {
    private String sceneId;
    private String tourId;
    private String name;
    private SceneType sceneType;
    private String entryImageId;
    private BigDecimal floorPlanXRatio;
    private BigDecimal floorPlanYRatio;
    private RenderMode renderMode;
    private BigDecimal initialYaw;
    private BigDecimal initialPitch;
    private BigDecimal initialHfov;
    private Integer sortOrder;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImmersiveImageResponse> images;

    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }
    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SceneType getSceneType() { return sceneType; }
    public void setSceneType(SceneType sceneType) { this.sceneType = sceneType; }
    public String getEntryImageId() { return entryImageId; }
    public void setEntryImageId(String entryImageId) { this.entryImageId = entryImageId; }
    public BigDecimal getFloorPlanXRatio() { return floorPlanXRatio; }
    public void setFloorPlanXRatio(BigDecimal floorPlanXRatio) { this.floorPlanXRatio = floorPlanXRatio; }
    public BigDecimal getFloorPlanYRatio() { return floorPlanYRatio; }
    public void setFloorPlanYRatio(BigDecimal floorPlanYRatio) { this.floorPlanYRatio = floorPlanYRatio; }
    public RenderMode getRenderMode() { return renderMode; }
    public void setRenderMode(RenderMode renderMode) { this.renderMode = renderMode; }
    public BigDecimal getInitialYaw() { return initialYaw; }
    public void setInitialYaw(BigDecimal initialYaw) { this.initialYaw = initialYaw; }
    public BigDecimal getInitialPitch() { return initialPitch; }
    public void setInitialPitch(BigDecimal initialPitch) { this.initialPitch = initialPitch; }
    public BigDecimal getInitialHfov() { return initialHfov; }
    public void setInitialHfov(BigDecimal initialHfov) { this.initialHfov = initialHfov; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ImmersiveImageResponse> getImages() { return images; }
    public void setImages(List<ImmersiveImageResponse> images) { this.images = images; }
}
