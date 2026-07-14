package com.zhuxiang.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 支付宝网关地址，沙箱默认：https://openapi-sandbox.dl.alipaydev.com/gateway.do */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 应用 APP_ID */
    private String appId;

    /** 商户私钥（PKCS8 格式） */
    private String merchantPrivateKey;

    /** 支付宝公钥 */
    private String alipayPublicKey;

    /** 异步通知地址 */
    private String notifyUrl;

    /** 支付完成后的同步跳转地址 */
    private String returnUrl;

    /** 签名算法，默认 RSA2 */
    private String signType = "RSA2";

    /** 字符编码 */
    private String charset = "UTF-8";

    /** 数据格式 */
    private String format = "json";
}
