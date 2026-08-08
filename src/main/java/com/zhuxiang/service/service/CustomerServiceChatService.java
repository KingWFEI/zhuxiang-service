package com.zhuxiang.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.CustomerServiceAgentClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceLlmLog;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.entity.CustomerServiceRetrievalLog;
import com.zhuxiang.service.mapper.CustomerServiceLlmLogMapper;
import com.zhuxiang.service.mapper.CustomerServiceRetrievalLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 智能客服聊天编排：消息持久化、Agent 调用、SSE 转发和调用日志。 */
@Service
public class CustomerServiceChatService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final CustomerServiceSessionService sessionService;
    private final CustomerServiceMessageService messageService;
    private final CustomerServiceAgentClient agentClient;
    private final CustomerServiceLlmLogMapper llmLogMapper;
    private final CustomerServiceRetrievalLogMapper retrievalLogMapper;
    private final Executor executor;

    public CustomerServiceChatService(
            CustomerServiceSessionService sessionService,
            CustomerServiceMessageService messageService,
            CustomerServiceAgentClient agentClient,
            CustomerServiceLlmLogMapper llmLogMapper,
            CustomerServiceRetrievalLogMapper retrievalLogMapper,
            @Qualifier("customerServiceExecutor") Executor executor
    ) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.agentClient = agentClient;
        this.llmLogMapper = llmLogMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.executor = executor;
    }

    public SseEmitter streamMessage(
            String userId, String sessionId, String content, String incomingRequestId) {
        String requestId = normalizeRequestId(incomingRequestId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicReference<String> assistantMessageId = new AtomicReference<>();
        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        StringBuffer fullAnswer = new StringBuffer();

        emitter.onTimeout(() -> {
            connectionClosed.set(true);
            markDisconnected(assistantMessageId.get(), fullAnswer.toString(), "SSE 响应超时");
            log.warn("客服 SSE 超时: requestId={} sessionId={}", requestId, sessionId);
        });
        emitter.onError(error -> {
            connectionClosed.set(true);
            markDisconnected(assistantMessageId.get(), fullAnswer.toString(), "SSE 客户端连接断开");
            log.warn("客服 SSE 断开: requestId={} sessionId={} error={}",
                    requestId, sessionId, error.getClass().getSimpleName());
        });

        executor.execute(() -> process(
                emitter, connectionClosed, assistantMessageId, fullAnswer,
                requestId, userId, sessionId, content));
        return emitter;
    }

    private void process(
            SseEmitter emitter,
            AtomicBoolean connectionClosed,
            AtomicReference<String> assistantMessageIdRef,
            StringBuffer fullAnswer,
            String requestId,
            String userId,
            String sessionId,
            String content
    ) {
        long startMs = System.currentTimeMillis();
        try {
            var session = sessionService.requireOwnedSession(userId, sessionId);
            if (CustomerServiceEnums.SessionStatus.CLOSED.equals(session.getStatus())) {
                throw BusinessException.badRequest("会话已关闭");
            }
            if (sessionService.isSessionTimedOut(session)) {
                sessionService.archiveSession(sessionId, CustomerServiceEnums.ClosedReason.TIMEOUT);
                send(emitter, "session_timeout", Map.of(
                        "requestId", requestId,
                        "message", "当前会话已超时，请重新进入"));
                emitter.complete();
                return;
            }

            CustomerServiceDtos.MessageItem userMessage =
                    messageService.saveUserMessage(sessionId, userId, content);
            sessionService.updateTitleIfEmpty(sessionId, content);
            sessionService.incrementMessageCount(sessionId);
            sessionService.updateLastMessagePreview(sessionId, content);
            sessionService.touchSession(sessionId);

            List<CustomerServiceDtos.AgentHistoryItem> history =
                    buildHistory(userId, sessionId, userMessage.id());
            CustomerServiceMessage assistantMessage =
                    messageService.createAssistantMessagePlaceholder(sessionId);
            assistantMessageIdRef.set(assistantMessage.getId());

            if (connectionClosed.get()) {
                markDisconnected(assistantMessage.getId(), "", "SSE 客户端已断开");
                return;
            }

            log.info("开始调用 Agent: requestId={} sessionId={} userMessageId={} assistantMessageId={}",
                    requestId, sessionId, userMessage.id(), assistantMessage.getId());
            CustomerServiceAgentClient.AgentStreamMetadata metadata = agentClient.streamChat(
                    requestId,
                    sessionId,
                    userId,
                    userMessage.id(),
                    assistantMessage.getId(),
                    content,
                    history,
                    event -> processAndForwardEvent(
                            emitter, event, fullAnswer, requestId, sessionId, assistantMessage.getId())
            );
            int latencyMs = (int) (System.currentTimeMillis() - startMs);

            if (connectionClosed.get() || metadata.failed()) {
                String reason = connectionClosed.get() ? "SSE 客户端连接断开" : "Agent 返回错误事件";
                messageService.markAssistantMessageFailed(
                        assistantMessage.getId(), fullAnswer.toString(), reason);
                writeLlmLog(requestId, sessionId, assistantMessage.getId(), metadata, latencyMs, reason);
            } else {
                messageService.updateAssistantMessage(
                        assistantMessage.getId(), fullAnswer.toString(),
                        CustomerServiceEnums.MessageStatus.DONE, null);
                writeLlmLog(requestId, sessionId, assistantMessage.getId(), metadata, latencyMs, null);
            }

            sessionService.incrementMessageCount(sessionId);
            sessionService.updateLastMessagePreview(
                    sessionId,
                    fullAnswer.isEmpty() ? "AI 回复失败，请稍后重试" : fullAnswer.toString());
            if (!connectionClosed.get()) emitter.complete();
            log.info("Agent 调用结束: requestId={} sessionId={} latencyMs={} failed={}",
                    requestId, sessionId, latencyMs, metadata.failed());
        } catch (Exception error) {
            String assistantMessageId = assistantMessageIdRef.get();
            String safeError = safeErrorMessage(error);
            if (assistantMessageId != null) {
                messageService.markAssistantMessageFailed(
                        assistantMessageId, fullAnswer.toString(), safeError);
            }
            writeLlmLog(requestId, sessionId, assistantMessageId, null,
                    (int) (System.currentTimeMillis() - startMs), safeError);
            log.error("客服流式处理失败: requestId={} sessionId={} error={}",
                    requestId, sessionId, error.getClass().getSimpleName(), error);
            if (!connectionClosed.get()) {
                try {
                    send(emitter, "error", Map.of(
                            "requestId", requestId,
                            "code", "SERVICE_PROCESSING_FAILED",
                            "message", "处理您的问题时遇到错误，请稍后再试。"));
                } catch (Exception ignored) {
                    connectionClosed.set(true);
                }
                emitter.complete();
            }
        }
    }

    private void processAndForwardEvent(
            SseEmitter emitter,
            CustomerServiceAgentClient.AgentSseEvent event,
            StringBuffer fullAnswer,
            String requestId,
            String sessionId,
            String assistantMessageId
    ) throws Exception {
        if ("delta".equals(event.event())) {
            Map<String, Object> delta = objectMapper.readValue(event.data(), Map.class);
            Object content = delta.get("content");
            if (content != null) fullAnswer.append(content);
        } else if ("retrieval".equals(event.event())) {
            writeRetrievalLog(requestId, sessionId, assistantMessageId, event.data());
        }

        if (List.of("start", "tool", "retrieval", "delta", "done", "error")
                .contains(event.event())) {
            emitter.send(SseEmitter.event().name(event.event()).data(event.data()));
        }
    }

    private List<CustomerServiceDtos.AgentHistoryItem> buildHistory(
            String userId, String sessionId, String currentUserMessageId) {
        List<CustomerServiceDtos.AgentHistoryItem> history = new ArrayList<>();
        for (CustomerServiceDtos.MessageItem message : messageService.getMessages(userId, sessionId)) {
            if (message.id().equals(currentUserMessageId)
                    || (CustomerServiceEnums.MessageRole.ASSISTANT.equals(message.role())
                    && (message.content() == null || message.content().isBlank()))) {
                continue;
            }
            history.add(new CustomerServiceDtos.AgentHistoryItem(
                    CustomerServiceEnums.MessageRole.USER.equals(message.role()) ? "user" : "assistant",
                    message.content() == null ? "" : message.content()));
        }
        return history;
    }

    private void writeRetrievalLog(
            String requestId, String sessionId, String messageId, String data) {
        try {
            Map<String, Object> retrieval = objectMapper.readValue(data, Map.class);
            if (!"knowledge_found".equals(retrieval.get("action"))) return;
            CustomerServiceRetrievalLog retrievalLog = new CustomerServiceRetrievalLog();
            retrievalLog.setId(UUID.randomUUID().toString());
            retrievalLog.setSessionId(sessionId);
            retrievalLog.setMessageId(messageId);
            retrievalLog.setQueryText("knowledge_found");
            Object count = retrieval.get("count");
            retrievalLog.setTopK(count instanceof Number ? ((Number) count).intValue() : 0);
            retrievalLog.setRetrievedChunks(data);
            retrievalLog.setCreatedAt(LocalDateTime.now());
            retrievalLogMapper.insert(retrievalLog);
        } catch (Exception error) {
            log.warn("写入检索日志失败: requestId={} error={}", requestId, error.getClass().getSimpleName());
        }
    }

    private void writeLlmLog(
            String requestId,
            String sessionId,
            String messageId,
            CustomerServiceAgentClient.AgentStreamMetadata metadata,
            int latencyMs,
            String errorMessage
    ) {
        try {
            CustomerServiceLlmLog llmLog = new CustomerServiceLlmLog();
            llmLog.setId(UUID.randomUUID().toString());
            llmLog.setSessionId(sessionId);
            llmLog.setMessageId(messageId);
            llmLog.setModel(agentClient.getModel());
            llmLog.setLatencyMs(latencyMs > 0 ? latencyMs : null);
            llmLog.setIntent(metadata == null ? null : metadata.intent());
            llmLog.setNeedHuman(metadata != null && metadata.needHuman() ? 1 : 0);
            llmLog.setErrorMessage(errorMessage);
            llmLog.setCreatedAt(LocalDateTime.now());
            llmLogMapper.insert(llmLog);
        } catch (Exception error) {
            log.warn("写入 LLM 日志失败: requestId={} error={}", requestId, error.getClass().getSimpleName());
        }
    }

    private void markDisconnected(String messageId, String partialContent, String reason) {
        if (messageId != null) {
            messageService.markAssistantMessageFailed(messageId, partialContent, reason);
        }
    }

    private void send(SseEmitter emitter, String event, Map<String, Object> data) throws Exception {
        emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(data)));
    }

    private String normalizeRequestId(String value) {
        if (value != null && value.matches("[A-Za-z0-9._:-]{1,64}")) return value;
        return UUID.randomUUID().toString();
    }

    private String safeErrorMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
