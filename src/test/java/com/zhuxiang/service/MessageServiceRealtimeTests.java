package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.dto.MessageDtos;
import com.zhuxiang.service.dto.MessageUnreadCountRow;
import com.zhuxiang.service.entity.Message;
import com.zhuxiang.service.event.MessageDomainEventPublisher;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import com.zhuxiang.service.mapper.MessageMapper;
import com.zhuxiang.service.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceRealtimeTests {

    private final MessageMapper mapper = mock(MessageMapper.class);
    private final MessageDomainEventPublisher eventPublisher = mock(MessageDomainEventPublisher.class);
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(eventPublisher);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void unreadCountsAreAggregatedByDatabaseQuery() {
        MessageUnreadCountRow system = count("system", 2);
        MessageUnreadCountRow appointment = count("appointment", 3);
        when(mapper.selectUnreadCounts("user-1")).thenReturn(List.of(system, appointment));

        MessageDtos.UnreadCounts counts = service.getUnreadCounts("user-1");

        assertThat(counts.total()).isEqualTo(5);
        assertThat(counts.system()).isEqualTo(2);
        assertThat(counts.appointment()).isEqualTo(3);
        verify(mapper).selectUnreadCounts("user-1");
    }

    @Test
    void newMessagePublishesCreatedEvent() {
        when(mapper.insert(any(Message.class))).thenReturn(1);

        service.sendMessage(
                "user-1", "system", "系统通知", "通知内容", "none", ""
        );

        ArgumentCaptor<MessageRealtimeEvent> captor =
                ArgumentCaptor.forClass(MessageRealtimeEvent.class);
        verify(eventPublisher).publish(captor.capture());
        MessageRealtimeEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("message.created");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.message().title()).isEqualTo("系统通知");
    }

    @Test
    @SuppressWarnings("unchecked")
    void markingMessageReadPublishesChangedEvent() {
        Message message = new Message();
        message.setId("message-1");
        message.setUserId("user-1");
        message.setIsRead(0);
        message.setIsDeleted(0);
        message.setCreatedAt(LocalDateTime.now());
        when(mapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(message);
        when(mapper.updateById(message)).thenReturn(1);

        service.markRead("user-1", "message-1");

        ArgumentCaptor<MessageRealtimeEvent> captor =
                ArgumentCaptor.forClass(MessageRealtimeEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().operation()).isEqualTo("read");
        assertThat(captor.getValue().messageId()).isEqualTo("message-1");
    }

    private MessageUnreadCountRow count(String category, long value) {
        MessageUnreadCountRow row = new MessageUnreadCountRow();
        row.setCategory(category);
        row.setUnreadCount(value);
        return row;
    }
}
