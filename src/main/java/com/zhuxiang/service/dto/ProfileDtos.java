package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    @Schema(description = "个人资料更新请求，字段均可选")
    public record UpdateProfileRequest(
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位")
            @Schema(description = "新昵称，长度 1-30 位", example = "小筑") String nickname,
            @Size(max = 500, message = "头像地址过长")
            @Schema(description = "新头像 URL", example = "/api/uploads/avatar.jpg") String avatarUrl
    ) {
    }

    @Schema(description = "密码修改请求")
    public record ChangePasswordRequest(
            @NotBlank(message = "旧密码不能为空")
            @Schema(description = "当前密码") String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位")
            @Schema(description = "新密码，长度 6-32 位", example = "NewExample123") String newPassword
    ) {
    }

    @Schema(description = "首次设置登录密码请求")
    public record SetPasswordRequest(
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位")
            @Schema(description = "首次设置的登录密码，长度 6-32 位", example = "Example123") String newPassword
    ) {
    }

    @Schema(description = "手机号换绑请求")
    public record ChangePhoneRequest(
            @NotBlank(message = "新手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "新手机号", example = "13900139000") String newPhone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误")
            @Schema(description = "新手机号收到的 6 位验证码", example = "123456") String code
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

    /**
     * 个人中心门锁展示信息（关联当前用户租约的门锁摘要）。
     */
    public record LockInfo(
            String lockId,
            String lockName,
            String lockBrand,
            String lockStatus,
            Integer batteryLevel,
            String leaseId,
            String leaseStatus,
            String startDate,
            String endDate,
            String permissionStatus,
            String validFrom,
            String validTo
    ) {
    }
}
