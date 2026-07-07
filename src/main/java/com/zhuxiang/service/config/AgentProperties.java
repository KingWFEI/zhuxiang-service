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
    private String baseUrl = "http://localhost:8100";

    /** Agent 服务间调用密钥 */
    private String apiKey = "change-this-agent-api-key";

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
}
