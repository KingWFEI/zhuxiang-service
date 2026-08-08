package com.zhuxiang.service;

import com.zhuxiang.service.auth.InternalAgentRequestValidator;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalAgentRequestValidatorTests {

    private InternalAgentRequestValidator validator;

    @BeforeEach
    void setUp() {
        AgentProperties properties = new AgentProperties();
        properties.setApiKey("test-secret");
        properties.setExpectedSource("zhuxiang-agent");
        properties.setAllowedSourceAddresses("127.0.0.1,::1");
        validator = new InternalAgentRequestValidator(properties);
    }

    @Test
    void acceptsMatchingKeySourceAndAddress() {
        MockHttpServletRequest request = request("test-secret", "zhuxiang-agent", "127.0.0.1");
        request.addHeader("X-Request-Id", "request-123");
        assertEquals("request-123", validator.validate(request));
    }

    @Test
    void rejectsWrongKey() {
        assertThrows(BusinessException.class,
                () -> validator.validate(request("wrong", "zhuxiang-agent", "127.0.0.1")));
    }

    @Test
    void rejectsWrongSourceOrAddress() {
        assertThrows(BusinessException.class,
                () -> validator.validate(request("test-secret", "other", "127.0.0.1")));
        assertThrows(BusinessException.class,
                () -> validator.validate(request("test-secret", "zhuxiang-agent", "10.0.0.8")));
    }

    private MockHttpServletRequest request(String key, String source, String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", key);
        request.addHeader("X-Internal-Source", source);
        request.setRemoteAddr(address);
        return request;
    }
}
