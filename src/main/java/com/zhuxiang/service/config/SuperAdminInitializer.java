package com.zhuxiang.service.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class SuperAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    @Value("${app.super-admin.phone:13800000000}")
    private String phone;

    @Value("${app.super-admin.password:admin123}")
    private String password;

    @Value("${app.super-admin.nickname:超级管理员}")
    private String nickname;

    @Bean
    @ConditionalOnProperty(name = "app.super-admin.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner initSuperAdmin(UserMapper userMapper) {
        return args -> {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            if (userMapper.selectCount(wrapper) > 0) {
                log.info("Super admin with phone {} already exists, skipping.", phone);
                return;
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setPhone(phone);
            admin.setPasswordHash(encoder.encode(password));
            admin.setNickname(nickname);
            admin.setAvatarUrl("");
            admin.setRole("ADMIN");
            admin.setIsVerified(1);
            admin.setStatus("active");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(admin);

            log.info("Super admin initialized — phone: {}", phone);
        };
    }
}
