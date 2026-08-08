package com.zhuxiang.service.event;

import com.zhuxiang.service.dto.MessageDtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageRealtimeEvent(
        String eventId,
        String userId,
        String type,
        String operation,
        String messageId,
        MessageDtos.MessageView message,
        LocalDateTime occurredAt
) {

    public static MessageRealtimeEvent created(String userId, MessageDtos.MessageView message) {
        return new MessageRealtimeEvent(
                UUID.randomUUID().toString(),
                userId,
                "message.created",
                null,
                message.id(),
                message,
                LocalDateTime.now()
        );
    }

    public static MessageRealtimeEvent changed(String userId, String operation, String messageId) {
        return new MessageRealtimeEvent(
                UUID.randomUUID().toString(),
                userId,
                "messages.changed",
                operation,
                messageId,
                null,
                LocalDateTime.now()
        );
    }
}
