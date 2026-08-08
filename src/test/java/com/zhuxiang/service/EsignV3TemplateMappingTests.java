package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignRequestSigner;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.LeaseContractFillData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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
}
