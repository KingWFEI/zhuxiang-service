package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.entity.SmsCode;
import com.zhuxiang.service.service.SmsCodeService;
import com.zhuxiang.service.mapper.SmsCodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Set<String> SMS_SCENES =
            Set.of("login", "register", "reset_password", "real_name");
    private static final long SMS_EXPIRES_SECONDS = 300;

    private final String fixedSmsCode;

    public SmsCodeServiceImpl(@Value("${app.auth.fixed-sms-code:}") String fixedSmsCode) {
        this.fixedSmsCode = fixedSmsCode;
    }

    /**
     * 生成并保存指定场景的短信验证码。
     */
    @Override
    public AuthDtos.SmsCodeResult sendSmsCode(AuthDtos.SmsCodeRequest request) {
        if (!SMS_SCENES.contains(request.scene())) {
            throw BusinessException.badRequest("验证码场景不支持");
        }
        SmsCode latest = getOne(
                Wrappers.<SmsCode>lambdaQuery()
                        .eq(SmsCode::getPhone, request.phone())
                        .eq(SmsCode::getScene, request.scene())
                        .orderByDesc(SmsCode::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
        LocalDateTime now = LocalDateTime.now();
        if (latest != null && latest.getCreatedAt().plusSeconds(60).isAfter(now)) {
            throw BusinessException.tooManyRequests("验证码发送过于频繁，请稍后再试");
        }
        SmsCode smsCode = new SmsCode();
        smsCode.setId(UUID.randomUUID().toString());
        smsCode.setPhone(request.phone());
        smsCode.setScene(request.scene());
        smsCode.setCode(fixedSmsCode == null || fixedSmsCode.isBlank()
                ? String.format("%06d", (int) (Math.random() * 1_000_000))
                : fixedSmsCode);
        System.out.println("【开发测试】手机号 " + request.phone()
                + "，场景 " + request.scene()
                + " 的验证码是：" + smsCode.getCode());
        smsCode.setExpiresAt(now.plusSeconds(SMS_EXPIRES_SECONDS));
        smsCode.setUsed(0);
        smsCode.setCreatedAt(now);
        save(smsCode);
        return new AuthDtos.SmsCodeResult(SMS_EXPIRES_SECONDS);
    }

    /**
     * 校验验证码并将其标记为已使用。
     */
    @Override
    @Transactional
    public void consumeSmsCode(String phone, String scene, String code) {
        SmsCode smsCode = getOne(
                Wrappers.<SmsCode>lambdaQuery()
                        .eq(SmsCode::getPhone, phone)
                        .eq(SmsCode::getScene, scene)
                        .eq(SmsCode::getCode, code)
                        .eq(SmsCode::getUsed, 0)
                        .orderByDesc(SmsCode::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
        LocalDateTime now = LocalDateTime.now();
        if (smsCode == null || smsCode.getExpiresAt().isBefore(now)) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }
        smsCode.setUsed(1);
        smsCode.setUsedAt(now);
        updateById(smsCode);
    }
}




