package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordAuthDtos;

public interface LandlordAuthService {
    LandlordAuthDtos.StatusView getMyStatus(String userId);

    LandlordAuthDtos.ApplicationView submit(String userId, LandlordAuthDtos.SubmitRequest request);

    PageData<LandlordAuthDtos.AdminListItem> listAdmin(
            String operatorId, String status, String keyword, long page, long pageSize);

    LandlordAuthDtos.ApplicationView adminDetail(String operatorId, String applicationId);

    LandlordAuthDtos.ApplicationView review(
            String operatorId, String applicationId, LandlordAuthDtos.ReviewRequest request);
}
