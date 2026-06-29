package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.service.LeaseTerminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/admin/termination-applications")
@Tag(name = "退租申请(管理端)", description = "后台退租审核、验房、结算、退款管理")
@SecurityRequirement(name = "bearerAuth")
public class LeaseTerminationAdminController {

    private final LeaseTerminationService leaseTerminationService;

    public LeaseTerminationAdminController(LeaseTerminationService leaseTerminationService) {
        this.leaseTerminationService = leaseTerminationService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询退租申请详情", description = "管理端查看退租申请详细信息及时间线。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> getDetail(@PathVariable String id) {
        return ApiResponse.success(leaseTerminationService.getDetailForAdmin(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过", description = "审核通过后自动创建验房任务，状态变为 inspection_pending。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> approve(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success("审核通过",
                leaseTerminationService.approve(CurrentUser.id(request), id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回申请", description = "驳回退租申请，需填写驳回原因。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> reject(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody LeaseTerminationDtos.RejectRequest body
    ) {
        return ApiResponse.success("已驳回",
                leaseTerminationService.reject(CurrentUser.id(request), id, body));
    }

    @PostMapping("/{id}/request-supplement")
    @Operation(summary = "要求补充材料", description = "要求租户补充退租材料。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> requestSupplement(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody LeaseTerminationDtos.SupplementReasonRequest body
    ) {
        return ApiResponse.success("已要求补充材料",
                leaseTerminationService.requestSupplement(CurrentUser.id(request), id, body));
    }

    @PostMapping("/{id}/inspection/complete")
    @Operation(summary = "验房完成", description = "验房完成后进入待结算状态。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> completeInspection(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success("验房完成",
                leaseTerminationService.completeInspection(CurrentUser.id(request), id));
    }

    @PostMapping("/{id}/settlement/confirm")
    @Operation(summary = "结算确认", description = "确认退租结算，如需退款则进入退款流程。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> confirmSettlement(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success("结算已确认",
                leaseTerminationService.confirmSettlement(CurrentUser.id(request), id));
    }

    @PostMapping("/{id}/refund/complete")
    @Operation(summary = "退款完成", description = "确认退款完成，退租流程全部结束。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> completeRefund(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success("退款已完成",
                leaseTerminationService.completeRefund(CurrentUser.id(request), id));
    }
}
