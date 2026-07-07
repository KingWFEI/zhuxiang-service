package com.zhuxiang.service.immersive.dto.request;

public class UpdateImmersiveTourRequest {
    private String title;
    private String coverImageUrl;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
}
