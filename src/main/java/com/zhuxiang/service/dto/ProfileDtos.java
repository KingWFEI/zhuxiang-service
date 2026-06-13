package com.zhuxiang.service.dto;

import jakarta.validation.constraints.Size;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位") String nickname,
            @Size(max = 500, message = "头像地址过长") String avatarUrl
    ) {
    }

    public record AvatarResult(String avatarUrl) {
    }

    public record CurrentHome(
            String houseId,
            String community,
            String building,
            String unit,
            String room,
            String leaseId,
            String leaseStatus,
            String lockId,
            String lockStatus
    ) {
    }
}
