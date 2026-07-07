package com.zhuxiang.service.immersive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SortImagesRequest {
    @NotEmpty private List<String> imageIds;
    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }
}
