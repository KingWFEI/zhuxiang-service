package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminMessageDtos;
import com.zhuxiang.service.service.AdminMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@RequestMapping("/admin/messages")
@Tag(name = "管理端消息", description = "管理端发送站内系统消息")
@SecurityRequirement(name = "bearerAuth")
public class AdminMessageController {

    private final AdminMessageService adminMessageService;

    public AdminMessageController(AdminMessageService adminMessageService) {
        this.adminMessageService = adminMessageService;
    }

    @PostMapping
    @Operation(summary = "发送系统消息", description = "仅 ADMIN 可向指定用户批量发送站内系统消息。")
    public ApiResponse<Integer> sendSystemMessage(
            @Valid @RequestBody AdminMessageDtos.SendSystemMessageRequest body,
            HttpServletRequest request
    ) {
        int recipientCount = adminMessageService.sendSystemMessage(CurrentUser.id(request), body);
        return ApiResponse.success("发送成功", recipientCount);
    }
}
