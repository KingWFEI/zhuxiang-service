package com.zhuxiang.service.realtime;

import com.zhuxiang.service.event.MessageRealtimeEvent;

public class LocalMessageEventBroker implements MessageEventBroker {

    private final MessageSseHub sseHub;

    public LocalMessageEventBroker(MessageSseHub sseHub) {
        this.sseHub = sseHub;
    }

    @Override
    public void publish(MessageRealtimeEvent event) {
        sseHub.broadcast(event);
    }
}
