package com.zhuxiang.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignApiException;
import com.zhuxiang.service.config.EsignFaceProperties;
import com.zhuxiang.service.dto.EsignFaceAuthCreateRequest;
import com.zhuxiang.service.dto.EsignFaceAuthCreateResponse;
import com.zhuxiang.service.dto.EsignFaceAuthDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * e签宝个人人脸认证底层客户端。
 * <p>
 * 保证 rawBody 只序列化一次：同一份 JSON 同时用于 Content-MD5 计算、签名和 HTTP Body。
 */
@Component
public class EsignFaceAuthClient {

    private static final Logger log = LoggerFactory.getLogger(EsignFaceAuthClient.class);

    static final String FACE_AUTH_PATH = "/v2/identity/auth/api/individual/face";
    static final String FACE_AUTH_DETAIL_PATH_PREFIX = "/v2/identity/auth/api/common/";

    private final RestTemplate restTemplate;
    private final EsignFaceProperties properties;
    private final ObjectMapper objectMapper;
    private final EsignRequestSigner signer;

    public EsignFaceAuthClient(RestTemplate restTemplate,
                               EsignFaceProperties properties,
                               ObjectMapper objectMapper,
                               EsignRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.signer = signer;
    }

    /**
     * 发起个人人脸认证。
     *
     * @param request 认证请求参数
     * @return 认证发起响应（flowId / authUrl / expire）
     */
    public EsignFaceAuthCreateResponse createFaceAuth(EsignFaceAuthCreateRequest request) {
        requireConfigured();
        String path = FACE_AUTH_PATH;

        try {
            // 1. 只序列化一次
            String rawBody = objectMapper.writeValueAsString(request);
            long timestamp = System.currentTimeMillis();

            // 2. 计算签名
            String contentMd5 = signer.computeContentMd5(rawBody);
            String stringToSign = signer.buildPostStringToSign(path, contentMd5);
            String signature = signer.sign(properties.getAppSecret(), stringToSign);

            // 3. 构建 URL
            String url = normalizeHost() + path;

            // 4. 使用 execute 发送 rawBody，保证 body 不被二次序列化
            long start = System.currentTimeMillis();
            String respBody = restTemplate.execute(
                    URI.create(url),
                    HttpMethod.POST,
                    clientRequest -> {
                        clientRequest.getHeaders().set("X-Tsign-Open-App-Id", properties.getAppId());
                        clientRequest.getHeaders().set("X-Tsign-Open-Auth-Mode", "Signature");
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Signature", signature);
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Timestamp", String.valueOf(timestamp));
                        clientRequest.getHeaders().set("Accept", EsignRequestSigner.ACCEPT);
                        clientRequest.getHeaders().set("Content-Type", EsignRequestSigner.CONTENT_TYPE_JSON);
                        clientRequest.getHeaders().set("Content-MD5", contentMd5);
                        clientRequest.getBody().write(rawBody.getBytes(StandardCharsets.UTF_8));
                    },
                    this::extractResponseBody
            );

            long elapsed = System.currentTimeMillis() - start;
            EsignFaceAuthCreateResponse response = objectMapper.readValue(respBody, EsignFaceAuthCreateResponse.class);

            if (response.getCode() != 0) {
                log.warn("e签宝人脸认证调用失败: httpStatus=200, code={}, message={}, path={}, elapsed={}ms",
                        response.getCode(), safeMessage(response.getMessage()), path, elapsed);
                throw new EsignApiException(200, response.getCode(),
                        safeMessage(response.getMessage()), path);
            }

            String flowId = response.getData() != null ? response.getData().getFlowId() : null;
            log.info("e签宝人脸认证发起成功: flowId={}, path={}, elapsed={}ms",
                    maskFlowId(flowId), path, elapsed);
            return response;

        } catch (EsignApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new EsignApiException(0, 0, "e签宝服务连接超时或网络不可用", path);
        } catch (RestClientException e) {
            throw new EsignApiException(0, 0, "e签宝服务请求失败", path);
        } catch (Exception e) {
            throw new EsignApiException(0, 0, "e签宝人脸认证调用异常", path);
        }
    }

    /**
     * 查询人脸认证详情。
     *
     * @param flowId 认证流程 ID，必须为数字字符串
     * @return 认证详情
     */
    public EsignFaceAuthDetailResponse queryFaceAuthDetail(String flowId) {
        requireConfigured();
        validateFlowId(flowId);

        String path = FACE_AUTH_DETAIL_PATH_PREFIX + flowId + "/detail";

        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = signer.buildGetStringToSign(path);
            String signature = signer.sign(properties.getAppSecret(), stringToSign);
            String url = normalizeHost() + path;

            long start = System.currentTimeMillis();

            // GET 无 Body，使用 execute 完全控制 headers，避免 RestTemplate 自动添加 Content-Type
            String respBody = restTemplate.execute(
                    URI.create(url),
                    HttpMethod.GET,
                    clientRequest -> {
                        clientRequest.getHeaders().set("X-Tsign-Open-App-Id", properties.getAppId());
                        clientRequest.getHeaders().set("X-Tsign-Open-Auth-Mode", "Signature");
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Signature", signature);
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Timestamp", String.valueOf(timestamp));
                        clientRequest.getHeaders().set("Accept", EsignRequestSigner.ACCEPT);
                        // GET 不发送 Content-Type 和 Content-MD5
                    },
                    this::extractResponseBody
            );

            long elapsed = System.currentTimeMillis() - start;
            EsignFaceAuthDetailResponse response = objectMapper.readValue(respBody, EsignFaceAuthDetailResponse.class);

            if (response.getCode() != 0) {
                log.warn("e签宝认证查询失败: httpStatus=200, code={}, message={}, path={}, elapsed={}ms",
                        response.getCode(), safeMessage(response.getMessage()), path, elapsed);
                throw new EsignApiException(200, response.getCode(),
                        safeMessage(response.getMessage()), path);
            }

            log.info("e签宝认证查询成功: flowId={}, status={}, path={}, elapsed={}ms",
                    maskFlowId(flowId),
                    response.getData() != null ? response.getData().getStatus() : "UNKNOWN",
                    path, elapsed);
            return response;

        } catch (EsignApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new EsignApiException(0, 0, "e签宝服务连接超时或网络不可用", path);
        } catch (RestClientException e) {
            throw new EsignApiException(0, 0, "e签宝服务请求失败", path);
        } catch (Exception e) {
            throw new EsignApiException(0, 0, "e签宝认证查询异常", path);
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw BusinessException.badRequest(
                    "e签宝人脸认证未配置：请设置 ESIGN_FACE_APP_ID 和 ESIGN_FACE_APP_SECRET 环境变量");
        }
    }

    private void validateFlowId(String flowId) {
        if (flowId == null || flowId.isBlank()) {
            throw BusinessException.badRequest("flowId 不能为空");
        }
        if (!flowId.matches("\\d+")) {
            throw BusinessException.badRequest("flowId 必须为数字字符串");
        }
    }

    private String extractResponseBody(ClientHttpResponse response) throws java.io.IOException {
        byte[] bytes = response.getBody().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String normalizeHost() {
        String host = properties.getHost();
        if (host == null || host.isBlank()) {
            host = "https://smlopenapi.esign.cn";
        }
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    public static String maskFlowId(String flowId) {
        if (flowId == null || flowId.length() <= 8) {
            return flowId;
        }
        return flowId.substring(0, 4) + "****" + flowId.substring(flowId.length() - 4);
    }

    public static String safeMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }
}
