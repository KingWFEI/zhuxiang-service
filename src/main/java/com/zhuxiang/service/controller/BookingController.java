package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.BookingDtos;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.ConversationService;
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
