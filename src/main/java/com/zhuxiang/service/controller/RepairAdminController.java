package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairItem;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairDetail;
import com.zhuxiang.service.dto.RepairDtos.AssignRepairRequest;
import com.zhuxiang.service.service.RepairRecordService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/repairs")
@Tag(name = "管理端报修", description = "管理端报修记录管理接口")
@SecurityRequirement(name = "bearerAuth")
public class RepairAdminController {

    private final RepairRecordService repairRecordService;
    private final UserService userService;

    public RepairAdminController(RepairRecordService repairRecordService, UserService userService) {
        this.repairRecordService = repairRecordService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "报修记录列表", description = "分页查询全平台报修记录，支持关键字搜索和状态筛选")
    public ApiResponse<PageData<AdminRepairItem>> listRepairs(
            @Parameter(description = "关键字搜索（工单号、租客姓名、手机号、房源名称、房源地址、报修内容）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) String status,
            @Parameter(description = "当前页码，从 1 开始")
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，最大 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success(repairRecordService.listAdminRepairs(
                operator.getId(), keyword, status, page, pageSize
        ));
    }

    @GetMapping("/{repairId}")
    @Operation(summary = "报修记录详情", description = "查询报修详情、图片、评价及处理时间线")
    public ApiResponse<AdminRepairDetail> detail(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success(repairRecordService.getAdminRepairDetail(operator.getId(), repairId));
    }

    @PostMapping("/{repairId}/accept")
    @Operation(summary = "受理报修", description = "将待受理报修更新为已受理状态")
    public ApiResponse<AdminRepairDetail> accept(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success("报修已受理", repairRecordService.acceptAdminRepair(operator.getId(), repairId));
    }

    @PostMapping("/{repairId}/assign")
    @Operation(summary = "报修派单", description = "为已受理报修指定处理人或维修人员")
    public ApiResponse<AdminRepairDetail> assign(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            @Valid @RequestBody AssignRepairRequest body,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success("报修已派单", repairRecordService.assignAdminRepair(operator.getId(), repairId, body));
    }

    @PostMapping("/{repairId}/start")
    @Operation(summary = "开始处理报修", description = "将已派单报修更新为处理中状态")
    public ApiResponse<AdminRepairDetail> start(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success("报修已开始处理", repairRecordService.startAdminRepair(operator.getId(), repairId));
    }

    @PostMapping("/{repairId}/finish")
    @Operation(summary = "完成维修", description = "记录维修完成时间并将报修更新为待评价状态")
    public ApiResponse<AdminRepairDetail> finish(
            @Parameter(description = "报修记录 ID") @PathVariable String repairId,
            HttpServletRequest request
    ) {
        User operator = requireManagementUser(request);
        return ApiResponse.success("维修已完成，等待用户评价", repairRecordService.finishAdminRepair(operator.getId(), repairId));
    }

    private User requireManagementUser(HttpServletRequest request) {
        User operator = userService.requireActiveUser(CurrentUser.id(request));
        String role = operator.getRole();
        if (!"ADMIN".equals(role) && !"HOUSEKEEPER".equals(role) && !"LANDLORD".equals(role)) {
            throw BusinessException.forbidden("无权处理管理端报修");
        }
        return operator;
    }
}
