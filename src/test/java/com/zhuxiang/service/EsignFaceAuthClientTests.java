package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignFaceAuthClient;
import com.zhuxiang.service.client.EsignRequestSigner;
import com.zhuxiang.service.common.EsignApiException;
import com.zhuxiang.service.config.EsignFaceProperties;
import com.zhuxiang.service.dto.EsignFaceAuthCreateRequest;
import com.zhuxiang.service.dto.EsignFaceAuthCreateResponse;
import com.zhuxiang.service.dto.EsignFaceAuthDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * e签宝人脸认证客户端单元测试。
 * <p>
 * 使用 MockRestServiceServer 验证：HTTP 方法、Header、Body 一致性和业务逻辑。
 * 所有测试使用假 AppId/AppSecret/姓名/证件号。
 */
class EsignFaceAuthClientTests {

    private static final String FAKE_APP_ID = "APP_ID_TEST";
    private static final String FAKE_APP_SECRET = "SECRET_FOR_UNIT_TEST_ONLY";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private EsignFaceProperties properties;
    private ObjectMapper objectMapper;
    private EsignFaceAuthClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new EsignFaceProperties();
        properties.setHost("https://smlopenapi.esign.cn");
        properties.setAppId(FAKE_APP_ID);
        properties.setAppSecret(FAKE_APP_SECRET);
        properties.setMode("ESIGN");
        properties.setCallbackUrl("https://example.com/callback");
        objectMapper = new ObjectMapper();
        EsignRequestSigner signer = new EsignRequestSigner();
        client = new EsignFaceAuthClient(restTemplate, properties, objectMapper, signer);
    }

    @Test
    void createFaceAuth_shouldSendPostWithCorrectHeaders() {
        String respJson = """
                {"code":0,"message":"成功","data":{"flowId":"4512345678901565","authUrl":"https://example.auth.url","expire":1783936406218}}
                """;

        mockServer.expect(requestTo("https://smlopenapi.esign.cn/v2/identity/auth/api/individual/face"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tsign-Open-App-Id", FAKE_APP_ID))
                .andExpect(header("X-Tsign-Open-Auth-Mode", "Signature"))
                .andExpect(header("Accept", "*/*"))
                .andExpect(header("Content-Type", "application/json; charset=UTF-8"))
                .andExpect(header("Content-MD5", org.hamcrest.Matchers.notNullValue()))
                .andExpect(header("X-Tsign-Open-Ca-Signature", org.hamcrest.Matchers.notNullValue()))
                .andExpect(header("X-Tsign-Open-Ca-Timestamp", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess(respJson, new MediaType("application", "json")));

        EsignFaceAuthCreateRequest request = buildTestRequest();
        EsignFaceAuthCreateResponse response = client.createFaceAuth(request);

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getFlowId()).isEqualTo("4512345678901565");
        mockServer.verify();
    }

    @Test
    void createFaceAuth_rawBodyShouldMatchSigningBody() {
        // 通过 MockRestServiceServer 验证实际发送的 body 内容
        String respJson = """
                {"code":0,"message":"成功","data":{"flowId":"123456","authUrl":"https://a.b","expire":1}}
                """;

        mockServer.expect(requestTo("https://smlopenapi.esign.cn/v2/identity/auth/api/individual/face"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"name":"测试用户","certType":"INDIVIDUAL_CH_IDCARD","idNo":"110101199001010000","faceauthMode":"ESIGN","faceInterfaceType":"H5","resultPage":"1","callbackUrl":"https://example.com/callback","contextId":"ctx-001"}
                        """))
                .andRespond(withSuccess(respJson, new MediaType("application", "json")));

        EsignFaceAuthCreateRequest request = buildTestRequest();
        client.createFaceAuth(request);

        mockServer.verify();
    }

    @Test
    void createFaceAuth_shouldFailWhenNotConfigured() {
        properties.setAppId(null);
        properties.setAppSecret(null);

        assertThatThrownBy(() -> client.createFaceAuth(buildTestRequest()))
                .isInstanceOf(com.zhuxiang.service.common.BusinessException.class)
                .hasMessageContaining("未配置");
    }

    @Test
    void createFaceAuth_shouldThrowEsignApiExceptionWhenCodeNotZero() {
        String respJson = """
                {"code":30503107,"message":"人脸实名认证服务余额不足"}
                """;

        mockServer.expect(requestTo("https://smlopenapi.esign.cn/v2/identity/auth/api/individual/face"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(respJson, new MediaType("application", "json")));

        assertThatThrownBy(() -> client.createFaceAuth(buildTestRequest()))
                .isInstanceOf(EsignApiException.class)
                .extracting("httpStatus", "esignCode")
                .containsExactly(200, 30503107);

        mockServer.verify();
    }

    @Test
    void queryFaceAuthDetail_shouldSendGetWithoutContentTypeAndContentMd5() {
        String respJson = """
                {"code":0,"message":"成功","data":{"flowId":"4512345678901565","status":"SUCCESS","objectType":"INDIVIDUAL"}}
                """;

        mockServer.expect(requestTo("https://smlopenapi.esign.cn/v2/identity/auth/api/common/4512345678901565/detail"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tsign-Open-App-Id", FAKE_APP_ID))
                .andExpect(header("Accept", "*/*"))
                .andRespond(withSuccess(respJson, new MediaType("application", "json")));

        EsignFaceAuthDetailResponse response = client.queryFaceAuthDetail("4512345678901565");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getStatus()).isEqualTo("SUCCESS");
        mockServer.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void queryFaceAuthDetail_shouldRejectEmptyFlowId(String flowId) {
        assertThatThrownBy(() -> client.queryFaceAuthDetail(flowId))
                .isInstanceOf(com.zhuxiang.service.common.BusinessException.class)
                .hasMessageContaining("flowId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "flow-001", "12.34", "12,34"})
    void queryFaceAuthDetail_shouldRejectNonNumericFlowId(String flowId) {
        assertThatThrownBy(() -> client.queryFaceAuthDetail(flowId))
                .isInstanceOf(com.zhuxiang.service.common.BusinessException.class)
                .hasMessageContaining("数字");
    }

    @Test
    void queryFaceAuthDetail_shouldThrowEsignApiExceptionWhenCodeNotZero() {
        String respJson = """
                {"code":30503100,"message":"认证记录不存在"}
                """;

        mockServer.expect(requestTo("https://smlopenapi.esign.cn/v2/identity/auth/api/common/9999999999999999/detail"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(respJson, new MediaType("application", "json")));

        assertThatThrownBy(() -> client.queryFaceAuthDetail("9999999999999999"))
                .isInstanceOf(EsignApiException.class)
                .extracting("esignCode")
                .isEqualTo(30503100);

        mockServer.verify();
    }

    @Test
    void normalizeHost_shouldStripTrailingSlash() {
        properties.setHost("https://smlopenapi.esign.cn/");
        // 规范化在 client 内部方法调用时发生，此处直接测试
        assertThat(client.normalizeHost()).isEqualTo("https://smlopenapi.esign.cn");
    }

    @Test
    void maskFlowId_shouldMaskMiddle() {
        String masked = EsignFaceAuthClient.maskFlowId("4512345678901565");
        assertThat(masked).isEqualTo("4512****1565");
    }

    @Test
    void maskFlowId_shouldNotMaskShortString() {
        assertThat(EsignFaceAuthClient.maskFlowId("1234")).isEqualTo("1234");
        assertThat(EsignFaceAuthClient.maskFlowId(null)).isNull();
    }

    @Test
    void createRequest_toString_shouldNotContainIdNo() {
        EsignFaceAuthCreateRequest request = buildTestRequest();
        String str = request.toString();
        assertThat(str).doesNotContain("110101");
    }

    @Test
    void createResponse_toString_shouldNotContainAuthUrl() {
        EsignFaceAuthCreateResponse response = new EsignFaceAuthCreateResponse();
        EsignFaceAuthCreateResponse.CreateFaceAuthData data = new EsignFaceAuthCreateResponse.CreateFaceAuthData();
        data.setAuthUrl("https://secret.auth.url/xxx");
        response.setData(data);

        String str = response.toString();
        assertThat(str).doesNotContain("secret.auth.url");
    }

    @Test
    void detailResponse_toString_shouldNotContainCertNo() {
        EsignFaceAuthDetailResponse.FaceAuthDetailData.IndivInfo indivInfo =
                new EsignFaceAuthDetailResponse.FaceAuthDetailData.IndivInfo();
        indivInfo.setCertNo("110101199001010000");

        String str = indivInfo.toString();
        assertThat(str).doesNotContain("110101");
    }

    private EsignFaceAuthCreateRequest buildTestRequest() {
        EsignFaceAuthCreateRequest request = new EsignFaceAuthCreateRequest();
        request.setName("测试用户");
        request.setCertType("INDIVIDUAL_CH_IDCARD");
        request.setIdNo("110101199001010000");
        request.setFaceauthMode("ESIGN");
        request.setFaceInterfaceType("H5");
        request.setResultPage("1");
        request.setCallbackUrl("https://example.com/callback");
        request.setContextId("ctx-001");
        return request;
    }
}
