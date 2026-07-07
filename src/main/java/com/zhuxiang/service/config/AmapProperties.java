package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图开放平台配置。
 */
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapProperties {

    /**
     * 高德地图 Web 服务 API Key。
     */
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
