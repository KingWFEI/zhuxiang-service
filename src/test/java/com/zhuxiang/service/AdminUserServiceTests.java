package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminUserDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTests {

    private final UserService userService = mock(UserService.class);
    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(userService);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void adminCanQueryPagedUsers() {
        User tenant = user("tenant-1", "TENANT", "active");
        tenant.setPhone("13800138000");
        tenant.setNickname("张三");
        tenant.setIsVerified(1);

        Page<User> databasePage = new Page<>(1, 20, 1);
        databasePage.setRecords(List.of(tenant));
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));
        when(userService.page(any(Page.class), any(Wrapper.class))).thenReturn(databasePage);

        PageData<AdminUserDtos.UserView> result = service.getUsers(
                "admin-1", "tenant", "ACTIVE", "张三", 1, 20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("tenant-1");
            assertThat(item.verified()).isTrue();
            assertThat(item.status()).isEqualTo("active");
        });
    }

    @Test
    void nonAdminCannotQueryUsers() {
        when(userService.requireActiveUser("housekeeper-1"))
                .thenReturn(user("housekeeper-1", "HOUSEKEEPER", "active"));

        assertThatThrownBy(() -> service.getUsers(
                "housekeeper-1", null, null, null, 1, 20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(403));

        verify(userService, never()).page(any(Page.class), any(Wrapper.class));
    }

    @Test
    void adminCanDisableAnotherUser() {
        User tenant = user("tenant-1", "TENANT", "active");
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));
        when(userService.getById("tenant-1")).thenReturn(tenant);

        AdminUserDtos.UserView result = service.updateStatus(
                "admin-1", "tenant-1", new AdminUserDtos.UpdateStatusRequest("DISABLED")
        );

        assertThat(result.status()).isEqualTo("disabled");
        assertThat(tenant.getUpdatedAt()).isNotNull();
        verify(userService).updateById(tenant);
    }

    @Test
    void adminCannotDisableCurrentAccount() {
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));

        assertThatThrownBy(() -> service.updateStatus(
                "admin-1", "admin-1", new AdminUserDtos.UpdateStatusRequest("disabled")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(409));

        verify(userService, never()).updateById(any());
    }

    @Test
    void cancelledUserCannotBeReactivated() {
        User cancelled = user("tenant-1", "TENANT", "cancelled");
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));
        when(userService.getById("tenant-1")).thenReturn(cancelled);

        assertThatThrownBy(() -> service.updateStatus(
                "admin-1", "tenant-1", new AdminUserDtos.UpdateStatusRequest("active")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(409));

        verify(userService, never()).updateById(cancelled);
    }

    private User user(String id, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        user.setAvatarUrl("");
        user.setIsVerified(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
