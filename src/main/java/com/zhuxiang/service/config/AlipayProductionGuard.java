package com.zhuxiang.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 生产环境支付安全门禁。禁止带着沙箱、Mock 或缺失的商户配置启动。
 */
@Component
@Profile({"prod", "production"})
public class AlipayProductionGuard implements InitializingBean {

    static final String OFFICIAL_GATEWAY = "https://openapi.alipay.com/gateway.do";

    private final AlipayProperties properties;

    public AlipayProductionGuard(AlipayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!OFFICIAL_GATEWAY.equals(trim(properties.getGatewayUrl()))) {
            throw invalid("必须使用支付宝正式网关");
        }
        if (!"app".equalsIgnoreCase(trim(properties.getPayType()))) {
            throw invalid("必须使用 APP 支付");
        }
        if (properties.isMockEnabled()) {
            throw invalid("不允许开启 Mock 支付");
        }
        require("支付宝 APP_ID", properties.getAppId());
        require("商户私钥", properties.getMerchantPrivateKey());
        require("支付宝公钥", properties.getAlipayPublicKey());
        requireHttpsNotifyUrl(properties.getNotifyUrl());
    }

    private static void require(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw invalid(name + "未配置");
        }
    }

    private static void requireHttpsNotifyUrl(String value) {
        require("支付宝异步通知地址", value);
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw invalid("异步通知地址格式错误");
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(host)
                || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            throw invalid("异步通知地址必须是外网 HTTPS 地址");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("生产支付配置不安全：" + reason);
    }
}
