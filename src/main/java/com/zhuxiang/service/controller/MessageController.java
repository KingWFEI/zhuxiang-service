package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.MessageDtos;
import com.zhuxiang.service.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "消息", description = "当前用户站内消息查询、已读和删除操作")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页查询当前用户消息。
     */
    @GetMapping
    @Operation(summary = "分页查询消息", description = "按消息分类和已读状态筛选当前用户的站内消息。")
    public ApiResponse<PageData<MessageDtos.MessageView>> list(
            @Parameter(description = "消息分类：system、lease、lock、bill、appointment 或 repair") @RequestParam(required = false) String category,
            @Parameter(description = "是否已读；不传则查询全部") @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
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
    @Operation(summary = "获取未读消息数", description = "返回全部未读数以及各消息分类的未读数。")
    public ApiResponse<MessageDtos.UnreadCounts> unreadCounts(HttpServletRequest request) {
        return ApiResponse.success(messageService.getUnreadCounts(CurrentUser.id(request)));
    }

    /**
     * 将指定消息标记为已读。
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条消息已读", description = "将当前用户的指定消息标记为已读。")
    public ApiResponse<Boolean> markRead(
            @Parameter(description = "消息 ID", example = "message_001") @PathVariable String id,
            HttpServletRequest request
    ) {
        return ApiResponse.success(messageService.markRead(CurrentUser.id(request), id));
    }

    /**
     * 将当前用户全部消息标记为已读。
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读", description = "将当前用户的全部消息标记为已读。")
    public ApiResponse<Boolean> markAllRead(HttpServletRequest request) {
        return ApiResponse.success(messageService.markAllRead(CurrentUser.id(request)));
    }

    /**
     * 删除指定消息。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息", description = "删除当前用户的指定消息。")
    public ApiResponse<Boolean> delete(
            @Parameter(description = "消息 ID", example = "message_001") @PathVariable String id,
            HttpServletRequest request
    ) {
        return ApiResponse.success(messageService.deleteMessage(CurrentUser.id(request), id));
    }

    /**
     * 清空当前用户的已读消息。
     */
    @DeleteMapping("/read")
    @Operation(summary = "清空已读消息", description = "删除当前用户的全部已读消息，未读消息不受影响。")
    public ApiResponse<Boolean> clearRead(HttpServletRequest request) {
        return ApiResponse.success(messageService.clearReadMessages(CurrentUser.id(request)));
    }
}
