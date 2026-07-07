package com.zhuxiang.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

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

    /** POST JSON 到 Agent，返回响应体 */
    private String postJson(String url, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Internal-Api-Key", agentProperties.getApiKey());
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
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        int status = conn.getResponseCode();
        try (var is = conn.getInputStream()) { is.readAllBytes(); }
        conn.disconnect();
        return status;
    }
}
