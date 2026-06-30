package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    @Schema(description = "管理端用户信息")
    public record UserView(
            @Schema(description = "用户 ID") String id,
            @Schema(description = "手机号") String phone,
            @Schema(description = "昵称") String nickname,
            @Schema(description = "头像 URL") String avatarUrl,
            @Schema(description = "用户角色：TENANT、HOUSEKEEPER、LANDLORD 或 ADMIN") String role,
            @Schema(description = "是否已实名认证") boolean verified,
            @Schema(description = "用户状态：active、disabled 或 cancelled") String status,
            @Schema(description = "最后登录时间") LocalDateTime lastLoginAt,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "管理端修改用户状态请求")
    public record UpdateStatusRequest(
            @NotBlank(message = "用户状态不能为空")
            @Schema(description = "目标状态，仅支持 active 或 disabled", example = "disabled")
            String status
    ) {
    }
}
