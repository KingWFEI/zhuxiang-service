package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairItem;
import com.zhuxiang.service.dto.RepairDtos.CreateRepairRequest;
import com.zhuxiang.service.dto.RepairDtos.RepairItem;
import com.zhuxiang.service.entity.RepairRecord;

public interface RepairRecordService extends IService<RepairRecord> {

    String createRepair(String userId, CreateRepairRequest request);

    RepairItem getRepairDetail(String userId, String repairId);

    PageData<RepairItem> listMyRepairs(String userId, long page, long pageSize);

    void cancelRepair(String userId, String repairId, String cancelReason);

    void reviewRepair(String userId, String repairId, Integer rating, String reviewContent);

    PageData<AdminRepairItem> listAdminRepairs(String keyword, String status, long page, long pageSize);
}
