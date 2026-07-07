package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CreateHotspotRequest {
    @NotBlank private String label;
    private BigDecimal xRatio;
    private BigDecimal yRatio;
    private BigDecimal yaw;
    private BigDecimal pitch;
    private String targetType;
    private String targetSceneId;
    private String targetImageId;

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
}
