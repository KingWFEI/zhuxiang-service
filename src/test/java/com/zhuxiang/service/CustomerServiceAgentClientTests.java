package com.zhuxiang.service;

import com.sun.net.httpserver.HttpServer;
import com.zhuxiang.service.client.CustomerServiceAgentClient;
import com.zhuxiang.service.config.AgentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomerServiceAgentClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void parsesSseAndPropagatesInternalHeaders() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/agent/chat/stream", exchange -> {
            assertEquals("test-secret", exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            assertEquals("zhuxiang-service", exchange.getRequestHeaders().getFirst("X-Internal-Source"));
            assertEquals("request-123", exchange.getRequestHeaders().getFirst("X-Request-Id"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = ("event: delta\n"
                    + "data: {\"content\":\"你好\"}\n\n"
                    + "event: done\n"
                    + "data: {\"intent\":\"GREETING\",\"needHuman\":false}\n\n")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        AgentProperties properties = new AgentProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-secret");
        CustomerServiceAgentClient client = new CustomerServiceAgentClient(properties);
        List<CustomerServiceAgentClient.AgentSseEvent> events = new ArrayList<>();

        var metadata = client.streamChat(
                "request-123", "session", "user", "user-message", "assistant-message",
                "你好", List.of(), events::add);

        assertEquals(2, events.size());
        assertEquals("GREETING", metadata.intent());
        assertFalse(metadata.needHuman());
        assertFalse(metadata.failed());
    }
}
