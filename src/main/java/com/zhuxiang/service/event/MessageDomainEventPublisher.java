package com.zhuxiang.service.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MessageDomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public MessageDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(MessageRealtimeEvent event) {
        eventPublisher.publishEvent(event);
    }
}
