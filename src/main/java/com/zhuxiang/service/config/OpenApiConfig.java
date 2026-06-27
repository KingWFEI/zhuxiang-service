package com.zhuxiang.service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 接口文档配置。
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI zhuxiangOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("筑享租房平台接口文档")
                        .description("筑享移动端和管理端后端接口。除明确标注为公开接口外，受保护接口需在 Authorization 请求头中携带 Bearer 访问令牌。")
                        .version("v1")
                        .contact(new Contact().name("筑享开发团队"))
                        .license(new License().name("内部使用")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("访问令牌，格式：Bearer {accessToken}")
                ));
    }
}
