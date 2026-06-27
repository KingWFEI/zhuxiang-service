package com.zhuxiang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 确保新增接口不会在缺少中文 OpenAPI 描述的情况下进入导出文档。
 */
@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTests {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void everyOperationParameterAndSchemaHasDescription() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(content);

        assertThat(document.path("info").path("title").asText()).isEqualTo("筑享租房平台接口文档");
        assertThat(document.path("components").path("securitySchemes").has("bearerAuth")).isTrue();

        document.path("paths").properties().forEach(path ->
                path.getValue().properties().forEach(method -> {
                    if (!HTTP_METHODS.contains(method.getKey())) {
                        return;
                    }
                    JsonNode operation = method.getValue();
                    assertThat(operation.path("summary").asText())
                            .as("%s %s 的接口摘要", method.getKey(), path.getKey())
                            .isNotBlank();
                    assertThat(operation.path("description").asText())
                            .as("%s %s 的接口说明", method.getKey(), path.getKey())
                            .isNotBlank();
                    operation.path("parameters").forEach(parameter ->
                            assertThat(parameter.path("description").asText())
                                    .as("%s %s 的参数 %s", method.getKey(), path.getKey(), parameter.path("name").asText())
                                    .isNotBlank());
                }));

        document.path("components").path("schemas").properties().forEach(schema -> {
            assertThat(schema.getValue().path("description").asText())
                    .as("模型 %s 的说明", schema.getKey())
                    .isNotBlank();
            schema.getValue().path("properties").properties().forEach(property ->
                    assertThat(property.getValue().path("description").asText())
                            .as("模型字段 %s.%s 的说明", schema.getKey(), property.getKey())
                            .isNotBlank());
        });
    }
}
