package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.entity.LockPasscodePermission;

/**
 * 租约期限密码权限服务。
 */
public interface LockPasscodePermissionService extends IService<LockPasscodePermission> {

    /** 幂等生成租约对应的 TTLock V4 期限密码。 */
    LockPasscodePermission grantTenantPeriodPasscodeForLease(String leaseId);

    /** 校验租约归属并限流后，为租客重试生成期限密码。 */
    LockPasscodePermission retryTenantPeriodPasscodeForLease(String leaseId, String currentUserId);

    /** 校验租客、租约和权限后解密返回期限密码。 */
    LeaseLockPasscodeResponse getTenantPasscode(String leaseId, String currentUserId);

    /** 正常到期时将仍生效的密码权限标记为 EXPIRED。 */
    void expirePasscodesForLease(String leaseId);

    /**
     * 提前退租、终止或取消时撤销权限；未来对应业务流程必须调用此方法。
     */
    void revokePasscodesForLease(String leaseId);

    /** 批量标记已经自然到期的密码权限。 */
    void expireDuePasscodes();
}
