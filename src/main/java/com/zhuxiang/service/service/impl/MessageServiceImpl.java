package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.MessageDtos;
import com.zhuxiang.service.entity.Message;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author king-wang
* @description 针对表【message(用户消息表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:52
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{

    private static final Set<String> CATEGORIES =
            Set.of("system", "lease", "lock", "bill", "appointment", "repair");

    /**
     * 按条件分页查询用户消息。
     */
    @Override
    public PageData<MessageDtos.MessageView> getMessages(
            String userId,
            String category,
            Boolean isRead,
            long page,
            long pageSize
    ) {
        if (StringUtils.hasText(category) && !CATEGORIES.contains(category)) {
            throw BusinessException.badRequest("消息分类不支持");
        }
        IPage<Message> result = page(
                new Page<>(page, pageSize),
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsDeleted, 0)
                        .eq(StringUtils.hasText(category), Message::getCategory, category)
                        .eq(isRead != null, Message::getIsRead, Boolean.TRUE.equals(isRead) ? 1 : 0)
                        .orderByDesc(Message::getCreatedAt)
        );
        return PageData.of(
                result.getRecords().stream().map(this::toView).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    /**
     * 统计用户各分类的未读消息数量。
     */
    @Override
    public MessageDtos.UnreadCounts getUnreadCounts(String userId) {
        Map<String, Long> counts = list(
                        Wrappers.<Message>lambdaQuery()
                                .select(Message::getCategory)
                                .eq(Message::getUserId, userId)
                                .eq(Message::getIsDeleted, 0)
                                .eq(Message::getIsRead, 0)
                ).stream()
                .collect(Collectors.groupingBy(Message::getCategory, Collectors.counting()));
        return MessageDtos.UnreadCounts.from(counts);
    }

    /**
     * 将指定消息更新为已读状态。
     */
    @Override
    @Transactional
    public boolean markRead(String userId, String messageId) {
        Message message = requireOwnedMessage(userId, messageId);
        if (!Integer.valueOf(1).equals(message.getIsRead())) {
            message.setIsRead(1);
            message.setReadAt(LocalDateTime.now());
            updateById(message);
        }
        return true;
    }

    /**
     * 将用户全部未读消息更新为已读状态。
     */
    @Override
    @Transactional
    public boolean markAllRead(String userId) {
        update(
                Wrappers.<Message>lambdaUpdate()
                        .set(Message::getIsRead, 1)
                        .set(Message::getReadAt, LocalDateTime.now())
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsDeleted, 0)
                        .eq(Message::getIsRead, 0)
        );
        return true;
    }

    /**
     * 软删除用户的指定消息。
     */
    @Override
    @Transactional
    public boolean deleteMessage(String userId, String messageId) {
        Message message = requireOwnedMessage(userId, messageId);
        message.setIsDeleted(1);
        updateById(message);
        return true;
    }

    /**
     * 软删除用户的全部已读消息。
     */
    @Override
    @Transactional
    public boolean clearReadMessages(String userId) {
        update(
                Wrappers.<Message>lambdaUpdate()
                        .set(Message::getIsDeleted, 1)
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsDeleted, 0)
                        .eq(Message::getIsRead, 1)
        );
        return true;
    }

    /**
     * 创建一条用户可见的站内消息。
     */
    @Override
    @Transactional
    public void sendMessage(
            String userId,
            String category,
            String title,
            String content,
            String actionType,
            String actionTarget
    ) {
        if (!StringUtils.hasText(userId)) {
            throw BusinessException.badRequest("接收用户不能为空");
        }
        if (!StringUtils.hasText(category) || !CATEGORIES.contains(category)) {
            throw BusinessException.badRequest("消息分类不支持");
        }
        if (!StringUtils.hasText(title)) {
            throw BusinessException.badRequest("消息标题不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw BusinessException.badRequest("消息内容不能为空");
        }

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setCategory(category);
        message.setTitle(title);
        message.setContent(content);
        message.setIconKey(category);
        message.setActionType(StringUtils.hasText(actionType) ? actionType : "none");
        message.setActionTarget(actionTarget == null ? "" : actionTarget);
        message.setIsRead(0);
        message.setIsDeleted(0);
        message.setCreatedAt(LocalDateTime.now());
        save(message);
    }

    /**
     * 为新用户创建系统欢迎消息。
     */
    @Override
    public void createWelcomeMessage(String userId) {
        sendMessage(
                userId,
                "system",
                "欢迎使用住享",
                "欢迎来到你的安心居住空间",
                "none",
                ""
        );
    }

    /**
     * 查询并校验消息归属于指定用户。
     */
    private Message requireOwnedMessage(String userId, String messageId) {
        Message message = getOne(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getId, messageId)
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsDeleted, 0)
                        .last("LIMIT 1"),
                false
        );
        if (message == null) {
            throw BusinessException.notFound("消息不存在");
        }
        return message;
    }

    /**
     * 将消息实体转换为消息视图。
     */
    private MessageDtos.MessageView toView(Message message) {
        return new MessageDtos.MessageView(
                message.getId(),
                message.getCategory(),
                message.getTitle(),
                message.getContent(),
                message.getCreatedAt(),
                Integer.valueOf(1).equals(message.getIsRead()),
                message.getActionType(),
                message.getActionTarget(),
                message.getIconKey()
        );
    }
}




