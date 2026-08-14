package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceSession;
import com.zhuxiang.service.mapper.CustomerServiceSessionMapper;
import com.zhuxiang.service.service.impl.CustomerServiceSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceSessionServiceTests {

    private CustomerServiceSessionMapper mapper;
    private CustomerServiceSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(CustomerServiceSessionMapper.class);
        service = spy(new CustomerServiceSessionServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void reusesExistingEmptySessionInsteadOfCreatingDuplicates() {
        CustomerServiceSession empty = session("empty-session", 0, CustomerServiceEnums.SessionStatus.ACTIVE);
        doReturn(empty).when(service).getOne(any(Wrapper.class), eq(false));

        CustomerServiceDtos.SessionItem result = service.createSession("user-1");

        assertEquals("empty-session", result.id());
        verify(mapper, never()).insert(any(CustomerServiceSession.class));
    }

    @Test
    void createsNewConversationWithoutClosingExistingConversations() {
        doReturn(null).when(service).getOne(any(Wrapper.class), eq(false));

        CustomerServiceDtos.SessionItem result = service.createSession("user-1");

        assertFalse(result.id().isBlank());
        assertEquals(CustomerServiceEnums.SessionStatus.ACTIVE, result.status());
        verify(mapper).insert(any(CustomerServiceSession.class));
        verify(mapper, never()).update(any(CustomerServiceSession.class), any(Wrapper.class));
    }

    @Test
    void enteringRestoresLatestClosedConversationInsteadOfReplacingIt() {
        CustomerServiceSession closed = session("history-session", 4, CustomerServiceEnums.SessionStatus.CLOSED);
        closed.setClosedReason(CustomerServiceEnums.ClosedReason.TIMEOUT);
        doReturn(closed).when(service).getOne(any(Wrapper.class), eq(false));

        CustomerServiceDtos.EnterSessionResponse result = service.enterSession("user-1");

        assertEquals("history-session", result.sessionId());
        assertFalse(result.isNew());
        assertEquals(CustomerServiceEnums.SessionStatus.ACTIVE, closed.getStatus());
        assertEquals(null, closed.getClosedReason());
        verify(mapper).updateById(closed);
        verify(mapper, never()).insert(any(CustomerServiceSession.class));
    }

    private CustomerServiceSession session(String id, int messageCount, String status) {
        CustomerServiceSession session = new CustomerServiceSession();
        session.setId(id);
        session.setUserId("user-1");
        session.setStatus(status);
        session.setMessageCount(messageCount);
        session.setCreatedAt(LocalDateTime.now().minusDays(1));
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }
}
