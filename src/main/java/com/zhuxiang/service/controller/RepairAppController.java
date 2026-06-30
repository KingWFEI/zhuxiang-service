package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.RepairDtos.CancelRepairRequest;
import com.zhuxiang.service.dto.RepairDtos.CreateRepairRequest;
import com.zhuxiang.service.dto.RepairDtos.RepairItem;
import com.zhuxiang.service.dto.RepairDtos.ReviewRepairRequest;
import com.zhuxiang.service.service.RepairRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/repairs")
@Tag(name = "App 报修", description = "App 端报修记录接口")
@SecurityRequirement(name = "bearerAuth")
public class RepairAppController {

    private final RepairRecordService repairRecordService;

    public RepairAppController(RepairRecordService repairRecordService) {
        this.repairRecordService = repairRecordService;
    }

    @GetMapping("/my")
    @Operation(summary = "我的报修记录", description = "分页查询当前登录用户的报修记录，按创建时间倒序")
    public ApiResponse<PageData<RepairItem>> listMyRepairs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        return ApiResponse.success(repairRecordService.listMyRepairs(userId, page, pageSize));
    }

    @GetMapping("/{repairId}")
    @Operation(summary = "报修记录详情", description = "查看单条报修记录的详细信息，必须校验当前用户权限")
    public ApiResponse<RepairItem> getRepairDetail(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        return ApiResponse.success(repairRecordService.getRepairDetail(userId, repairId));
    }

    @PostMapping
    @Operation(summary = "创建报修", description = "提交新的报修申请")
    public ApiResponse<String> createRepair(
            @Valid @RequestBody CreateRepairRequest body,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        String repairId = repairRecordService.createRepair(userId, body);
        return ApiResponse.success("报修提交成功", repairId);
    }

    @PostMapping("/{repairId}/cancel")
    @Operation(summary = "取消报修", description = "取消已提交的报修申请（仅待受理状态可取消）")
    public ApiResponse<Void> cancelRepair(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            @RequestBody(required = false) CancelRepairRequest body,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        String cancelReason = body != null ? body.cancelReason() : null;
        repairRecordService.cancelRepair(userId, repairId, cancelReason);
        return ApiResponse.success("报修已取消", null);
    }

    @PostMapping("/{repairId}/review")
    @Operation(summary = "评价报修", description = "对已完成的维修服务进行评价（仅待评价状态可评价）")
    public ApiResponse<Void> reviewRepair(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            @Valid @RequestBody ReviewRepairRequest body,
            HttpServletRequest request
    ) {
        String userId = CurrentUser.id(request);
        repairRecordService.reviewRepair(userId, repairId, body.rating(), body.reviewContent());
        return ApiResponse.success("评价提交成功", null);
    }
}
