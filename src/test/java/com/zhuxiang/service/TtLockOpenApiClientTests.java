package com.zhuxiang.service;

import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockSendEKeyResponse;
import com.zhuxiang.service.dto.TtLockDetailResponse;
import com.zhuxiang.service.dto.TtLockPeriodPasscodeResponse;
import com.zhuxiang.service.dto.TtLockOperationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TtLockOpenApiClientTests {

    @Test
    void sendsEKeyAsFormWithRequiredPlatformParameters() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TtLockProperties properties = new TtLockProperties();
        properties.setBaseUrl("https://ttlock.example.com/");
        TtLockOpenApiClient client = new TtLockOpenApiClient(restTemplate, properties);

        server.expect(requestTo("https://ttlock.example.com/v3/key/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(containsString("clientId=client-id")))
                .andExpect(content().string(containsString("accessToken=access-token")))
                .andExpect(content().string(containsString("lockId=12345")))
                .andExpect(content().string(containsString("receiverUsername=13800138000")))
                .andExpect(content().string(containsString("startDate=1000")))
                .andExpect(content().string(containsString("endDate=2000")))
                .andExpect(content().string(containsString("createUser=1")))
                .andExpect(content().string(containsString("remoteEnable=2")))
                .andExpect(content().string(containsString("date=")))
                .andRespond(withSuccess("{\"keyId\":9876,\"errcode\":0}", MediaType.APPLICATION_JSON));

        TtLockSendEKeyResponse response = client.sendEKey(
                "client-id",
                "access-token",
                12345L,
                "13800138000",
                "1508King门锁钥匙",
                1000L,
                2000L
        );

        assertThat(response.success()).isTrue();
        assertThat(response.getKeyId()).isEqualTo(9876L);
        server.verify();
    }

    @Test
    void deletesEKeyByPlatformKeyId() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TtLockProperties properties = new TtLockProperties();
        properties.setBaseUrl("https://ttlock.example.com/");
        TtLockOpenApiClient client = new TtLockOpenApiClient(restTemplate, properties);

        server.expect(requestTo("https://ttlock.example.com/v3/key/delete"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(containsString("clientId=client-id")))
                .andExpect(content().string(containsString("accessToken=access-token")))
                .andExpect(content().string(containsString("keyId=9876")))
                .andExpect(content().string(containsString("date=")))
                .andRespond(withSuccess("{\"errcode\":0,\"errmsg\":\"none error message\"}", MediaType.APPLICATION_JSON));

        TtLockOperationResponse response = client.deleteEKey("client-id", "access-token", 9876L);

        assertThat(response.success()).isTrue();
        server.verify();
    }

    @Test
    void readsOnlyRequiredLockCapabilityFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TtLockProperties properties = new TtLockProperties();
        properties.setBaseUrl("https://ttlock.example.com/");
        TtLockOpenApiClient client = new TtLockOpenApiClient(restTemplate, properties);

        server.expect(requestTo("https://ttlock.example.com/v3/lock/detail"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("clientId=client-id")))
                .andExpect(content().string(containsString("accessToken=access-token")))
                .andExpect(content().string(containsString("lockId=12345")))
                .andRespond(withSuccess("""
                        {"keyboardPwdVersion":4,"timezoneRawOffset":28800000,
                         "adminPwd":"must-not-map","noKeyPwd":"must-not-map"}
                        """, MediaType.APPLICATION_JSON));

        TtLockDetailResponse response = client.getLockDetail("client-id", "access-token", 12345L);

        assertThat(response.success()).isTrue();
        assertThat(response.getKeyboardPwdVersion()).isEqualTo(4);
        assertThat(response.getTimezoneRawOffset()).isEqualTo(28_800_000L);
        server.verify();
    }

    @Test
    void getsPeriodPasscodeAsFormWithV4PeriodParameters() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TtLockProperties properties = new TtLockProperties();
        properties.setBaseUrl("https://ttlock.example.com/");
        TtLockOpenApiClient client = new TtLockOpenApiClient(restTemplate, properties);

        server.expect(requestTo("https://ttlock.example.com/v3/keyboardPwd/get"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(containsString("keyboardPwdVersion=4")))
                .andExpect(content().string(containsString("keyboardPwdType=3")))
                .andExpect(content().string(containsString("keyboardPwdName=lease-1")))
                .andExpect(content().string(containsString("startDate=1000")))
                .andExpect(content().string(containsString("endDate=2000")))
                .andExpect(content().string(containsString("date=")))
                .andRespond(withSuccess(
                        "{\"keyboardPwd\":\"839204\",\"keyboardPwdId\":9001}",
                        MediaType.APPLICATION_JSON
                ));

        TtLockPeriodPasscodeResponse response = client.getPeriodPasscode(
                "client-id", "access-token", 12345L, 4, 3, "lease-1", 1000L, 2000L
        );

        assertThat(response.success()).isTrue();
        assertThat(response.getKeyboardPwd()).isEqualTo("839204");
        assertThat(response.toString()).doesNotContain("839204");
        server.verify();
    }
}
