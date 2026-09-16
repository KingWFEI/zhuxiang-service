package com.zhuxiang.service.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.test-user.enabled", havingValue = "true")
public class TestUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestUserInitializer.class);

    private final TestUserProperties properties;
    private final UserMapper userMapper;

    public TestUserInitializer(TestUserProperties properties, UserMapper userMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        properties.validate();
        User existingById = userMapper.selectById(properties.getId());
        if (existingById != null) {
            validateExisting(existingById);
            log.info("App test user already exists — phone: {}", maskPhone(properties.getPhone()));
            return;
        }

        Long phoneCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, properties.getPhone())
        );
        if (phoneCount != null && phoneCount > 0) {
            throw new IllegalStateException("测试用户手机号已被其他用户占用");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(properties.getId());
        user.setPhone(properties.getPhone());
        user.setPasswordHash(null);
        user.setNickname(properties.getNickname());
        user.setAvatarUrl("");
        user.setRole("TENANT");
        user.setStatus("active");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        log.info("App test user initialized — phone: {}", maskPhone(properties.getPhone()));
    }

    private void validateExisting(User existing) {
        if (!properties.getPhone().equals(existing.getPhone())
                || !"TENANT".equals(existing.getRole())
                || !"active".equals(existing.getStatus())) {
            throw new IllegalStateException("测试用户ID已被其他用户占用");
        }
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
