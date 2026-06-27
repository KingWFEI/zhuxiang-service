package com.zhuxiang.service;

import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockSendEKeyResponse;
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
}
