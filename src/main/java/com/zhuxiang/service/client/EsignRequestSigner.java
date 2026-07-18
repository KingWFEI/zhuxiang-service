package com.zhuxiang.service.client;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * e签宝请求签名工具。
 * <p>
 * 时间戳由调用方显式传入，保证单元测试可重复。
 * AppSecret 不暴露在返回值或日志中。
 */
@Component
public class EsignRequestSigner {

    static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    static final String ACCEPT = "*/*";

    /**
     * 计算 Content-MD5：Base64(MD5(rawBody UTF-8 bytes))
     */
    public String computeContentMd5(String rawBody) {
        return computeContentMd5(rawBody.getBytes(StandardCharsets.UTF_8));
    }

    public String computeContentMd5(byte[] rawBody) {
        byte[] md5 = md5(rawBody);
        return Base64.getEncoder().encodeToString(md5);
    }

    /**
     * 构造 POST 待签名字符串。
     */
    public String buildPostStringToSign(String path, String contentMd5) {
        return "POST" + "\n"
                + ACCEPT + "\n"
                + contentMd5 + "\n"
                + CONTENT_TYPE_JSON + "\n"
                + "\n"
                + path;
    }

    /**
     * 构造 GET 待签名字符串（Content-MD5 和 Content-Type 均为空字符串）。
     */
    public String buildGetStringToSign(String path) {
        return "GET" + "\n"
                + ACCEPT + "\n"
                + "\n"
                + "\n"
                + "\n"
                + path;
    }

    /**
     * 计算签名：Base64(HmacSHA256(stringToSign UTF-8, appSecret UTF-8))
     */
    public String sign(String appSecret, String stringToSign) {
        byte[] hmac = hmacSha256(appSecret.getBytes(StandardCharsets.UTF_8),
                stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmac);
    }

    private static byte[] md5(byte[] data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 algorithm not available", e);
        }
    }
}
