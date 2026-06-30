package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.MessageDtos;

/**
* @author king-wang
* @description 针对表【message(用户消息表)】的数据库操作Service
* @createDate 2026-06-12 19:57:52
*/
public interface MessageService extends IService<Message> {

    /**
     * 分页查询用户消息。
     */
    PageData<MessageDtos.MessageView> getMessages(
            String userId,
            String category,
            Boolean isRead,
            long page,
            long pageSize
    );

    /**
     * 获取用户各分类未读消息数量。
     */
    MessageDtos.UnreadCounts getUnreadCounts(String userId);

    /**
     * 将指定消息标记为已读。
     */
    boolean markRead(String userId, String messageId);

    /**
     * 将用户全部消息标记为已读。
     */
    boolean markAllRead(String userId);

    /**
     * 删除指定用户消息。
     */
    boolean deleteMessage(String userId, String messageId);

    /**
     * 清空用户已读消息。
     */
    boolean clearReadMessages(String userId);

    /**
     * 向指定用户发送一条站内消息。
     *
     * @param userId 接收用户 ID
     * @param category 消息分类
     * @param title 消息标题
     * @param content 消息正文
     * @param actionType 点击消息后的动作类型
     * @param actionTarget 动作目标，例如业务 ID 或 URL
     */
    void sendMessage(
            String userId,
            String category,
            String title,
            String content,
            String actionType,
            String actionTarget
    );

    /**
     * 创建新用户欢迎消息。
     */
    void createWelcomeMessage(String userId);
}
