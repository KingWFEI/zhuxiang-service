package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.service.CustomerServiceChatService;
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
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** 当前用户的智能客服会话接口。 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/customer-service")
@Tag(name = "智能客服", description = "当前用户智能客服会话与消息操作")
@SecurityRequirement(name = "bearerAuth")
public class CustomerServiceController {

    private final CustomerServiceSessionService sessionService;
    private final CustomerServiceMessageService messageService;
    private final CustomerServiceChatService chatService;

    public CustomerServiceController(
            CustomerServiceSessionService sessionService,
            CustomerServiceMessageService messageService,
            CustomerServiceChatService chatService
    ) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建客服会话", description = "为当前用户创建新的智能客服会话")
    public ApiResponse<CustomerServiceDtos.SessionItem> createSession(HttpServletRequest request) {
        return ApiResponse.success(sessionService.createSession(CurrentUser.id(request)));
    }

    @PostMapping("/sessions/enter")
    @Operation(summary = "进入智能客服", description = "恢复未超时会话或创建新会话")
    public ApiResponse<CustomerServiceDtos.EnterSessionResponse> enterSession(HttpServletRequest request) {
        return ApiResponse.success(sessionService.enterSession(CurrentUser.id(request)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "客服会话列表")
    public ApiResponse<PageData<CustomerServiceDtos.SessionItem>> listSessions(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                sessionService.listSessions(CurrentUser.id(request), page, pageSize));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取历史消息")
    public ApiResponse<List<CustomerServiceDtos.MessageItem>> getMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                messageService.getMessages(CurrentUser.id(request), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/close")
    @Operation(summary = "关闭会话")
    public ApiResponse<Void> closeSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        sessionService.closeSession(CurrentUser.id(request), sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/sessions/{sessionId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（流式）")
    public SseEmitter streamMessage(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody @Valid CustomerServiceDtos.SendMessageRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        return chatService.streamMessage(
                CurrentUser.id(request), sessionId, body.message(), requestId);
    }

    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "提交反馈")
    public ApiResponse<Void> submitFeedback(
            @Parameter(description = "消息ID") @PathVariable String messageId,
            @RequestBody @Valid CustomerServiceDtos.SubmitFeedbackRequest body,
            HttpServletRequest request
    ) {
        messageService.submitFeedback(
                CurrentUser.id(request), messageId, body.feedbackType(), body.comment());
        return ApiResponse.success(null);
    }
}
