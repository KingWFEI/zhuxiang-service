package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "amap")
public class AmapProperties {

    private String baseUrl = "https://restapi.amap.com";
    private String webServiceKey;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getWebServiceKey() { return webServiceKey; }
    public void setWebServiceKey(String webServiceKey) { this.webServiceKey = webServiceKey; }

    @Deprecated
    public String getKey() { return webServiceKey; }
}
