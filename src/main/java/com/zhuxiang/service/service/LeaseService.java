package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Lease;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.ProfileDtos;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service
* @createDate 2026-06-12 19:57:39
*/
public interface LeaseService extends IService<Lease> {

    /**
     * 获取用户当前生效的租约。
     */
    ProfileDtos.CurrentHome getCurrentHome(String userId);

    /**
     * 获取用户当前租约对应的门锁展示信息。
     */
    ProfileDtos.LockInfo getLockInfo(String userId);

    /**
     * 获取用户全部租约，按当前生效和历史分类。
     */
    LeaseDtos.LeaseListResponse getUserLeases(String userId);

    /** 根据租约 ID 查询当前租客自己的单条租约详情。 */
    LeaseDtos.LeaseDetail getLeaseDetail(String leaseId, String currentUserId);

    /**
     * 校验租约归属后，获取租约关联的门锁权限摘要及蓝牙 SDK lockData。
     */
    LeaseDtos.UnlockDataResponse getUnlockData(String leaseId, String currentUserId);

    /** 校验当前租客后返回租约期限密码。 */
    LeaseLockPasscodeResponse getLockPasscode(String leaseId, String currentUserId);

    /** 校验当前租客后重试生成并返回租约期限密码。 */
    LeaseLockPasscodeResponse retryLockPasscode(String leaseId, String currentUserId);
}
