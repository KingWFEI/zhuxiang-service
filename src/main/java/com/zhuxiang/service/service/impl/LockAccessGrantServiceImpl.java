package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.service.LockAccessGrantService;
import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.LockPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 门锁权限失败隔离编排实现。
 */
@Service
public class LockAccessGrantServiceImpl implements LockAccessGrantService {

    private static final Logger log = LoggerFactory.getLogger(LockAccessGrantServiceImpl.class);
    private final LockPermissionService lockPermissionService;
    private final LockPasscodePermissionService passcodePermissionService;

    public LockAccessGrantServiceImpl(
            LockPermissionService lockPermissionService,
            LockPasscodePermissionService passcodePermissionService
    ) {
        this.lockPermissionService = lockPermissionService;
        this.passcodePermissionService = passcodePermissionService;
    }

    /** 分别执行两个独立事务，日志不包含平台凭证或密码。 */
    @Override
    public void grantLockAccessForLease(String leaseId) {
        try {
            lockPermissionService.grantTenantEKeyForLease(leaseId);
        } catch (RuntimeException exception) {
            log.warn("租约 eKey 授权未完成: leaseId={}, exceptionType={}",
                    leaseId, exception.getClass().getSimpleName());
        }
        try {
            passcodePermissionService.grantTenantPeriodPasscodeForLease(leaseId);
        } catch (RuntimeException exception) {
            log.warn("租约期限密码授权未完成: leaseId={}, exceptionType={}",
                    leaseId, exception.getClass().getSimpleName());
        }
    }
}
