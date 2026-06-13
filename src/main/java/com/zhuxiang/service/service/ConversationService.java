package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Conversation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.BookingDtos;

/**
* @author king-wang
* @description 针对表【conversation(用户咨询会话表)】的数据库操作Service
* @createDate 2026-06-12 19:56:53
*/
public interface ConversationService extends IService<Conversation> {

    /**
     * 创建房源咨询会话。
     */
    BookingDtos.ConversationResult createConversation(
            String userId,
            BookingDtos.ConversationRequest request
    );
}
