package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.BindRoomRequest;
import com.zhuxiang.service.dto.BleStatusRequest;
import com.zhuxiang.service.dto.InitializeLockResponse;
import com.zhuxiang.service.dto.LocalInitializedLockRequest;
import com.zhuxiang.service.dto.LocalInitializedLockResponse;
import com.zhuxiang.service.dto.SmartLockByMacResponse;
import com.zhuxiang.service.dto.SmartLockDetailResponse;
import com.zhuxiang.service.dto.SmartLockUnlockDataResponse;
import com.zhuxiang.service.entity.SmartLock;

/**
 * 管理端门锁服务。
 */
public interface AdminLockService extends IService<SmartLock> {

    /**
     * 保存App端SDK初始化成功后的门锁数据。
     */
    LocalInitializedLockResponse saveLocalInitializedLock(LocalInitializedLockRequest request, String operatorId);

    /**
     * 将已录入的门锁绑定到指定房源或房间。
     */
    InitializeLockResponse bindRoom(String smartLockId, BindRoomRequest request, String operatorId);

    /**
     * 将已保存的门锁初始化数据同步到通通锁开放平台。
     */
    InitializeLockResponse syncPlatform(String smartLockId, String operatorId);

    /**
     * 删除门锁和房源之间的绑定关系。
     */
    InitializeLockResponse deleteRoomBinding(String smartLockId, String operatorId);

    /**
     * 根据MAC查询门锁本地记录。
     */
    SmartLockByMacResponse getByLockMac(String lockMac, String operatorId);

    /**
     * 查询门锁管理详情。
     */
    SmartLockDetailResponse getDetail(String smartLockId, String operatorId);

    /**
     * Update battery and signal data from a nearby BLE scan.
     */
    SmartLockDetailResponse updateBleStatus(String smartLockId, BleStatusRequest request, String operatorId);

    /**
     * 查询蓝牙开锁所需数据。
     */
    SmartLockUnlockDataResponse getUnlockData(String smartLockId, String operatorId);
}
