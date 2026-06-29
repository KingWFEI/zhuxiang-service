package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminFileServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminFileServiceTests {

    private final UserService userService = mock(UserService.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final AdminFileServiceImpl service = new AdminFileServiceImpl(userService, fileRecordService);
    private final MockMultipartFile file = new MockMultipartFile(
            "file", "room.webp", "image/webp", new byte[]{1}
    );

    @Test
    void adminRoleCanUploadHouseImage() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        FileUploadResponse expected = new FileUploadResponse("https://cdn.example.com/room.webp", "file-1");
        when(fileRecordService.uploadHouseImage("admin-1", file)).thenReturn(expected);

        assertThat(service.uploadHouseImage("admin-1", file)).isSameAs(expected);
        verify(fileRecordService).uploadHouseImage("admin-1", file);
    }

    @Test
    void tenantRoleCannotUploadHouseImage() {
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setRole("TENANT");
        when(userService.requireActiveUser("tenant-1")).thenReturn(tenant);

        assertThatThrownBy(() -> service.uploadHouseImage("tenant-1", file))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(403);
        verify(fileRecordService, never()).uploadHouseImage("tenant-1", file);
    }
}
