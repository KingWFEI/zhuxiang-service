package com.zhuxiang.service.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能客服相关DTO
 */
public final class CustomerServiceDtos {

    private CustomerServiceDtos() {}

    // ========== 会话相关 ==========

    /** 创建会话请求 */
    public record CreateSessionRequest() {}

    /** 会话列表项 */
    public record SessionItem(
            String id,
            String title,
            String status,
            Integer messageCount,
            String lastMessagePreview,
            String closedReason,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /** 进入会话响应 */
    public record EnterSessionResponse(
            String sessionId,
            String title,
            String status,
            boolean isNew,
            String closedReason,
            String hint
            // isNew=true: "已为你开启新会话"
            // isNew=false: "恢复了上次的对话"
    ) {}

    /** 消息列表项 */
    public record MessageItem(
            String id,
            String sessionId,
            String role,
            String content,
            String metadataJson,
            String status,
            LocalDateTime createdAt
    ) {}

    // ========== 知识库相关 ==========

    /** 知识库文档视图 */
    public record KbDocumentView(
            String id,
            String title,
            String category,
            String originalFilename,
            String fileType,
            Long fileSize,
            String filePath,
            Integer chunkCount,
            String status,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /** 更新文档请求 */
    public record UpdateKbDocumentRequest(
            String title,
            String category,
            String status
    ) {}

    // ========== 反馈相关 ==========

    /** 发送消息请求 */
    public record SendMessageRequest(
            String message
    ) {}

    /** 提交反馈请求 */
    public record SubmitFeedbackRequest(
            String feedbackType,
            String comment
    ) {}

    /** 反馈视图 */
    public record FeedbackView(
            String id,
            String messageId,
            String feedbackType,
            String comment,
            LocalDateTime createdAt
    ) {}

    // ========== 检索日志 ==========

    /** 检索日志视图 */
    public record RetrievalLogItem(
            String id,
            String messageId,
            String queryText,
            Integer topK,
            String retrievedChunks,
            Integer retrievalDurationMs,
            LocalDateTime createdAt
    ) {}

    // ========== LLM调用日志 ==========

    /** LLM调用日志视图 */
    public record LlmLogItem(
            String id,
            String messageId,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Integer latencyMs,
            String intent,
            Integer needHuman,
            String errorMessage,
            LocalDateTime createdAt
    ) {}

    // ========== 管理端会话详情 ==========

    /** 管理端会话视图（含消息列表） */
    public record AdminSessionDetail(
            String id,
            String userId,
            String title,
            String status,
            Integer messageCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<MessageItem> messages
    ) {}

    /** 管理端消息视图（含检索日志） */
    public record AdminMessageDetail(
            String id,
            String sessionId,
            String role,
            String content,
            String metadataJson,
            String status,
            LocalDateTime createdAt,
            List<RetrievalLogItem> retrievalLogs
    ) {}

    // ========== SSE 流式请求（Spring Boot -> Python Agent） ==========

    /** 发送给 Python Agent 的聊天请求 */
    public record AgentChatRequest(
            String sessionId,
            String userId,
            String userMessageId,
            String assistantMessageId,
            String message,
            List<AgentHistoryItem> history
    ) {}

    /** Agent 历史消息项 */
    public record AgentHistoryItem(
            String role,
            String content
    ) {}
}
