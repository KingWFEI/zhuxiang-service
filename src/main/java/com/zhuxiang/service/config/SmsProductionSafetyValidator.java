package com.zhuxiang.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 非本地环境的短信安全门禁。测试环境也会加载，用于提前暴露生产配置问题。
 */
@Component
@Profile("!dev")
public class SmsProductionSafetyValidator implements InitializingBean {

    private final SmsCodeProperties properties;

    public SmsProductionSafetyValidator(SmsCodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.getFixedCode() != null && !properties.getFixedCode().isBlank()) {
            throw new IllegalStateException("非本地环境禁止配置固定短信验证码");
        }
        if (properties.isExposeCode()) {
            throw new IllegalStateException("非本地环境禁止输出短信验证码");
        }
        if (!"redis".equalsIgnoreCase(properties.getRateLimitStore())) {
            throw new IllegalStateException("非本地环境短信限流必须使用 Redis");
        }
        if (!"aliyun".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalStateException("非本地环境必须使用阿里云短信发送验证码");
        }
        SmsCodeProperties.Aliyun aliyun = properties.getAliyun();
        requireText(aliyun.getAccessKeyId(), "ALIYUN_ACCESS_KEY_ID");
        requireText(aliyun.getAccessKeySecret(), "ALIYUN_ACCESS_KEY_SECRET");
        requireText(aliyun.getSignName(), "ALIYUN_SMS_SIGN_NAME");
        requireText(aliyun.getTemplateCode(), "ALIYUN_SMS_TEMPLATE_CODE");
    }

    private static void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("非本地环境短信配置缺失：" + name);
        }
    }
}
