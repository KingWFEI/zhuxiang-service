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
@Tag(name = "退租申请(App)", description = "租户退租申请相关接口")
@SecurityRequirement(name = "bearerAuth")
public class LeaseTerminationAppController {

    private final LeaseTerminationService leaseTerminationService;

    public LeaseTerminationAppController(LeaseTerminationService leaseTerminationService) {
        this.leaseTerminationService = leaseTerminationService;
    }

    @GetMapping("/app/contracts/{contractId}/termination/check")
    @Operation(summary = "查询是否可申请退租", description = "校验合同归属和状态，返回退租条件是否满足及欠费信息。")
    public ApiResponse<LeaseTerminationDtos.TerminationCheckResponse> check(
            HttpServletRequest request,
            @PathVariable String contractId
    ) {
        return ApiResponse.success(
                leaseTerminationService.checkTermination(CurrentUser.id(request), contractId)
        );
    }

    @PostMapping("/app/contracts/{contractId}/termination/apply")
    @Operation(summary = "提交退租申请", description = "创建退租申请，状态为待审核。")
    public ApiResponse<LeaseTerminationDtos.ApplyResponse> apply(
            HttpServletRequest request,
            @PathVariable String contractId,
            @Valid @RequestBody LeaseTerminationDtos.ApplyRequest body
    ) {
        return ApiResponse.success("退租申请已提交",
                leaseTerminationService.apply(CurrentUser.id(request), contractId, body));
    }

    @GetMapping("/app/contracts/{contractId}/termination/current")
    @Operation(summary = "查询当前退租申请", description = "返回当前合同进行中的退租申请，没有则返回null。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> getCurrent(
            HttpServletRequest request,
            @PathVariable String contractId
    ) {
        return ApiResponse.success(
                leaseTerminationService.getCurrent(CurrentUser.id(request), contractId)
        );
    }

    @GetMapping("/app/termination-applications/{id}")
    @Operation(summary = "查询退租申请详情", description = "返回退租申请详细信息及时间线。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> getDetail(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success(
                leaseTerminationService.getDetail(CurrentUser.id(request), id)
        );
    }

    @PutMapping("/app/termination-applications/{id}/supplement")
    @Operation(summary = "补充材料", description = "仅允许状态为 need_supplement 时补充材料。")
    public ApiResponse<Void> supplement(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody LeaseTerminationDtos.SupplementRequest body
    ) {
        leaseTerminationService.supplement(CurrentUser.id(request), id, body);
        return ApiResponse.success("材料已补充", null);
    }

    @PostMapping("/app/termination-applications/{id}/cancel")
    @Operation(summary = "撤销申请", description = "仅允许 pending_review 和 need_supplement 状态时撤销。")
    public ApiResponse<Void> cancel(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody LeaseTerminationDtos.CancelRequest body
    ) {
        leaseTerminationService.cancel(CurrentUser.id(request), id, body);
        return ApiResponse.success("退租申请已撤销", null);
    }
}
