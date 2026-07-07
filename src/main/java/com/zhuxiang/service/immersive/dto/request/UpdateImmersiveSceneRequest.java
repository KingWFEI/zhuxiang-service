package com.zhuxiang.service.immersive.dto.request;

import com.zhuxiang.service.immersive.enums.RenderMode;
import com.zhuxiang.service.immersive.enums.SceneType;
import java.math.BigDecimal;

public class UpdateImmersiveSceneRequest {
    private String name;
    private SceneType sceneType;
    private RenderMode renderMode;
    private BigDecimal initialYaw;
    private BigDecimal initialPitch;
    private BigDecimal initialHfov;
    private Integer enabled;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SceneType getSceneType() { return sceneType; }
    public void setSceneType(SceneType sceneType) { this.sceneType = sceneType; }
    public RenderMode getRenderMode() { return renderMode; }
    public void setRenderMode(RenderMode renderMode) { this.renderMode = renderMode; }
    public BigDecimal getInitialYaw() { return initialYaw; }
    public void setInitialYaw(BigDecimal initialYaw) { this.initialYaw = initialYaw; }
    public BigDecimal getInitialPitch() { return initialPitch; }
    public void setInitialPitch(BigDecimal initialPitch) { this.initialPitch = initialPitch; }
    public BigDecimal getInitialHfov() { return initialHfov; }
    public void setInitialHfov(BigDecimal initialHfov) { this.initialHfov = initialHfov; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
