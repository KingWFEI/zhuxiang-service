package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceMessage;

import java.util.List;

/**
 * 客服消息管理服务
 */
public interface CustomerServiceMessageService extends IService<CustomerServiceMessage> {

    /**
     * 查询指定会话的消息列表，按创建时间正序。
     */
    List<CustomerServiceDtos.MessageItem> getMessages(String userId, String sessionId);

    /**
     * 保存一条用户消息并返回。
     */
    CustomerServiceDtos.MessageItem saveUserMessage(String sessionId, String userId, String content);

    /**
     * 创建一条assistant消息占位，状态为STREAMING。
     */
    CustomerServiceMessage createAssistantMessagePlaceholder(String sessionId);

    /**
     * 更新assistant消息内容和状态。
     */
    void updateAssistantMessage(String messageId, String content, String status, String metadataJson);

    /** 标记 AI 消息失败，并保留已生成的部分内容。 */
    void markAssistantMessageFailed(String messageId, String partialContent, String errorMessage);

    /**
     * 提交用户反馈。
     */
    void submitFeedback(String userId, String messageId, String feedbackType, String comment);

    /**
     * 查询指定会话的全部消息（管理端用，不做用户校验）。
     */
    List<CustomerServiceDtos.MessageItem> getMessagesBySessionId(String sessionId);

    /**
     * 校验消息归属：当前用户是否为该消息所属会话的拥有者。
     */
    void validateMessageOwnership(String userId, String messageId);
}
