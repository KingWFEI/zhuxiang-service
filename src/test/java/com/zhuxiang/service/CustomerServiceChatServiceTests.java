package com.zhuxiang.service;

import com.zhuxiang.service.client.CustomerServiceAgentClient;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.entity.CustomerServiceSession;
import com.zhuxiang.service.entity.CustomerServiceLlmLog;
import com.zhuxiang.service.mapper.CustomerServiceLlmLogMapper;
import com.zhuxiang.service.mapper.CustomerServiceRetrievalLogMapper;
import com.zhuxiang.service.service.CustomerServiceChatService;
import com.zhuxiang.service.service.CustomerServiceMessageService;
import com.zhuxiang.service.service.CustomerServiceSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerServiceChatServiceTests {

    @Test
    void excludesCurrentMessageAndPersistsCompletedAnswer() throws Exception {
        CustomerServiceSessionService sessionService = mock(CustomerServiceSessionService.class);
        CustomerServiceMessageService messageService = mock(CustomerServiceMessageService.class);
        CustomerServiceAgentClient agentClient = mock(CustomerServiceAgentClient.class);
        CustomerServiceLlmLogMapper llmLogMapper = mock(CustomerServiceLlmLogMapper.class);
        CustomerServiceRetrievalLogMapper retrievalLogMapper = mock(CustomerServiceRetrievalLogMapper.class);
        Executor directExecutor = Runnable::run;

        CustomerServiceSession session = new CustomerServiceSession();
        session.setStatus(CustomerServiceEnums.SessionStatus.ACTIVE);
        when(sessionService.requireOwnedSession("user", "session")).thenReturn(session);
        when(sessionService.isSessionTimedOut(session)).thenReturn(false);

        CustomerServiceDtos.MessageItem currentMessage = new CustomerServiceDtos.MessageItem(
                "user-message", "session", CustomerServiceEnums.MessageRole.USER,
                "我的账单", null, CustomerServiceEnums.MessageStatus.SENT, LocalDateTime.now());
        when(messageService.saveUserMessage("session", "user", "我的账单"))
                .thenReturn(currentMessage);
        when(messageService.getMessages("user", "session")).thenReturn(List.of(currentMessage));
        CustomerServiceMessage assistant = new CustomerServiceMessage();
        assistant.setId("assistant-message");
        when(messageService.createAssistantMessagePlaceholder("session")).thenReturn(assistant);

        when(agentClient.getModel()).thenReturn("deepseek-v4-flash");
        when(agentClient.streamChat(
                eq("request-123"), eq("session"), eq("user"), eq("user-message"),
                eq("assistant-message"), eq("我的账单"), anyList(), any()))
                .thenAnswer(invocation -> {
                    CustomerServiceAgentClient.AgentSseEventConsumer consumer = invocation.getArgument(7);
                    consumer.accept(new CustomerServiceAgentClient.AgentSseEvent(
                            "delta", "{\"content\":\"账单正常\"}"));
                    consumer.accept(new CustomerServiceAgentClient.AgentSseEvent(
                            "done", "{\"intent\":\"BILL_QUERY\",\"needHuman\":false}"));
                    return new CustomerServiceAgentClient.AgentStreamMetadata(
                            "BILL_QUERY", false, false);
                });

        CustomerServiceChatService service = new CustomerServiceChatService(
                sessionService, messageService, agentClient, llmLogMapper,
                retrievalLogMapper, directExecutor);
        service.streamMessage("user", "session", "我的账单", "request-123");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerServiceDtos.AgentHistoryItem>> historyCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(agentClient).streamChat(
                eq("request-123"), eq("session"), eq("user"), eq("user-message"),
                eq("assistant-message"), eq("我的账单"), historyCaptor.capture(), any());
        assertTrue(historyCaptor.getValue().isEmpty());
        verify(messageService).updateAssistantMessage(
                "assistant-message", "账单正常", CustomerServiceEnums.MessageStatus.DONE, null);
        verify(llmLogMapper).insert(any(CustomerServiceLlmLog.class));
    }
}
