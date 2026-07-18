package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.EsignSignResponse;
import com.zhuxiang.service.dto.EsignSignStatusResponse;
import com.zhuxiang.service.dto.LandlordContractDtos;

public interface LandlordContractService {

    PageData<LandlordContractDtos.ContractItem> listPendingSign(
            String landlordUserId, long page, long pageSize);

    LandlordContractDtos.ContractDetail getDetail(String landlordUserId, String orderId);

    EsignSignResponse sign(String landlordUserId, String orderId);

    EsignSignStatusResponse refresh(String landlordUserId, String orderId);
}
