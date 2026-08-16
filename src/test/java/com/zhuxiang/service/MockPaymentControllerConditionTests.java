package com.zhuxiang.service;

import com.zhuxiang.service.controller.MockPaymentController;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.RentOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockPaymentControllerConditionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RentOrderService.class, () -> mock(RentOrderService.class))
            .withBean(PaymentRecordService.class, () -> mock(PaymentRecordService.class))
            .withUserConfiguration(MockPaymentController.class);

    @Test
    void mockEndpointIsAbsentUnlessExplicitlyEnabled() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(MockPaymentController.class));
    }

    @Test
    void developmentCanExplicitlyEnableMockEndpoint() {
        contextRunner
                .withPropertyValues("alipay.mock-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MockPaymentController.class));
    }
}
