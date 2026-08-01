package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.LandlordDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.User;

public interface LandlordService {

    Landlord findByUserId(String userId);

    Landlord requireByUserId(String userId);

    Landlord ensureProfile(User user);

    LandlordDtos.ProfileView getPublicProfile(String userId);

    LandlordDtos.ProfileView getMyProfile(String userId);

    LandlordDtos.ProfileView updateMyProfile(
            String userId,
            LandlordDtos.UpdateLandlordProfileRequest request
    );
}
