package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceSession;

/**
 * 客服会话管理服务
 */
public interface CustomerServiceSessionService extends IService<CustomerServiceSession> {

    /**
     * 为用户创建新的客服会话。
     */
    CustomerServiceDtos.SessionItem createSession(String userId);

    /**
     * 分页查询当前用户的客服会话列表，按更新时间倒序。
     */
    PageData<CustomerServiceDtos.SessionItem> listSessions(String userId, long page, long pageSize);

    /**
     * 关闭指定会话，关闭后不能再发送消息。
     */
    void closeSession(String userId, String sessionId);

    /**
     * 更新会话标题，取用户第一句话前20个中文字符。
     */
    void updateTitleIfEmpty(String sessionId, String firstMessage);

    /**
     * 递增会话消息计数。
     */
    void incrementMessageCount(String sessionId);

    /**
     * 更新最后一条消息预览。
     */
    void updateLastMessagePreview(String sessionId, String preview);

    /**
     * 进入智能客服 —— 自动判断是否有可继续的ACTIVE会话，超时则归档并新建。
     * @return EnterSessionResponse，isNew标识是否新会话
     */
    CustomerServiceDtos.EnterSessionResponse enterSession(String userId);

    /**
     * 以指定原因归档会话（关闭且不允许继续）。
     */
    void archiveSession(String sessionId, String closedReason);

    /**
     * 用户发送消息时更新会话活跃时间，重置15min计时器。
     */
    void touchSession(String sessionId);

    /**
     * 检查会话是否已超时（lastMessageAt距今超过15分钟）。
     */
    boolean isSessionTimedOut(CustomerServiceSession session);

    /**
     * 查询并校验会话归属于指定用户。
     */
    CustomerServiceSession requireOwnedSession(String userId, String sessionId);
}
