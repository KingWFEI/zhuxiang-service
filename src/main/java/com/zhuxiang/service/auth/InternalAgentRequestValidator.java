package com.zhuxiang.service.auth;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AgentProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 校验 Python Agent 到 Spring Boot 内部接口的密钥、来源标识和源地址。 */
@Component
public class InternalAgentRequestValidator {

    private static final Logger log = LoggerFactory.getLogger(InternalAgentRequestValidator.class);
    private final AgentProperties properties;

    public InternalAgentRequestValidator(AgentProperties properties) {
        this.properties = properties;
    }

    public String validate(HttpServletRequest request) {
        String requestId = normalizeRequestId(request.getHeader("X-Request-Id"));
        String actualKey = request.getHeader("X-Internal-Api-Key");
        String source = request.getHeader("X-Internal-Source");
        String remoteAddress = normalizeAddress(request.getRemoteAddr());

        boolean valid = secureEquals(actualKey, properties.getApiKey())
                && secureEquals(source, properties.getExpectedSource())
                && allowedAddresses().contains(remoteAddress);
        if (!valid) {
            log.warn("拒绝内部客服接口调用: requestId={} source={} remote={}",
                    requestId, source, remoteAddress);
            throw BusinessException.forbidden("内部接口鉴权失败");
        }
        return requestId;
    }

    private Set<String> allowedAddresses() {
        return Arrays.stream(properties.getAllowedSourceAddresses().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::normalizeAddress)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeAddress(String address) {
        if (address == null) return "";
        String normalized = address.trim().toLowerCase();
        return normalized.startsWith("::ffff:") ? normalized.substring(7) : normalized;
    }

    private boolean secureEquals(String actual, String expected) {
        if (actual == null || expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeRequestId(String value) {
        if (value != null && value.matches("[A-Za-z0-9._:-]{1,64}")) return value;
        return UUID.randomUUID().toString();
    }
}
