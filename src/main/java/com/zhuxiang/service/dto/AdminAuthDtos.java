package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminAuthDtos {

    private AdminAuthDtos() {
    }

    public record AdminRegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位") String password,
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位") String nickname,
            @NotBlank(message = "角色不能为空")
            @Pattern(regexp = "ADMIN|HOUSEKEEPER|LANDLORD", message = "角色仅支持 ADMIN、HOUSEKEEPER、LANDLORD") String role
    ) {
    }
}
