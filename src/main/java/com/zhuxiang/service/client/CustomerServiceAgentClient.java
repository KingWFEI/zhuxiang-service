package com.zhuxiang.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Python Agent 服务 HTTP 客户端
 */
@Component
public class CustomerServiceAgentClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceAgentClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentProperties agentProperties;

    public CustomerServiceAgentClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    /**
     * 通知 Agent 对指定文档进行向量化，返回分块数量；失败返回 -1。
     */
    public int triggerVectorize(String documentId, String filePath, String title, String category) {
        try {
            String url = agentProperties.getBaseUrl() + "/agent/knowledge/vectorize";
            log.info("转发向量化请求到 Agent: url={}, documentId={}, title={}", url, documentId, title);
            String json = objectMapper.writeValueAsString(Map.of(
                    "document_id", documentId,
                    "file_path", filePath,
                    "title", title,
                    "category", category
            ));
            String respBody = postJson(url, json);
            log.info("Agent 向量化响应: documentId={}, body={}", documentId, respBody);
            Map<String, Object> resp = objectMapper.readValue(respBody, Map.class);
            Object chunkCount = resp.get("chunkCount");
            return chunkCount instanceof Number ? ((Number) chunkCount).intValue() : -1;
        } catch (Exception e) {
            log.error("调用 Agent 向量化接口失败: documentId={}, error={}", documentId, e.getMessage());
            return -1;
        }
    }

    /**
     * 通知 Agent 删除指定文档的向量数据。
     */
    public boolean deleteVectors(String documentId) {
        try {
            String url = agentProperties.getBaseUrl() + "/agent/knowledge/documents/" + documentId;
            int status = deleteJson(url);
            return status == 200;
        } catch (Exception e) {
            log.error("调用 Agent 删除向量接口失败: documentId={}, error={}", documentId, e.getMessage());
            return false;
        }
    }

    /** 获取 Agent 基础地址 */
    public String getBaseUrl() {
        return agentProperties.getBaseUrl();
    }

    /** 获取内部 API Key */
    public String getApiKey() {
        return agentProperties.getApiKey();
    }

    public String getModel() {
        return agentProperties.getModel();
    }

    /** Agent SSE 事件。解析工作集中在客户端，Controller 不再处理 HTTP 协议细节。 */
    public record AgentSseEvent(String event, String data) {}

    public record AgentStreamMetadata(String intent, boolean needHuman, boolean failed) {}

    @FunctionalInterface
    public interface AgentSseEventConsumer {
        void accept(AgentSseEvent event) throws Exception;
    }

    /** 调用 Agent 聊天接口并解析 SSE，事件按到达顺序交给上层处理。 */
    public AgentStreamMetadata streamChat(
            String requestId,
            String sessionId,
            String userId,
            String userMessageId,
            String assistantMessageId,
            String message,
            List<com.zhuxiang.service.dto.CustomerServiceDtos.AgentHistoryItem> history,
            AgentSseEventConsumer consumer
    ) throws Exception {
        String url = agentProperties.getBaseUrl() + "/agent/chat/stream";
        Map<String, Object> request = Map.of(
                "session_id", sessionId,
                "user_id", userId,
                "user_message_id", userMessageId,
                "assistant_message_id", assistantMessageId,
                "message", message,
                "history", history
        );

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("X-Internal-Api-Key", agentProperties.getApiKey());
        conn.setRequestProperty("X-Internal-Source", "zhuxiang-service");
        conn.setRequestProperty("X-Request-Id", requestId);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);

        try {
            try (var output = conn.getOutputStream()) {
                objectMapper.writeValue(output, request);
            }
            int status = conn.getResponseCode();
            if (status != 200) {
                String errorBody = "";
                try (var error = conn.getErrorStream()) {
                    if (error != null) errorBody = new String(error.readAllBytes());
                }
                log.error("Agent 返回非 200: requestId={} status={} body={}",
                        requestId, status, sanitize(errorBody));
                throw new IOException("Agent 返回 HTTP " + status);
            }

            String intent = null;
            boolean needHuman = false;
            boolean failed = false;
            boolean doneReceived = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                String event = "";
                StringBuilder data = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        event = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        if (data.length() > 0) data.append('\n');
                        data.append(line.substring(5).trim());
                    } else if (line.isEmpty() && data.length() > 0) {
                        AgentSseEvent parsed = new AgentSseEvent(event, data.toString());
                        consumer.accept(parsed);
                        if ("error".equals(event)) failed = true;
                        if ("done".equals(event)) {
                            doneReceived = true;
                            Map<String, Object> done = objectMapper.readValue(data.toString(), Map.class);
                            intent = done.get("intent") == null ? null : done.get("intent").toString();
                            needHuman = Boolean.TRUE.equals(done.get("needHuman"));
                        }
                        event = "";
                        data.setLength(0);
                    }
                }
                if (data.length() > 0) {
                    String finalData = data.toString();
                    consumer.accept(new AgentSseEvent(event, finalData));
                    if ("error".equals(event)) failed = true;
                    if ("done".equals(event)) {
                        doneReceived = true;
                        Map<String, Object> done = objectMapper.readValue(finalData, Map.class);
                        intent = done.get("intent") == null ? null : done.get("intent").toString();
                        needHuman = Boolean.TRUE.equals(done.get("needHuman"));
                    }
                }
            }
            return new AgentStreamMetadata(intent, needHuman, failed || !doneReceived);
        } finally {
            conn.disconnect();
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "";
        String compact = value.replaceAll("\\s+", " ");
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }

    /** POST JSON 到 Agent，返回响应体 */
    private String postJson(String url, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Internal-Api-Key", agentProperties.getApiKey());
        conn.setRequestProperty("X-Internal-Source", "zhuxiang-service");
        conn.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        if (conn.getResponseCode() != 200) {
            String errorBody = "";
            try (var es = conn.getErrorStream()) {
                if (es != null) errorBody = new String(es.readAllBytes());
            }
            throw new RuntimeException("Agent 返回 " + conn.getResponseCode() + ": " + errorBody);
        }
        String body;
        try (var is = conn.getInputStream()) { body = new String(is.readAllBytes()); }
        conn.disconnect();
        return body;
    }

    /** DELETE JSON 到 Agent */
    private int deleteJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("X-Internal-Api-Key", agentProperties.getApiKey());
        conn.setRequestProperty("X-Internal-Source", "zhuxiang-service");
        conn.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        int status = conn.getResponseCode();
        try (var is = conn.getInputStream()) { is.readAllBytes(); }
        conn.disconnect();
        return status;
    }
}
