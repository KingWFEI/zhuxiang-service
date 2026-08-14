package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.mapper.CustomerServiceFeedbackMapper;
import com.zhuxiang.service.mapper.CustomerServiceMessageMapper;
import com.zhuxiang.service.service.impl.CustomerServiceMessageServiceImpl;
import com.zhuxiang.service.service.impl.CustomerServiceSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceMessageServiceTests {

    private CustomerServiceMessageMapper mapper;
    private CustomerServiceMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "customer-service-message-test"),
                CustomerServiceMessage.class
        );
        mapper = mock(CustomerServiceMessageMapper.class);
        service = new CustomerServiceMessageServiceImpl(
                mock(CustomerServiceFeedbackMapper.class),
                mock(CustomerServiceSessionServiceImpl.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void allocatesConversationSequenceWhenSavingMessages() {
        when(mapper.lockSessionForMessage("session-1")).thenReturn("session-1");
        when(mapper.selectNextSequenceNo("session-1")).thenReturn(7L, 8L);

        service.saveUserMessage("session-1", "user-1", "你好");
        service.createAssistantMessagePlaceholder("session-1");

        ArgumentCaptor<CustomerServiceMessage> captor = ArgumentCaptor.forClass(CustomerServiceMessage.class);
        verify(mapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(7L, captor.getAllValues().get(0).getSequenceNo());
        assertEquals(8L, captor.getAllValues().get(1).getSequenceNo());
    }

    @Test
    void loadsHistoryOrderedByConversationSequence() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.getMessagesBySessionId("session-1");

        ArgumentCaptor<Wrapper<CustomerServiceMessage>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("ORDER BY sequence_no ASC"));
    }
}
