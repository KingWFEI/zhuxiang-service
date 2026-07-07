package com.zhuxiang.service.immersive.dto.response;

import com.zhuxiang.service.immersive.enums.ProjectionType;
import java.time.LocalDateTime;
import java.util.List;

public class ImmersiveImageResponse {
    private String imageId;
    private String sceneId;
    private String name;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private ProjectionType projectionType;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer sortOrder;
    private Boolean entry;
    private LocalDateTime createdAt;
    private List<ImmersiveHotspotResponse> hotspots;

    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }
    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public ProjectionType getProjectionType() { return projectionType; }
    public void setProjectionType(ProjectionType projectionType) { this.projectionType = projectionType; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEntry() { return entry; }
    public void setEntry(Boolean entry) { this.entry = entry; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<ImmersiveHotspotResponse> getHotspots() { return hotspots; }
    public void setHotspots(List<ImmersiveHotspotResponse> hotspots) { this.hotspots = hotspots; }
}
