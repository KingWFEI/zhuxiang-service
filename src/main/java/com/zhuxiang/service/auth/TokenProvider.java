package com.zhuxiang.service.auth;

import com.zhuxiang.service.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class TokenProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] secret;
    private final long accessTokenSeconds;

    public TokenProvider(
            @Value("${app.auth.token-secret}") String secret,
            @Value("${app.auth.access-token-seconds}") long accessTokenSeconds
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String createAccessToken(String userId) {
        long expiresAt = Instant.now().getEpochSecond() + accessTokenSeconds;
        String payload = userId + "." + expiresAt;
        return encode(userId) + "." + expiresAt + "." + encode(sign(payload));
    }

    public String parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw BusinessException.unauthorized("Token 格式错误");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
                    + "." + parts[1];
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(payload), actualSignature)) {
                throw BusinessException.unauthorized("Token 签名无效");
            }
            String decodedUserId = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw BusinessException.unauthorized("Token 已过期");
            }
            return decodedUserId;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BusinessException.unauthorized("Token 无效");
        }
    }

    public String createRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return encode(bytes);
    }

    public long accessTokenSeconds() {
        return accessTokenSeconds;
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 Token", exception);
        }
    }

    private String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
