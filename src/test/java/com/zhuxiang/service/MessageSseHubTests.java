package com.zhuxiang.service;

import com.zhuxiang.service.config.MessageRealtimeProperties;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import com.zhuxiang.service.realtime.MessageSseHub;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MessageSseHubTests {

    @Test
    void rolloutCanDisableRealtimeForEveryUser() {
        MessageRealtimeProperties properties = new MessageRealtimeProperties();
        properties.setRolloutPercent(0);
        MessageSseHub hub = new MessageSseHub(properties);

        assertThat(hub.isEnabledFor("user-1")).isFalse();
    }

    @Test
    void connectionLimitClosesOldestConnectionAndEventsAreUserScoped() throws Exception {
        MessageRealtimeProperties properties = new MessageRealtimeProperties();
        properties.setMaxConnectionsPerUser(1);
        SseEmitter first = mock(SseEmitter.class);
        SseEmitter second = mock(SseEmitter.class);
        SseEmitter otherUser = mock(SseEmitter.class);
        TestMessageSseHub hub = new TestMessageSseHub(properties, first, second, otherUser);

        hub.connect("user-1");
        hub.connect("user-1");
        hub.connect("user-2");
        hub.broadcast(MessageRealtimeEvent.changed("user-1", "read_all", null));

        assertThat(hub.activeConnectionCount("user-1")).isEqualTo(1);
        assertThat(hub.activeConnectionCount("user-2")).isEqualTo(1);
        verify(first).complete();
        verify(second, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void sendFailureRemovesConnectionWithoutCompletingWithError() throws Exception {
        MessageRealtimeProperties properties = new MessageRealtimeProperties();
        SseEmitter disconnected = mock(SseEmitter.class);
        doThrow(new IOException("client disconnected"))
                .when(disconnected)
                .send(any(SseEmitter.SseEventBuilder.class));
        TestMessageSseHub hub = new TestMessageSseHub(properties, disconnected);

        hub.connect("user-1");

        assertThat(hub.activeConnectionCount("user-1")).isZero();
        verify(disconnected, never()).completeWithError(any(Throwable.class));
    }

    private static final class TestMessageSseHub extends MessageSseHub {

        private final Queue<SseEmitter> emitters = new ArrayDeque<>();

        private TestMessageSseHub(
                MessageRealtimeProperties properties,
                SseEmitter... emitters
        ) {
            super(properties);
            this.emitters.addAll(java.util.List.of(emitters));
        }

        @Override
        protected SseEmitter createEmitter(long timeoutMs) {
            return emitters.remove();
        }
    }
}
