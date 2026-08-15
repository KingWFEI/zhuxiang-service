package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminSystemDtos {

    private AdminSystemDtos() {
    }

    @Schema(description = "数据库角色账号统计行")
    public record RoleStatisticsRow(
            @Schema(description = "角色编码")
            String role,
            @Schema(description = "账号总数")
            long accountCount,
            @Schema(description = "启用账号数")
            long activeCount,
            @Schema(description = "禁用账号数")
            long disabledCount,
            @Schema(description = "已注销账号数")
            long cancelledCount,
            @Schema(description = "近 30 天登录账号数")
            long recentLoginCount
    ) {
    }

    @Schema(description = "系统内置角色的真实账号统计")
    public record RoleView(
            @Schema(description = "角色编码")
            String role,
            @Schema(description = "角色名称")
            String name,
            @Schema(description = "角色职责说明")
            String description,
            @Schema(description = "角色数据权限范围")
            String dataScope,
            @Schema(description = "是否允许登录管理端")
            boolean managementLoginAllowed,
            @Schema(description = "账号总数")
            long accountCount,
            @Schema(description = "启用账号数")
            long activeCount,
            @Schema(description = "禁用账号数")
            long disabledCount,
            @Schema(description = "已注销账号数")
            long cancelledCount,
            @Schema(description = "近 30 天登录账号数")
            long recentLoginCount
    ) {
    }

    @Schema(description = "管理端系统概览")
    public record SystemOverview(
            @Schema(description = "角色管理模式")
            String roleModel,
            @Schema(description = "平台账号总数")
            long totalAccounts,
            @Schema(description = "允许登录管理端的账号总数")
            long backendAccounts,
            @Schema(description = "启用的管理端账号数")
            long activeBackendAccounts,
            @Schema(description = "近 30 天登录的管理端账号数")
            long recentBackendLogins,
            @Schema(description = "统计生成时间")
            LocalDateTime generatedAt,
            @Schema(description = "系统内置角色及账号统计")
            List<RoleView> roles
    ) {
    }
}
