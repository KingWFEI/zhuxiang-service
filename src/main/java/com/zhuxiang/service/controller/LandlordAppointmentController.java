package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.service.AppointmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@RequestMapping("/landlord/appointments")
@Tag(name = "房东看房预约")
@SecurityRequirement(name = "bearerAuth")
public class LandlordAppointmentController {

    private final AppointmentService appointmentService;

    public LandlordAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ApiResponse<PageData<AppointmentDtos.Summary>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.success(
                appointmentService.listLandlordAppointments(
                        CurrentUser.id(request), status, page, pageSize
                )
        );
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentDtos.Detail> detail(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        return ApiResponse.success(
                appointmentService.getLandlordAppointment(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/{appointmentId}/confirm")
    public ApiResponse<AppointmentDtos.Detail> confirm(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody(required = false) AppointmentDtos.ConfirmRequest body
    ) {
        return ApiResponse.success(
                "预约已确认",
                appointmentService.confirmLandlordAppointment(
                        CurrentUser.id(request), appointmentId, body
                )
        );
    }

    @PostMapping("/{appointmentId}/reject")
    public ApiResponse<AppointmentDtos.Detail> reject(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.ReasonRequest body
    ) {
        return ApiResponse.success(
                "预约已拒绝",
                appointmentService.rejectLandlordAppointment(
                        CurrentUser.id(request), appointmentId, body.reason()
                )
        );
    }

    @PostMapping("/{appointmentId}/reschedule")
    public ApiResponse<AppointmentDtos.Detail> reschedule(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.RescheduleRequest body
    ) {
        return ApiResponse.success(
                "已向租客提出新的预约时间",
                appointmentService.rescheduleLandlordAppointment(
                        CurrentUser.id(request), appointmentId, body
                )
        );
    }

    @PostMapping("/{appointmentId}/check-in")
    public ApiResponse<AppointmentDtos.Detail> checkIn(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.CheckinRequest body
    ) {
        return ApiResponse.success(
                "签到成功",
                appointmentService.checkInLandlordAppointment(
                        CurrentUser.id(request), appointmentId, body.checkinCode()
                )
        );
    }

    @PostMapping("/{appointmentId}/complete")
    public ApiResponse<AppointmentDtos.Detail> complete(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        return ApiResponse.success(
                "看房已完成",
                appointmentService.completeLandlordAppointment(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/{appointmentId}/no-show")
    public ApiResponse<AppointmentDtos.Detail> noShow(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        return ApiResponse.success(
                "已标记租客未到场",
                appointmentService.markLandlordNoShow(
                        CurrentUser.id(request), appointmentId
                )
        );
    }
}
