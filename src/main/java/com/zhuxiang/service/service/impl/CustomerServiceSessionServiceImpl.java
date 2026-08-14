package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceSession;
import com.zhuxiang.service.mapper.CustomerServiceSessionMapper;
import com.zhuxiang.service.service.CustomerServiceSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 客服会话管理服务实现
 */
@Service
public class CustomerServiceSessionServiceImpl
        extends ServiceImpl<CustomerServiceSessionMapper, CustomerServiceSession>
        implements CustomerServiceSessionService {

    /** 标题截取长度（中文字符数） */
    private static final int TITLE_MAX_CHARS = 20;

    /** 消息预览截取长度（字符数） */
    private static final int PREVIEW_MAX_CHARS = 50;

    @Override
    @Transactional
    public CustomerServiceDtos.SessionItem createSession(String userId) {
        CustomerServiceSession emptySession = getOne(
                Wrappers.<CustomerServiceSession>lambdaQuery()
                        .eq(CustomerServiceSession::getUserId, userId)
                        .isNull(CustomerServiceSession::getDeletedAt)
                        .and(wrapper -> wrapper
                                .eq(CustomerServiceSession::getMessageCount, 0)
                                .or()
                                .isNull(CustomerServiceSession::getMessageCount))
                        .orderByDesc(CustomerServiceSession::getUpdatedAt)
                        .last("LIMIT 1 FOR UPDATE"),
                false
        );
        if (emptySession != null) {
            reopenIfNeeded(emptySession);
            return toItem(emptySession);
        }

        CustomerServiceSession session = buildSession(userId);
        save(session);
        return toItem(session);
    }

    @Override
    public PageData<CustomerServiceDtos.SessionItem> listSessions(String userId, long page, long pageSize) {
        IPage<CustomerServiceSession> result = page(
                new Page<>(page, pageSize),
                Wrappers.<CustomerServiceSession>lambdaQuery()
                        .eq(CustomerServiceSession::getUserId, userId)
                        .isNull(CustomerServiceSession::getDeletedAt)
                        .orderByDesc(CustomerServiceSession::getUpdatedAt)
        );
        return PageData.of(
                result.getRecords().stream().map(this::toItem).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    @Override
    @Transactional
    public void closeSession(String userId, String sessionId) {
        CustomerServiceSession session = requireOwnedSession(userId, sessionId);
        if (CustomerServiceEnums.SessionStatus.CLOSED.equals(session.getStatus())) {
            throw BusinessException.badRequest("会话已关闭");
        }
        session.setStatus(CustomerServiceEnums.SessionStatus.CLOSED);
        session.setClosedReason(CustomerServiceEnums.ClosedReason.USER_CLOSED);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional
    public void updateTitleIfEmpty(String sessionId, String firstMessage) {
        CustomerServiceSession session = getById(sessionId);
        if (session == null) {
            return;
        }
        if (session.getTitle() != null && !session.getTitle().isBlank()) {
            return;
        }
        String title = truncateChinese(firstMessage, TITLE_MAX_CHARS);
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional
    public void incrementMessageCount(String sessionId) {
        CustomerServiceSession session = getById(sessionId);
        if (session == null) {
            return;
        }
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional
    public void updateLastMessagePreview(String sessionId, String preview) {
        CustomerServiceSession session = getById(sessionId);
        if (session == null) {
            return;
        }
        if (preview != null && preview.length() > PREVIEW_MAX_CHARS) {
            preview = preview.substring(0, PREVIEW_MAX_CHARS);
        }
        session.setLastMessagePreview(preview);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    // ========== 进入客服核心逻辑 ==========

    @Override
    @Transactional
    public CustomerServiceDtos.EnterSessionResponse enterSession(String userId) {
        // ChatGPT 式会话：进入客服时恢复最近使用的会话，不按空闲时间关闭。
        CustomerServiceSession latestSession = getOne(
                Wrappers.<CustomerServiceSession>lambdaQuery()
                        .eq(CustomerServiceSession::getUserId, userId)
                        .isNull(CustomerServiceSession::getDeletedAt)
                        .orderByDesc(CustomerServiceSession::getUpdatedAt)
                        .last("LIMIT 1"),
                false
        );

        if (latestSession == null) {
            CustomerServiceSession newSession = buildSession(userId);
            save(newSession);
            return new CustomerServiceDtos.EnterSessionResponse(
                    newSession.getId(), newSession.getTitle(), newSession.getStatus(),
                    true, null, "已为你开启新会话"
            );
        }

        reopenIfNeeded(latestSession);
        return new CustomerServiceDtos.EnterSessionResponse(
                latestSession.getId(), latestSession.getTitle(), latestSession.getStatus(),
                false, null, "恢复了上次的对话"
        );
    }

    @Override
    @Transactional
    public void archiveSession(String sessionId, String closedReason) {
        CustomerServiceSession session = getById(sessionId);
        if (session == null) return;
        session.setStatus(CustomerServiceEnums.SessionStatus.CLOSED);
        session.setClosedReason(closedReason);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional
    public void touchSession(String sessionId) {
        CustomerServiceSession session = getById(sessionId);
        if (session == null) return;
        LocalDateTime now = LocalDateTime.now();
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        updateById(session);
    }

    @Override
    @Transactional
    public CustomerServiceSession resumeSession(String userId, String sessionId) {
        CustomerServiceSession session = requireOwnedSession(userId, sessionId);
        reopenIfNeeded(session);
        return session;
    }

    private void reopenIfNeeded(CustomerServiceSession session) {
        if (CustomerServiceEnums.SessionStatus.ACTIVE.equals(session.getStatus())
                && session.getClosedReason() == null) {
            return;
        }
        session.setStatus(CustomerServiceEnums.SessionStatus.ACTIVE);
        session.setClosedReason(null);
        session.setUpdatedAt(LocalDateTime.now());
        updateById(session);
    }

    private CustomerServiceSession buildSession(String userId) {
        CustomerServiceSession session = new CustomerServiceSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus(CustomerServiceEnums.SessionStatus.ACTIVE);
        session.setMessageCount(0);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return session;
    }

    // ========== 原有方法 ==========

    @Override
    public CustomerServiceSession requireOwnedSession(String userId, String sessionId) {
        CustomerServiceSession session = getOne(
                Wrappers.<CustomerServiceSession>lambdaQuery()
                        .eq(CustomerServiceSession::getId, sessionId)
                        .eq(CustomerServiceSession::getUserId, userId)
                        .isNull(CustomerServiceSession::getDeletedAt)
                        .last("LIMIT 1"),
                false
        );
        if (session == null) {
            throw BusinessException.notFound("会话不存在");
        }
        return session;
    }

    /** 截取前N个中文字符（忽略英文和数字） */
    private String truncateChinese(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "新会话";
        }
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                count++;
            }
            if (count >= maxChars) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private CustomerServiceDtos.SessionItem toItem(CustomerServiceSession session) {
        return new CustomerServiceDtos.SessionItem(
                session.getId(),
                session.getTitle(),
                session.getStatus(),
                session.getMessageCount(),
                session.getLastMessagePreview(),
                session.getClosedReason(),
                session.getLastMessageAt(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
