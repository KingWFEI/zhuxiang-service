package com.zhuxiang.service.realtime;

import com.zhuxiang.service.config.MessageRealtimeProperties;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(
        name = "app.message.realtime.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MessageSseHub {

    private static final Logger log = LoggerFactory.getLogger(MessageSseHub.class);

    private final MessageRealtimeProperties properties;
    private final Map<String, CopyOnWriteArrayList<Connection>> connections =
            new ConcurrentHashMap<>();

    public MessageSseHub(MessageRealtimeProperties properties) {
        this.properties = properties;
    }

    public SseEmitter connect(String userId) {
        SseEmitter emitter = createEmitter(properties.getConnectionTimeoutMs());
        Connection connection = new Connection(UUID.randomUUID().toString(), emitter);
        CopyOnWriteArrayList<Connection> userConnections =
                connections.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());
        userConnections.add(connection);

        emitter.onCompletion(() -> remove(userId, connection));
        emitter.onTimeout(() -> {
            remove(userId, connection);
            emitter.complete();
        });
        emitter.onError(ignored -> remove(userId, connection));

        while (userConnections.size() > properties.getMaxConnectionsPerUser()) {
            Connection oldest = userConnections.remove(0);
            oldest.emitter().complete();
        }

        send(userId, connection, SseEmitter.event()
                .name("connected")
                .id(connection.id())
                .reconnectTime(3_000)
                .data(Map.of(
                        "connectionId", connection.id(),
                        "serverTime", LocalDateTime.now().toString()
                )));
        return emitter;
    }

    public boolean isEnabledFor(String userId) {
        int percent = properties.getRolloutPercent();
        return percent >= 100 || (percent > 0 && Math.floorMod(userId.hashCode(), 100) < percent);
    }

    public void broadcast(MessageRealtimeEvent event) {
        CopyOnWriteArrayList<Connection> userConnections = connections.get(event.userId());
        if (userConnections == null || userConnections.isEmpty()) {
            return;
        }
        for (Connection connection : userConnections) {
            send(event.userId(), connection, SseEmitter.event()
                    .id(event.eventId())
                    .name(event.type())
                    .data(event));
        }
    }

    @Scheduled(fixedDelayString = "${app.message.realtime.heartbeat-ms:20000}")
    public void heartbeat() {
        String serverTime = LocalDateTime.now().toString();
        connections.forEach((userId, userConnections) -> {
            for (Connection connection : userConnections) {
                send(userId, connection, SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("serverTime", serverTime)));
            }
        });
    }

    public int activeConnectionCount(String userId) {
        CopyOnWriteArrayList<Connection> userConnections = connections.get(userId);
        return userConnections == null ? 0 : userConnections.size();
    }

    @PreDestroy
    public void shutdown() {
        connections.values().forEach(userConnections ->
                userConnections.forEach(connection -> connection.emitter().complete())
        );
        connections.clear();
    }

    protected SseEmitter createEmitter(long timeoutMs) {
        return new SseEmitter(timeoutMs);
    }

    private void send(String userId, Connection connection, SseEmitter.SseEventBuilder event) {
        try {
            synchronized (connection) {
                connection.emitter().send(event);
            }
        } catch (IOException | IllegalStateException exception) {
            remove(userId, connection);
            log.debug("SSE连接已移除: userId={}, connectionId={}, reason={}",
                    userId, connection.id(), exception.getMessage());
        }
    }

    private void remove(String userId, Connection connection) {
        CopyOnWriteArrayList<Connection> userConnections = connections.get(userId);
        if (userConnections == null) {
            return;
        }
        userConnections.remove(connection);
        if (userConnections.isEmpty()) {
            connections.remove(userId, userConnections);
        }
    }

    private record Connection(String id, SseEmitter emitter) {
    }
}
