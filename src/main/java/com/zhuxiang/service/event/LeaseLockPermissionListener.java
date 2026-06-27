package com.zhuxiang.service.event;

import com.zhuxiang.service.service.LockPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 租约事务提交后触发租客eKey下发，确保平台异常不会回滚租约。
 */
@Component
public class LeaseLockPermissionListener {

    private static final Logger log = LoggerFactory.getLogger(LeaseLockPermissionListener.class);

    private final LockPermissionService lockPermissionService;

    public LeaseLockPermissionListener(LockPermissionService lockPermissionService) {
        this.lockPermissionService = lockPermissionService;
    }

    /**
     * 在租约事务成功提交后调用内部eKey授权服务。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeaseActivated(LeaseActivatedEvent event) {
        try {
            lockPermissionService.grantTenantEKeyForLease(event.leaseId());
        } catch (RuntimeException exception) {
            log.error("租约生效后下发TTLock eKey失败: leaseId={}, message={}",
                    event.leaseId(), exception.getMessage(), exception);
        }
    }
}
