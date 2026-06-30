package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminUserDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdminUserService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Set<String> ROLES = Set.of("TENANT", "HOUSEKEEPER", "LANDLORD", "ADMIN");
    private static final Set<String> QUERY_STATUSES = Set.of("active", "disabled", "cancelled");
    private static final Set<String> EDITABLE_STATUSES = Set.of("active", "disabled");

    private final UserService userService;

    public AdminUserServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public PageData<AdminUserDtos.UserView> getUsers(
            String operatorId,
            String role,
            String status,
            String keyword,
            long page,
            long pageSize
    ) {
        requireAdmin(operatorId);
        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeQueryStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        IPage<User> result = userService.page(
                new Page<>(page, pageSize),
                Wrappers.<User>lambdaQuery()
                        .eq(normalizedRole != null, User::getRole, normalizedRole)
                        .eq(normalizedStatus != null, User::getStatus, normalizedStatus)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(User::getNickname, normalizedKeyword)
                                .or().like(User::getPhone, normalizedKeyword)
                                .or().like(User::getId, normalizedKeyword))
                        .orderByDesc(User::getCreatedAt)
        );
        return PageData.of(
                result.getRecords().stream().map(this::toView).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    @Override
    public AdminUserDtos.UserView getUser(String operatorId, String userId) {
        requireAdmin(operatorId);
        return toView(requireUser(userId));
    }

    @Override
    @Transactional
    public AdminUserDtos.UserView updateStatus(
            String operatorId,
            String userId,
            AdminUserDtos.UpdateStatusRequest request
    ) {
        requireAdmin(operatorId);
        String targetStatus = request.status().trim().toLowerCase(Locale.ROOT);
        if (!EDITABLE_STATUSES.contains(targetStatus)) {
            throw BusinessException.badRequest("用户状态仅支持 active 或 disabled");
        }
        if (operatorId.equals(userId) && "disabled".equals(targetStatus)) {
            throw BusinessException.conflict("不能禁用当前登录账号");
        }

        User user = requireUser(userId);
        if ("cancelled".equals(user.getStatus())) {
            throw BusinessException.conflict("已注销用户不能修改状态");
        }
        if (!targetStatus.equals(user.getStatus())) {
            user.setStatus(targetStatus);
            user.setUpdatedAt(LocalDateTime.now());
            userService.updateById(user);
        }
        return toView(user);
    }

    private void requireAdmin(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!"ADMIN".equals(operator.getRole())) {
            throw BusinessException.forbidden("当前账号无权管理用户");
        }
    }

    private User requireUser(String userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw BusinessException.badRequest("不支持的用户角色");
        }
        return normalized;
    }

    private String normalizeQueryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!QUERY_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest("不支持的用户状态");
        }
        return normalized;
    }

    private AdminUserDtos.UserView toView(User user) {
        return new AdminUserDtos.UserView(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                Integer.valueOf(1).equals(user.getIsVerified()),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
