package com.zhuxiang.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.client.CustomerServiceAgentClient;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceLlmLog;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.entity.CustomerServiceRetrievalLog;
import com.zhuxiang.service.mapper.CustomerServiceLlmLogMapper;
import com.zhuxiang.service.mapper.CustomerServiceRetrievalLogMapper;
import com.zhuxiang.service.service.CustomerServiceMessageService;
import com.zhuxiang.service.service.CustomerServiceSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 智能客服接口
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/customer-service")
@Tag(name = "智能客服", description = "当前用户智能客服会话与消息操作")
@SecurityRequirement(name = "bearerAuth")
public class CustomerServiceController {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CustomerServiceSessionService sessionService;
    private final CustomerServiceMessageService messageService;
    private final CustomerServiceAgentClient agentClient;
    private final CustomerServiceLlmLogMapper llmLogMapper;
    private final CustomerServiceRetrievalLogMapper retrievalLogMapper;

    public CustomerServiceController(
            CustomerServiceSessionService sessionService,
            CustomerServiceMessageService messageService,
            CustomerServiceAgentClient agentClient,
            CustomerServiceLlmLogMapper llmLogMapper,
            CustomerServiceRetrievalLogMapper retrievalLogMapper
    ) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.agentClient = agentClient;
        this.llmLogMapper = llmLogMapper;
        this.retrievalLogMapper = retrievalLogMapper;
    }

    /**
     * 创建新的客服会话。
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建客服会话", description = "为当前用户创建新的智能客服会话")
    public ApiResponse<CustomerServiceDtos.SessionItem> createSession(HttpServletRequest request) {
        return ApiResponse.success(sessionService.createSession(CurrentUser.id(request)));
    }

    /**
     * 进入智能客服 —— 自动判断是否恢复已有会话或创建新会话。
     */
    @PostMapping("/sessions/enter")
    @Operation(summary = "进入智能客服", description = "自动判断：有活跃未超时会话则恢复，超时则归档并新建，无活跃会话则新建")
    public ApiResponse<CustomerServiceDtos.EnterSessionResponse> enterSession(HttpServletRequest request) {
        return ApiResponse.success(sessionService.enterSession(CurrentUser.id(request)));
    }

    /**
     * 获取当前用户的客服会话列表。
     */
    @GetMapping("/sessions")
    @Operation(summary = "客服会话列表", description = "分页查询当前用户的客服会话，按更新时间倒序")
    public ApiResponse<PageData<CustomerServiceDtos.SessionItem>> listSessions(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围1-50") @RequestParam(defaultValue = "20") @Min(1) @Max(50) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(sessionService.listSessions(CurrentUser.id(request), page, pageSize));
    }

    /**
     * 获取指定会话的历史消息列表。
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取历史消息", description = "获取指定会话的所有消息，按创建时间正序")
    public ApiResponse<List<CustomerServiceDtos.MessageItem>> getMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(messageService.getMessages(CurrentUser.id(request), sessionId));
    }

    /**
     * 关闭指定客服会话。
     */
    @PostMapping("/sessions/{sessionId}/close")
    @Operation(summary = "关闭会话", description = "关闭指定客服会话，关闭后不能再发送消息")
    public ApiResponse<Void> closeSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        sessionService.closeSession(CurrentUser.id(request), sessionId);
        return ApiResponse.success(null);
    }

    /**
     * 发送消息并流式获取AI回复。
     */
    @PostMapping(value = "/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（流式）", description = "发送用户消息并以SSE流式获取AI回复")
    public SseEmitter streamMessage(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody @Valid CustomerServiceDtos.SendMessageRequest body,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        String userMessageContent = body.message();
        SseEmitter emitter = new SseEmitter(120_000L);  // 120秒超时

        CompletableFuture.runAsync(() -> {
            CustomerServiceMessage assistantMsg = null;
            StringBuilder fullAnswer = new StringBuilder();
            try {
                // 1. 校验会话归属和状态
                var session = sessionService.requireOwnedSession(userId, sessionId);
                if (CustomerServiceEnums.SessionStatus.CLOSED.equals(session.getStatus())) {
                    emitter.completeWithError(new BusinessException(400, "会话已关闭"));
                    return;
                }
                // 校验：发送消息时若会话已超时则拒绝，提示前端重新进入
                if (sessionService.isSessionTimedOut(session)) {
                    sessionService.archiveSession(sessionId, CustomerServiceEnums.ClosedReason.TIMEOUT);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("session_timeout")
                                .data("{\"message\":\"当前会话已超时，请重新进入\"}"));
                    } catch (Exception ignored) {}
                    emitter.complete();
                    return;
                }

                // 3. 保存用户消息
                CustomerServiceDtos.MessageItem userMsg = messageService.saveUserMessage(
                        sessionId, userId, userMessageContent
                );

                // 4. 更新会话标题和活跃时间
                sessionService.updateTitleIfEmpty(sessionId, userMessageContent);
                sessionService.incrementMessageCount(sessionId);
                sessionService.updateLastMessagePreview(sessionId, userMessageContent);
                sessionService.touchSession(sessionId);

                // 5. 创建assistant消息占位
                List<CustomerServiceDtos.AgentHistoryItem> history = buildHistory(userId, sessionId, userMsg.id());
                assistantMsg = messageService.createAssistantMessagePlaceholder(sessionId);

                // 6. 构建历史消息

                // 7. 调用Agent并转发SSE，记录耗时
                long startMs = System.currentTimeMillis();
                SseMetadata meta = forwardAgentSse(emitter, sessionId, userId, userMsg.id(),
                        assistantMsg.getId(), userMessageContent, history, fullAnswer);
                int latencyMs = (int) (System.currentTimeMillis() - startMs);

                // 8. 写入 LLM 调用日志
                writeLlmLog(sessionId, assistantMsg.getId(), meta, latencyMs, null);

                // 9. 更新assistant消息
                messageService.updateAssistantMessage(
                        assistantMsg.getId(), fullAnswer.toString(),
                        CustomerServiceEnums.MessageStatus.DONE, null
                );
                sessionService.incrementMessageCount(sessionId);
                sessionService.updateLastMessagePreview(sessionId, fullAnswer.toString());

                emitter.complete();
            } catch (Exception e) {
                // 记录失败日志
                writeLlmLog(sessionId, null, null, 0, e.getMessage());
                log.error("SSE流式处理失败: sessionId={}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"处理您的问题时遇到错误，请稍后再试。\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE超时: sessionId={}", sessionId));
        emitter.onError(e -> log.error("SSE错误: sessionId={}", sessionId, e));

        return emitter;
    }

    /** 构建历史消息列表 */
    private List<CustomerServiceDtos.AgentHistoryItem> buildHistory(String userId, String sessionId) {
        List<CustomerServiceDtos.MessageItem> messages = messageService.getMessages(userId, sessionId);
        List<CustomerServiceDtos.AgentHistoryItem> history = new ArrayList<>();
        for (CustomerServiceDtos.MessageItem msg : messages) {
            history.add(new CustomerServiceDtos.AgentHistoryItem(
                    msg.role().equals("USER") ? "USER" : "ASSISTANT",
                    msg.content() != null ? msg.content() : ""
            ));
        }
        return history;
    }

    /** SSE 响应中提取的元数据 */
    private record SseMetadata(String intent, boolean needHuman) {}

    /** 转发Agent SSE事件到Flutter，返回元数据用于写日志 */
    private SseMetadata forwardAgentSse(
            SseEmitter emitter,
            String sessionId, String userId,
            String userMessageId, String assistantMessageId,
            String message, List<CustomerServiceDtos.AgentHistoryItem> history,
            StringBuilder fullAnswer
    ) throws Exception {
        // 构建请求体（注意：Python Pydantic 期望 snake_case 字段名）
        Map<String, Object> agentRequest = Map.of(
                "session_id", sessionId,
                "user_id", userId,
                "user_message_id", userMessageId,
                "assistant_message_id", assistantMessageId,
                "message", message,
                "history", history
        );

        String agentUrl = agentClient.getBaseUrl() + "/agent/chat/stream";
        URI uri = URI.create(agentUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Internal-Api-Key", agentClient.getApiKey());
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);

        // 写入请求体
        try (var os = conn.getOutputStream()) {
            objectMapper.writeValue(os, agentRequest);
        }

        // 读取SSE响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // 读取错误响应体以便日志排查
            String errorBody = "";
            try (var es = conn.getErrorStream()) {
                if (es != null) errorBody = new String(es.readAllBytes());
            } catch (Exception ignored) {}
            log.error("Agent返回非200: status={}, body={}", responseCode, errorBody);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"message\":\"AI服务暂时不可用\"}"));
            emitter.complete();
            return new SseMetadata(null, false);
        }

        String capturedIntent = null;
        boolean capturedNeedHuman = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            String currentEvent = "";
            StringBuilder dataBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    dataBuilder.append(line.substring(5).trim());
                } else if (line.isEmpty() && !dataBuilder.isEmpty()) {
                    String data = dataBuilder.toString();
                    var meta = processAndForwardEvent(emitter, currentEvent, data,
                            fullAnswer, sessionId, assistantMessageId);
                    if (meta != null) {
                        capturedIntent = meta.intent();
                        capturedNeedHuman = meta.needHuman();
                    }
                    dataBuilder.setLength(0);
                    currentEvent = "";
                }
            }
            // 处理最后的事件
            if (!dataBuilder.isEmpty()) {
                processAndForwardEvent(emitter, currentEvent, dataBuilder.toString(),
                        fullAnswer, sessionId, assistantMessageId);
            }
        } finally {
            conn.disconnect();
        }
        return new SseMetadata(capturedIntent, capturedNeedHuman);
    }

    /** 处理单个SSE事件并转发，done事件返回元数据 */
    private SseMetadata processAndForwardEvent(
            SseEmitter emitter, String event, String data, StringBuilder fullAnswer,
            String sessionId, String assistantMessageId
    ) throws Exception {
        // 累积delta内容
        if ("delta".equals(event)) {
            try {
                Map<String, Object> delta = objectMapper.readValue(data, Map.class);
                String content = (String) delta.get("content");
                if (content != null) {
                    fullAnswer.append(content);
                }
            } catch (Exception ignored) {}
        }

        // retrieval 事件：写入检索日志
        if ("retrieval".equals(event)) {
            writeRetrievalLog(sessionId, assistantMessageId, data);
        }

        // 转发事件到Flutter（只转发安全的事件类型）
        if ("start".equals(event) || "tool".equals(event) || "retrieval".equals(event)
                || "delta".equals(event) || "done".equals(event) || "error".equals(event)) {
            emitter.send(SseEmitter.event().name(event).data(data));
        }

        // done 事件：解析 intent / needHuman
        if ("done".equals(event)) {
            try {
                Map<String, Object> done = objectMapper.readValue(data, Map.class);
                String intent = done.get("intent") != null ? done.get("intent").toString() : null;
                boolean needHuman = Boolean.TRUE.equals(done.get("needHuman"));
                return new SseMetadata(intent, needHuman);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** 写入检索日志 */
    private void writeRetrievalLog(String sessionId, String messageId, String data) {
        try {
            Map<String, Object> retrieval = objectMapper.readValue(data, Map.class);
            CustomerServiceRetrievalLog log = new CustomerServiceRetrievalLog();
            log.setId(UUID.randomUUID().toString());
            log.setSessionId(sessionId);
            log.setMessageId(messageId);
            // queryText: 取 sources 的第一个 title 作为检索关键词摘要
            Object sources = retrieval.get("sources");
            Object count = retrieval.get("count");
            log.setQueryText(retrieval.getOrDefault("action", "knowledge_found").toString());
            log.setTopK(count instanceof Number ? ((Number) count).intValue() : 0);
            log.setRetrievedChunks(data);
            log.setCreatedAt(LocalDateTime.now());
            retrievalLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("写入检索日志失败: {}", e.getMessage());
        }
    }

    /** 写入 LLM 调用日志 */
    private void writeLlmLog(String sessionId, String messageId, SseMetadata meta,
                              int latencyMs, String errorMessage) {
        try {
            CustomerServiceLlmLog log = new CustomerServiceLlmLog();
            log.setId(UUID.randomUUID().toString());
            log.setSessionId(sessionId);
            log.setMessageId(messageId);
            log.setModel("deepseek-v4-pro"); // TODO 从配置读取
            log.setLatencyMs(latencyMs > 0 ? latencyMs : null);
            log.setIntent(meta != null ? meta.intent() : null);
            log.setNeedHuman(meta != null && meta.needHuman() ? 1 : 0);
            log.setErrorMessage(errorMessage);
            log.setCreatedAt(LocalDateTime.now());
            llmLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("写入 LLM 调用日志失败: {}", e.getMessage());
        }
    }

    /**
     * 对AI消息提交反馈（点赞/点踩）。
     */
    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "提交反馈", description = "对AI消息进行点赞或点踩，不能重复评价")
    public ApiResponse<Void> submitFeedback(
            @Parameter(description = "消息ID") @PathVariable String messageId,
            @RequestBody @Valid CustomerServiceDtos.SubmitFeedbackRequest body,
            HttpServletRequest request
    ) {
        messageService.submitFeedback(CurrentUser.id(request), messageId, body.feedbackType(), body.comment());
        return ApiResponse.success(null);
    }
}
