package com.zhuxiang.service.event;

import com.zhuxiang.service.service.LockAccessGrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 租约事务提交后触发门锁权限下发，确保平台异常不会回滚租约。
 */
@Component
public class LeaseLockPermissionListener {

    private static final Logger log = LoggerFactory.getLogger(LeaseLockPermissionListener.class);

    private final LockAccessGrantService lockAccessGrantService;

    public LeaseLockPermissionListener(LockAccessGrantService lockAccessGrantService) {
        this.lockAccessGrantService = lockAccessGrantService;
    }

    /**
     * 在租约事务成功提交后调用统一门锁授权服务。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeaseActivated(LeaseActivatedEvent event) {
        try {
            lockAccessGrantService.grantLockAccessForLease(event.leaseId());
        } catch (RuntimeException exception) {
            log.error("租约生效后的门锁权限编排异常: leaseId={}, exceptionType={}",
                    event.leaseId(), exception.getClass().getSimpleName());
        }
    }
}
