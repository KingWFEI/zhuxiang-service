package com.zhuxiang.service.immersive.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ImmersiveHotspotResponse {
    private String hotspotId;
    private String sourceImageId;
    private String label;
    private BigDecimal xRatio;
    private BigDecimal yRatio;
    private BigDecimal yaw;
    private BigDecimal pitch;
    private String targetType;
    private String targetSceneId;
    private String targetImageId;
    private String targetSceneName;
    private String targetImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getHotspotId() { return hotspotId; }
    public void setHotspotId(String hotspotId) { this.hotspotId = hotspotId; }
    public String getSourceImageId() { return sourceImageId; }
    public void setSourceImageId(String sourceImageId) { this.sourceImageId = sourceImageId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getXRatio() { return xRatio; }
    public void setXRatio(BigDecimal xRatio) { this.xRatio = xRatio; }
    public BigDecimal getYRatio() { return yRatio; }
    public void setYRatio(BigDecimal yRatio) { this.yRatio = yRatio; }
    public BigDecimal getYaw() { return yaw; }
    public void setYaw(BigDecimal yaw) { this.yaw = yaw; }
    public BigDecimal getPitch() { return pitch; }
    public void setPitch(BigDecimal pitch) { this.pitch = pitch; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetSceneId() { return targetSceneId; }
    public void setTargetSceneId(String targetSceneId) { this.targetSceneId = targetSceneId; }
    public String getTargetImageId() { return targetImageId; }
    public void setTargetImageId(String targetImageId) { this.targetImageId = targetImageId; }
    public String getTargetSceneName() { return targetSceneName; }
    public void setTargetSceneName(String targetSceneName) { this.targetSceneName = targetSceneName; }
    public String getTargetImageUrl() { return targetImageUrl; }
    public void setTargetImageUrl(String targetImageUrl) { this.targetImageUrl = targetImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
