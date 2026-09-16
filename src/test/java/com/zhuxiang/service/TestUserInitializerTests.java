package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.config.TestUserInitializer;
import com.zhuxiang.service.config.TestUserProperties;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestUserInitializerTests {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final TestUserProperties properties = new TestUserProperties();
    private TestUserInitializer initializer;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setPhone("13900000000");
        properties.setVerificationCode("246810");
        initializer = new TestUserInitializer(properties, userMapper);
    }

    @Test
    void createsActiveTenantForConfiguredTestPhone() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPhone()).isEqualTo("13900000000");
        assertThat(captor.getValue().getRole()).isEqualTo("TENANT");
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
        assertThat(captor.getValue().getPasswordHash()).isNull();
    }

    @Test
    void keepsMatchingExistingTestUser() {
        User existing = new User();
        existing.setId(TestUserProperties.DEFAULT_ID);
        existing.setPhone("13900000000");
        existing.setRole("TENANT");
        existing.setStatus("active");
        when(userMapper.selectById(TestUserProperties.DEFAULT_ID)).thenReturn(existing);

        initializer.run(null);

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void rejectsInvalidVerificationCode() {
        properties.setVerificationCode("1234");

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("验证码必须是6位数字");
    }
}
