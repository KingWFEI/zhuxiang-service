package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SetEntryImageRequest {
    @NotBlank private String imageId;
    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }
}
