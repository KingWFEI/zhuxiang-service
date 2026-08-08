package com.zhuxiang.service.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.HouseSourceType;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 幂等初始化平台自营房源使用的系统房东，并回绑已有平台房源。
 */
@Component
@ConditionalOnProperty(
        name = "app.platform-landlord.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlatformLandlordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformLandlordInitializer.class);

    private final PlatformLandlordProperties properties;
    private final UserMapper userMapper;
    private final LandlordMapper landlordMapper;
    private final HouseMapper houseMapper;

    public PlatformLandlordInitializer(
            PlatformLandlordProperties properties,
            UserMapper userMapper,
            LandlordMapper landlordMapper,
            HouseMapper houseMapper
    ) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.landlordMapper = landlordMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateProperties();
        LocalDateTime now = LocalDateTime.now();
        initializeUser(now);
        initializeLandlord(now);
        int rebound = bindExistingPlatformHouses(now);
        log.info(
                "Platform landlord initialized — id: {}, rebound houses: {}",
                properties.getId(),
                rebound
        );
    }

    private void initializeUser(LocalDateTime now) {
        User existing = userMapper.selectById(properties.getId());
        if (existing != null) {
            validateExistingUser(existing);
            return;
        }
        Long phoneCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, properties.getPhone())
        );
        if (phoneCount != null && phoneCount > 0) {
            throw new IllegalStateException("平台房东手机号已被其他用户占用");
        }

        User user = new User();
        user.setId(properties.getId());
        user.setPhone(properties.getPhone());
        user.setPasswordHash(null);
        user.setNickname(properties.getName());
        user.setAvatarUrl(properties.getAvatarUrl());
        user.setRole("LANDLORD");
        // 系统主体不允许登录，但可作为 house.landlord_id 的外键目标。
        user.setStatus("disabled");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
    }

    private void initializeLandlord(LocalDateTime now) {
        Landlord existing = landlordMapper.selectOne(
                Wrappers.<Landlord>lambdaQuery()
                        .eq(Landlord::getUserId, properties.getId())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }

        Landlord landlord = new Landlord();
        landlord.setId(properties.getId());
        landlord.setUserId(properties.getId());
        landlord.setName(properties.getName());
        landlord.setAvatarUrl(properties.getAvatarUrl());
        landlord.setPhone(properties.getPhone());
        landlord.setShowPhone(0);
        landlord.setShowWechat(0);
        landlord.setShowEmail(0);
        landlord.setServiceYears(0);
        landlord.setIsVerified(1);
        landlord.setRating(BigDecimal.ZERO);
        landlord.setRentedCount(0);
        landlord.setResponseDescription(properties.getResponseDescription());
        landlord.setCreatedAt(now);
        landlord.setUpdatedAt(now);
        landlordMapper.insert(landlord);
    }

    private int bindExistingPlatformHouses(LocalDateTime now) {
        House patch = new House();
        patch.setLandlordId(properties.getId());
        patch.setUpdatedAt(now);
        return houseMapper.update(
                patch,
                Wrappers.<House>lambdaUpdate()
                        .eq(House::getSourceType, HouseSourceType.PLATFORM.name())
                        .ne(House::getLandlordId, properties.getId())
        );
    }

    private void validateExistingUser(User existing) {
        if (!properties.getPhone().equals(existing.getPhone())
                || !"LANDLORD".equals(existing.getRole())) {
            throw new IllegalStateException("平台房东ID已被其他用户占用");
        }
        if (!"disabled".equals(existing.getStatus())) {
            throw new IllegalStateException("平台房东必须是禁止登录的系统用户");
        }
    }

    private void validateProperties() {
        if (!StringUtils.hasText(properties.getId()) || properties.getId().length() > 36) {
            throw new IllegalStateException("平台房东ID不能为空且不能超过36个字符");
        }
        if (!StringUtils.hasText(properties.getPhone()) || properties.getPhone().length() > 20) {
            throw new IllegalStateException("平台房东手机号不能为空且不能超过20个字符");
        }
        if (!StringUtils.hasText(properties.getName()) || properties.getName().length() > 30) {
            throw new IllegalStateException("平台房东名称不能为空且不能超过30个字符");
        }
    }
}
