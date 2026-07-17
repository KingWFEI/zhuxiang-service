package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.service.InspectionService;
import com.zhuxiang.service.service.InspectionTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端退租验收接口。
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/admin")
@Tag(name = "管理端-退租验收", description = "配置房源验收模板、查看入住/退租对比、处理押金扣款")
@SecurityRequirement(name = "bearerAuth")
public class AdminInspectionController {

    private final InspectionTemplateService templateService;
    private final InspectionService inspectionService;

    public AdminInspectionController(InspectionTemplateService templateService,
                                      InspectionService inspectionService) {
        this.templateService = templateService;
        this.inspectionService = inspectionService;
    }

    // ==================== 模板配置 ====================

    @GetMapping("/houses/{houseId}/inspection-template")
    @Operation(summary = "查询房源验收模板", description = "返回指定房源的退租验收标准配置。未配置时返回空 rooms 列表。")
    public ApiResponse<InspectionDtos.TemplateResponse> getTemplate(
            @Parameter(description = "房源 ID", required = true) @PathVariable String houseId
    ) {
        return ApiResponse.success(templateService.getTemplate(houseId));
    }

    @PutMapping("/houses/{houseId}/inspection-template")
    @Operation(summary = "保存房源验收模板", description = "保存或更新指定房源的退租验收标准。版本号自动递增。")
    public ApiResponse<InspectionDtos.TemplateResponse> saveTemplate(
            HttpServletRequest request,
            @Parameter(description = "房源 ID", required = true) @PathVariable String houseId,
            @Valid @RequestBody InspectionDtos.SaveTemplateRequest saveRequest
    ) {
        return ApiResponse.success(
                templateService.saveTemplate(houseId, CurrentUser.id(request), saveRequest));
    }

    // ==================== 对比与结算 ====================

    @GetMapping("/contracts/{contractId}/inspection-comparison")
    @Operation(summary = "查看验收对比", description = "按验收项分组展示入住和退租照片对比，含已有扣款明细。")
    public ApiResponse<InspectionDtos.ComparisonResponse> getComparison(
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        return ApiResponse.success(inspectionService.getComparison(contractId));
    }

    @GetMapping("/contracts/{contractId}/deposit-settlement")
    @Operation(summary = "查看押金扣款结算", description = "管理端查看已有的押金扣款明细和租客确认状态。")
    public ApiResponse<InspectionDtos.SettlementResponse> getSettlement(
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId
    ) {
        return ApiResponse.success(inspectionService.getAdminSettlement(contractId));
    }

    @PostMapping("/contracts/{contractId}/deposit-settlement")
    @Operation(summary = "创建/覆盖押金扣款结算", description = "管理端根据验收对比结果创建押金扣款明细。重复提交会覆盖之前的扣款。")
    public ApiResponse<InspectionDtos.SettlementResponse> createSettlement(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId,
            @Valid @RequestBody InspectionDtos.CreateSettlementRequest settlementRequest
    ) {
        return ApiResponse.success(
                inspectionService.createSettlement(CurrentUser.id(request), contractId, settlementRequest));
    }

    // ==================== 锁定验房 ====================

    @PostMapping("/contracts/{contractId}/inspection/lock")
    @Operation(summary = "锁定验房", description = "管理端确认已完成线下验房，并锁定现场照片证据（SUBMITTED → LOCKED）。锁定后禁止删除或覆盖照片，只能追加更正记录。幂等，重复调用直接返回当前锁定结果。")
    public ApiResponse<String> lockInspection(
            HttpServletRequest request,
            @Parameter(description = "合同 ID", required = true) @PathVariable String contractId,
            @Valid @RequestBody InspectionDtos.LockRequest lockRequest
    ) {
        inspectionService.lockInspection(CurrentUser.id(request), contractId, lockRequest);
        return ApiResponse.success("验房已锁定");
    }
}
