package com.zhuxiang.service;

import com.zhuxiang.service.event.MessageRealtimeEvent;
import com.zhuxiang.service.event.MessageRealtimeEventRelay;
import com.zhuxiang.service.realtime.MessageEventBroker;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class MessageRealtimeEventRelayTests {

    @Test
    void committedEventIsForwardedToConfiguredBroker() {
        MessageEventBroker broker = mock(MessageEventBroker.class);
        MessageRealtimeEventRelay relay = new MessageRealtimeEventRelay(broker);
        MessageRealtimeEvent event = MessageRealtimeEvent.changed(
                "user-1", "read_all", null
        );

        relay.afterCommit(event);

        verify(broker).publish(event);
    }

    @Test
    void brokerFailureDoesNotEscapeAfterBusinessTransactionCommitted() {
        MessageEventBroker broker = mock(MessageEventBroker.class);
        MessageRealtimeEventRelay relay = new MessageRealtimeEventRelay(broker);
        MessageRealtimeEvent event = MessageRealtimeEvent.changed(
                "user-1", "read_all", null
        );
        doThrow(new IllegalStateException("redis unavailable"))
                .when(broker).publish(event);

        relay.afterCommit(event);

        verify(broker).publish(event);
    }
}
