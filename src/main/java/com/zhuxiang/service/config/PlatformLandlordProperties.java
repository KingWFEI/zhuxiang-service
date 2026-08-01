package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 平台自营房源使用的固定出租主体配置。
 */
@Component
@ConfigurationProperties(prefix = "app.platform-landlord")
public class PlatformLandlordProperties {

    public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000001";

    private String id = DEFAULT_ID;
    private String phone = "00000000000";
    private String name = "勿忧管家";
    private String avatarUrl = "";
    private String responseDescription = "平台自营房源";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }
}
