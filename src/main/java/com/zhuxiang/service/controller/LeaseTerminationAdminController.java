package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.zhuxiang.service.service.LeaseTerminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Validated
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

    @GetMapping
    @Operation(summary = "分页查询退租申请", description = "按状态以及租约ID、退租单号、联系人、手机号或退租原因筛选退租申请。")
    public ApiResponse<PageData<LeaseTerminationDtos.TerminationDetailResponse>> getApplications(
            @Parameter(description = "退租状态") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LambdaQueryWrapper<LeaseTerminationApplication> query = Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .eq(normalizedStatus != null, LeaseTerminationApplication::getStatus, normalizedStatus)
                .isNull(LeaseTerminationApplication::getDeletedAt)
                .orderByDesc(LeaseTerminationApplication::getCreatedAt);

        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like(LeaseTerminationApplication::getLeaseId, normalizedKeyword)
                    .or().like(LeaseTerminationApplication::getApplicationNo, normalizedKeyword)
                    .or().like(LeaseTerminationApplication::getContactName, normalizedKeyword)
                    .or().like(LeaseTerminationApplication::getContactPhone, normalizedKeyword)
                    .or().like(LeaseTerminationApplication::getReason, normalizedKeyword));
        }

        IPage<LeaseTerminationApplication> result = leaseTerminationService.page(new Page<>(page, pageSize), query);
        List<LeaseTerminationDtos.TerminationDetailResponse> items = result.getRecords().stream()
                .map(application -> leaseTerminationService.getDetailForAdmin(application.getId()))
                .toList();
        return ApiResponse.success(PageData.of(items, page, pageSize, result.getTotal()));
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
            @PathVariable String id,
            @RequestBody(required = false) LeaseTerminationDtos.SettlementConfirmRequest body
    ) {
        LeaseTerminationApplication application = leaseTerminationService.getById(id);
        if (application != null && body != null) {
            application.setTotalDeduction(Optional.ofNullable(body.settlementAmount()).orElse(0));
            application.setRefundAmount(Optional.ofNullable(body.refundAmount()).orElse(0));
            application.setSettlementDetail(body.remark());
            leaseTerminationService.updateById(application);
        }
        if (application != null && LeaseTerminationApplication.STATUS_INSPECTION_PENDING.equals(application.getStatus())) {
            leaseTerminationService.completeInspection(CurrentUser.id(request), id);
        }
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

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退租", description = "简单版完成按钮：待退款则完成退款，待结算则确认结算并完成。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> complete(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        LeaseTerminationApplication application = leaseTerminationService.getById(id);
        if (application != null && LeaseTerminationApplication.STATUS_REFUND_PENDING.equals(application.getStatus())) {
            return ApiResponse.success("退租已完成",
                    leaseTerminationService.completeRefund(CurrentUser.id(request), id));
        }
        return ApiResponse.success("退租已完成",
                leaseTerminationService.confirmSettlement(CurrentUser.id(request), id));
    }
}
