package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class LeaseDtos {

    private LeaseDtos() {
    }

    public record UnlockDataResponse(
            String leaseId,
            String smartLockId,
            String roomName,
            String houseName,
            String lockName,
            String lockMac,
            String lockData,
            Long ttlockKeyId,
            String startTime,
            String endTime,
            String permissionStatus,
            boolean bluetoothUnlockAvailable,
            boolean passcodeAvailable,
            String passcodeStatus,
            String passcodeStartTime,
            String passcodeEndTime
    ) {
    }

    public record LeaseListResponse(
            List<LeaseItem> currentLeases,
            List<LeaseItem> historyLeases
    ) {
    }

    @Schema(description = "租客单条租约详情")
    public record LeaseDetail(
            @Schema(description = "租约 ID") String id,
            @Schema(description = "合同 ID") String contractId,
            @Schema(description = "房源 ID") String houseId,
            @Schema(description = "房间名称") String houseName,
            @Schema(description = "房源详细地址") String houseAddress,
            @Schema(description = "房源摘要") String houseSummary,
            @Schema(description = "租客姓名") String tenantName,
            @Schema(description = "租客手机号") String tenantPhone,
            @Schema(description = "租客身份证号") String tenantIdCard,
            @Schema(description = "租约开始日期") LocalDate startDate,
            @Schema(description = "租约结束日期") LocalDate endDate,
            @Schema(description = "月租金，单位元") BigDecimal monthlyRent,
            @Schema(description = "押金，单位元") BigDecimal deposit,
            @Schema(description = "付款方式") String paymentMethod,
            @Schema(description = "每月付款日") Integer paymentDay,
            @Schema(description = "租约状态") String status,
            @Schema(description = "合同状态") String contractStatus,
            @Schema(description = "账单状态：unpaid 或 paid") String billStatus,
            @Schema(description = "蓝牙开锁 eKey 权限状态") String lockPermissionStatus,
            @Schema(description = "管家姓名") String keeperName,
            @Schema(description = "管家联系电话") String keeperPhone,
            @Schema(description = "待支付账单标题；没有待支付账单时为空") String pendingBillTitle,
            @Schema(description = "待支付账单金额，单位元；没有时为空") BigDecimal pendingBillAmount,
            @Schema(description = "待支付账单截止日期；没有时为空") LocalDate pendingBillDueDate
    ) {
    }

    public record LeaseItem(
            String leaseId,
            String contractId,
            String houseId,
            String houseName,
            String houseAddress,
            String houseSummary,
            String houseImageUrl,
            LocalDate startDate,
            LocalDate endDate,
            Integer monthlyRent,
            Integer deposit,
            String paymentMethod,
            Integer paymentDay,
            String leaseStatus,
            String contractStatus,
            String billStatus,
            String lockPermissionStatus,
            String lockId,
            String keeperName,
            String keeperPhone
    ) {
    }
}
