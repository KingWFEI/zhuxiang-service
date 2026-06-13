package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.BookingDtos;
import com.zhuxiang.service.entity.Conversation;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.ConversationService;
import com.zhuxiang.service.mapper.ConversationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【conversation(用户咨询会话表)】的数据库操作Service实现
* @createDate 2026-06-12 19:56:53
*/
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
    implements ConversationService{

    private final HouseService houseService;
    private final LandlordService landlordService;

    public ConversationServiceImpl(
            HouseService houseService,
            LandlordService landlordService
    ) {
        this.houseService = houseService;
        this.landlordService = landlordService;
    }

    /**
     * 查找或创建房源咨询会话。
     */
    @Override
    @Transactional
    public BookingDtos.ConversationResult createConversation(
            String userId,
            BookingDtos.ConversationRequest request
    ) {
        if ("house_detail".equals(request.source())) {
            if (!StringUtils.hasText(request.houseId()) || !StringUtils.hasText(request.landlordId())) {
                throw BusinessException.badRequest("房源会话必须提供 houseId 和 landlordId");
            }
            houseService.requireAvailableHouse(request.houseId());
            landlordService.requireLandlord(request.landlordId());
        }
        Conversation existing = getOne(
                Wrappers.<Conversation>lambdaQuery()
                        .eq(Conversation::getUserId, userId)
                        .eq(StringUtils.hasText(request.houseId()), Conversation::getHouseId, request.houseId())
                        .eq(StringUtils.hasText(request.landlordId()), Conversation::getLandlordId, request.landlordId())
                        .eq(Conversation::getSource, request.source())
                        .eq(Conversation::getStatus, "active")
                        .last("LIMIT 1"),
                false
        );
        if (existing != null) {
            return new BookingDtos.ConversationResult(existing.getId());
        }
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setHouseId(request.houseId());
        conversation.setLandlordId(request.landlordId());
        conversation.setSource(request.source());
        conversation.setStatus("active");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        save(conversation);
        return new BookingDtos.ConversationResult(conversation.getId());
    }
}




