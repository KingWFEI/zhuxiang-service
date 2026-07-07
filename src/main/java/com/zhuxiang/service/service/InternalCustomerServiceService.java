package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.InternalCustomerServiceDtos;

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
}
