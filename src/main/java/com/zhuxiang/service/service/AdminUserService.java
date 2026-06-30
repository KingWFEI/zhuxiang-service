package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminUserDtos;

public interface AdminUserService {

    PageData<AdminUserDtos.UserView> getUsers(
            String operatorId,
            String role,
            String status,
            String keyword,
            long page,
            long pageSize
    );

    AdminUserDtos.UserView getUser(String operatorId, String userId);

    AdminUserDtos.UserView updateStatus(
            String operatorId,
            String userId,
            AdminUserDtos.UpdateStatusRequest request
    );
}
