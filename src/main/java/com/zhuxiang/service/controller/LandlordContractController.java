package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.EsignSignResponse;
import com.zhuxiang.service.dto.EsignSignStatusResponse;
import com.zhuxiang.service.dto.LandlordContractDtos;
import com.zhuxiang.service.service.LandlordContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireAuth
@RequestMapping("/landlord/contracts")
@Tag(name = "房东合同", description = "房东工作台合同查询和电子签署")
@SecurityRequirement(name = "bearerAuth")
public class LandlordContractController {

    private final LandlordContractService landlordContractService;

    public LandlordContractController(LandlordContractService landlordContractService) {
        this.landlordContractService = landlordContractService;
    }

    @GetMapping("/pending-sign")
    @Operation(summary = "待我签署合同", description = "查询当前房东尚未签署的合同，不要求租客先签署。")
    public ApiResponse<PageData<LandlordContractDtos.ContractItem>> listPendingSign(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.success(landlordContractService.listPendingSign(
                CurrentUser.id(request), page, pageSize));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "合同详情", description = "查看属于当前房东的租房合同及双方签署状态。")
    public ApiResponse<LandlordContractDtos.ContractDetail> getDetail(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                landlordContractService.getDetail(CurrentUser.id(request), orderId));
    }

    @PostMapping("/{orderId}/sign")
    @Operation(summary = "获取房东签署链接", description = "发起或复用电子签署流程，返回当前房东的 e签宝签署链接。")
    public ApiResponse<EsignSignResponse> sign(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID") @PathVariable String orderId
    ) {
        return ApiResponse.success("请打开签署链接完成签名",
                landlordContractService.sign(CurrentUser.id(request), orderId));
    }

    @PostMapping("/{orderId}/refresh")
    @Operation(summary = "刷新合同签署状态", description = "同步双方签署状态；双方完成后自动完成订单并生成租约。")
    public ApiResponse<EsignSignStatusResponse> refresh(
            HttpServletRequest request,
            @Parameter(description = "租房订单 ID") @PathVariable String orderId
    ) {
        return ApiResponse.success(
                landlordContractService.refresh(CurrentUser.id(request), orderId));
    }
}
