package com.zhuxiang.service.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.service.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.auth.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);
    private static final String SUCCESS_CODE = "OK";

    private final SmsCodeProperties.Aliyun properties;
    private final ObjectMapper objectMapper;
    private final Client client;

    public AliyunSmsSender(SmsCodeProperties smsProperties, ObjectMapper objectMapper) throws Exception {
        this.properties = smsProperties.getAliyun();
        this.objectMapper = objectMapper;
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret())
                .setEndpoint(properties.getEndpoint())
                .setConnectTimeout(5000)
                .setReadTimeout(10000);
        this.client = new Client(config);
    }

    @Override
    public void sendVerificationCode(String phone, String code) {
        SendSmsResponseBody body = send(phone, code);
        if (!SUCCESS_CODE.equals(body.getCode())) {
            log.warn("阿里云短信发送失败: code={}, message={}, requestId={}",
                    body.getCode(), body.getMessage(), body.getRequestId());
            throw BusinessException.serviceUnavailable("短信发送失败，请稍后重试");
        }
    }

    private SendSmsResponseBody send(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(objectMapper.writeValueAsString(Map.of("code", code)));
            return client.sendSms(request).getBody();
        } catch (Exception exception) {
            log.error("调用阿里云短信接口异常", exception);
            throw BusinessException.serviceUnavailable("短信发送失败，请稍后重试");
        }
    }
}
