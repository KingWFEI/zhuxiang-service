package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminSystemDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.AdminSystemService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminSystemServiceImpl implements AdminSystemService {

    private static final List<RoleDefinition> ROLE_DEFINITIONS = List.of(
            new RoleDefinition("ADMIN", "平台管理员", "负责平台配置和全局运营管理", "全部业务数据", true),
            new RoleDefinition("HOUSEKEEPER", "管家", "负责房源、租约和服务履约运营", "平台运营业务数据", true),
            new RoleDefinition("LANDLORD", "房东", "管理本人名下房源和关联业务", "本人名下业务数据", true),
            new RoleDefinition("TENANT", "租客", "使用移动端找房、签约和履约", "本人租住与交易数据", false)
    );

    private final UserService userService;
    private final UserMapper userMapper;

    public AdminSystemServiceImpl(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Override
    public AdminSystemDtos.SystemOverview getOverview(String operatorId) {
        requireAdmin(operatorId);
        List<AdminSystemDtos.RoleStatisticsRow> statistics = userMapper.selectRoleStatistics();
        Map<String, AdminSystemDtos.RoleStatisticsRow> statisticsByRole = statistics.stream()
                .collect(Collectors.toMap(AdminSystemDtos.RoleStatisticsRow::role, Function.identity()));

        List<AdminSystemDtos.RoleView> roles = ROLE_DEFINITIONS.stream()
                .map(definition -> toView(definition, statisticsByRole.get(definition.role())))
                .toList();
        long totalAccounts = statistics.stream().mapToLong(AdminSystemDtos.RoleStatisticsRow::accountCount).sum();
        long backendAccounts = roles.stream().filter(AdminSystemDtos.RoleView::managementLoginAllowed)
                .mapToLong(AdminSystemDtos.RoleView::accountCount).sum();
        long activeBackendAccounts = roles.stream().filter(AdminSystemDtos.RoleView::managementLoginAllowed)
                .mapToLong(AdminSystemDtos.RoleView::activeCount).sum();
        long recentBackendLogins = roles.stream().filter(AdminSystemDtos.RoleView::managementLoginAllowed)
                .mapToLong(AdminSystemDtos.RoleView::recentLoginCount).sum();

        return new AdminSystemDtos.SystemOverview(
                "CODE_MANAGED", totalAccounts, backendAccounts, activeBackendAccounts,
                recentBackendLogins, LocalDateTime.now(), roles
        );
    }

    private AdminSystemDtos.RoleView toView(
            RoleDefinition definition,
            AdminSystemDtos.RoleStatisticsRow statistics
    ) {
        AdminSystemDtos.RoleStatisticsRow safeStatistics = statistics == null
                ? new AdminSystemDtos.RoleStatisticsRow(definition.role(), 0, 0, 0, 0, 0)
                : statistics;
        return new AdminSystemDtos.RoleView(
                definition.role(), definition.name(), definition.description(), definition.dataScope(),
                definition.managementLoginAllowed(), safeStatistics.accountCount(), safeStatistics.activeCount(),
                safeStatistics.disabledCount(), safeStatistics.cancelledCount(), safeStatistics.recentLoginCount()
        );
    }

    private void requireAdmin(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!"ADMIN".equals(operator.getRole())) {
            throw BusinessException.forbidden("当前账号无权查看系统管理信息");
        }
    }

    private record RoleDefinition(
            String role,
            String name,
            String description,
            String dataScope,
            boolean managementLoginAllowed
    ) {
    }
}
