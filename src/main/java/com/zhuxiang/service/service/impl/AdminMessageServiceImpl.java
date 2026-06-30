package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminMessageDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdminMessageService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminMessageServiceImpl implements AdminMessageService {

    private final UserService userService;
    private final MessageService messageService;

    public AdminMessageServiceImpl(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public int sendSystemMessage(
            String operatorId,
            AdminMessageDtos.SendSystemMessageRequest request
    ) {
        User operator = userService.requireActiveUser(operatorId);
        if (!"ADMIN".equals(operator.getRole())) {
            throw BusinessException.forbidden("当前账号无权发送系统消息");
        }

        Set<String> recipientIds = request.userIds().stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (recipientIds.isEmpty()) {
            throw BusinessException.badRequest("接收用户不能为空");
        }

        Map<String, User> recipients = userService.listByIds(recipientIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<String> missingIds = recipientIds.stream()
                .filter(id -> !recipients.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw BusinessException.badRequest("接收用户不存在：" + String.join(", ", missingIds));
        }

        List<String> inactiveIds = recipientIds.stream()
                .filter(id -> !"active".equals(recipients.get(id).getStatus()))
                .toList();
        if (!inactiveIds.isEmpty()) {
            throw BusinessException.badRequest("接收用户状态不可用：" + String.join(", ", inactiveIds));
        }

        recipientIds.forEach(userId -> messageService.sendMessage(
                userId,
                "system",
                request.title().trim(),
                request.content().trim(),
                request.actionType(),
                request.actionTarget()
        ));
        return recipientIds.size();
    }
}
