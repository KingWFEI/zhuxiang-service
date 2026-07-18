package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractDtos;

public interface AdminContractService {
    PageData<AdminContractDtos.ContractSummary> list(String operatorId, String status, String keyword,
                                                     long page, long pageSize);
    AdminContractDtos.ContractDetail get(String operatorId, String contractId);
    AdminContractDtos.DownloadUrl downloadUrl(String operatorId, String contractId);
}
