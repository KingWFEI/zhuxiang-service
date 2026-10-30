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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class SuperAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    @Value("${app.super-admin.phone:}")
    private String phone;

    @Value("${app.super-admin.password:}")
    private String password;

    @Value("${app.super-admin.nickname:}")
    private String nickname;

    @Bean
    @ConditionalOnProperty(name = "app.super-admin.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner initSuperAdmin(UserMapper userMapper) {
        return args -> {
            if (!StringUtils.hasText(phone)) {
                throw new IllegalStateException("请配置 app.super-admin.phone（可通过 SUPER_ADMIN_PHONE 环境变量覆盖）");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            if (userMapper.selectCount(wrapper) > 0) {
                log.info("Super admin with phone {} already exists, skipping.", phone);
                return;
            }
            if (!StringUtils.hasText(password) || !StringUtils.hasText(nickname)) {
                throw new IllegalStateException("首次创建超级管理员时必须配置 app.super-admin.password 和 app.super-admin.nickname（可通过 SUPER_ADMIN_* 环境变量覆盖）");
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setPhone(phone);
            admin.setPasswordHash(encoder.encode(password));
            admin.setNickname(nickname);
            admin.setAvatarUrl("");
            admin.setRole("ADMIN");
            admin.setStatus("active");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(admin);

            log.info("Super admin initialized — phone: {}", phone);
        };
    }
}
