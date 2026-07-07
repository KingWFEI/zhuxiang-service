package com.zhuxiang.service.immersive.dto.response;

public class AvailabilityResponse {
    private boolean available;
    private String tourId;
    private String coverImageUrl;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
}
