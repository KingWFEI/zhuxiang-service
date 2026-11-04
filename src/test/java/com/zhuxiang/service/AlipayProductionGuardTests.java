package com.zhuxiang.service;

import com.zhuxiang.service.config.AlipayProductionGuard;
import com.zhuxiang.service.config.AlipayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlipayProductionGuardTests {

    @Test
    void acceptsCompleteOfficialAppPaymentConfiguration() {
        AlipayProductionGuard guard = new AlipayProductionGuard(validProperties(), prodEnvironment());

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsSandboxOrMockProductionConfiguration() {
        AlipayProperties sandbox = validProperties();
        sandbox.setGatewayUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        assertThatThrownBy(() -> new AlipayProductionGuard(sandbox, prodEnvironment())
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("正式网关");

        AlipayProperties mock = validProperties();
        mock.setMockEnabled(true);
        assertThatThrownBy(() -> new AlipayProductionGuard(mock, prodEnvironment())
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mock");
    }

    @Test
    void rejectsMissingCredentialsAndLocalNotifyUrl() {
        AlipayProperties missingKey = validProperties();
        missingKey.setMerchantPrivateKey("");
        assertThatThrownBy(() -> new AlipayProductionGuard(missingKey, prodEnvironment())
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("商户私钥");

        AlipayProperties localNotify = validProperties();
        localNotify.setNotifyUrl("http://localhost:8000/api/payments/alipay/notify");
        assertThatThrownBy(() -> new AlipayProductionGuard(localNotify, prodEnvironment())
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void allowsSandboxGatewayAndH5InTestProfile() {
        AlipayProperties sandbox = validProperties();
        sandbox.setGatewayUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        sandbox.setPayType("h5");

        assertThatCode(() -> new AlipayProductionGuard(sandbox, testEnvironment())
                .afterPropertiesSet()).doesNotThrowAnyException();
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private static MockEnvironment testEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        return environment;
    }

    private static AlipayProperties validProperties() {
        AlipayProperties properties = new AlipayProperties();
        properties.setGatewayUrl("https://openapi.alipay.com/gateway.do");
        properties.setAppId("production-app-id");
        properties.setMerchantPrivateKey("merchant-private-key");
        properties.setAlipayPublicKey("alipay-public-key");
        properties.setNotifyUrl("https://api.wuyou.invalid/api/payments/alipay/notify");
        properties.setPayType("app");
        properties.setMockEnabled(false);
        return properties;
    }
}
