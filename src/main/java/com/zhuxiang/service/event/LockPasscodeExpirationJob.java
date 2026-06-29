package com.zhuxiang.service.event;

import com.zhuxiang.service.service.LockPasscodePermissionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 将自然到期的期限密码权限更新为 EXPIRED。
 */
@Component
public class LockPasscodeExpirationJob {

    private final LockPasscodePermissionService permissionService;

    public LockPasscodeExpirationJob(LockPasscodePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /** 按配置周期扫描到期权限，不调用无网关门锁的删除接口。 */
    @Scheduled(
            fixedDelayString = "${app.lock-passcode.expiration-scan-ms:300000}",
            initialDelayString = "${app.lock-passcode.expiration-scan-ms:300000}"
    )
    public void expireDuePasscodes() {
        permissionService.expireDuePasscodes();
    }
}
