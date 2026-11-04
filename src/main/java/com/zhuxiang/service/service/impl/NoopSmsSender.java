package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.service.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 未配置短信通道时不发送，验证码只能通过本地固定验证码或开发日志获取。
 */
@Component
@ConditionalOnProperty(prefix = "app.auth.sms", name = "provider", havingValue = "none", matchIfMissing = true)
public class NoopSmsSender implements SmsSender {

    @Override
    public void sendVerificationCode(String phone, String code) {
    }
}
