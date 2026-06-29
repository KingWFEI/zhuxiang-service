package com.zhuxiang.service.service;

/**
 * 租约生效后的统一门锁权限编排入口。
 */
public interface LockAccessGrantService {

    /** 独立下发 eKey 和期限密码，任一失败不影响另一权限。 */
    void grantLockAccessForLease(String leaseId);
}
