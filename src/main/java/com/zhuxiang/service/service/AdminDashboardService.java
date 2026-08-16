package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AdminDashboardDtos.DashboardOverview;

public interface AdminDashboardService {
    DashboardOverview getOverview(String operatorId);
}
