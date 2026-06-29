package com.zhuxiang.service;

import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.impl.LockAccessGrantServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LockAccessGrantServiceTests {

    @Test
    void eKeyFailureDoesNotPreventPasscodeGrant() {
        LockPermissionService ekeyService = mock(LockPermissionService.class);
        LockPasscodePermissionService passcodeService = mock(LockPasscodePermissionService.class);
        doThrow(new IllegalStateException("platform failure"))
                .when(ekeyService).grantTenantEKeyForLease("lease-1");

        new LockAccessGrantServiceImpl(ekeyService, passcodeService).grantLockAccessForLease("lease-1");

        verify(passcodeService).grantTenantPeriodPasscodeForLease("lease-1");
    }
}
