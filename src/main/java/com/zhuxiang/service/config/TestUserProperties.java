package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.test-user")
public class TestUserProperties {

    public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000002";

    private boolean enabled;
    private String id = DEFAULT_ID;
    private String phone = "13900000000";
    private String nickname = "App测试用户";
    private String verificationCode = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public boolean matchesLogin(String candidatePhone, String scene) {
        return enabled && phone.equals(candidatePhone) && "login".equals(scene);
    }

    public void validate() {
        if (!enabled) {
            return;
        }
        if (id == null || id.isBlank() || id.length() > 36) {
            throw new IllegalStateException("测试用户ID不能为空且不能超过36个字符");
        }
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new IllegalStateException("测试用户手机号格式错误");
        }
        if (nickname == null || nickname.isBlank() || nickname.length() > 30) {
            throw new IllegalStateException("测试用户昵称不能为空且不能超过30个字符");
        }
        if (verificationCode == null || !verificationCode.matches("^\\d{6}$")) {
            throw new IllegalStateException("测试用户验证码必须是6位数字");
        }
    }
}
