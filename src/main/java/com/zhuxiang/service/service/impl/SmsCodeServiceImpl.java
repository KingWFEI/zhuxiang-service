package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.entity.SmsCode;
import com.zhuxiang.service.service.SmsCodeService;
import com.zhuxiang.service.service.SmsRateLimiter;
import com.zhuxiang.service.mapper.SmsCodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【sms_code(短信验证码表)】的数据库操作Service实现
* @createDate 2026-06-12 19:58:07
*/
@Service
public class SmsCodeServiceImpl extends ServiceImpl<SmsCodeMapper, SmsCode>
    implements SmsCodeService{

    private static final Logger log = LoggerFactory.getLogger(SmsCodeServiceImpl.class);
    private static final Set<String> SMS_SCENES =
            Set.of("login", "register", "reset_password", "real_name");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SmsCodeProperties properties;
    private final SmsRateLimiter rateLimiter;

    public SmsCodeServiceImpl(SmsCodeProperties properties, SmsRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 生成并保存指定场景的短信验证码。
     */
    @Override
    @Transactional
    public AuthDtos.SmsCodeResult sendSmsCode(AuthDtos.SmsCodeRequest request, String clientIp) {
        if (!SMS_SCENES.contains(request.scene())) {
            throw BusinessException.badRequest("验证码场景不支持");
        }
        SmsRateLimiter.RateLimitDecision decision = rateLimiter.acquire(
                request.phone(), request.scene(), clientIp
        );
        if (!decision.allowed()) {
            throw BusinessException.tooManyRequests(
                    decision.retryAfter() + " 秒后可重新获取验证码",
                    new AuthDtos.SmsCodeRetry(decision.retryAfter())
            );
        }

        LocalDateTime now = LocalDateTime.now();
        baseMapper.update(null, new UpdateWrapper<SmsCode>()
                .eq("phone", request.phone())
                .eq("scene", request.scene())
                .eq("used", 0)
                .set("used", 1)
                .set("used_at", now));

        SmsCode smsCode = new SmsCode();
        smsCode.setId(UUID.randomUUID().toString());
        smsCode.setPhone(request.phone());
        smsCode.setScene(request.scene());
        smsCode.setCode(generateCode());
        smsCode.setExpiresAt(now.plusSeconds(properties.getExpiresSeconds()));
        smsCode.setUsed(0);
        smsCode.setFailedAttempts(0);
        smsCode.setCreatedAt(now);
        save(smsCode);

        if (properties.isExposeCode()) {
            log.info("【仅限开发测试】手机号 {}，场景 {} 的验证码是：{}",
                    maskPhone(request.phone()), request.scene(), smsCode.getCode());
        }
        return new AuthDtos.SmsCodeResult(
                properties.getExpiresSeconds(), properties.getRetryAfterSeconds()
        );
    }

    /**
     * 校验验证码并将其标记为已使用。
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = BusinessException.class
    )
    public void consumeSmsCode(String phone, String scene, String code) {
        SmsCode smsCode = getOne(
                Wrappers.<SmsCode>lambdaQuery()
                        .eq(SmsCode::getPhone, phone)
                        .eq(SmsCode::getScene, scene)
                        .eq(SmsCode::getUsed, 0)
                        .orderByDesc(SmsCode::getCreatedAt)
                        .last("LIMIT 1 FOR UPDATE"),
                false
        );
        LocalDateTime now = LocalDateTime.now();
        if (smsCode == null || smsCode.getExpiresAt().isBefore(now)) {
            if (smsCode != null) {
                smsCode.setUsed(1);
                smsCode.setUsedAt(now);
                updateById(smsCode);
            }
            throw BusinessException.badRequest("验证码错误或已过期");
        }

        if (!constantTimeEquals(smsCode.getCode(), code)) {
            int failedAttempts = (smsCode.getFailedAttempts() == null ? 0 : smsCode.getFailedAttempts()) + 1;
            smsCode.setFailedAttempts(failedAttempts);
            if (failedAttempts >= properties.getMaxVerifyAttempts()) {
                smsCode.setUsed(1);
                smsCode.setUsedAt(now);
            }
            updateById(smsCode);
            throw BusinessException.badRequest("验证码错误或已过期");
        }
        smsCode.setUsed(1);
        smsCode.setUsedAt(now);
        updateById(smsCode);
    }

    private String generateCode() {
        String fixedCode = properties.getFixedCode();
        if (fixedCode != null && !fixedCode.isBlank()) {
            if (!fixedCode.matches("\\d{6}")) {
                throw new IllegalStateException("FIXED_SMS_CODE 必须是 6 位数字");
            }
            return fixedCode;
        }
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}




