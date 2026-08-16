package com.zhuxiang.service;

import com.zhuxiang.service.dto.PayRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestDefaultsTests {

    @Test
    void missingPaymentChannelDefaultsToAlipayInsteadOfMock() {
        PayRequest request = new PayRequest("monthly", null);

        assertThat(request.paymentChannel()).isEqualTo("alipay");
    }
}
