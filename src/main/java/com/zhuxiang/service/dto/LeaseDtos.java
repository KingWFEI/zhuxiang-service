package com.zhuxiang.service.dto;

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
