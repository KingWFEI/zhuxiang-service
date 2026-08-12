package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordContractDtos;

public interface LandlordTerminationService {
    PageData<LandlordContractDtos.TerminationItem> listPendingSign(
            String landlordUserId, long page, long pageSize);
    LandlordContractDtos.TerminationDetail getDetail(String landlordUserId, String applicationId);
    LandlordContractDtos.RescissionAction getSignUrl(String landlordUserId, String applicationId);
    LandlordContractDtos.RescissionAction refresh(String landlordUserId, String applicationId);
}
