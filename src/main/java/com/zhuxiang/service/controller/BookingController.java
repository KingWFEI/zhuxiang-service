package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.BookingDtos;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房源预约和咨询接口。
 */
@RequireAuth
@RestController
@Tag(name = "预约与咨询", description = "用户预约看房和创建房源咨询会话")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final AppointmentService appointmentService;
    private final ConversationService conversationService;

    public BookingController(
            AppointmentService appointmentService,
            ConversationService conversationService
    ) {
        this.appointmentService = appointmentService;
        this.conversationService = conversationService;
    }

    /**
     * 提交预约看房申请。
     */
    @PostMapping("/appointments")
    @Operation(summary = "预约看房", description = "为指定房源提交预约日期、时间段和联系人信息。")
    public ApiResponse<BookingDtos.AppointmentResult> createAppointment(
            HttpServletRequest servletRequest,
            @Valid @RequestBody BookingDtos.AppointmentRequest request
    ) {
        return ApiResponse.success(
                "预约提交成功",
                appointmentService.createAppointment(CurrentUser.id(servletRequest), request)
        );
    }

    /**
     * 创建房源咨询会话。
     */
    @PostMapping("/conversations")
    @Operation(summary = "创建咨询会话", description = "按房源详情、个人中心或客服入口创建咨询会话。")
    public ApiResponse<BookingDtos.ConversationResult> createConversation(
            HttpServletRequest servletRequest,
            @Valid @RequestBody BookingDtos.ConversationRequest request
    ) {
        return ApiResponse.success(
                "会话创建成功",
                conversationService.createConversation(CurrentUser.id(servletRequest), request)
        );
    }
}
