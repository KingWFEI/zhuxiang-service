package com.zhuxiang.service;

import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.config.SmsProductionSafetyValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsProductionSafetyValidatorTests {

    @Test
    void rejectsNoneProviderOutsideDev() {
        SmsCodeProperties properties = completeAliyunProperties();
        properties.setProvider("none");

        assertThatThrownBy(() -> validate(properties))
                .hasMessageContaining("阿里云短信");
    }

    @Test
    void rejectsMissingAliyunCredentialsOrTemplate() {
        SmsCodeProperties properties = completeAliyunProperties();
        properties.getAliyun().setAccessKeySecret(" ");

        assertThatThrownBy(() -> validate(properties))
                .hasMessageContaining("ALIYUN_ACCESS_KEY_SECRET");
    }

    @Test
    void acceptsCompleteAliyunConfiguration() {
        assertThatCode(() -> validate(completeAliyunProperties())).doesNotThrowAnyException();
    }

    private static SmsCodeProperties completeAliyunProperties() {
        SmsCodeProperties properties = new SmsCodeProperties();
        properties.setProvider("aliyun");
        properties.setRateLimitStore("redis");
        properties.getAliyun().setAccessKeyId("test-id");
        properties.getAliyun().setAccessKeySecret("test-secret");
        properties.getAliyun().setSignName("测试签名");
        properties.getAliyun().setTemplateCode("SMS_000000000");
        return properties;
    }

    private static void validate(SmsCodeProperties properties) {
        new SmsProductionSafetyValidator(properties).afterPropertiesSet();
    }
}
