package com.zhuxiang.service.immersive.service;

import com.zhuxiang.service.immersive.dto.request.CreateHotspotRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateHotspotRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveHotspotResponse;
import java.util.List;

public interface ImmersiveHotspotService {
    ImmersiveHotspotResponse create(String imageId, CreateHotspotRequest request, String userId);
    List<ImmersiveHotspotResponse> listByImage(String imageId);
    List<ImmersiveHotspotResponse> listByImages(List<String> imageIds);
    void update(String hotspotId, UpdateHotspotRequest request, String userId);
    void delete(String hotspotId, String userId);
}
