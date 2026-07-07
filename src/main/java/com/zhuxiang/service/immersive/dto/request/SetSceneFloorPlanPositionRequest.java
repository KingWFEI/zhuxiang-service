package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SetSceneFloorPlanPositionRequest {
    @NotNull @DecimalMin("0") @DecimalMax("1") private BigDecimal xRatio;
    @NotNull @DecimalMin("0") @DecimalMax("1") private BigDecimal yRatio;

    public BigDecimal getXRatio() { return xRatio; }
    public void setXRatio(BigDecimal xRatio) { this.xRatio = xRatio; }
    public BigDecimal getYRatio() { return yRatio; }
    public void setYRatio(BigDecimal yRatio) { this.yRatio = yRatio; }
}
