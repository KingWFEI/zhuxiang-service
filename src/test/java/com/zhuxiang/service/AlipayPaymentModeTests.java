package com.zhuxiang.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AlipayProperties;
import com.zhuxiang.service.service.impl.AlipayServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlipayPaymentModeTests {

    @Test
    void productionModeBuildsSignedAppOrderPayload() throws Exception {
        AlipayProperties properties = new AlipayProperties();
        properties.setPayType("app");
        properties.setNotifyUrl("https://api.example.test/payments/alipay/notify");
        AlipayServiceImpl service = new AlipayServiceImpl(properties);
        AlipayClient client = mock(AlipayClient.class);
        AlipayTradeAppPayResponse response = mock(AlipayTradeAppPayResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getBody()).thenReturn("signed-app-order");
        when(client.sdkExecute(any(AlipayTradeAppPayRequest.class))).thenReturn(response);
        ReflectionTestUtils.setField(service, "alipayClient", client);

        String payload = service.buildPayPayload("ZF001", 1234, "测试租金");

        assertThat(service.getPayType()).isEqualTo("app");
        assertThat(payload).isEqualTo("signed-app-order");
    }

    @Test
    void mockChannelIsRejectedUnlessDevelopmentExplicitlyEnablesIt() {
        AlipayProperties properties = new AlipayProperties();
        AlipayServiceImpl service = new AlipayServiceImpl(properties);

        assertThatThrownBy(() -> service.validatePaymentChannel("mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未启用");

        properties.setMockEnabled(true);
        service.validatePaymentChannel("mock");
    }
}
