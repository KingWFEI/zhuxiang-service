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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDate;

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

    @GetMapping("/users/{userId}/overview")
    @Operation(summary = "查询用户客服业务概览")
    public ApiResponse<InternalCustomerServiceDtos.ServiceOverview> getOverview(
            @PathVariable String userId, HttpServletRequest request) {
        validateAndLog(request, "overview");
        return ApiResponse.success(internalService.getUserOverview(userId));
    }

    @GetMapping("/houses/search")
    @Operation(summary = "搜索可租房源")
    public ApiResponse<List<InternalCustomerServiceDtos.HouseDetail>> searchHouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {
        validateAndLog(request, "house-search");
        return ApiResponse.success(internalService.searchHouses(
                keyword, roomType, minPrice, maxPrice, limit));
    }

    @GetMapping("/houses/{houseId}/detail")
    @Operation(summary = "查询房源脱敏详情")
    public ApiResponse<InternalCustomerServiceDtos.HouseDetail> getHouseDetail(
            @PathVariable String houseId, HttpServletRequest request) {
        validateAndLog(request, "house-detail");
        return ApiResponse.success(internalService.getHouseDetail(houseId));
    }

    @GetMapping("/houses/{houseId}/viewing-slots")
    @Operation(summary = "查询房源可预约时段")
    public ApiResponse<?> getViewingSlots(
            @PathVariable String houseId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        validateAndLog(request, "viewing-slots");
        return ApiResponse.success(internalService.getViewingSlots(houseId, startDate, days));
    }

    @GetMapping("/users/{userId}/rent-orders")
    @Operation(summary = "查询用户租房订单")
    public ApiResponse<List<InternalCustomerServiceDtos.RentOrderBrief>> getRentOrders(
            @PathVariable String userId, HttpServletRequest request) {
        validateAndLog(request, "rent-orders");
        return ApiResponse.success(internalService.getUserRentOrders(userId));
    }

    @GetMapping("/users/{userId}/rent-orders/{orderId}")
    @Operation(summary = "查询租房订单详情")
    public ApiResponse<InternalCustomerServiceDtos.RentOrderBrief> getRentOrderDetail(
            @PathVariable String userId, @PathVariable String orderId, HttpServletRequest request) {
        validateAndLog(request, "rent-order-detail");
        return ApiResponse.success(internalService.getRentOrderDetail(userId, orderId));
    }

    @GetMapping("/users/{userId}/leases/{leaseId}")
    @Operation(summary = "查询用户租约详情")
    public ApiResponse<InternalCustomerServiceDtos.LeaseDetail> getLeaseDetail(
            @PathVariable String userId, @PathVariable String leaseId, HttpServletRequest request) {
        validateAndLog(request, "lease-detail");
        return ApiResponse.success(internalService.getLeaseDetail(userId, leaseId));
    }

    @GetMapping("/users/{userId}/leases/{leaseId}/contract")
    @Operation(summary = "查询用户合同摘要")
    public ApiResponse<InternalCustomerServiceDtos.ContractSummary> getContractSummary(
            @PathVariable String userId, @PathVariable String leaseId, HttpServletRequest request) {
        validateAndLog(request, "contract-summary");
        return ApiResponse.success(internalService.getContractSummary(userId, leaseId));
    }

    @GetMapping("/users/{userId}/bills/{billId}")
    @Operation(summary = "查询用户账单详情")
    public ApiResponse<InternalCustomerServiceDtos.BillDetail> getBillDetail(
            @PathVariable String userId, @PathVariable String billId, HttpServletRequest request) {
        validateAndLog(request, "bill-detail");
        return ApiResponse.success(internalService.getBillDetail(userId, billId));
    }

    @GetMapping("/users/{userId}/payments")
    @Operation(summary = "查询用户支付记录")
    public ApiResponse<List<InternalCustomerServiceDtos.PaymentBrief>> getPayments(
            @PathVariable String userId, HttpServletRequest request) {
        validateAndLog(request, "payments");
        return ApiResponse.success(internalService.getUserPayments(userId));
    }

    @GetMapping("/users/{userId}/payments/{paymentId}")
    @Operation(summary = "查询用户支付详情")
    public ApiResponse<InternalCustomerServiceDtos.PaymentBrief> getPaymentDetail(
            @PathVariable String userId, @PathVariable String paymentId, HttpServletRequest request) {
        validateAndLog(request, "payment-detail");
        return ApiResponse.success(internalService.getPaymentDetail(userId, paymentId));
    }

    @GetMapping("/users/{userId}/leases/{leaseId}/termination/check")
    @Operation(summary = "查询用户退租资格")
    public ApiResponse<?> checkTermination(
            @PathVariable String userId, @PathVariable String leaseId, HttpServletRequest request) {
        validateAndLog(request, "termination-check");
        return ApiResponse.success(internalService.checkTermination(userId, leaseId));
    }

    @GetMapping("/users/{userId}/leases/{leaseId}/termination")
    @Operation(summary = "查询用户退租进度")
    public ApiResponse<InternalCustomerServiceDtos.TerminationStatus> getTerminationStatus(
            @PathVariable String userId, @PathVariable String leaseId, HttpServletRequest request) {
        validateAndLog(request, "termination-status");
        return ApiResponse.success(internalService.getTerminationStatus(userId, leaseId));
    }

    @GetMapping("/users/{userId}/leases/{leaseId}/deposit")
    @Operation(summary = "查询用户押金及结算信息")
    public ApiResponse<InternalCustomerServiceDtos.DepositSummary> getDepositDetail(
            @PathVariable String userId, @PathVariable String leaseId, HttpServletRequest request) {
        validateAndLog(request, "deposit-detail");
        return ApiResponse.success(internalService.getDepositDetail(userId, leaseId));
    }

    @GetMapping("/users/{userId}/repairs/{repairId}")
    @Operation(summary = "查询用户报修详情")
    public ApiResponse<InternalCustomerServiceDtos.RepairDetail> getRepairDetail(
            @PathVariable String userId, @PathVariable String repairId, HttpServletRequest request) {
        validateAndLog(request, "repair-detail");
        return ApiResponse.success(internalService.getRepairDetail(userId, repairId));
    }

    @GetMapping("/users/{userId}/appointments/{appointmentId}")
    @Operation(summary = "查询用户预约详情")
    public ApiResponse<InternalCustomerServiceDtos.AppointmentDetail> getAppointmentDetail(
            @PathVariable String userId, @PathVariable String appointmentId, HttpServletRequest request) {
        validateAndLog(request, "appointment-detail");
        return ApiResponse.success(internalService.getAppointmentDetail(userId, appointmentId));
    }

    @GetMapping("/users/{userId}/auth/real-name")
    @Operation(summary = "查询用户实名认证状态")
    public ApiResponse<InternalCustomerServiceDtos.AuthStatus> getRealNameAuthStatus(
            @PathVariable String userId, HttpServletRequest request) {
        validateAndLog(request, "real-name-auth");
        return ApiResponse.success(internalService.getRealNameAuthStatus(userId));
    }

    @GetMapping("/users/{userId}/auth/landlord")
    @Operation(summary = "查询用户房东认证状态")
    public ApiResponse<InternalCustomerServiceDtos.LandlordAuthStatus> getLandlordAuthStatus(
            @PathVariable String userId, HttpServletRequest request) {
        validateAndLog(request, "landlord-auth");
        return ApiResponse.success(internalService.getLandlordAuthStatus(userId));
    }
}
