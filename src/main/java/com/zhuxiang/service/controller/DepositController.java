package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.DepositDtos;
import com.zhuxiang.service.entity.DepositDeduction;
import com.zhuxiang.service.entity.DepositRecord;
import com.zhuxiang.service.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequireAuth
@RestController
@Tag(name = "押金", description = "押金记录查询")
@SecurityRequirement(name = "bearerAuth")
public class DepositController {

    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping("/deposits/my/{leaseId}")
    @Operation(summary = "查看押金详情", description = "查看某租约的押金记录和扣款明细")
    public ApiResponse<DepositDtos.DepositDetail> getMyDeposit(
            HttpServletRequest request,
            @Parameter(description = "租约 ID") @PathVariable String leaseId
    ) {
        String userId = CurrentUser.id(request);
        DepositRecord record = depositService.getByLeaseId(leaseId);
        if (record == null || !userId.equals(record.getUserId())) {
            return ApiResponse.success(null);
        }

        List<DepositDeduction> deductions = depositService.getDeductions(record.getId());
        List<DepositDtos.DeductionView> deductionViews = deductions.stream()
                .map(d -> new DepositDtos.DeductionView(
                        d.getId(),
                        d.getDeductionType(),
                        d.getAmount(),
                        d.getDescription(),
                        parseEvidenceUrls(d.getEvidenceUrls())
                ))
                .toList();

        return ApiResponse.success(new DepositDtos.DepositDetail(
                record.getId(),
                record.getLeaseId(),
                record.getAmount(),
                record.getWithheldAmount(),
                record.getRefundedAmount(),
                record.getStatus(),
                deductionViews,
                record.getRefundedAt(),
                record.getCreatedAt()
        ));
    }

    @SuppressWarnings("unchecked")
    private List<String> parseEvidenceUrls(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
