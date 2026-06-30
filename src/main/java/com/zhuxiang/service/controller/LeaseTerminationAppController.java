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
@RequestMapping("/leases")
@Tag(name = "退租申请(App)", description = "租户退租申请相关接口")
@SecurityRequirement(name = "bearerAuth")
public class LeaseTerminationAppController {

    private final LeaseTerminationService leaseTerminationService;

    public LeaseTerminationAppController(LeaseTerminationService leaseTerminationService) {
        this.leaseTerminationService = leaseTerminationService;
    }

    @GetMapping("/{leaseId}/termination/check")
    @Operation(summary = "查询是否可申请退租", description = "校验租约归属、租约及合同状态，返回退租条件是否满足及欠费信息。")
    public ApiResponse<LeaseTerminationDtos.TerminationCheckResponse> check(
            HttpServletRequest request,
            @PathVariable String leaseId
    ) {
        return ApiResponse.success(
                leaseTerminationService.checkTermination(CurrentUser.id(request), leaseId)
        );
    }

    @PostMapping("/{leaseId}/termination/apply")
    @Operation(summary = "提交退租申请", description = "按租约创建完整退租申请，状态为待审核。")
    public ApiResponse<LeaseTerminationDtos.ApplyResponse> apply(
            HttpServletRequest request,
            @PathVariable String leaseId,
            @Valid @RequestBody LeaseTerminationDtos.ApplyRequest body
    ) {
        return ApiResponse.success("退租申请已提交",
                leaseTerminationService.apply(CurrentUser.id(request), leaseId, body));
    }

    @GetMapping("/{leaseId}/termination/current")
    @Operation(summary = "查询当前退租申请", description = "返回当前租约进行中的退租申请，没有则返回null。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> getCurrent(
            HttpServletRequest request,
            @PathVariable String leaseId
    ) {
        return ApiResponse.success(
                leaseTerminationService.getCurrent(CurrentUser.id(request), leaseId)
        );
    }

    @GetMapping("/termination-applications/{id}")
    @Operation(summary = "查询退租申请详情", description = "返回退租申请详细信息及时间线。")
    public ApiResponse<LeaseTerminationDtos.TerminationDetailResponse> getDetail(
            HttpServletRequest request,
            @PathVariable String id
    ) {
        return ApiResponse.success(
                leaseTerminationService.getDetail(CurrentUser.id(request), id)
        );
    }

    @PutMapping("/termination-applications/{id}/supplement")
    @Operation(summary = "补充材料", description = "仅允许状态为 need_supplement 时补充材料。")
    public ApiResponse<Void> supplement(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody LeaseTerminationDtos.SupplementRequest body
    ) {
        leaseTerminationService.supplement(CurrentUser.id(request), id, body);
        return ApiResponse.success("材料已补充", null);
    }

    @PostMapping("/termination-applications/{id}/cancel")
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
