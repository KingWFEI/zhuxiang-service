package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.entity.SmsCode;
import com.zhuxiang.service.mapper.SmsCodeMapper;
import com.zhuxiang.service.service.SmsRateLimiter;
import com.zhuxiang.service.service.impl.SmsCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsCodeServiceTests {

    private final SmsCodeMapper mapper = mock(SmsCodeMapper.class);
    private final SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
    private final SmsCodeProperties properties = new SmsCodeProperties();
    private SmsCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SmsCodeServiceImpl(properties, rateLimiter);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(rateLimiter.acquire(any(), any(), any()))
                .thenReturn(SmsRateLimiter.RateLimitDecision.permit());
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(mapper.insert(any(SmsCode.class))).thenReturn(1);
    }

    @Test
    void sendInvalidatesOldCodeAndReturnsRetryAfter() {
        AuthDtos.SmsCodeResult result = service.sendSmsCode(
                new AuthDtos.SmsCodeRequest("13800138000", "login"), "127.0.0.1"
        );

        ArgumentCaptor<SmsCode> captor = ArgumentCaptor.forClass(SmsCode.class);
        verify(mapper).update(isNull(), any(Wrapper.class));
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getCode()).matches("\\d{6}");
        assertThat(captor.getValue().getFailedAttempts()).isZero();
        assertThat(result.expiresIn()).isEqualTo(300);
        assertThat(result.retryAfter()).isEqualTo(60);
    }

    @Test
    void rejectedSendReturnsRetryAfterIn429Data() {
        when(rateLimiter.acquire(any(), any(), any()))
                .thenReturn(SmsRateLimiter.RateLimitDecision.rejected(37));

        assertThatThrownBy(() -> service.sendSmsCode(
                new AuthDtos.SmsCodeRequest("13800138000", "login"), "127.0.0.1"
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(429);
            assertThat(exception.getData()).isEqualTo(new AuthDtos.SmsCodeRetry(37));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void codeBecomesInvalidAfterMaximumFailedAttempts() {
        SmsCode code = activeCode("123456");
        when(mapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(code);
        when(mapper.updateById(code)).thenReturn(1);

        for (int attempt = 1; attempt <= properties.getMaxVerifyAttempts(); attempt++) {
            assertThatThrownBy(() -> service.consumeSmsCode(
                    "13800138000", "login", "000000"
            )).isInstanceOf(BusinessException.class);
        }

        assertThat(code.getFailedAttempts()).isEqualTo(properties.getMaxVerifyAttempts());
        assertThat(code.getUsed()).isEqualTo(1);
        assertThat(code.getUsedAt()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void correctCodeIsConsumedOnce() {
        SmsCode code = activeCode("123456");
        when(mapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(code);
        when(mapper.updateById(code)).thenReturn(1);

        service.consumeSmsCode("13800138000", "login", "123456");

        assertThat(code.getUsed()).isEqualTo(1);
        assertThat(code.getUsedAt()).isNotNull();
    }

    private SmsCode activeCode(String value) {
        SmsCode code = new SmsCode();
        code.setId("sms-1");
        code.setPhone("13800138000");
        code.setScene("login");
        code.setCode(value);
        code.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        code.setUsed(0);
        code.setFailedAttempts(0);
        code.setCreatedAt(LocalDateTime.now());
        return code;
    }
}
