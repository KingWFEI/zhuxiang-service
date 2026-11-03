package com.zhuxiang.service;

import com.zhuxiang.service.auth.AgentToolPermissionService;
import com.zhuxiang.service.auth.InternalAgentRequestValidator;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalAgentRequestValidatorTests {

    private static final String TOOL = "lease_query";

    private InternalAgentRequestValidator validator;

    @BeforeEach
    void setUp() {
        AgentProperties properties = new AgentProperties();
        properties.setApiKey("test-secret");
        properties.setExpectedSource("zhuxiang-agent");
        properties.setAllowedSourceAddresses("127.0.0.1,::1");
        AgentToolPermissionService permissionService = mock(AgentToolPermissionService.class);
        when(permissionService.mayAccessAnyUser(anyString())).thenReturn(false);
        validator = new InternalAgentRequestValidator(properties, permissionService);
    }

    @Test
    void acceptsMatchingKeySourceAndAddress() {
        MockHttpServletRequest request = request("test-secret", "zhuxiang-agent", "127.0.0.1");
        request.addHeader("X-Request-Id", "request-123");
        assertEquals("request-123", validator.validate(request, TOOL, null));
    }

    @Test
    void acceptsEquivalentExpandedIpv6LoopbackAddress() {
        MockHttpServletRequest request = request(
                "test-secret", "zhuxiang-agent", "0:0:0:0:0:0:0:1");
        assertEquals("request-123", validator.validate(withRequestId(request), TOOL, null));
    }

    @Test
    void acceptsIpv4MappedIpv6Address() {
        MockHttpServletRequest request = request(
                "test-secret", "zhuxiang-agent", "::ffff:127.0.0.1");
        assertEquals("request-123", validator.validate(withRequestId(request), TOOL, null));
    }

    @Test
    void rejectsWrongKey() {
        assertThrows(BusinessException.class,
                () -> validator.validate(
                        request("wrong", "zhuxiang-agent", "127.0.0.1"), TOOL, null));
    }

    @Test
    void rejectsWrongSourceOrAddress() {
        assertThrows(BusinessException.class,
                () -> validator.validate(request("test-secret", "other", "127.0.0.1"), TOOL, null));
        assertThrows(BusinessException.class,
                () -> validator.validate(
                        request("test-secret", "zhuxiang-agent", "10.0.0.8"), TOOL, null));
    }

    @Test
    void rejectsCallWithoutTrustedAgentContext() {
        MockHttpServletRequest request = request("test-secret", "zhuxiang-agent", "127.0.0.1");
        request.removeHeader("X-Agent-User-Id");
        request.removeHeader("X-Agent-User-Role");
        request.removeHeader("X-Agent-Tool");
        assertThrows(BusinessException.class, () -> validator.validate(request, TOOL, null));
    }

    /** 构造带完整可信 Agent 上下文的请求，用例可覆写任一维度以验证拒绝路径。 */
    private MockHttpServletRequest request(String key, String source, String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", key);
        request.addHeader("X-Internal-Source", source);
        request.addHeader("X-Agent-User-Id", "user-1");
        request.addHeader("X-Agent-User-Role", "TENANT");
        request.addHeader("X-Agent-Tool", TOOL);
        request.setRemoteAddr(address);
        return request;
    }

    private MockHttpServletRequest withRequestId(MockHttpServletRequest request) {
        request.addHeader("X-Request-Id", "request-123");
        return request;
    }
}
