package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python Agent 服务配置
 */
@Component
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    /** Python Agent 服务地址 */
    private String baseUrl = "http://localhost:8101";

    /** Agent 服务间调用密钥 */
    private String apiKey = "";

    private String model = "deepseek-v4-flash";

    private String expectedSource = "zhuxiang-agent";

    private String allowedSourceAddresses = "127.0.0.1,::1";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getExpectedSource() {
        return expectedSource;
    }

    public void setExpectedSource(String expectedSource) {
        this.expectedSource = expectedSource;
    }

    public String getAllowedSourceAddresses() {
        return allowedSourceAddresses;
    }

    public void setAllowedSourceAddresses(String allowedSourceAddresses) {
        this.allowedSourceAddresses = allowedSourceAddresses;
    }
}
