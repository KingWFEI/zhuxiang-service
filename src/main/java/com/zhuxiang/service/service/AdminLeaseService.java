package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminLeaseDtos;

public interface AdminLeaseService {

    PageData<AdminLeaseDtos.AdminLeaseView> getLeases(
            String operatorId,
            String status,
            String keyword,
            long page,
            long pageSize
    );
}
