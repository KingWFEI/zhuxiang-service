package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.AppointmentAccessGrantService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.User;
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

import java.util.Set;

@RequireAuth
@RestController
@RequestMapping("/admin/appointments")
@Tag(name = "管理端看房预约")
@SecurityRequirement(name = "bearerAuth")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentAccessGrantService accessGrantService;
    private final UserService userService;

    public AdminAppointmentController(
            AppointmentService appointmentService,
            AppointmentAccessGrantService accessGrantService,
            UserService userService
    ) {
        this.appointmentService = appointmentService;
        this.accessGrantService = accessGrantService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageData<AppointmentDtos.Summary>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String viewingMode,
            @RequestParam(required = false) String appointmentId,
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) String tenantKeyword,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        requirePlatformStaff(request);
        return ApiResponse.success(
                appointmentService.listAdminAppointments(
                        appointmentId, houseId, tenantKeyword, landlordId,
                        status, sourceType, viewingMode, startDate, endDate,
                        page, pageSize
                )
        );
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentDtos.Detail> detail(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        requirePlatformStaff(request);
        return ApiResponse.success(
                appointmentService.getAdminAppointment(appointmentId)
        );
    }

    @PostMapping("/{appointmentId}/confirm")
    public ApiResponse<AppointmentDtos.Detail> confirm(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody(required = false) AppointmentDtos.ConfirmRequest body
    ) {
        requirePlatformStaff(request);
        return ApiResponse.success(
                "预约已确认",
                appointmentService.confirmAdminAppointment(
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
        requirePlatformStaff(request);
        return ApiResponse.success(
                "预约已拒绝",
                appointmentService.rejectAdminAppointment(
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
        requirePlatformStaff(request);
        return ApiResponse.success(
                "已提出新的预约时间",
                appointmentService.rescheduleAdminAppointment(
                        CurrentUser.id(request), appointmentId, body
                )
        );
    }

    @PostMapping("/{appointmentId}/complete")
    public ApiResponse<AppointmentDtos.Detail> complete(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        requirePlatformStaff(request);
        return ApiResponse.success(
                "看房已完成",
                appointmentService.completeAdminAppointment(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/{appointmentId}/no-show")
    public ApiResponse<AppointmentDtos.Detail> noShow(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        requirePlatformStaff(request);
        return ApiResponse.success(
                "已标记租客未到场",
                appointmentService.markAdminNoShow(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/{appointmentId}/access/retry")
    public ApiResponse<String> retryAccess(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        requirePlatformStaff(request);
        requirePlatformSelfService(appointmentId);
        return ApiResponse.success(
                "门锁授权已重试",
                accessGrantService.retry(appointmentId).getStatus()
        );
    }

    @PostMapping("/{appointmentId}/access/revoke")
    public ApiResponse<Void> revokeAccess(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        requirePlatformStaff(request);
        requirePlatformSelfService(appointmentId);
        accessGrantService.revoke(appointmentId);
        return ApiResponse.success("门锁授权已撤销", null);
    }

    private void requirePlatformStaff(HttpServletRequest request) {
        User operator = userService.requireActiveUser(CurrentUser.id(request));
        if (!Set.of("ADMIN", "HOUSEKEEPER").contains(operator.getRole())) {
            throw BusinessException.forbidden("无权处理平台看房预约");
        }
    }

    private void requirePlatformSelfService(String appointmentId) {
        AppointmentDtos.Detail appointment =
                appointmentService.getAdminAppointment(appointmentId);
        if (!"PLATFORM".equals(appointment.sourceType())
                || !"SELF_SERVICE_LOCK".equals(appointment.viewingMode())) {
            throw BusinessException.forbidden("只能处理平台自助看房门锁授权");
        }
    }
}
