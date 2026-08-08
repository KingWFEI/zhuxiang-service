package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.AppointmentAccessGrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController
@Tag(name = "我的看房预约")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentAccessGrantService accessGrantService;

    public AppointmentController(
            AppointmentService appointmentService,
            AppointmentAccessGrantService accessGrantService
    ) {
        this.appointmentService = appointmentService;
        this.accessGrantService = accessGrantService;
    }

    @GetMapping("/houses/{houseId}/viewing-slots")
    @Operation(summary = "查询房源可预约时段")
    public ApiResponse<AppointmentDtos.ViewingSlotResult> getViewingSlots(
            @PathVariable String houseId,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean includeTestSlot
    ) {
        return ApiResponse.success(
                appointmentService.getViewingSlots(
                        houseId, startDate, days, includeTestSlot
                )
        );
    }

    @GetMapping("/appointments/my")
    @RequireAuth
    @Operation(summary = "查询我的看房预约")
    public ApiResponse<PageData<AppointmentDtos.Summary>> listMyAppointments(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.success(
                appointmentService.listMyAppointments(
                        CurrentUser.id(request), status, page, pageSize
                )
        );
    }

    @GetMapping("/appointments/{appointmentId}")
    @RequireAuth
    @Operation(summary = "查询我的预约详情")
    public ApiResponse<AppointmentDtos.Detail> getMyAppointment(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        return ApiResponse.success(
                appointmentService.getMyAppointment(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    @RequireAuth
    @Operation(summary = "取消我的预约")
    public ApiResponse<AppointmentDtos.Detail> cancelMyAppointment(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.ReasonRequest body
    ) {
        return ApiResponse.success(
                "预约已取消",
                appointmentService.cancelMyAppointment(
                        CurrentUser.id(request), appointmentId, body.reason()
                )
        );
    }

    @PostMapping("/appointments/{appointmentId}/reschedule/accept")
    @RequireAuth
    @Operation(summary = "接受预约改期")
    public ApiResponse<AppointmentDtos.Detail> acceptReschedule(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        return ApiResponse.success(
                "已接受新的预约时间",
                appointmentService.acceptMyReschedule(
                        CurrentUser.id(request), appointmentId
                )
        );
    }

    @PostMapping("/appointments/{appointmentId}/reschedule/reject")
    @RequireAuth
    @Operation(summary = "拒绝预约改期")
    public ApiResponse<AppointmentDtos.Detail> rejectReschedule(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.ReasonRequest body
    ) {
        return ApiResponse.success(
                "已拒绝新的预约时间",
                appointmentService.rejectMyReschedule(
                        CurrentUser.id(request), appointmentId, body.reason()
                )
        );
    }

    @GetMapping("/appointments/{appointmentId}/access")
    @RequireAuth
    @Operation(summary = "获取预约自助看房开门凭证")
    public ResponseEntity<ApiResponse<AppointmentDtos.AccessView>> getAccess(
            HttpServletRequest request,
            @PathVariable String appointmentId
    ) {
        AppointmentDtos.AccessView data = accessGrantService.getTenantAccess(
                CurrentUser.id(request), appointmentId
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(data));
    }

    @PostMapping("/appointments/{appointmentId}/unlock-attempts")
    @RequireAuth
    @Operation(summary = "上报预约开锁结果")
    public ApiResponse<AppointmentDtos.Detail> reportUnlockAttempt(
            HttpServletRequest request,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDtos.UnlockAttemptRequest body
    ) {
        return ApiResponse.success(
                appointmentService.recordUnlockAttempt(
                        CurrentUser.id(request), appointmentId, body
                )
        );
    }
}
