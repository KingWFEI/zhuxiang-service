package com.zhuxiang.service;

import com.zhuxiang.service.config.AlipayProductionGuard;
import com.zhuxiang.service.config.AlipayProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlipayProductionGuardTests {

    @Test
    void acceptsCompleteOfficialAppPaymentConfiguration() {
        AlipayProductionGuard guard = new AlipayProductionGuard(validProperties());

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsSandboxOrMockProductionConfiguration() {
        AlipayProperties sandbox = validProperties();
        sandbox.setGatewayUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        assertThatThrownBy(() -> new AlipayProductionGuard(sandbox).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("正式网关");

        AlipayProperties mock = validProperties();
        mock.setMockEnabled(true);
        assertThatThrownBy(() -> new AlipayProductionGuard(mock).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mock");
    }

    @Test
    void rejectsMissingCredentialsAndLocalNotifyUrl() {
        AlipayProperties missingKey = validProperties();
        missingKey.setMerchantPrivateKey("");
        assertThatThrownBy(() -> new AlipayProductionGuard(missingKey).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("商户私钥");

        AlipayProperties localNotify = validProperties();
        localNotify.setNotifyUrl("http://localhost:8000/api/payments/alipay/notify");
        assertThatThrownBy(() -> new AlipayProductionGuard(localNotify).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
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
