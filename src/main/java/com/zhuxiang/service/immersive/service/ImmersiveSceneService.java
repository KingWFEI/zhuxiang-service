package com.zhuxiang.service.immersive.service;

import com.zhuxiang.service.immersive.dto.request.*;
import com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse;
import java.util.List;

public interface ImmersiveSceneService {
    ImmersiveSceneResponse create(String tourId, CreateImmersiveSceneRequest request, String userId);
    List<ImmersiveSceneResponse> listByTourId(String tourId);
    void update(String sceneId, UpdateImmersiveSceneRequest request, String userId);
    void delete(String sceneId, String userId);
    void delete(String sceneId, boolean cascade, String userId);
    void sort(String tourId, SortImmersiveScenesRequest request, String userId);
    void setEntryScene(String tourId, SetEntrySceneRequest request, String userId);
    void setFloorPlanPosition(String sceneId, SetSceneFloorPlanPositionRequest request, String userId);
}
