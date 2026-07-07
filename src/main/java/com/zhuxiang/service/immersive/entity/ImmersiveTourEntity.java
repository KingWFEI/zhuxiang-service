package com.zhuxiang.service.immersive.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhuxiang.service.immersive.enums.TourStatus;

import java.time.LocalDateTime;

/**
 * 沉浸式看房项目实体。
 */
@TableName("immersive_tour")
public class ImmersiveTourEntity {

    @TableId
    private String id;
    private String houseId;
    private String title;
    private String coverImageUrl;
    private String floorPlanUrl;
    private String entrySceneId;
    private TourStatus status;
    private LocalDateTime publishedAt;
    private String activeKey;
    private String createdBy;
    private String updatedBy;

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
    public String getHouseId() { return houseId; }
    public void setHouseId(String houseId) { this.houseId = houseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getFloorPlanUrl() { return floorPlanUrl; }
    public void setFloorPlanUrl(String floorPlanUrl) { this.floorPlanUrl = floorPlanUrl; }
    public String getEntrySceneId() { return entrySceneId; }
    public void setEntrySceneId(String entrySceneId) { this.entrySceneId = entrySceneId; }
    public TourStatus getStatus() { return status; }
    public void setStatus(TourStatus status) { this.status = status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getActiveKey() { return activeKey; }
    public void setActiveKey(String activeKey) { this.activeKey = activeKey; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
