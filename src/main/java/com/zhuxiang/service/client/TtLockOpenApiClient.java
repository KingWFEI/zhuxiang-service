package com.zhuxiang.service.client;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockInitializeResponse;
import com.zhuxiang.service.dto.TtLockSendEKeyResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 通通锁开放平台HTTP客户端。
 */
@Component
public class TtLockOpenApiClient {

    private final RestTemplate restTemplate;
    private final TtLockProperties properties;

    public TtLockOpenApiClient(RestTemplate restTemplate, TtLockProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 调用通通锁开放平台初始化门锁。
     */
    public TtLockInitializeResponse initializeLock(
            String clientId,
            String accessToken,
            String lockData,
            String lockAlias
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("clientId", clientId);
        params.add("accessToken", accessToken);
        params.add("lockData", lockData);
        params.add("lockAlias", lockAlias);
        params.add("date", String.valueOf(System.currentTimeMillis()));

        try {
            ResponseEntity<TtLockInitializeResponse> response = restTemplate.postForEntity(
                    normalizeBaseUrl() + "/v3/lock/initialize",
                    new HttpEntity<>(params, headers),
                    TtLockInitializeResponse.class
            );
            TtLockInitializeResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw BusinessException.badRequest("通通锁开放平台初始化失败");
            }
            return body;
        } catch (ResourceAccessException exception) {
            throw BusinessException.badRequest("通通锁开放平台请求超时或网络不可用");
        } catch (RestClientException exception) {
            throw BusinessException.badRequest("通通锁开放平台请求失败");
        }
    }

    /**
     * 给租客TTLock账号发送指定有效期的eKey。
     */
    public TtLockSendEKeyResponse sendEKey(
            String clientId,
            String accessToken,
            Long lockId,
            String receiverUsername,
            String keyName,
            long startDate,
            long endDate
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("clientId", clientId);
        params.add("accessToken", accessToken);
        params.add("lockId", String.valueOf(lockId));
        params.add("receiverUsername", receiverUsername);
        params.add("keyName", keyName);
        params.add("startDate", String.valueOf(startDate));
        params.add("endDate", String.valueOf(endDate));
        params.add("createUser", "1");
        params.add("remoteEnable", "2");
        params.add("date", String.valueOf(System.currentTimeMillis()));

        try {
            ResponseEntity<TtLockSendEKeyResponse> response = restTemplate.postForEntity(
                    normalizeBaseUrl() + "/v3/key/send",
                    new HttpEntity<>(params, headers),
                    TtLockSendEKeyResponse.class
            );
            TtLockSendEKeyResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw BusinessException.badRequest("TTLock eKey下发失败");
            }
            return body;
        } catch (ResourceAccessException exception) {
            throw BusinessException.badRequest("TTLock eKey请求超时或网络不可用");
        } catch (RestClientException exception) {
            throw BusinessException.badRequest("TTLock eKey请求失败");
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
