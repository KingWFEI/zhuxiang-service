package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AdminSystemDtos;

public interface AdminSystemService {

    AdminSystemDtos.SystemOverview getOverview(String operatorId);
}
