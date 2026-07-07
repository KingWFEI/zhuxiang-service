package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateImmersiveTourRequest {
    @NotBlank private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
