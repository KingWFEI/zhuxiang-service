package com.zhuxiang.service.immersive.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhuxiang.service.immersive.enums.TargetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图片热点实体。
 */
@TableName("immersive_image_hotspot")
public class ImmersiveImageHotspotEntity {

    @TableId
    private String id;
    private String sourceImageId;
    private String label;
    private BigDecimal xRatio;
    private BigDecimal yRatio;
    private BigDecimal yaw;
    private BigDecimal pitch;
    private TargetType targetType;
    private String targetSceneId;
    private String targetImageId;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public String getTargetSceneId() { return targetSceneId; }
    public void setTargetSceneId(String targetSceneId) { this.targetSceneId = targetSceneId; }
    public String getTargetImageId() { return targetImageId; }
    public void setTargetImageId(String targetImageId) { this.targetImageId = targetImageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
