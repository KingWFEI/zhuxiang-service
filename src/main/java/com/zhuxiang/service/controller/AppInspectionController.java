package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * App 端退租验收接口。
 */
@RequireAuth
@RestController
@Tag(name = "App-退租验收", description = "入住/退租照片采集与提交")
@SecurityRequirement(name = "bearerAuth")
public class AppInspectionController {

    private final InspectionService inspectionService;

    public AppInspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    // ==================== 入住验收 ====================

    @GetMapping("/app/contracts/{contractId}/move-in-inspection")
    @Operation(summary = "获取入住验收数据", description = "返回验收标准清单和已上传的入住照片。")
    public ApiResponse<InspectionDtos.MoveInInspectionResponse> getMoveInInspection(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        return ApiResponse.success(
                inspectionService.getMoveInInspection(CurrentUser.id(request), contractId));
    }

    @PostMapping("/app/contracts/{contractId}/move-in-inspection/submit")
    @Operation(summary = "提交入住验收", description = "上传验收照片并提交入住验收。必拍项未满足时拒绝提交。")
    public ApiResponse<String> submitMoveInInspection(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId,
            @Valid @RequestBody InspectionDtos.SubmitMoveInRequest submitRequest
    ) {
        inspectionService.submitMoveInInspection(CurrentUser.id(request), contractId, submitRequest);
        return ApiResponse.success("入住验收已提交");
    }

    @PostMapping("/app/contracts/{contractId}/move-in-inspection/confirm")
    @Operation(summary = "确认入住验收", description = "租客确认入住验收照片无误（SUBMITTED → TENANT_CONFIRMED）。")
    public ApiResponse<String> confirmMoveIn(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        inspectionService.confirmMoveIn(CurrentUser.id(request), contractId);
        return ApiResponse.success("入住验收已确认");
    }

    // ==================== 退租验收 ====================

    @GetMapping("/app/contracts/{contractId}/move-out-inspection")
    @Operation(summary = "获取退租验收数据", description = "返回入住验收快照标准和已上传的退租照片。")
    public ApiResponse<InspectionDtos.MoveOutInspectionResponse> getMoveOutInspection(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        return ApiResponse.success(
                inspectionService.getMoveOutInspection(CurrentUser.id(request), contractId));
    }

    @PostMapping("/app/contracts/{contractId}/move-out-inspection/submit")
    @Operation(summary = "提交退租验收", description = "上传退租照片并提交验收。必拍项未满足时拒绝提交。重复提交会覆盖之前的退租照片。")
    public ApiResponse<String> submitMoveOutInspection(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId,
            @Valid @RequestBody InspectionDtos.SubmitMoveOutRequest submitRequest
    ) {
        inspectionService.submitMoveOutInspection(CurrentUser.id(request), contractId, submitRequest);
        return ApiResponse.success("退租验收已提交");
    }

    // ==================== 押金结算 ====================

    @GetMapping("/app/contracts/{contractId}/deposit-settlement")
    @Operation(summary = "查看押金扣款明细", description = "租客查看管理端创建的押金扣款明细。")
    public ApiResponse<InspectionDtos.SettlementResponse> getTenantSettlement(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        return ApiResponse.success(
                inspectionService.getTenantSettlement(CurrentUser.id(request), contractId));
    }

}
