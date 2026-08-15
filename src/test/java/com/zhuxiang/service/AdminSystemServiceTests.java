package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminSystemDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminSystemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSystemServiceTests {

    private final UserService userService = mock(UserService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private AdminSystemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminSystemServiceImpl(userService, userMapper);
    }

    @Test
    void adminReceivesCodeManagedRolesWithDatabaseCounts() {
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN"));
        when(userMapper.selectRoleStatistics()).thenReturn(List.of(
                new AdminSystemDtos.RoleStatisticsRow("ADMIN", 3, 2, 1, 0, 2),
                new AdminSystemDtos.RoleStatisticsRow("HOUSEKEEPER", 8, 7, 1, 0, 6),
                new AdminSystemDtos.RoleStatisticsRow("LANDLORD", 20, 18, 1, 1, 12),
                new AdminSystemDtos.RoleStatisticsRow("TENANT", 100, 90, 5, 5, 70)
        ));

        AdminSystemDtos.SystemOverview result = service.getOverview("admin-1");

        assertThat(result.roleModel()).isEqualTo("CODE_MANAGED");
        assertThat(result.totalAccounts()).isEqualTo(131);
        assertThat(result.backendAccounts()).isEqualTo(31);
        assertThat(result.activeBackendAccounts()).isEqualTo(27);
        assertThat(result.recentBackendLogins()).isEqualTo(20);
        assertThat(result.roles()).extracting(AdminSystemDtos.RoleView::role)
                .containsExactly("ADMIN", "HOUSEKEEPER", "LANDLORD", "TENANT");
    }

    @Test
    void missingRoleRowsAreReturnedWithZeroCounts() {
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN"));
        when(userMapper.selectRoleStatistics()).thenReturn(List.of(
                new AdminSystemDtos.RoleStatisticsRow("ADMIN", 1, 1, 0, 0, 1)
        ));

        AdminSystemDtos.SystemOverview result = service.getOverview("admin-1");

        assertThat(result.roles()).filteredOn(role -> "HOUSEKEEPER".equals(role.role()))
                .singleElement().satisfies(role -> assertThat(role.accountCount()).isZero());
    }

    @Test
    void nonAdminCannotReadSystemOverview() {
        when(userService.requireActiveUser("housekeeper-1"))
                .thenReturn(user("housekeeper-1", "HOUSEKEEPER"));

        assertThatThrownBy(() -> service.getOverview("housekeeper-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(403));
        verify(userMapper, never()).selectRoleStatistics();
    }

    private User user(String id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus("active");
        return user;
    }
}
