package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceFeedback;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.entity.CustomerServiceSession;
import com.zhuxiang.service.mapper.CustomerServiceFeedbackMapper;
import com.zhuxiang.service.mapper.CustomerServiceMessageMapper;
import com.zhuxiang.service.service.CustomerServiceMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 客服消息管理服务实现
 */
@Service
public class CustomerServiceMessageServiceImpl
        extends ServiceImpl<CustomerServiceMessageMapper, CustomerServiceMessage>
        implements CustomerServiceMessageService {

    private final CustomerServiceFeedbackMapper feedbackMapper;
    private final CustomerServiceSessionServiceImpl sessionService;

    public CustomerServiceMessageServiceImpl(
            CustomerServiceFeedbackMapper feedbackMapper,
            CustomerServiceSessionServiceImpl sessionService
    ) {
        this.feedbackMapper = feedbackMapper;
        this.sessionService = sessionService;
    }

    @Override
    public List<CustomerServiceDtos.MessageItem> getMessages(String userId, String sessionId) {
        // 校验会话归属
        sessionService.requireOwnedSession(userId, sessionId);
        return getMessagesBySessionId(sessionId);
    }

    @Override
    public List<CustomerServiceDtos.MessageItem> getMessagesBySessionId(String sessionId) {
        List<CustomerServiceMessage> messages = list(
                Wrappers.<CustomerServiceMessage>lambdaQuery()
                        .eq(CustomerServiceMessage::getSessionId, sessionId)
                        .orderByAsc(CustomerServiceMessage::getCreatedAt)
        );
        return messages.stream().map(this::toItem).toList();
    }

    @Override
    @Transactional
    public CustomerServiceDtos.MessageItem saveUserMessage(String sessionId, String userId, String content) {
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(CustomerServiceEnums.MessageRole.USER);
        message.setContent(content);
        message.setStatus(CustomerServiceEnums.MessageStatus.SENT);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        save(message);
        return toItem(message);
    }

    @Override
    @Transactional
    public CustomerServiceMessage createAssistantMessagePlaceholder(String sessionId) {
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(CustomerServiceEnums.MessageRole.ASSISTANT);
        message.setContent("");
        message.setStatus(CustomerServiceEnums.MessageStatus.STREAMING);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        save(message);
        return message;
    }

    @Override
    @Transactional
    public void updateAssistantMessage(String messageId, String content, String status, String metadataJson) {
        CustomerServiceMessage message = getById(messageId);
        if (message == null) {
            return;
        }
        if (content != null) {
            message.setContent(content);
        }
        if (status != null) {
            message.setStatus(status);
        }
        if (metadataJson != null) {
            message.setMetadataJson(metadataJson);
        }
        message.setUpdatedAt(LocalDateTime.now());
        updateById(message);
    }

    @Override
    @Transactional
    public void markAssistantMessageFailed(String messageId, String partialContent, String errorMessage) {
        CustomerServiceMessage message = getById(messageId);
        if (message == null) {
            return;
        }
        message.setContent(partialContent == null || partialContent.isBlank()
                ? "抱歉，AI 服务暂时不可用，请稍后重试。"
                : partialContent);
        message.setStatus(CustomerServiceEnums.MessageStatus.FAILED);
        message.setErrorMessage(errorMessage == null ? "AI 服务调用失败" : errorMessage);
        message.setUpdatedAt(LocalDateTime.now());
        updateById(message);
    }

    @Override
    @Transactional
    public void submitFeedback(String userId, String messageId, String feedbackType, String comment) {
        // 校验消息存在且归属于当前用户的会话
        validateMessageOwnership(userId, messageId);

        // 校验反馈类型
        if (!CustomerServiceEnums.FeedbackType.LIKE.equals(feedbackType)
                && !CustomerServiceEnums.FeedbackType.DISLIKE.equals(feedbackType)) {
            throw BusinessException.badRequest("反馈类型无效");
        }

        // 检查是否已评价
        Long count = feedbackMapper.selectCount(
                Wrappers.<CustomerServiceFeedback>lambdaQuery()
                        .eq(CustomerServiceFeedback::getMessageId, messageId)
                        .eq(CustomerServiceFeedback::getUserId, userId)
        );
        if (count > 0) {
            throw BusinessException.badRequest("不能重复评价");
        }

        CustomerServiceFeedback feedback = new CustomerServiceFeedback();
        feedback.setId(UUID.randomUUID().toString());
        feedback.setMessageId(messageId);
        feedback.setUserId(userId);
        feedback.setSessionId(getById(messageId).getSessionId());
        feedback.setFeedbackType(feedbackType);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    @Override
    public void validateMessageOwnership(String userId, String messageId) {
        CustomerServiceMessage message = getById(messageId);
        if (message == null) {
            throw BusinessException.notFound("消息不存在");
        }
        // 校验消息归属的会话是否属于当前用户
        sessionService.requireOwnedSession(userId, message.getSessionId());
    }

    private CustomerServiceDtos.MessageItem toItem(CustomerServiceMessage message) {
        return new CustomerServiceDtos.MessageItem(
                message.getId(),
                message.getSessionId(),
                message.getRole(),
                message.getContent(),
                message.getMetadataJson(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
