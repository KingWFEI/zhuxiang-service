package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminMessageDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMessageServiceTests {

    private final UserService userService = mock(UserService.class);
    private final MessageService messageService = mock(MessageService.class);
    private AdminMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminMessageServiceImpl(userService, messageService);
    }

    @Test
    void adminCanSendSystemMessageAndDuplicateRecipientsAreRemoved() {
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));
        when(userService.listByIds(any(Collection.class))).thenReturn(List.of(
                user("tenant-1", "TENANT", "active"),
                user("tenant-2", "TENANT", "active")
        ));

        int count = service.sendSystemMessage("admin-1", request(List.of(
                "tenant-1", "tenant-2", "tenant-1"
        )));

        assertThat(count).isEqualTo(2);
        verify(messageService).sendMessage(
                "tenant-1", "system", "系统维护通知", "系统将于今晚进行维护", "none", ""
        );
        verify(messageService).sendMessage(
                "tenant-2", "system", "系统维护通知", "系统将于今晚进行维护", "none", ""
        );
    }

    @Test
    void nonAdminCannotSendSystemMessage() {
        when(userService.requireActiveUser("housekeeper-1"))
                .thenReturn(user("housekeeper-1", "HOUSEKEEPER", "active"));

        assertThatThrownBy(() -> service.sendSystemMessage(
                "housekeeper-1", request(List.of("tenant-1"))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(403));

        verify(messageService, never()).sendMessage(any(), any(), any(), any(), any(), any());
    }

    @Test
    void missingRecipientPreventsAllMessagesFromBeingSent() {
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN", "active"));
        when(userService.listByIds(any(Collection.class))).thenReturn(List.of(
                user("tenant-1", "TENANT", "active")
        ));

        assertThatThrownBy(() -> service.sendSystemMessage(
                "admin-1", request(List.of("tenant-1", "missing-user"))
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).contains("missing-user");
        });

        verify(messageService, never()).sendMessage(any(), any(), any(), any(), any(), any());
    }

    private AdminMessageDtos.SendSystemMessageRequest request(List<String> userIds) {
        return new AdminMessageDtos.SendSystemMessageRequest(
                userIds, "系统维护通知", "系统将于今晚进行维护", "none", ""
        );
    }

    private User user(String id, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
