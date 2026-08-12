package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignRequestSigner;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.LeaseContractFillData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class EsignV3TemplateMappingTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EsignV3Client client = new EsignV3Client(
            new RestTemplate(), new EsignV3Properties(), objectMapper, new EsignRequestSigner());

    @Test
    void shouldDeserializeComponentsAndNestedSignerRoleFromTemplateDetail() throws Exception {
        String json = """
                {
                  "code": 0,
                  "data": {
                    "docTemplateId": "template-id",
                    "components": [{
                      "componentId": "sign-id",
                      "componentName": "个人章/签名1",
                      "componentType": 6,
                      "required": true,
                      "componentPosition": {
                        "componentPositionX": 99.03,
                        "componentPositionY": 775.47,
                        "componentPageNum": 3
                      },
                      "componentSize": {"componentWidth": 100, "componentHeight": 50},
                      "componentSpecialAttribute": {"signerRole": "甲方"}
                    }]
                  }
                }
                """;

        EsignV3Client.TemplateDetailResponse response =
                objectMapper.readValue(json, EsignV3Client.TemplateDetailResponse.class);

        assertThat(response.getData().getComponents()).hasSize(1);
        EsignV3Client.TemplateDetailResponse.StructComponent component =
                response.getData().getComponents().get(0);
        assertThat(component.getComponentName()).isEqualTo("个人章/签名1");
        assertThat(component.getComponentSpecialAttribute().getSignerRole()).isEqualTo("甲方");
        assertThat(component.getComponentPosition().getPageNum()).isEqualTo(3);
        assertThat(component.getComponentPosition().getX()).isEqualByComparingTo("99.03");
        assertThat(component.getComponentSize().getWidth()).isEqualByComparingTo("100");
    }

    @Test
    void shouldConfigureTenantFirstAndLandlordSecond() {
        LeaseContractFillData data = LeaseContractFillData.builder()
                .tenantName("租客").tenantMobile("13800138000").tenantIdCard("tenant-id")
                .lessorName("房东").lessorMobile("13900139000").lessorIdCard("lessor-id")
                .build();

        EsignV3Client.CreateSignFlowRequest request = ReflectionTestUtils.invokeMethod(
                client, "buildSignFlowRequest", "file-1", data,
                1, 10D, 20D, 1, 30D, 40D);

        assertThat(request).isNotNull();
        assertThat(request.getSigners()).hasSize(2);
        assertThat(request.getSigners().get(0).getPsnSignerInfo().getPsnAccount())
                .isEqualTo("13800138000");
        assertThat(request.getSigners().get(0).getSignConfig().getSignOrder()).isEqualTo(1);
        assertThat(request.getSigners().get(1).getPsnSignerInfo().getPsnAccount())
                .isEqualTo("13900139000");
        assertThat(request.getSigners().get(1).getSignConfig().getSignOrder()).isEqualTo(2);
    }

    @Test
    void shouldConfigureTenantAndPlatformAutoSealForPlatformHouse() {
        LeaseContractFillData data = LeaseContractFillData.builder()
                .tenantName("租客").tenantMobile("13800138000").tenantIdCard("tenant-id")
                .lessorName("重庆踏山河科技有限公司")
                .build();

        EsignV3Client.CreateSignFlowRequest request = ReflectionTestUtils.invokeMethod(
                client, "buildPlatformSignFlowRequest", "file-1", data,
                1, 10D, 20D, 1, 30D, 40D);

        assertThat(request).isNotNull();
        assertThat(request.getSigners()).hasSize(2);
        assertThat(request.getSigners().get(0).getSignerType()).isZero();
        assertThat(request.getSigners().get(0).getPsnSignerInfo().getPsnAccount())
                .isEqualTo("13800138000");
        assertThat(request.getSigners().get(1).getSignerType()).isEqualTo(1);
        assertThat(request.getSigners().get(1).getPsnSignerInfo()).isNull();
        assertThat(request.getSigners().get(1).getSignFields().get(0)
                .getNormalSignFieldConfig().isAutoSign()).isTrue();
    }

    @Test
    void personalHouseRescissionShouldPreserveDefaultPlatformInitiatorWithoutTransactor() throws Exception {
        EsignV3Client.InitiateRescissionRequest request = ReflectionTestUtils.invokeMethod(
                client, "buildPlatformRescissionRequest",
                List.of("file-1"), "合作终止",
                "platform-org-1", null, false);

        assertThat(request).isNotNull();
        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"rescissionInitiator\":{\"orgInitiator\":{");
        assertThat(json).contains("\"orgId\":\"platform-org-1\"");
        assertThat(json).doesNotContain("transactor");
        assertThat(json).doesNotContain("psnInitiator");
        assertThat(json).doesNotContain("autoSignOrg");
    }

    @Test
    void platformHouseRescissionShouldUseConfiguredContractSealForAutoSign() throws Exception {
        EsignV3Properties properties = new EsignV3Properties();
        properties.setPlatformSealId("contract-seal-1");
        EsignV3Client configuredClient = new EsignV3Client(
                new RestTemplate(), properties, objectMapper, new EsignRequestSigner());

        EsignV3Client.InitiateRescissionRequest request = ReflectionTestUtils.invokeMethod(
                configuredClient, "buildPlatformRescissionRequest", List.of("file-1"), "合作终止",
                "platform-org-1", "platform-transactor-1", true);

        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"autoSignOrg\":[{\"orgId\":\"platform-org-1\"");
        assertThat(json).contains("\"sealId\":\"contract-seal-1\"");
        assertThat(json).doesNotContain("orgSignerTransactor");
    }

    @Test
    void personalHouseContractShouldAllowRescission() throws Exception {
        LeaseContractFillData data = LeaseContractFillData.builder()
                .tenantName("租客").tenantMobile("13800138000").tenantIdCard("tenant-id")
                .lessorName("房东").lessorMobile("13900139000").lessorIdCard("lessor-id")
                .build();

        EsignV3Client.CreateSignFlowRequest request = ReflectionTestUtils.invokeMethod(
                client, "buildSignFlowRequest", "file-1", data,
                1, 10D, 20D, 1, 30D, 40D);

        assertThat(request).isNotNull();
        assertThat(request.getContractConfig().isAllowToRescind()).isTrue();
        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"contractConfig\":{\"allowToRescind\":true}");
        assertThat(json).doesNotContain("\"signFlowConfig\":{\"allowToRescind\"");
    }

    @Test
    void shouldReadPlatformOrgIdFromOriginalSignFlowDetail() throws Exception {
        String json = """
                {
                  "code": 0,
                  "data": {
                    "signFlowStatus": 2,
                    "signFlowInitiator": {
                      "orgInitiator": {
                        "orgId": "platform-org-1",
                        "orgName": "重庆踏山河科技有限公司",
                        "transactor": {"psnId": "platform-transactor-1"}
                      }
                    },
                    "signers": [{
                      "signerType": 1,
                      "orgSigner": {
                        "orgId": "platform-org-1",
                        "orgName": "重庆踏山河科技有限公司",
                        "transactorInfo": {"psnId": "platform-transactor-1"}
                      }
                    }]
                  }
                }
                """;

        EsignV3Client.SignFlowDetailResponse response = objectMapper.readValue(
                json, EsignV3Client.SignFlowDetailResponse.class);

        assertThat(response.getData().getSignFlowInitiator()
                .getOrgInitiator().getOrgId()).isEqualTo("platform-org-1");
        assertThat(response.getData().getSignFlowInitiator()
                .getOrgInitiator().getTransactor().getPsnId())
                .isEqualTo("platform-transactor-1");
        assertThat(response.getData().getSigners().get(0)
                .getOrgSigner().getOrgId()).isEqualTo("platform-org-1");
        assertThat(response.getData().getSigners().get(0)
                .getOrgSigner().getTransactor().getPsnId())
                .isEqualTo("platform-transactor-1");
    }

}
