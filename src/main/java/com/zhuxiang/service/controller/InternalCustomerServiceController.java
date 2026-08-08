package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.auth.InternalAgentRequestValidator;
import com.zhuxiang.service.dto.InternalCustomerServiceDtos;
import com.zhuxiang.service.service.InternalCustomerServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 内部白名单接口 —— 仅供 Python Agent 服务调用
 */
@RestController
@RequestMapping("/internal/customer-service")
@Tag(name = "内部客服数据接口", description = "为 Python Agent 提供脱敏后的用户业务数据")
public class InternalCustomerServiceController {

    private static final Logger log = LoggerFactory.getLogger(InternalCustomerServiceController.class);

    private final InternalCustomerServiceService internalService;
    private final InternalAgentRequestValidator requestValidator;

    public InternalCustomerServiceController(
            InternalCustomerServiceService internalService,
            InternalAgentRequestValidator requestValidator
    ) {
        this.internalService = internalService;
        this.requestValidator = requestValidator;
    }

    private void validateAndLog(HttpServletRequest request, String resource) {
        String requestId = requestValidator.validate(request);
        log.info("Agent 内部数据查询: requestId={} resource={}", requestId, resource);
    }

    /**
     * 查询用户当前有效租约。
     */
    @GetMapping("/users/{userId}/leases")
    @Operation(summary = "查询用户租约", description = "返回当前用户的租约简要信息列表，不包含身份证、合同完整内容等敏感字段")
    public ApiResponse<List<InternalCustomerServiceDtos.LeaseBrief>> getLeases(
            @Parameter(description = "用户ID") @PathVariable String userId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "leases");
        return ApiResponse.success(internalService.getUserLeases(userId));
    }

    /**
     * 查询用户账单。
     */
    @GetMapping("/users/{userId}/bills")
    @Operation(summary = "查询用户账单", description = "返回当前用户的账单简要信息列表，不含支付流水号等敏感字段")
    public ApiResponse<List<InternalCustomerServiceDtos.BillBrief>> getBills(
            @Parameter(description = "用户ID") @PathVariable String userId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "bills");
        return ApiResponse.success(internalService.getUserBills(userId));
    }

    /**
     * 查询用户门锁权限。
     */
    @GetMapping("/users/{userId}/locks")
    @Operation(summary = "查询用户门锁", description = "返回当前用户的智能门锁权限信息，不包含开锁密码等敏感字段")
    public ApiResponse<List<InternalCustomerServiceDtos.LockBrief>> getLocks(
            @Parameter(description = "用户ID") @PathVariable String userId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "locks");
        return ApiResponse.success(internalService.getUserLocks(userId));
    }

    /**
     * 查询用户预约看房记录。
     */
    @GetMapping("/users/{userId}/appointments")
    @Operation(summary = "查询用户预约", description = "返回当前用户的预约看房记录，手机号已脱敏")
    public ApiResponse<List<InternalCustomerServiceDtos.AppointmentBrief>> getAppointments(
            @Parameter(description = "用户ID") @PathVariable String userId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "appointments");
        return ApiResponse.success(internalService.getUserAppointments(userId));
    }

    /**
     * 查询用户报修记录。
     */
    @GetMapping("/users/{userId}/repairs")
    @Operation(summary = "查询用户报修", description = "返回当前用户的报修记录，不包含详细手机号等敏感字段")
    public ApiResponse<List<InternalCustomerServiceDtos.RepairBrief>> getRepairs(
            @Parameter(description = "用户ID") @PathVariable String userId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "repairs");
        return ApiResponse.success(internalService.getUserRepairs(userId));
    }

    /**
     * 查询房源简要信息。
     */
    @GetMapping("/houses/{houseId}/brief")
    @Operation(summary = "查询房源简要信息", description = "返回指定房源的简要信息")
    public ApiResponse<InternalCustomerServiceDtos.HouseBrief> getHouseBrief(
            @Parameter(description = "房源ID") @PathVariable String houseId,
            HttpServletRequest request
    ) {
        validateAndLog(request, "house-brief");
        return ApiResponse.success(internalService.getHouseBrief(houseId));
    }
}
