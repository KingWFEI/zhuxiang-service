package com.zhuxiang.service.client;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockInitializeResponse;
import com.zhuxiang.service.dto.TtLockDetailResponse;
import com.zhuxiang.service.dto.TtLockPeriodPasscodeResponse;
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
     * 查询门锁密码版本和门锁时区；响应中的管理员凭证不会被映射。
     */
    public TtLockDetailResponse getLockDetail(String clientId, String accessToken, Long lockId) {
        MultiValueMap<String, String> params = baseLockParams(clientId, accessToken, lockId);
        return postForm("/v3/lock/detail", params, TtLockDetailResponse.class, "TTLock 门锁详情查询");
    }

    /**
     * 生成 V4 期限密码。密码需在生效后的 24 小时内至少使用一次，否则可能失效。
     */
    public TtLockPeriodPasscodeResponse getPeriodPasscode(
            String clientId,
            String accessToken,
            Long lockId,
            int keyboardPwdVersion,
            int keyboardPwdType,
            String keyboardPwdName,
            long startDate,
            long endDate
    ) {
        MultiValueMap<String, String> params = baseLockParams(clientId, accessToken, lockId);
        params.add("keyboardPwdVersion", String.valueOf(keyboardPwdVersion));
        params.add("keyboardPwdType", String.valueOf(keyboardPwdType));
        params.add("keyboardPwdName", keyboardPwdName);
        params.add("startDate", String.valueOf(startDate));
        params.add("endDate", String.valueOf(endDate));
        return postForm(
                "/v3/keyboardPwd/get",
                params,
                TtLockPeriodPasscodeResponse.class,
                "TTLock 期限密码生成"
        );
    }

    /** 构造门锁接口通用表单参数。 */
    private MultiValueMap<String, String> baseLockParams(String clientId, String accessToken, Long lockId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("clientId", clientId);
        params.add("accessToken", accessToken);
        params.add("lockId", String.valueOf(lockId));
        params.add("date", String.valueOf(System.currentTimeMillis()));
        return params;
    }

    /** 统一发送表单请求并转换安全响应模型。 */
    private <T> T postForm(
            String path,
            MultiValueMap<String, String> params,
            Class<T> responseType,
            String operationName
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<T> response = restTemplate.postForEntity(
                    normalizeBaseUrl() + path,
                    new HttpEntity<>(params, headers),
                    responseType
            );
            T body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw BusinessException.badRequest(operationName + "失败");
            }
            return body;
        } catch (ResourceAccessException exception) {
            throw BusinessException.badRequest(operationName + "请求超时或网络不可用");
        } catch (RestClientException exception) {
            throw BusinessException.badRequest(operationName + "请求失败");
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
