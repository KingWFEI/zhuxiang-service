package com.zhuxiang.service.immersive.dto.response;

import com.zhuxiang.service.immersive.enums.TourStatus;
import java.time.LocalDateTime;

public class ImmersiveTourSummaryResponse {
    private String tourId;
    private String houseId;
    private String title;
    private String coverImageUrl;
    private String floorPlanUrl;
    private String entrySceneId;
    private TourStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
