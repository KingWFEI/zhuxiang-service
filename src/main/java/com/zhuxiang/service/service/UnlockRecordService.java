package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.UnlockRecordDtos;
import com.zhuxiang.service.entity.UnlockRecord;

public interface UnlockRecordService extends IService<UnlockRecord> {

    UnlockRecordDtos.UnlockRecordResponse record(
            String leaseId,
            String currentUserId,
            UnlockRecordDtos.UnlockRecordRequest request
    );

    UnlockRecordDtos.UnlockRecordListResponse listMyRecords(String userId);

    UnlockRecordDtos.LockPermissionResponse getMyPermission(String userId);
}
