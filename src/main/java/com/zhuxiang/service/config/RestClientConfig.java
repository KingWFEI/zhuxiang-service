package com.zhuxiang.service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 外部HTTP客户端配置。
 */
@Configuration
public class RestClientConfig {

    /**
     * 创建带超时的RestTemplate实例。
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** e签宝模板和签署接口响应相对较慢，使用独立且可配置的超时。 */
    @Bean("esignRestTemplate")
    public RestTemplate esignRestTemplate(
            RestTemplateBuilder builder,
            EsignV3Properties properties
    ) {
        return builder
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .build();
    }
}
