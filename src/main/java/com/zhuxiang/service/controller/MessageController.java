package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.MessageDtos;
import com.zhuxiang.service.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户消息管理接口。
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页查询当前用户消息。
     */
    @GetMapping
    public ApiResponse<PageData<MessageDtos.MessageView>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(messageService.getMessages(
                CurrentUser.id(request), category, isRead, page, pageSize
        ));
    }

    /**
     * 获取各分类未读消息数量。
     */
    @GetMapping("/unread-counts")
    public ApiResponse<MessageDtos.UnreadCounts> unreadCounts(HttpServletRequest request) {
        return ApiResponse.success(messageService.getUnreadCounts(CurrentUser.id(request)));
    }

    /**
     * 将指定消息标记为已读。
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Boolean> markRead(@PathVariable String id, HttpServletRequest request) {
        return ApiResponse.success(messageService.markRead(CurrentUser.id(request), id));
    }

    /**
     * 将当前用户全部消息标记为已读。
     */
    @PutMapping("/read-all")
    public ApiResponse<Boolean> markAllRead(HttpServletRequest request) {
        return ApiResponse.success(messageService.markAllRead(CurrentUser.id(request)));
    }

    /**
     * 删除指定消息。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable String id, HttpServletRequest request) {
        return ApiResponse.success(messageService.deleteMessage(CurrentUser.id(request), id));
    }

    /**
     * 清空当前用户的已读消息。
     */
    @DeleteMapping("/read")
    public ApiResponse<Boolean> clearRead(HttpServletRequest request) {
        return ApiResponse.success(messageService.clearReadMessages(CurrentUser.id(request)));
    }
}
