package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.InternalCustomerServiceDtos;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.dto.LeaseTerminationDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * 内部白名单业务数据查询服务 —— 为 Python Agent 提供脱敏数据
 */
public interface InternalCustomerServiceService {

    /** 查询用户租约简要信息 */
    List<InternalCustomerServiceDtos.LeaseBrief> getUserLeases(String userId);

    /** 查询用户账单简要信息 */
    List<InternalCustomerServiceDtos.BillBrief> getUserBills(String userId);

    /** 查询用户门锁权限简要信息 */
    List<InternalCustomerServiceDtos.LockBrief> getUserLocks(String userId);

    /** 查询用户预约看房记录 */
    List<InternalCustomerServiceDtos.AppointmentBrief> getUserAppointments(String userId);

    /** 查询用户报修记录 */
    List<InternalCustomerServiceDtos.RepairBrief> getUserRepairs(String userId);

    /** 查询房源简要信息 */
    InternalCustomerServiceDtos.HouseBrief getHouseBrief(String houseId);

    InternalCustomerServiceDtos.ServiceOverview getUserOverview(String userId);

    List<InternalCustomerServiceDtos.HouseDetail> searchHouses(
            String keyword, String roomType, Integer minPrice, Integer maxPrice, int limit);

    InternalCustomerServiceDtos.HouseDetail getHouseDetail(String houseId);

    AppointmentDtos.ViewingSlotResult getViewingSlots(String houseId, LocalDate startDate, int days);

    List<InternalCustomerServiceDtos.RentOrderBrief> getUserRentOrders(String userId);

    InternalCustomerServiceDtos.RentOrderBrief getRentOrderDetail(String userId, String orderId);

    InternalCustomerServiceDtos.LeaseDetail getLeaseDetail(String userId, String leaseId);

    InternalCustomerServiceDtos.ContractSummary getContractSummary(String userId, String leaseId);

    InternalCustomerServiceDtos.BillDetail getBillDetail(String userId, String billId);

    List<InternalCustomerServiceDtos.PaymentBrief> getUserPayments(String userId);

    InternalCustomerServiceDtos.PaymentBrief getPaymentDetail(String userId, String paymentId);

    LeaseTerminationDtos.TerminationCheckResponse checkTermination(String userId, String leaseId);

    InternalCustomerServiceDtos.TerminationStatus getTerminationStatus(String userId, String leaseId);

    InternalCustomerServiceDtos.DepositSummary getDepositDetail(String userId, String leaseId);

    InternalCustomerServiceDtos.RepairDetail getRepairDetail(String userId, String repairId);

    InternalCustomerServiceDtos.AppointmentDetail getAppointmentDetail(String userId, String appointmentId);

    InternalCustomerServiceDtos.AuthStatus getRealNameAuthStatus(String userId);

    InternalCustomerServiceDtos.LandlordAuthStatus getLandlordAuthStatus(String userId);
}
