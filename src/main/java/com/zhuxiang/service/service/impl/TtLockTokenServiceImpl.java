package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockOAuthTokenResponse;
import com.zhuxiang.service.service.TtLockTokenService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 通通锁OAuth令牌服务。
 */
@Service
public class TtLockTokenServiceImpl implements TtLockTokenService {

    private static final long EXPIRE_SKEW_SECONDS = 300;

    private final TtLockProperties properties;
    private final RestTemplate restTemplate;

    private String cachedAccessToken;
    private String cachedRefreshToken;
    private Instant cachedExpiresAt;

    public TtLockTokenServiceImpl(TtLockProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取当前有效的通通锁accessToken，优先使用OAuth换取并缓存。
     */
    @Override
    public synchronized String getAccessToken() {
        if (isCachedTokenValid()) {
            return cachedAccessToken;
        }
        if (hasOAuthConfig()) {
            return fetchAccessTokenByPasswordGrant();
        }
        if (StringUtils.hasText(properties.getAccessToken())) {
            return properties.getAccessToken();
        }
        throw BusinessException.badRequest("通通锁 OAuth 配置未完整填写");
    }

    /**
     * 判断缓存令牌是否仍可安全使用。
     */
    private boolean isCachedTokenValid() {
        return StringUtils.hasText(cachedAccessToken)
                && cachedExpiresAt != null
                && cachedExpiresAt.isAfter(Instant.now().plusSeconds(EXPIRE_SKEW_SECONDS));
    }

    /**
     * 判断OAuth账号密码配置是否完整。
     */
    private boolean hasOAuthConfig() {
        return StringUtils.hasText(properties.getClientId())
                && StringUtils.hasText(properties.getClientSecret())
                && StringUtils.hasText(properties.getUsername())
                && StringUtils.hasText(properties.getPassword());
    }

    /**
     * 使用通通锁账号密码模式换取accessToken。
     */
    private String fetchAccessTokenByPasswordGrant() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("username", properties.getUsername());
        params.add("password", md5Password(properties.getPassword()));
        params.add("grant_type", "password");

        try {
            ResponseEntity<TtLockOAuthTokenResponse> response = restTemplate.postForEntity(
                    normalizeBaseUrl() + "/oauth2/token",
                    new HttpEntity<>(params, headers),
                    TtLockOAuthTokenResponse.class
            );
            TtLockOAuthTokenResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw BusinessException.badRequest("通通锁 OAuth 获取 accessToken 失败");
            }
            if (!StringUtils.hasText(body.getAccessToken())) {
                throw BusinessException.badRequest(safeOAuthError(body));
            }
            cachedAccessToken = body.getAccessToken();
            cachedRefreshToken = body.getRefreshToken();
            long expiresIn = body.getExpiresIn() == null ? 0 : body.getExpiresIn();
            cachedExpiresAt = Instant.now().plusSeconds(Math.max(expiresIn, EXPIRE_SKEW_SECONDS + 1));
            return cachedAccessToken;
        } catch (ResourceAccessException exception) {
            throw BusinessException.badRequest("通通锁 OAuth 请求超时或网络不可用");
        } catch (RestClientException exception) {
            throw BusinessException.badRequest("通通锁 OAuth 请求失败");
        }
    }

    /**
     * 生成不包含账号密码的OAuth错误信息。
     */
    private String safeOAuthError(TtLockOAuthTokenResponse body) {
        if (StringUtils.hasText(body.getErrorMessage())) {
            return "通通锁 OAuth 获取 accessToken 失败：" + body.getErrorMessage();
        }
        if (StringUtils.hasText(body.getErrorCode())) {
            return "通通锁 OAuth 获取 accessToken 失败，错误码：" + body.getErrorCode();
        }
        return "通通锁 OAuth 未返回 accessToken";
    }

    /**
     * 通通锁OAuth要求密码参数为MD5摘要。
     */
    private String md5Password(String password) {
        String value = password.trim();
        if (value.matches("(?i)^[0-9a-f]{32}$")) {
            return value.toLowerCase();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JDK不支持MD5算法", exception);
        }
    }

    /**
     * 规范化开放平台基础地址。
     */
    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.sciener.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
