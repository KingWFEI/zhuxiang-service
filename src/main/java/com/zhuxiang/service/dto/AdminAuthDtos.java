package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminAuthDtos {

    private AdminAuthDtos() {
    }

    @Schema(description = "管理端用户注册请求")
    public record AdminRegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "登录手机号", example = "13800138000") String phone,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位")
            @Schema(description = "登录密码，长度 6-32 位", example = "Example123") String password,
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位")
            @Schema(description = "用户昵称，长度 1-30 位", example = "张管家") String nickname,
            @NotBlank(message = "角色不能为空")
            @Pattern(regexp = "ADMIN|HOUSEKEEPER|LANDLORD", message = "角色仅支持 ADMIN、HOUSEKEEPER、LANDLORD")
            @Schema(description = "管理端角色", allowableValues = {"ADMIN", "HOUSEKEEPER", "LANDLORD"}, example = "HOUSEKEEPER") String role
    ) {
    }
}
