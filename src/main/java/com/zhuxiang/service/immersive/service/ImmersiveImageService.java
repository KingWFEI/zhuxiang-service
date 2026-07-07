package com.zhuxiang.service.immersive.service;

import com.zhuxiang.service.immersive.dto.request.SetEntryImageRequest;
import com.zhuxiang.service.immersive.dto.request.SortImagesRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveImageRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveImageResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ImmersiveImageService {
    List<ImmersiveImageResponse> uploadImages(String sceneId, List<MultipartFile> files, String projectionType, String userId);
    ImmersiveImageResponse getImage(String imageId);
    List<ImmersiveImageResponse> listImages(String sceneId);
    void sortImages(String sceneId, SortImagesRequest request, String userId);
    void setEntryImage(String sceneId, SetEntryImageRequest request, String userId);
    void deleteImage(String imageId, String userId);
    void deleteImage(String imageId, boolean force, String userId);
    void updateImage(String imageId, UpdateImmersiveImageRequest request, String userId);
    String uploadFloorPlan(String tourId, MultipartFile file, String userId);
}
